package com.centurylink.mdw.studio.prefs

import com.centurylink.mdw.discovery.GitDiscoverer
import com.centurylink.mdw.discovery.GitHubDiscoverer
import com.centurylink.mdw.discovery.GitLabDiscoverer
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.studio.Secrets
import com.intellij.ide.util.PropertiesComponent
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files

class MdwSettings {

    var mdwHome: String
        get() = PropertiesComponent.getInstance().getValue(MDW_HOME, System.getenv("MDW_HOME") ?: "")
        set(value) {
            PropertiesComponent.getInstance().setValue(MDW_HOME, value)
            System.setProperty("mdw.home", value)
        }

    fun getOrMakeMdwHome(): File {
        var mdwHome: String? = System.getenv("MDW_HOME")
        if (mdwHome == null) {
            mdwHome = System.getProperty("mdw.home")
        }
        if (mdwHome == null) {
            // create under temp loc
            mdwHome = Files.createTempDirectory("mdw.studio").toString()
            System.setProperty("mdw.home", mdwHome)
        }
        return File(mdwHome)
    }

    var isSuppressServerPolling: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(SUPPRESS_SERVER_POLLING, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(SUPPRESS_SERVER_POLLING, value)
        }

    var isHideCanvasGridLines: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(HIDE_CANVAS_GRIDLINES, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(HIDE_CANVAS_GRIDLINES, value)
        }

    var isCanvasSnapToGrid: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(CANVAS_SNAP_TO_GRID, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(CANVAS_SNAP_TO_GRID, value)
        }

    var canvasZoom: Int
        get() {
            return PropertiesComponent.getInstance().getInt(CANVAS_ZOOM, 100)
        }
        set(value) {
            PropertiesComponent.getInstance().setValue(CANVAS_ZOOM, value.toString())
        }

    var isSyncDynamicJavaClassName: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(SYNC_DYNAMIC_JAVA_CLASS_NAME, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(SYNC_DYNAMIC_JAVA_CLASS_NAME, value)
        }

    var isCreateAndAssociateTaskTemplate: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(CREATE_AND_ASSOCIATE_TASK_TEMPLATE, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(CREATE_AND_ASSOCIATE_TASK_TEMPLATE, value)
        }

    var isSaveProcessAsJson: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(SAVE_PROCESS_AS_JSON, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(SAVE_PROCESS_AS_JSON, value)
        }

    var isAssetVercheckAutofix: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(ASSET_VERCHECK_AUTOFIX, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(ASSET_VERCHECK_AUTOFIX, value)
        }

    var isImportUnmetDependencies: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(IMPORT_UNMET_DEPENDENCIES, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(IMPORT_UNMET_DEPENDENCIES, value)
        }

    var isSuppressPackageIncrement: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(SUPPRESS_PACKAGE_INCREMENT, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(SUPPRESS_PACKAGE_INCREMENT, value)
        }

    var discoveryRepoUrls: List<String>
        get() {
            val str = PropertiesComponent.getInstance().getValue(DISCOVERY_REPO_URLS, Data.GIT_REPO_URL)
            val list = mutableListOf<String>()
            if (!str.isBlank()) {
                for (repoUrl in str.split(",")) {
                    list.add(repoUrl.trim())
                }
            }
            return list
        }
        set(value) {
            val discoveryUrls = value.toMutableList()
            // mdw discovery comes first; mdw-ctl-internal comes second
            val mdwCtlDiscovererIdx = discoveryUrls.indexOfFirst { URL(it).path.endsWith("mdw-ctl-internal.git") }
            if (mdwCtlDiscovererIdx >= 0) {
                discoveryUrls.add(0, discoveryUrls.removeAt(mdwCtlDiscovererIdx))
            }
            val mdwDiscovererIdx = discoveryUrls.indexOf(Data.GIT_REPO_URL)
            if (mdwDiscovererIdx >= 0) {
                discoveryUrls.add(0, discoveryUrls.removeAt(mdwDiscovererIdx))
            }
            PropertiesComponent.getInstance().setValue(DISCOVERY_REPO_URLS, if (discoveryUrls.isEmpty()) {
                ""
            } else {
                discoveryUrls.joinToString(",")
            })
        }

    var discoveryMaxBranchesTags: Int
        get() {
            return PropertiesComponent.getInstance().getInt(DISCOVERY_MAX_BRANCHES_TAGS, 10)
        }
        set(value) {
            PropertiesComponent.getInstance().setValue(DISCOVERY_MAX_BRANCHES_TAGS, value.toString())
        }

    val discoverers: List<GitDiscoverer>
        get() {
            val discoverers = mutableListOf<GitDiscoverer>()
            discoveryRepoUrls.map { url ->
                val repoUrl = URL(url)
                val discoverer = if (repoUrl.host == "github.com") {
                    GitHubDiscoverer(repoUrl)
                } else {
                    GitLabDiscoverer(repoUrl)
                }
                discoverers.add(discoverer)
                Secrets.DISCOVERY_TOKENS[repoUrl.host]?.let { token ->
                    discoverer.setToken(token)
                }
                discoverer
            }

            return discoverers
        }

    companion object {
        val instance = MdwSettings()
        const val ID = "com.centurylink.mdw.studio"
        private const val MDW_HOME = "$ID.mdwHome"

        private const val SUPPRESS_SERVER_POLLING = "$ID.isSuppressServerPolling"
        // canvas
        private const val CANVAS_ZOOM = "$ID.canvasZoom"
        private const val HIDE_CANVAS_GRIDLINES = "$ID.isHideCanvasGridLines"
        private const val CANVAS_SNAP_TO_GRID = "$ID.isCanvasSnapToGrid"

        // editing
        private const val SYNC_DYNAMIC_JAVA_CLASS_NAME = "$ID.isSyncDynamicJavaClassName"
        private const val CREATE_AND_ASSOCIATE_TASK_TEMPLATE = "$ID.createAndAssociateTaskTemplate"
        private const val SAVE_PROCESS_AS_JSON = "$ID.saveProcessAsJson"

        // assets
        private const val ASSET_VERCHECK_AUTOFIX = "$ID.isAssetVercheckAutofix"
        const val SUPPRESS_PROMPT_VERCHECK_AUTOFIX = "$ID.isSuppressPromptVercheckAutofix"
        private const val SUPPRESS_PACKAGE_INCREMENT = "$ID.isSuppressPackageIncrement"
        private const val IMPORT_UNMET_DEPENDENCIES = "$ID.isImportUnmetDependencies"
        const val SUPPRESS_PROMPT_IMPORT_DEPENDENCIES = "$ID.isSuppressPromptImportDependencies"

        // discovery
        private const val DISCOVERY_REPO_URLS = "$ID.discoveryRepoUrls"
        private const val DISCOVERY_MAX_BRANCHES_TAGS = "$ID.discoveryMaxBranchesTags"
    }
}