package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

abstract class AssetAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation
        val applicable =  Locator(event).asset?.let { true } ?: false
        presentation.isVisible = applicable
        presentation.isEnabled = applicable
    }
}