package com.centurylink.mdw.draw.model

import com.centurylink.mdw.activity.types.GeneralActivity
import com.centurylink.mdw.model.Project
import com.centurylink.mdw.model.workflow.ActivityImplementor
import org.json.JSONArray

class Data {

    companion object {
        const val HELP_LINK_URL = "http://centurylinkcloud.github.io/mdw/docs"
        const val BASE_PKG = "com.centurylink.mdw.base"

        fun getWorkgroups(project: Project): List<String> {
            return project.readDataList("data.workgroups") ?: DEFAULT_WORKGROUPS
        }
        // excludes Site Admin on purpose
        private val DEFAULT_WORKGROUPS = listOf(
                "MDW Support",
                "Developers"
        )

        private val DEFAULT_BINARY_ASSET_EXTS = listOf(
                "png",
                "jpg",
                "gif",
                "svg",
                "xlsx",
                "docx",
                "class",
                "jar",
                "zip",
                "eot",
                "ttf",
                "woff",
                "woff2"
        )

        fun getBinaryAssetExts(project: Project): List<String> {
            return project.readDataList("data.binary.asset.exts") ?: DEFAULT_BINARY_ASSET_EXTS
        }

        fun getTaskCategories(project: Project): Map<String,String> {
            return project.readDataMap("data.task.categories") ?: DEFAULT_TASK_CATEGORIES
        }
        private val DEFAULT_TASK_CATEGORIES = sortedMapOf(
                "Ordering" to "ORD",
                "General Inquiry" to "GEN",
                "Billing" to "BIL",
                "Complaint" to "COM",
                "Portal Support" to "POR",
                "Training" to "TRN",
                "Repair" to "RPR",
                "Inventory" to "INV",
                "Test" to "TST",
                "Vacation Planning" to "VAC",
                "Customer Contact" to "CNT"
        )

        fun getDocumentTypes(project: Project): Map<String,String> {
            return project.readDataMap("data.document.types") ?: DEFAULT_DOCUMENT_TYPES
        }
        private val DEFAULT_DOCUMENT_TYPES = sortedMapOf(
                "org.w3c.dom.Document" to "xml",
                "org.apache.xmlbeans.XmlObject" to "xml",
                "java.lang.Object" to "java",
                "org.json.JSONObject" to "json",
                "groovy.util.Node" to "xml",
                "com.centurylink.mdw.xml.XmlBeanWrapper" to "xml",
                "com.centurylink.mdw.model.StringDocument" to "text",
                "com.centurylink.mdw.model.HTMLDocument" to "html",
                "javax.xml.bind.JAXBElement" to "xml",
                "org.apache.camel.component.cxf.CxfPayload" to "xml",
                "com.centurylink.mdw.common.service.Jsonable" to "json",
                "com.centurylink.mdw.model.Jsonable" to "json",
                "org.yaml.snakeyaml.Yaml" to "yaml",
                "java.lang.Exception" to "json",
                "java.util.List<Integer>" to "json",
                "java.util.List<Long>" to "json",
                "java.util.List<String>" to "json",
                "java.util.Map<String,String>" to "json"
        )

        private val TASK_OUTCOMES = listOf(
                "Open",
                "Assigned",
                "Completed",
                "Cancelled",
                "In Progress",
                "Alert",
                "Jeopardy",
                "Forward"
        )

        val DEFAULT_TASK_NOTICES = JSONArray()

        init {
            for (taskOutcome in TASK_OUTCOMES) {
                val notices = JSONArray()
                notices.put(taskOutcome)
                for (i in 1..4) { notices.put("") }
                DEFAULT_TASK_NOTICES.put(notices)
            }
        }
    }

    class Implementors {
        companion object {
            const val START_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity"
            const val STOP_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity"
            const val PAUSE_IMPL = "com.centurylink.mdw.base.PauseActivity"
            const val DYNAMIC_JAVA = "com.centurylink.mdw.workflow.activity.java.DynamicJavaActivity"
            val PSEUDO_IMPLS = listOf(
                    ActivityImplementor("Exception Handler", "subflow", "" +
                            "Exception Handler Subflow", "${Data.BASE_PKG}/subflow.png", null),
                    ActivityImplementor("Cancellation Handler", "subflow",
                            "Cancellation Handler Subflow", "${Data.BASE_PKG}/subflow.png", null),
                    ActivityImplementor("Delay Handler", "subflow",
                            "Delay Handler Subflow", "${Data.BASE_PKG}/subflow.png", null),
                    ActivityImplementor("TextNote", "note",
                            "Text Note", "$BASE_PKG/note.png", null),
                    ActivityImplementor("com.centurylink.mdw.workflow.activity.DefaultActivityImpl",
                            GeneralActivity::class.qualifiedName,"Dummy Activity", "shape:activity", "{}")
            )
        }
    }
}