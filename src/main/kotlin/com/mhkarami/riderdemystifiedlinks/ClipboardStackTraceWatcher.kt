/**
 * Watches the system clipboard and automatically triggers Rider's built-in
 * "Analyze Stack Trace or Thread Dump" action (internal action id: "Unscramble")
 * whenever the clipboard content looks like an exception stack trace.
 *
 * NOTE: Rider does not document or ship this behavior itself (unlike IntelliJ
 * IDEA's "Automatically detect..." checkbox) -- the Stacktrace window must
 * normally be opened manually via Tools | Analyze Stack Trace or Thread Dump.
 * This class replicates that manual trigger automatically.
 *
 * Heuristic: intentionally loose (same spirit as IntelliJ IDEA's own
 * detector) -- looks for the word "Exception" together with at least one
 * line that starts with "at ". This deliberately does NOT require the
 * strict CLR grammar, so Ben.Demystifier-formatted traces still match, since
 * the actual frame parsing inside the Stacktrace window already understands
 * that format natively (confirmed by manual testing).
 *
 * Trade-off: any copied text that merely contains "Exception" and an
 * indented "at " line (e.g. copied from Stack Overflow, chat, documentation)
 * will also trigger the popup. Tighten STACK_TRACE_HEURISTIC below if this
 * turns out to be too aggressive in daily use.
 */

package com.mhkarami.riderdemystifiedlinks

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

private val STACK_TRACE_HEURISTIC = Regex("""Exception[\s\S]*?\n\s*at\s+\S""")
private const val ANALYZE_STACK_TRACE_ACTION_ID = "Unscramble"

class ClipboardStackTraceWatcher(private val project: Project) : CopyPasteManager.ContentsChangedListener {

    private var lastHandledText: String? = null

    override fun contentChanged(oldTransferable: Transferable?, newTransferable: Transferable?) {
        val text = readClipboardText(newTransferable) ?: return

        if (text == lastHandledText || !looksLikeStackTrace(text)) return
        lastHandledText = text

        ApplicationManager.getApplication().invokeLater {
            triggerAnalyzeStackTrace()
        }
    }

    private fun readClipboardText(transferable: Transferable?): String? =
        try {
            transferable?.getTransferData(DataFlavor.stringFlavor) as? String
        } catch (ignored: Exception) {
            null
        }

    private fun looksLikeStackTrace(text: String): Boolean =
        text.contains("Exception") && STACK_TRACE_HEURISTIC.containsMatchIn(text)

    private fun triggerAnalyzeStackTrace() {
        val action = ActionManager.getInstance().getAction(ANALYZE_STACK_TRACE_ACTION_ID) ?: return
        val dataContext = DataContext { dataId ->
            if (PlatformDataKeys.PROJECT.`is`(dataId)) project else null
        }
        val event = AnActionEvent.createFromAnAction(action, null, "ClipboardStackTraceWatcher", dataContext)
        action.actionPerformed(event)
    }
}

/**
 * Registers the clipboard listener once per opened project.
 * Extension point: com.intellij.postStartupActivity
 */
class ClipboardStackTraceWatcherStarter : ProjectActivity {
    override suspend fun execute(project: Project) {
        val watcher = ClipboardStackTraceWatcher(project)
        CopyPasteManager.getInstance().addContentChangedListener(watcher, project)
    }
}
