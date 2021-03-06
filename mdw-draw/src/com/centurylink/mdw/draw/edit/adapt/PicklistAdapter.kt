package com.centurylink.mdw.draw.edit.adapt

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.model.asset.Pagelet.Widget
import org.json.JSONArray

@Suppress("unused")
class PicklistAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {

    override fun didInit(widget: Widget) {
        widget.value?.let {
            widget.value = toArray(it.toString())
        }
    }

    override fun willUpdate(widget: Widget) {
        widget.value?.let {
            widget.value = (it as JSONArray).toString()
        }
    }

    /**
     * handles compatibility for old process def attributes
     */
    private fun toArray(value: String): JSONArray {
        return if (value.startsWith('[')) {
            JSONArray(value)
        }
        else {
            safeSplit(value, "#")
        }
    }
}