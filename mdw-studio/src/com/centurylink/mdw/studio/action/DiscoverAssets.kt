package com.centurylink.mdw.studio.action

import com.centurylink.mdw.discovery.GitDiscoverer
import com.centurylink.mdw.discovery.GitHubDiscoverer
import com.centurylink.mdw.discovery.GitLabDiscoverer
import com.centurylink.mdw.model.PackageMeta
import com.centurylink.mdw.studio.MdwConfig
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.Secrets
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.JBSplitter
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.SwingWorker
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.net.URL
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class DiscoverAssets : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            val discoveryDialog = DiscoveryDialog(projectSetup)
            if (discoveryDialog.showAndGet()) {
                println("DISCOVERY")
            }
        }
    }

    override fun update(event: AnActionEvent) {
        var applicable = false
        Locator(event).getProjectSetup()?.let { projectSetup ->
            val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            applicable = file == projectSetup.project.baseDir || file == projectSetup.assetDir
        }
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}

class DiscoveryDialog(projectSetup: ProjectSetup) : DialogWrapper(projectSetup.project, true) {

    private val centerPanel = JPanel(BorderLayout())
    private val okButton: JButton?
        get() = getButton(okAction)

    private val discoverers = mutableListOf<GitDiscoverer>()

    private val rootNode = DefaultMutableTreeNode("Discovery Repositories")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree: Tree
    private val packageList = CheckBoxList<String>()
    private val buttonPanel = object: JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
        override fun getPreferredSize(): Dimension {
            return Dimension(super.getPreferredSize().width, BTM_HT)
        }
    }
    private val allButton = JButton("Select All")
    private val noneButton = JButton("Select None")

    init {
        init()
        title = "MDW Asset Discovery"
        okButton?.isEnabled = false

        MdwSettings.instance.discoveryRepoUrls.map { url ->
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

        discoverers.forEach { discoverer ->
            val discovererNode = DefaultMutableTreeNode(discoverer)
            discovererNode.add(RefsNode(discoverer, RefsNode.RefType.Tags))
            discovererNode.add(RefsNode(discoverer, RefsNode.RefType.Branches))
            rootNode.add(discovererNode)
        }

        val treePanel = JPanel(BorderLayout(5, 5))
        treePanel.add(JLabel("Select a Repository Tag or Branch:"), BorderLayout.NORTH)

        tree = Tree()
        tree.model = treeModel
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.alignmentX = Component.LEFT_ALIGNMENT
        tree.cellRenderer = object: NodeRenderer() {
            override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
                val selectable = leaf
                super.customizeCellRenderer(tree, value, selectable && selected, expanded, leaf, row, selectable && hasFocus)
            }
        }
        tree.addTreeWillExpandListener(object: TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {
                val path = event.path
                if (path.lastPathComponent is RefsNode) {
                    val refsNode = path.lastPathComponent as RefsNode
                        refsNode.add(DefaultMutableTreeNode("Loading...", false))
                        object: SwingWorker() {
                            override fun construct(): Any? {
                                return try {
                                    refsNode.refs
                                    null
                                } catch(ex: IOException) {
                                    LOG.error(ex)
                                    ex
                                }
                            }
                            override fun finished() {
                                refsNode.removeAllChildren()
                                val ex = get()
                                if (ex == null) {
                                    refsNode.refs.forEach { ref ->
                                        refsNode.add(DefaultMutableTreeNode(ref))
                                    }
                                }
                                else {
                                    JOptionPane.showMessageDialog(centerPanel, (ex as Exception).message,
                                            "Git Retrieval Error", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                                }
                                treeModel.nodeStructureChanged(refsNode)
                            }
                        }.start()
                }
            }
            override fun treeWillCollapse(event: TreeExpansionEvent) {
            }
        })
        tree.addTreeSelectionListener {
            tree.lastSelectedPathComponent?.let {
                if (it is DefaultMutableTreeNode) {
                    if (it.isLeaf) {
                        packageList.setEmptyText("Loading...")
                        packageList.setItems(listOf<String>(), null)
                        packageList.repaint()
                        val refsNode = it.parent as RefsNode
                        val discoverer = refsNode.discoverer
                        discoverer.ref = it.userObject as String
                        object: SwingWorker() {
                            override fun construct(): Any? {
                                return try {
                                    discoverer.packageInfo
                                } catch(ex: IOException) {
                                    LOG.error(ex)
                                    ex
                                }
                            }
                            override fun finished() {
                                packageList.setEmptyText("Nothing to show")
                                val res = get()
                                if (res is Exception) {
                                    JOptionPane.showMessageDialog(centerPanel, res.message,
                                            "Git Retrieval Error", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                                }
                                else {
                                    val packageInfo = res as Map<*,*>
                                    for ((pkg, meta) in packageInfo) {
                                        packageList.addItem(pkg.toString(), meta.toString(), false)
                                    }
                                }
                                packageList.repaint()
                                allButton.isVisible = packageList.model.size > 0
                                noneButton.isVisible = packageList.model.size > 0
                                buttonPanel.revalidate()
                                buttonPanel.repaint()
                            }
                        }.start()
                    }
                }
            }
        }

        treePanel.add(JScrollPane(tree), BorderLayout.CENTER)

        // link to prefs
        val linkText = "Change discovery repositories..."
        val linkHtml = if (UIUtil.isUnderDarcula()) {
            "<html><a href='.' style='color:white;'>$linkText</a></html>"
        }
        else {
            "<html><a href='.'>$linkText</a></html>"
        }
        val link = JLabel(linkHtml)
        link.alignmentX = Component.LEFT_ALIGNMENT
        link.cursor = Cursor(Cursor.HAND_CURSOR)
        link.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(projectSetup.project, MdwConfig::class.java)
                this@DiscoveryDialog.close(0)
            }
        })
        val linkPanel = object: JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
            override fun getPreferredSize(): Dimension {
                return Dimension(super.getPreferredSize().width, BTM_HT)
            }
        }
        linkPanel.add(link)
        treePanel.add(linkPanel, BorderLayout.SOUTH)

        val packagePanel = JPanel(BorderLayout(5, 5))
        packagePanel.add(JLabel("Packages to Import:"), BorderLayout.NORTH)

        packagePanel.add(JScrollPane(packageList), BorderLayout.CENTER)

        // to line up with tree panel
        allButton.addActionListener {
            for (i in 0..packageList.model.size-1) {
                packageList.setItemSelected(packageList.getItemAt(i), true)
            }
            packageList.repaint()
        }
        allButton.isVisible = false
        buttonPanel.add(allButton)
        noneButton.addActionListener {
            for (i in 0..packageList.model.size-1) {
                packageList.setItemSelected(packageList.getItemAt(i), false)
            }
            packageList.repaint()
        }
        noneButton.isVisible = false
        buttonPanel.add(noneButton)
        packagePanel.add(buttonPanel, BorderLayout.SOUTH)

        val splitter = JBSplitter(false, 0.5f)
        splitter.firstComponent = treePanel
        splitter.secondComponent = packagePanel
        centerPanel.add(splitter, BorderLayout.CENTER)
    }

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tree
    }

    companion object {
        val LOG = Logger.getInstance(DiscoverAssets::class.java)
        const val BTM_HT = 60
    }
}

class RefsNode(val discoverer: GitDiscoverer, val refType: RefType) : DefaultMutableTreeNode(refType) {

    enum class RefType {
        Tags,
        Branches
    }

    override fun isLeaf() = false

    val refs: List<String> by lazy {
        when (refType) {
            RefType.Branches -> {
                discoverer.getBranches(MdwSettings.instance.discoveryMaxBranchesTags)
            }
            RefType.Tags -> {
                discoverer.getTags(MdwSettings.instance.discoveryMaxBranchesTags)
            }
        }
    }
}
