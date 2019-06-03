package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Vercheck
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.centurylink.mdw.studio.vcs.CredentialsDialog
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import java.io.IOException
import java.lang.reflect.InvocationTargetException


class VercheckAssets : AssetToolsAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            var autofix = MdwSettings.instance.isAssetVercheckAutofix
            val assetVercheck = AssetVercheck(projectSetup, autofix)
            val errorCount = assetVercheck.performCheck()
            if (autofix) {
                VfsUtil.markDirtyAndRefresh(true, true, true, projectSetup.assetDir)
            }
            else if (errorCount > 0 && assetVercheck.vercheck.exception == null) {
                val setting = MdwSettings.SUPPRESS_PROMPT_VERCHECK_AUTOFIX;
                if (!PropertiesComponent.getInstance().getBoolean(setting, false)) {
                    val res = MessageDialogBuilder
                            .yesNoCancel("Asset Version Conflict(s)",
                                    "Vercheck failed with $errorCount errors.  Fix?")
                            .doNotAsk(object : DialogWrapper.DoNotAskOption.Adapter() {
                                override fun rememberChoice(isSelected: Boolean, res: Int) {
                                    if (isSelected) {
                                        autofix = res == Messages.YES
                                        MdwSettings.instance.isAssetVercheckAutofix = autofix
                                        PropertiesComponent.getInstance().setValue(setting, isSelected)
                                    }
                                }
                            })
                            .show()
                    autofix = res == Messages.YES
                }

                if (autofix) {
                    AssetVercheck(projectSetup, true).performCheck()
                    VfsUtil.markDirtyAndRefresh(true, true, true, projectSetup.assetDir)
                }
            }
        }
    }
}

class AssetVercheck(private val projectSetup: ProjectSetup, private val isFix: Boolean = false) {

    val vercheck = Vercheck()

    fun performCheck(): Int {
        vercheck.isWarn = true
        vercheck.isFix = isFix
        val gitUser = projectSetup.settings.gitUser
        if (!gitUser.isBlank()) {
            vercheck.gitUser = gitUser
            vercheck.setGitPassword(projectSetup.settings.gitPassword)
        }

        val title = if (isFix) {
            "Fixing asset versions..."
        }
        else {
            "Executing Vercheck..."
        }
        projectSetup.console.run(vercheck, title)
        vercheck.exception?.let { ex ->
            if (ex is IOException && ex.cause is InvocationTargetException) {
                (ex.cause as InvocationTargetException).cause?.let { targetEx ->
                    targetEx.message?.let { message ->
                        if (message.endsWith("Authentication is required but no CredentialsProvider has been registered")) {
                            val dialog = CredentialsDialog(projectSetup)
                            if (dialog.showAndGet()) {
                                val retryVercheck = Vercheck()
                                retryVercheck.isWarn = true
                                retryVercheck.isFix = isFix
                                retryVercheck.gitUser = dialog.user
                                retryVercheck.setGitPassword(dialog.password)
                                projectSetup.console.run(retryVercheck)
                                return retryVercheck.errorCount
                            }
                        }
                    }
                }
            }
        }
        return vercheck.errorCount
    }

    companion object {
        val LOG = Logger.getInstance(AssetVercheck::class.java)
    }
}