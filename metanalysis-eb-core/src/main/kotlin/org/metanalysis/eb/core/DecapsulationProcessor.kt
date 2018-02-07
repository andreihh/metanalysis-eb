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
import org.metanalysis.core.model.Variable
import org.metanalysis.core.model.sourcePath
import java.util.ServiceLoader

abstract class DecapsulationProcessor {
    protected abstract fun canProcess(sourcePath: String): Boolean

    protected abstract fun updateDecapsulations(
        project: Project,
        decapsulations: DecapsulationSet,
        edit: EditVariable,
        revisionId: String
    ): DecapsulationSet

    companion object {
        private val processors =
            ServiceLoader.load(DecapsulationProcessor::class.java)

        fun updateDecapsulations(
            project: Project,
            decapsulations: DecapsulationSet,
            edit: EditVariable,
            revisionId: String
        ): DecapsulationSet? {
            val sourcePath = project.get<Variable>(edit.id).sourcePath
            val processor = processors.find { it.canProcess(sourcePath) }
                ?: return null
            return processor.updateDecapsulations(
                project = project,
                decapsulations = decapsulations,
                edit = edit,
                revisionId = revisionId
            )
        }
    }
}
