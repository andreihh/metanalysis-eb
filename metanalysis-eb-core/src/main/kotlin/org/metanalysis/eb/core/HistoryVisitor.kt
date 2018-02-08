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
import org.metanalysis.core.model.Project
import org.metanalysis.core.model.ProjectEdit
import org.metanalysis.core.model.RemoveNode
import org.metanalysis.core.model.SourceNode
import org.metanalysis.core.model.Variable
import org.metanalysis.core.model.parentId
import org.metanalysis.core.model.walkSourceTree
import org.metanalysis.core.repository.Repository
import org.metanalysis.core.repository.Transaction

class HistoryVisitor private constructor() {
    private val decapsulationsByField = hashMapOf<String, List<Decapsulation>>()
    private val project = Project.empty()

    private fun getField(nodeId: String): String? =
        DecapsulationAnalyzer.getField(project, nodeId)

    private fun getVisibility(nodeId: String): Int? =
        DecapsulationAnalyzer.getVisibility(project, nodeId)

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

    private fun visit(edit: AddNode, revisionId: String) {
        for (node in edit.node.walkSourceTree()) {
            val fieldId = DecapsulationAnalyzer.getField(project, edit.id)
                ?: continue
            val old = getVisibility(fieldId)
            val new = getVisibility(node.id)
            if (old != null && new != null && old < new) {
                addDecapsulation(
                    fieldId = fieldId,
                    nodeId = node.id,
                    revisionId = revisionId,
                    message = "Added accessor with more relaxed visibility!"
                )
            }
        }
        project.apply(edit)
    }

    private fun visit(edit: EditVariable, revisionId: String) {
        val old = getVisibility(edit.id)
        project.apply(edit)
        val new = getVisibility(edit.id)
        if (old != null && new != null && old < new) {
            addDecapsulation(
                fieldId = edit.id,
                nodeId = edit.id,
                revisionId = revisionId,
                message = "Relaxed field visibility!"
            )
        }
    }

    private fun visit(edit: EditFunction, revisionId: String) {
        val fieldId = getField(edit.id) ?: return
        val old = getVisibility(edit.id)
        project.apply(edit)
        val new = getVisibility(edit.id)
        if (old != null && new != null && old < new) {
            addDecapsulation(
                fieldId = fieldId,
                nodeId = edit.id,
                revisionId = revisionId,
                message = "Relaxed accessor visibility!"
            )
        }
    }

    private fun visit(edit: RemoveNode) {
        val node = project.get<SourceNode>(edit.id)
        val removedIds = node.walkSourceTree().map(SourceNode::id).toSet()
        for (id in removedIds) {
            decapsulationsByField -= id
            val field = DecapsulationAnalyzer.getField(project, id) ?: continue
            val decapsulations = decapsulationsByField[field] ?: continue
            decapsulationsByField[field] =
                decapsulations.filter { it.sourceNodeId != id }
        }
        project.apply(edit)
    }

    private fun visit(edit: ProjectEdit, revisionId: String) {
        when (edit) {
            is AddNode -> visit(edit, revisionId)
            is EditFunction -> visit(edit, revisionId)
            is EditVariable -> visit(edit, revisionId)
            is RemoveNode -> visit(edit)
            else -> project.apply(edit)
        }
    }

    private fun visit(transaction: Transaction) {
        for (edit in transaction.edits) {
            visit(edit, transaction.id)
        }
    }

    private fun aggregate(): Map<String, List<Decapsulation>> =
        decapsulationsByField.entries.groupBy { (id, _) ->
            project.get<Variable>(id).parentId
        }.mapValues { (_, decapsulations) ->
            decapsulations.flatMap { (_, v) -> v }
        }

    companion object {
        fun visit(repository: Repository): Map<String, List<Decapsulation>> {
            val visitor = HistoryVisitor()
            for (transaction in repository.getHistory()) {
                visitor.visit(transaction)
            }
            return visitor.aggregate()
        }
    }
}
