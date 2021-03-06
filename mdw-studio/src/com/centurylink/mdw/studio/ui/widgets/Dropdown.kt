package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.init
import com.centurylink.mdw.draw.edit.isReadonly
import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

@Suppress("unused")
class Dropdown(widget: Widget) : SwingWidget(widget) {

    val combo = object : ComboBox<String>(widget.options?.toTypedArray() ?: emptyArray<String>()) {
        override fun getPreferredSize(): Dimension {
            val size = super.getPreferredSize()
            return Dimension(size.width, size.height - 2)
        }
    }
    private var comboPanel = JPanel()
    private val widgetNames = mutableListOf<String>()

    init {
        isOpaque = false

        combo.isEditable = !widget.isReadonly
        combo.isEnabled = !widget.isReadonly

        combo.editor.item = widget.value

        widget.widgets?.let {
            widget.widgets.forEach {
                widgetNames.add(it.name)
                widgetNames.add("${it.name}_assetVersion")
            }
        }

        val doc = (combo.editor.editorComponent as JTextComponent).document
        doc.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                widget.value = doc.getText(0, e.document.length)
                widgetNames.forEach { widgetName ->
                    workflowObj.removeAttribute(widgetName)
                }
                applyUpdate()
                if (!widget.value.equals("")) {
                    comboPanel.removeAll()
                    remove(comboPanel)
                    adjustWidget()
                    if (parent != null) {
                        parent.revalidate()
                        parent.repaint()
                    }
                }
            }
        })
        add(combo)
        adjustWidget()
    }
    private fun adjustWidget() {
        comboPanel = JPanel()
        comboPanel.background = UIManager.getColor("EditorPane.background")
        widget.widgets?.let {
            widget.widgets.forEach { widg ->
                widget.value?. let {
                    if (it.equals(widg.getAttribute("parentValue"))) {
                        widg.init("", workflowObj)
                        val paramLabel = Label(widg)
                        comboPanel.add(paramLabel)
                        val paramWidget = create(widg)
                        paramWidget.addUpdateListener { obj ->
                            notifyUpdateListeners(obj)
                        }
                        comboPanel.add(paramWidget)
                        widgetNames.forEach { widgetName ->
                            if (widgetName != widg.name && widgetName != "${widg.name}_assetVersion") {
                                workflowObj.removeAttribute(widgetName)
                            }
                        }
                    }
                }
            }
            add(comboPanel)
        }
    }
}