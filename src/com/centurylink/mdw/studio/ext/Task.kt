package com.centurylink.mdw.studio.ext

import com.centurylink.mdw.model.asset.Asset
import com.centurylink.mdw.model.attribute.Attribute
import com.centurylink.mdw.model.task.TaskTemplate
import com.centurylink.mdw.model.task.TaskType
import com.centurylink.mdw.studio.proj.ProjectSetup
import org.json.JSONObject

fun TaskTemplate.update(obj: JSONObject) {
    println("UPDATE: " + obj.toString(2))
    var attrsJson : JSONObject? = null
    if (obj.has("attributes")) {
        attrsJson = obj.getJSONObject("attributes")
    }
    if (attrsJson?.has("logicalId")!!) {
        logicalId = attrsJson.getString("logicalId")
        attrsJson.remove("logicalId")
    }
    else
        logicalId = obj.getString("logicalId")
    if (attrsJson.has("name")) {
        taskName = attrsJson.getString("name")
        attrsJson.remove("name")
    }
    else
        taskName = obj.getString("name")
    if ( attrsJson.has("category")) {
        taskCategory = ProjectSetup.categories.get(attrsJson.getString("category"))
        attrsJson.remove("category")
    }
    else
        taskCategory = ProjectSetup.categories.get(obj.getString("category"))
    if (attrsJson.has("description")) {
        comment = attrsJson.getString("description")
        attrsJson.remove("description")
    }
    else
        comment = obj.getString("description")
    if (attrsJson.has("version")) {
        version = Asset.parseVersion(attrsJson.getString("version"))
        attrsJson.remove("version")
    }
    else
        version = Asset.parseVersion(obj.getString("version"))
    language = "TASK"
    taskTypeId = TaskType.TASK_TYPE_TEMPLATE
    attributes = Attribute.getAttributes(attrsJson)
    val vars = getAttribute("Variables")
    if (vars != null) {
        setVariablesFromString(vars, null )
    }
    val groups = getAttribute("Groups")
    if (groups != null) {
        setUserGroupsFromString(groups)
    }
    removeAttribute("TaskSLA_UNITS");
    removeAttribute("ALERT_INTERVAL_UNITS");
}