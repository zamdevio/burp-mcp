package net.portswigger.mcp.config.components

import net.portswigger.mcp.config.Anchor
import net.portswigger.mcp.config.Design
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.tools.ToolCatalog
import net.portswigger.mcp.tools.ToolEntry
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.Box.createVerticalStrut
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Searchable MCP tool catalog for the suite MCP tab.
 */
class ToolsPanel(
    private val config: McpConfig,
    private val professionalEdition: Boolean,
) : JPanel(BorderLayout()) {

    private val searchField = JTextField()
    private val listModel = DefaultListModel<ToolEntry>()
    private val toolList = JList(listModel)
    private val detailArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Design.Typography.bodyMedium
    }
    private val badgesLabel = JLabel(" ")
    private val connectionLabel = JLabel()

    init {
        background = Design.Colors.surface
        border = BorderFactory.createEmptyBorder(
            Design.Spacing.MD, Design.Spacing.MD, Design.Spacing.MD, Design.Spacing.MD
        )
        ToolCatalog.ensureBuiltIn()
        build()
        refreshList()
    }

    private fun build() {
        val north = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = LEFT_ALIGNMENT
            add(Design.createSectionLabel("Tools & capabilities"))
            add(createVerticalStrut(Design.Spacing.SM))
            add(JLabel("Same names and descriptions as MCP list_tools.").apply {
                font = Design.Typography.bodyMedium
                foreground = Design.Colors.onSurfaceVariant
                alignmentX = LEFT_ALIGNMENT
            })
            add(createVerticalStrut(Design.Spacing.SM))
            connectionLabel.alignmentX = LEFT_ALIGNMENT
            connectionLabel.font = Design.Typography.bodyMedium
            add(connectionLabel)
            add(createVerticalStrut(Design.Spacing.SM))
            add(
                Anchor(
                    text = "Client setup guides",
                    url = "https://github.com/zamdevio/burp-mcp/tree/main/docs/guides",
                ).apply { alignmentX = LEFT_ALIGNMENT }
            )
            add(createVerticalStrut(Design.Spacing.MD))
            add(JLabel("Search").apply {
                font = Design.Typography.labelMedium
                alignmentX = LEFT_ALIGNMENT
            })
            add(createVerticalStrut(Design.Spacing.SM))
            searchField.maximumSize = Dimension(Int.MAX_VALUE, searchField.preferredSize.height)
            searchField.alignmentX = LEFT_ALIGNMENT
            add(searchField)
        }

        toolList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        toolList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): java.awt.Component {
                val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val entry = value as? ToolEntry
                if (entry != null) {
                    c.text = "${entry.group} · ${entry.name}"
                }
                return c
            }
        }
        toolList.addListSelectionListener {
            if (!it.valueIsAdjusting) showDetail(toolList.selectedValue)
        }

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = refreshList()
            override fun removeUpdate(e: DocumentEvent?) = refreshList()
            override fun changedUpdate(e: DocumentEvent?) = refreshList()
        })

        val copyButton = Design.createFilledButton("Copy tool name").apply {
            addActionListener {
                val name = toolList.selectedValue?.name ?: return@addActionListener
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(name), null)
            }
        }

        val detailSouth = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(badgesLabel, BorderLayout.CENTER)
            add(copyButton, BorderLayout.EAST)
        }

        val detailPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(Design.Spacing.SM, 0, 0, 0)
            add(JScrollPane(detailArea).apply {
                preferredSize = Dimension(100, 120)
            }, BorderLayout.CENTER)
            add(detailSouth, BorderLayout.SOUTH)
        }

        val center = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            JScrollPane(toolList),
            detailPanel,
        ).apply {
            resizeWeight = 0.55
            border = null
        }

        add(north, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)
        updateConnectionHint()
    }

    fun updateConnectionHint() {
        val host = config.host.ifBlank { "127.0.0.1" }
        val port = config.port
        connectionLabel.text = "SSE: http://$host:$port  (clients often use http://127.0.0.1:$port)"
    }

    fun refreshFromCatalog() {
        ToolCatalog.ensureBuiltIn()
        refreshList()
        updateConnectionHint()
    }

    private fun refreshList() {
        val q = searchField.text.trim().lowercase()
        val selected = toolList.selectedValue?.name
        listModel.clear()
        val items = ToolCatalog.visibleForEdition(professionalEdition)
            .filter {
                q.isEmpty() ||
                    it.name.contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.group.lowercase().contains(q)
            }
            .sortedWith(compareBy({ it.group }, { it.name }))
        items.forEach { listModel.addElement(it) }
        if (selected != null) {
            val idx = items.indexOfFirst { it.name == selected }
            if (idx >= 0) toolList.selectedIndex = idx
        } else if (listModel.size > 0) {
            toolList.selectedIndex = 0
        }
    }

    private fun showDetail(entry: ToolEntry?) {
        if (entry == null) {
            detailArea.text = ""
            badgesLabel.text = " "
            return
        }
        detailArea.text = entry.description
        val badges = entry.badgeLabels()
        badgesLabel.text = if (badges.isEmpty()) " " else badges.joinToString("  ·  ")
        badgesLabel.foreground = Design.Colors.onSurfaceVariant
    }
}
