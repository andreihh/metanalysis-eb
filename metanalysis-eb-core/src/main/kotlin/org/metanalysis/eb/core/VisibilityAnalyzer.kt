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

import org.metanalysis.core.model.Project
import org.metanalysis.core.model.SourceNode

import java.util.ServiceLoader

interface VisibilityAnalyzer {
    companion object {
        private val analyzers =
                ServiceLoader.load(VisibilityAnalyzer::class.java)

        private val String.fileName: String
            get() = substringAfterLast(SourceNode.PATH_SEPARATOR)

        fun getAnalyzer(path: String): VisibilityAnalyzer? =
                analyzers.firstOrNull { path.fileName.matches(it.pattern) }
    }

    val pattern: Regex

    fun getVisibility(project: Project, id: String): Int
}
