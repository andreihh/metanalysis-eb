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

package org.metanalysis.visibility

import org.metanalysis.core.delta.FunctionTransaction
import org.metanalysis.core.delta.NodeSetEdit
import org.metanalysis.core.delta.SourceFileTransaction
import org.metanalysis.core.delta.Transaction
import org.metanalysis.core.delta.Transaction.Companion.apply
import org.metanalysis.core.delta.TypeTransaction
import org.metanalysis.core.delta.VariableTransaction
import org.metanalysis.core.model.Node
import org.metanalysis.core.model.Node.Function
import org.metanalysis.core.model.Node.Type
import org.metanalysis.core.model.Node.Variable
import org.metanalysis.core.model.SourceFile

class HistoryEntryVisitor(private val analyzer: VisibilityAnalyzer) {
    fun visit(sourceFile: SourceFile, transaction: SourceFileTransaction): Int {
        var cost = 0
        cost += transaction.nodeEdits.sumBy { edit ->
            if (edit is NodeSetEdit.Change<*>) {
                val node = sourceFile.find(edit.nodeType, edit.identifier)!!
                visit(node, edit.transaction)
            } else 0
        }
        return cost
    }

    fun visit(node: Node, transaction: Transaction<out Node>): Int =
            when(node) {
                is Type -> visit(node, transaction as TypeTransaction)
                is Variable -> visit(node, transaction as VariableTransaction)
                is Function -> visit(node, transaction as FunctionTransaction)
            }

    fun visit(type: Type, transaction: TypeTransaction): Int {
        val visibility = analyzer.getVisibility(type.modifiers)
        val newType = type.apply(transaction)
        val newVisibility = analyzer.getVisibility(newType.modifiers)
        var sum = newVisibility - visibility
        sum += transaction.memberEdits.sumBy { edit ->
            if (edit is NodeSetEdit.Change<*>) {
                val member = type.find(edit.nodeType, edit.identifier)!!
                visit(member, edit.transaction)
            } else 0
        }
        return sum
    }

    fun visit(variable: Variable, transaction: VariableTransaction): Int {
        val visibility = analyzer.getVisibility(variable.modifiers)
        val newVariable = variable.apply(transaction)
        val newVisibility = analyzer.getVisibility(newVariable.modifiers)
        return newVisibility - visibility
    }

    fun visit(function: Function, transaction: FunctionTransaction): Int {
        val visibility = analyzer.getVisibility(function.modifiers)
        val newFunction = function.apply(transaction)
        val newVisibility = analyzer.getVisibility(newFunction.modifiers)
        return newVisibility - visibility
    }
}
