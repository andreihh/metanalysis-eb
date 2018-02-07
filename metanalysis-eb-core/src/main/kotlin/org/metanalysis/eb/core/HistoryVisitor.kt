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

import org.metanalysis.core.model.EditVariable
import org.metanalysis.core.model.Project
import org.metanalysis.core.model.ProjectEdit
import org.metanalysis.core.model.RemoveNode
import org.metanalysis.core.model.SourceNode
import org.metanalysis.core.model.walkSourceTree
import org.metanalysis.core.repository.Repository
import org.metanalysis.core.repository.Transaction

class HistoryVisitor private constructor() {
    private val decapsulations = hashMapOf<String, DecapsulationSet>()
    private val project = Project.empty()

    private fun visit(
        edit: EditVariable,
        revisionId: String
    ): DecapsulationSet? = DecapsulationProcessor.updateDecapsulations(
        project = project,
        decapsulations = decapsulations[edit.id] ?: DecapsulationSet(),
        edit = edit,
        revisionId = revisionId
    )

    private fun visit(edit: RemoveNode) {
        val removedNode = project.get<SourceNode>(edit.id)
        val removedIds = removedNode.walkSourceTree().map(SourceNode::id)
        for (id in removedIds) {
            decapsulations -= id
        }
    }

    private fun visit(edit: ProjectEdit, revisionId: String) {
        when (edit) {
            is RemoveNode -> visit(edit)
            is EditVariable -> {
                val newDecapsulations = visit(edit, revisionId)
                if (newDecapsulations != null) {
                    decapsulations[edit.id] = newDecapsulations
                } else {
                    decapsulations -= edit.id
                }
            }
        }
    }

    private fun visit(transaction: Transaction) {
        for (edit in transaction.edits) {
            visit(edit, transaction.id)
            project.apply(edit)
        }
    }

    companion object {
        fun visit(repository: Repository): Map<String, DecapsulationSet> {
            val visitor = HistoryVisitor()
            for (transaction in repository.getHistory()) {
                visitor.visit(transaction)
            }
            return visitor.decapsulations
        }
    }
}
