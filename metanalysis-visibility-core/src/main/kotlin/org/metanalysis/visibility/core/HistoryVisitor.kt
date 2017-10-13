/*
 * Copyright 2017 Andrei Heidelbacher <andrei.heidelbacher@gmail.com>
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

package org.metanalysis.visibility.core

import org.metanalysis.core.logging.LoggerFactory
import org.metanalysis.core.model.Project
import org.metanalysis.core.model.ProjectEdit
import org.metanalysis.core.model.ProjectEdit.EditFunction
import org.metanalysis.core.model.ProjectEdit.EditType
import org.metanalysis.core.model.ProjectEdit.EditVariable
import org.metanalysis.core.model.SourceNode
import org.metanalysis.core.model.SourceNode.SourceUnit
import org.metanalysis.core.model.Transaction
import org.metanalysis.core.model.children
import org.metanalysis.core.model.parentUnitId
import org.metanalysis.core.model.walkSourceTree
import org.metanalysis.visibility.core.VisibilityAnalyzer.Companion.getAnalyzer

class HistoryVisitor private constructor() {
    companion object {
        private val logger = LoggerFactory.getLogger<HistoryVisitor>()

        fun visit(history: Iterable<Transaction>): Map<String, Int> {
            val visitor = HistoryVisitor()
            visitor.visit(history)
            return visitor.values
        }
    }

    private var project = Project()
    private val values = hashMapOf<String, Int>()

    private fun visitAndApply(edit: ProjectEdit) {
        val id = edit.id
        val analyzer = getAnalyzer(id.parentUnitId) ?: return
        val visibility = analyzer.getVisibility(project, id)
        project.apply(edit)
        val newVisibility = analyzer.getVisibility(project, id)
        val delta = newVisibility - visibility
        values[id] = (values[id] ?: 0) + delta
    }

    private fun visit(edit: ProjectEdit) {
        when (edit) {
            is EditType -> visitAndApply(edit)
            is EditFunction -> visitAndApply(edit)
            is EditVariable -> visitAndApply(edit)
            else -> project.apply(edit)
        }
    }

    private fun visit(history: Iterable<Transaction>) {
        logger.info("Analyzing transactions...\n")
        for ((index, transaction) in history.withIndex()) {
            for (edit in transaction.edits) {
                visit(edit)
            }
            logger.info("Analyzed $index transactions...\r")
        }
        logger.info("\n")
        logger.info("Done!\n")
        logger.info("Computing results...\n")
        val allIds = project.units
                .flatMap(SourceUnit::walkSourceTree)
                .map(SourceNode::id)
        for (id in allIds.sortedByDescending(String::length)) {
            val node = project.find(id) ?: continue
            val childrenSum = node.children.sumBy { values[it.id] ?: 0 }
            values[id] = (values[id] ?: 0) + childrenSum
        }
        logger.info("Done!\n")
    }
}
