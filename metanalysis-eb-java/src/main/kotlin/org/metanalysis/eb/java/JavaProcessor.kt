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

package org.metanalysis.eb.java

import org.metanalysis.core.model.Function
import org.metanalysis.core.model.Project
import org.metanalysis.core.model.SourceNode
import org.metanalysis.core.model.Type
import org.metanalysis.core.model.Variable
import org.metanalysis.core.model.parentId
import org.metanalysis.eb.core.DecapsulationProcessor

class JavaProcessor : DecapsulationProcessor() {
    override fun canProcess(sourcePath: String): Boolean =
        sourcePath.endsWith(".java")

    private fun getFieldName(signature: String): String? =
        listOf("is", "get", "set")
            .filter { signature.startsWith(it) }
            .map { signature.removePrefix(it).substringBefore('(') }
            .firstOrNull { it.firstOrNull()?.isUpperCase() == true }
            ?.let { "${it[0].toLowerCase()}${it.substring(1)}" }

    override fun getField(project: Project, nodeId: String): String? {
        val node = project[nodeId] ?: return null
        return when (node) {
            is Variable -> node.id
            is Function -> {
                val fieldName = getFieldName(node.signature) ?: return null
                val field = project[fieldName] as? Variable? ?: return null
                field.id
            }
            else -> null
        }
    }

    private fun getVisibility(modifiers: Set<String>): Int = when {
        PRIVATE_MODIFIER in modifiers -> PRIVATE_LEVEL
        PROTECTED_MODIFIER in modifiers -> PROTECTED_LEVEL
        PUBLIC_MODIFIER in modifiers -> PUBLIC_LEVEL
        else -> PACKAGE_LEVEL
    }

    private val SourceNode?.isInterface: Boolean
        get() = this is Type && INTERFACE_MODIFIER in modifiers

    override fun getVisibility(project: Project, nodeId: String): Int {
        val node = project[nodeId]
        return when (node) {
            is Type -> getVisibility(node.modifiers)
            is Function ->
                if (project[node.parentId].isInterface) PUBLIC_LEVEL
                else getVisibility(node.modifiers)
            is Variable -> getVisibility(node.modifiers)
            else -> throw AssertionError("'$nodeId' has no visibility!")
        }
    }

    companion object {
        private const val PRIVATE_MODIFIER: String = "private"
        private const val PROTECTED_MODIFIER: String = "protected"
        private const val PUBLIC_MODIFIER: String = "public"
        private const val INTERFACE_MODIFIER = "interface"

        private const val PRIVATE_LEVEL: Int = 1
        private const val PACKAGE_LEVEL: Int = 2
        private const val PROTECTED_LEVEL: Int = 3
        private const val PUBLIC_LEVEL: Int = 4
    }
}
