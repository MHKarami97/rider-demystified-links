package com.mhkarami.riderdemystifiedlinks

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFrame
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.FlavorListener
import java.awt.datatransfer.Transferable
import java.util.concurrent.atomic.AtomicBoolean

private const val ANALYZE_STACK_TRACE_ACTION_ID = "Unscramble"
private const val CURRENT_STACK_TRACE_DATA_KEY_NAME = "current_stack_trace_key"
private const val MAX_CLIPBOARD_TEXT_LENGTH = 1_000_000

private val exceptionHeaderPattern = Regex(
    """(?im)^\s*(?:[\w.$+<>`]+(?:Exception|Error)(?::|\s)|[\w.$+<>`]+:\s)"""
)

private val stackFramePattern = Regex("""(?im)^\s*at\s+\S.+""")

class ClipboardStackTraceWatcher(private val project: Project) : CopyPasteManager.ContentChangedListener {

    private var lastHandledFingerprint: Int? = null
    private val actionPending = AtomicBoolean(false)

    override fun contentChanged(oldTransferable: Transferable?, newTransferable: Transferable?) {
        inspectAndOpenIfStackTrace(readText(newTransferable))
    }

    fun inspectSystemClipboard() {
        inspectAndOpenIfStackTrace(readTextFromSystemClipboard())
    }

    private fun inspectAndOpenIfStackTrace(text: String?) {
        if (text.isNullOrBlank() || !looksLikeStackTrace(text)) return

        val fingerprint = text.hashCode()
        if (lastHandledFingerprint == fingerprint || !actionPending.compareAndSet(false, true)) return

        lastHandledFingerprint = fingerprint

        ApplicationManager.getApplication().invokeLater {
            try {
                if (!project.isDisposed) {
                    openStackTraceExplorer(text)
                }
            } finally {
                actionPending.set(false)
            }
        }
    }

    private fun looksLikeStackTrace(text: String): Boolean =
        text.length <= MAX_CLIPBOARD_TEXT_LENGTH &&
            exceptionHeaderPattern.containsMatchIn(text) &&
            stackFramePattern.containsMatchIn(text)

    private fun readTextFromSystemClipboard(): String? =
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

    private fun openStackTraceExplorer(stackTrace: String) {
        val action = ActionManager.getInstance().getAction(ANALYZE_STACK_TRACE_ACTION_ID) ?: return
        val stackTraceKey = DataKey.create<String>(CURRENT_STACK_TRACE_DATA_KEY_NAME)
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(stackTraceKey, stackTrace)
            .build()

        ActionUtil.invokeAction(action, dataContext, ActionPlaces.UNKNOWN, null, null)
    }
}

class ClipboardStackTraceWatcherStarter : ProjectActivity {

    override suspend fun execute(project: Project) {
        val watcher = ClipboardStackTraceWatcher(project)

        CopyPasteManager.getInstance().addContentChangedListener(watcher, project)

        ApplicationManager.getApplication().messageBus
            .connect(project)
            .subscribe(
                ApplicationActivationListener.TOPIC,
                object : ApplicationActivationListener {
                    override fun applicationActivated(ideFrame: IdeFrame) {
                        if (!project.isDisposed) {
                            ApplicationManager.getApplication().invokeLater {
                                watcher.inspectSystemClipboard()
                            }
                        }
                    }
                }
            )

        registerSystemClipboardListener(watcher, project)

        ApplicationManager.getApplication().invokeLater {
            watcher.inspectSystemClipboard()
        }
    }

    private fun registerSystemClipboardListener(watcher: ClipboardStackTraceWatcher, project: Project) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val listener = FlavorListener {
                ApplicationManager.getApplication().invokeLater {
                    watcher.inspectSystemClipboard()
                }
            }

            clipboard.addFlavorListener(listener)
            Disposer.register(project) {
                clipboard.removeFlavorListener(listener)
            }
        } catch (_: Exception) {
            // The activation listener still covers Alt+Tab into Rider if clipboard notifications are unavailable.
        }
    }
}