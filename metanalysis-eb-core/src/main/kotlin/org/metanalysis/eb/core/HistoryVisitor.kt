/*
 * Copyright 2018 Andrei Heidelbacher <andrei.heidelbacher@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.metanalysis.eb.core

import org.metanalysis.core.model.AddNode
import org.metanalysis.core.model.EditFunction
import org.metanalysis.core.model.EditVariable
import org.metanalysis.core.model.Function
import org.metanalysis.core.model.Project
import org.metanalysis.core.model.RemoveNode
import org.metanalysis.core.model.SourceNode
import org.metanalysis.core.model.Variable
import org.metanalysis.core.model.parentId
import org.metanalysis.core.model.walkSourceTree
import org.metanalysis.core.repository.Repository
import org.metanalysis.core.repository.Transaction

class HistoryVisitor private constructor() {
    private val project = Project.empty()
    private val decapsulationsByField = hashMapOf<String, List<Decapsulation>>()

    private fun getField(nodeId: String): String? =
        DecapsulationAnalyzer.getField(project, nodeId)

    private fun getVisibility(nodeId: String): Int? =
        DecapsulationAnalyzer.getVisibility(project, nodeId)

    private fun isConstant(nodeId: String): Boolean =
        DecapsulationAnalyzer.isConstant(project, nodeId)

    private fun addDecapsulation(
        fieldId: String,
        nodeId: String,
        revisionId: String,
        message: String
    ) {
        val new = Decapsulation(fieldId, nodeId, revisionId, message)
        val current = decapsulationsByField[fieldId].orEmpty()
        decapsulationsByField[fieldId] = current + new
    }

    private fun visit(edit: AddNode): Set<String> {
        val editedIds = hashSetOf<String>()
        for (node in edit.node.walkSourceTree()) {
            if (node is Variable) {
                decapsulationsByField[node.id] = emptyList()
                editedIds += node.id
            } else if (node is Function) {
                editedIds += node.id
            }
        }
        return editedIds
    }

    private fun visit(edit: RemoveNode): Set<String> {
        val removedIds = hashSetOf<String>()
        val removedNode = project.get<SourceNode>(edit.id)
        for (node in removedNode.walkSourceTree()) {
            decapsulationsByField -= node.id
            removedIds += node.id
        }
        return removedIds
    }

    private fun visit(transaction: Transaction): Set<String> {
        val editedIds = hashSetOf<String>()
        for (edit in transaction.edits) {
            when (edit) {
                is AddNode -> editedIds += visit(edit)
                is RemoveNode -> editedIds -= visit(edit)
                is EditFunction -> editedIds += edit.id
                is EditVariable -> editedIds += edit.id
            }
        }
        return editedIds
    }

    private fun getVisibility(ids: Set<String>): Map<String, Int> {
        val visibility = hashMapOf<String, Int>()
        for (id in ids) {
            visibility[id] = getVisibility(id) ?: continue
            val fieldId = getField(id) ?: continue
            visibility[fieldId] = getVisibility(fieldId) ?: continue
        }
        return visibility
    }

    private fun analyze(transaction: Transaction) {
        val editedIds = visit(transaction)
        val oldVisibility = getVisibility(editedIds)
        project.apply(transaction.edits)
        val newVisibility = getVisibility(editedIds)

        fun analyze(variable: Variable) {
            val old = oldVisibility[variable.id] ?: return
            val new = newVisibility[variable.id] ?: return
            if (new > old) {
                addDecapsulation(
                    fieldId = variable.id,
                    nodeId = variable.id,
                    revisionId = transaction.id,
                    message = "Relaxed field visibility!"
                )
            }
        }

        fun analyze(function: Function) {
            val fieldId = getField(function.id) ?: return
            val fieldOld = oldVisibility[fieldId] ?: return
            val old = oldVisibility[function.id]
            val new = newVisibility[function.id] ?: return
            if (old == null && new > fieldOld) {
                addDecapsulation(
                    fieldId = fieldId,
                    nodeId = function.id,
                    revisionId = transaction.id,
                    message = "Added accessor with more relaxed visibility!"
                )
            } else if (old != null && new > old) {
                addDecapsulation(
                    fieldId = fieldId,
                    nodeId = function.id,
                    revisionId = transaction.id,
                    message = "Relaxed accessor visibility!"
                )
            }
        }

        for (id in editedIds) {
            val node = project[id]
            when (node) {
                is Variable -> analyze(node)
                is Function -> analyze(node)
            }
        }
    }

    private fun aggregate(
        ignoreConstants: Boolean
    ): Map<String, List<Decapsulation>> =
        decapsulationsByField
            .filterKeys { !ignoreConstants || !isConstant(it) }
            .entries
            .groupBy { (id, _) -> project.get<Variable>(id).parentId }
            .mapValues { (_, decapsulations) ->
                decapsulations.flatMap { (_, v) -> v }
            }

    companion object {
        fun visit(
            repository: Repository,
            ignoreConstants: Boolean = false
        ): Map<String, List<Decapsulation>> {
            val visitor = HistoryVisitor()
            for (transaction in repository.getHistory()) {
                visitor.analyze(transaction)
            }
            return visitor.aggregate(ignoreConstants)
        }
    }
}
