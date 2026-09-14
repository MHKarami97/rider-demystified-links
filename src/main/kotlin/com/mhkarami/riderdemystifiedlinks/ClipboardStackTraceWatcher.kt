package com.mhkarami.riderdemystifiedlinks

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.IdeFrame
import com.intellij.util.messages.MessageBusConnection
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.util.concurrent.atomic.AtomicBoolean

private val STACK_TRACE_HEURISTIC = Regex(
    """(?im)^\s*(?:[\w.$+<>]+(?:Exception|Error)(?::|\s)|at\s+)"""
)

private val STACK_FRAME_HEURISTIC = Regex("""(?m)^\s*at\s+.+""")
private const val ANALYZE_STACK_TRACE_ACTION_ID = "Unscramble"

class ClipboardStackTraceWatcher(private val project: Project) : CopyPasteManager.ContentChangedListener {

    private var lastHandledFingerprint: Int? = null
    private val actionPending = AtomicBoolean(false)

    override fun contentChanged(oldTransferable: Transferable?, newTransferable: Transferable?) {
        inspectAndOpenIfStackTrace(readText(newTransferable))
    }

    fun inspectSystemClipboardOnIdeActivation() {
        inspectAndOpenIfStackTrace(readSystemClipboardText())
    }

    private fun inspectAndOpenIfStackTrace(text: String?) {
        if (text.isNullOrBlank() || !looksLikeStackTrace(text)) return

        val fingerprint = text.hashCode()
        if (lastHandledFingerprint == fingerprint || !actionPending.compareAndSet(false, true)) return

        lastHandledFingerprint = fingerprint

        ApplicationManager.getApplication().invokeLater {
            try {
                if (!project.isDisposed) {
                    triggerAnalyzeStackTrace()
                }
            } finally {
                actionPending.set(false)
            }
        }
    }

    private fun looksLikeStackTrace(text: String): Boolean =
        text.length <= 1_000_000 &&
            STACK_TRACE_HEURISTIC.containsMatchIn(text) &&
            STACK_FRAME_HEURISTIC.containsMatchIn(text)

    private fun readSystemClipboardText(): String? =
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            readText(clipboard.getContents(null))
        } catch (_: Exception) {
            null
        }

    private fun readText(transferable: Transferable?): String? =
        try {
            transferable
                ?.takeIf { it.isDataFlavorSupported(DataFlavor.stringFlavor) }
                ?.getTransferData(DataFlavor.stringFlavor) as? String
        } catch (_: Exception) {
            null
        }

    private fun triggerAnalyzeStackTrace() {
        val action = ActionManager.getInstance().getAction(ANALYZE_STACK_TRACE_ACTION_ID) ?: return
        val dataContext = SimpleDataContext.getProjectContext(project)

        ActionUtil.invokeAction(
            action,
            dataContext,
            ActionPlaces.UNKNOWN,
            null,
            null
        )
    }
}

class ClipboardStackTraceWatcherStarter : ProjectActivity {

    override suspend fun execute(project: Project) {
        val watcher = ClipboardStackTraceWatcher(project)

        CopyPasteManager.getInstance().addContentChangedListener(watcher, project)

        val connection: MessageBusConnection = project.messageBus.connect(project)
        connection.subscribe(
            ApplicationActivationListener.TOPIC,
            object : ApplicationActivationListener {
                override fun applicationActivated(ideFrame: IdeFrame) {
                    if (ideFrame.project != project || !isIdeWindowFocused()) return

                    ApplicationManager.getApplication().invokeLater {
                        watcher.inspectSystemClipboardOnIdeActivation()
                    }
                }
            }
        )

        ApplicationManager.getApplication().invokeLater {
            watcher.inspectSystemClipboardOnIdeActivation()
        }
    }

    private fun isIdeWindowFocused(): Boolean =
        KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow != null
}