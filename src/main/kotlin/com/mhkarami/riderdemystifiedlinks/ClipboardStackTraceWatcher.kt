/**
 * Watches the system clipboard and automatically triggers Rider's built-in
 * "Analyze Stack Trace or Thread Dump" action (action id: "Unscramble" --
 * confirmed via IntelliJ Platform source: ActionsBundle.properties and
 * IdeErrorsDialog.kt both reference ActionManager.getInstance().getAction("Unscramble"))
 * whenever the clipboard content looks like an exception stack trace.
 *
 * Rider does not ship this "auto-detect on clipboard copy" behavior itself
 * (that specific checkbox lives only in the Java-plugin's UnscrambleDialog,
 * per java/openapi/resources/messages/JavaBundle.properties:
 * "unscramble.detect.analyze.threaddump.from.clipboard.item" -- a module
 * Rider does not bundle). This class replicates the same idea at the
 * platform level, independent of the Java plugin.
 */

package com.mhkarami.riderdemystifiedlinks

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

private val STACK_TRACE_HEURISTIC = Regex("""Exception[\s\S]*?\n\s*at\s+\S""")
private const val ANALYZE_STACK_TRACE_ACTION_ID = "Unscramble"

class ClipboardStackTraceWatcher(private val project: Project) : CopyPasteManager.ContentChangedListener {

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

    /**
     * Uses the platform-recommended ActionUtil.invokeAction helper instead of
     * calling action.actionPerformed(...) directly. invokeAction runs the
     * action's update() check first (so its enabled/visible state is
     * evaluated properly) and then performs it on the EDT in a write-safe
     * context -- calling actionPerformed directly bypasses that and can
     * silently no-op for some actions.
     */
    private fun triggerAnalyzeStackTrace() {
        val action = ActionManager.getInstance().getAction(ANALYZE_STACK_TRACE_ACTION_ID) ?: return
        val dataContext = SimpleDataContext.getProjectContext(project)
        ActionUtil.invokeAction(action, dataContext, ActionPlaces.UNKNOWN, null, null)
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
