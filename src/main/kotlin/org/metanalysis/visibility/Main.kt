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

@file:JvmName("Main")

package org.metanalysis.visibility

import org.metanalysis.core.logging.LoggerFactory
import org.metanalysis.core.model.SourceFile
import org.metanalysis.core.delta.Transaction.Companion.apply
import org.metanalysis.core.project.PersistentProject
import java.io.IOException

val logger = LoggerFactory.getLogger("metanalysis-visibility")

fun main(args: Array<String>) {
    LoggerFactory.loadConfiguration("/logging.properties")
    logger.info("Hello, world from logger!")
    val project = PersistentProject.load()
            ?: error("Project not found!")
    val visitor = HistoryEntryVisitor(VisibilityAnalyzer())
    val stats = project.listFiles().mapNotNull { path ->
        try {
            val history = project.getFileHistory(path)
            path to history
        } catch (e: IOException) {
            null
        }
    }.associate { (path, history) ->
        var sourceFile = SourceFile()
        var cost = 0.0
        for ((_, _, _, transaction) in history) {
            cost += transaction?.let { visitor.visit(sourceFile, it) } ?: 0
            sourceFile = sourceFile.apply(transaction)
        }
        path to cost
    }
    stats.forEach { (path, cost) -> println("$path: $cost") }
    println(stats.maxBy { it.value })
}
