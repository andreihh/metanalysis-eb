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

import org.junit.Test

import org.metanalysis.core.model.Project
import org.metanalysis.test.core.model.project
import org.metanalysis.eb.java.JavaVisibilityAnalyzer.Companion.PACKAGE_LEVEL
import org.metanalysis.eb.java.JavaVisibilityAnalyzer.Companion.PRIVATE_LEVEL
import org.metanalysis.eb.java.JavaVisibilityAnalyzer.Companion.PRIVATE_MODIFIER
import org.metanalysis.eb.java.JavaVisibilityAnalyzer.Companion.PROTECTED_LEVEL
import org.metanalysis.eb.java.JavaVisibilityAnalyzer.Companion.PROTECTED_MODIFIER
import org.metanalysis.eb.java.JavaVisibilityAnalyzer.Companion.PUBLIC_LEVEL
import org.metanalysis.eb.java.JavaVisibilityAnalyzer.Companion.PUBLIC_MODIFIER

import kotlin.test.assertEquals

class JavaVisibilityAnalyzerTest {
    private val analyzer = JavaVisibilityAnalyzer()

    private fun getProjectWithType(
            className: String,
            visibilityModifier: String? = null
    ): Project = project {
        sourceUnit("$className.java") {
            type(className) {
                if (visibilityModifier != null) {
                    modifiers(visibilityModifier)
                }
            }
        }
    }

    @Test fun `test private visibility`() {
        val name = "Main"
        val project = getProjectWithType(name, PRIVATE_MODIFIER)
        val id = "$name.java:$name"
        val expectedLevel = PRIVATE_LEVEL
        val actualLevel = analyzer.getVisibility(project, id)
        assertEquals(expectedLevel, actualLevel)
    }

    @Test fun `test package visibility`() {
        val name = "Main"
        val project = getProjectWithType(name)
        val id = "$name.java:$name"
        val expectedLevel = PACKAGE_LEVEL
        val actualLevel = analyzer.getVisibility(project, id)
        assertEquals(expectedLevel, actualLevel)
    }

    @Test fun `test protected visibility`() {
        val name = "Main"
        val project = getProjectWithType(name, PROTECTED_MODIFIER)
        val id = "$name.java:$name"
        val expectedLevel = PROTECTED_LEVEL
        val actualLevel = analyzer.getVisibility(project, id)
        assertEquals(expectedLevel, actualLevel)
    }

    @Test fun `test public visibility`() {
        val name = "Main"
        val project = getProjectWithType(name, PUBLIC_MODIFIER)
        val id = "$name.java:$name"
        val expectedLevel = PUBLIC_LEVEL
        val actualLevel = analyzer.getVisibility(project, id)
        assertEquals(expectedLevel, actualLevel)
    }
}
