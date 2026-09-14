package com.mhkarami.riderdemystifiedlinks

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

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
