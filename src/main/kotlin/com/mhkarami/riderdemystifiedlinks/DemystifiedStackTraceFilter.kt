/**
 * Rider/IntelliJ Platform plugin filter that recognizes ".cs:line N" fragments
 * inside Ben.Demystifier-formatted stack traces (which the built-in Stack Trace
 * Explorer does not parse) and turns them into clickable navigation links.
 *
 * Module: IntelliJ Platform Plugin SDK (Kotlin/JVM), NOT the ReSharper .NET SDK.
 * Extension point: com.intellij.consoleFilterProvider
 * Docs: https://plugins.jetbrains.com/docs/intellij/plugin-extension-points.html
 */

package com.mhkarami.riderdemystifiedlinks

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Matches fragments like:
 *   in D:/ag/WCA9/_w/757/s/src/Fund/Source/src/Infrastructure/Extensions/FlurlResponseExtensions.cs:line 24
 * Only the file name + line number are used; the absolute CI path is ignored,
 * because it will not exist on the local machine.
 */
private val STACK_FRAME_FILE_LINE_REGEX =
    Regex("""in\s+[^\s:]*?([A-Za-z0-9_.\-]+\.(?:cs|vb|fs)):line\s+(\d+)""")

class DemystifiedStackTraceFilterProvider : ConsoleFilterProvider {
    override fun getDefaultFilters(project: Project): Array<Filter> =
        arrayOf(DemystifiedStackTraceFilter(project))
}

class DemystifiedStackTraceFilter(private val project: Project) : Filter {

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val match = STACK_FRAME_FILE_LINE_REGEX.find(line) ?: return null

        val fileName = match.groupValues[1]
        val lineNumber = match.groupValues[2].toIntOrNull() ?: return null

        val virtualFile = FilenameIndex
            .getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project))
            .firstOrNull() ?: return null

        val matchStartInLine = match.range.first
        val matchEndInLine = match.range.last + 1
        val lineStartOffset = entireLength - line.length

        val hyperlinkInfo = OpenFileHyperlinkInfo(
            project,
            virtualFile,
            (lineNumber - 1).coerceAtLeast(0),
            0
        )

        return Filter.Result(
            lineStartOffset + matchStartInLine,
            lineStartOffset + matchEndInLine,
            hyperlinkInfo
        )
    }
}
