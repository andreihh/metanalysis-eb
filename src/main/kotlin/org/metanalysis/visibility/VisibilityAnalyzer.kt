package org.metanalysis.visibility

class VisibilityAnalyzer {
    fun getVisibility(modifiers: Set<String>): Int = when {
        "private" in modifiers -> 1
        "protected" in modifiers -> 3
        "public" in modifiers -> 4
        else -> 2
    }
}
