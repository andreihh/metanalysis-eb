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

import org.metanalysis.core.model.Project
import org.metanalysis.core.model.SourceNode
import org.metanalysis.core.model.SourceNode.SourceEntity.Function
import org.metanalysis.core.model.SourceNode.SourceEntity.Type
import org.metanalysis.core.model.SourceNode.SourceEntity.Variable
import org.metanalysis.core.model.parentId
import org.metanalysis.eb.core.VisibilityAnalyzer

class JavaVisibilityAnalyzer : VisibilityAnalyzer {
    companion object {
        /** The `Java` programming language supported by this analyzer. */
        const val LANGUAGE: String = "Java"

        const val PRIVATE_MODIFIER: String = "private"
        const val PROTECTED_MODIFIER: String = "protected"
        const val PUBLIC_MODIFIER: String = "public"
        const val INTERFACE_MODIFIER = "interface"

        const val PRIVATE_LEVEL: Int = 1
        const val PACKAGE_LEVEL: Int = 2
        const val PROTECTED_LEVEL: Int = 3
        const val PUBLIC_LEVEL: Int = 4
    }

    override val pattern: Regex = Regex(".*\\.java")

    private fun getVisibility(modifiers: Set<String>): Int = when {
        PRIVATE_MODIFIER in modifiers -> PRIVATE_LEVEL
        PROTECTED_MODIFIER in modifiers -> PROTECTED_LEVEL
        PUBLIC_MODIFIER in modifiers -> PUBLIC_LEVEL
        else -> PACKAGE_LEVEL
    }

    private val SourceNode?.isInterface: Boolean
        get() = this is Type && INTERFACE_MODIFIER in modifiers

    override fun getVisibility(project: Project, id: String): Int {
        val node = project.find(id)
        return when (node) {
            is Type -> getVisibility(node.modifiers)
            is Function ->
                if (project.find(node.parentId).isInterface) PUBLIC_LEVEL
                else getVisibility(node.modifiers)
            is Variable -> getVisibility(node.modifiers)
            else -> throw IllegalArgumentException("'$id' has no visibility!")
        }
    }
}
