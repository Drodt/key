/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;

/// A linearized proof-tree view (see [#buildBranch]) with the KeY-Java hide filters: hide
/// intermediate steps, show only interactive steps, hide closed subtrees and hide subtrees with no
/// automatic goals. Selecting a tree node updates the [ProofContext]; the view re-syncs when the
/// context changes.
public final class ProofTreePanel extends JPanel implements ProofContext.Listener {

    private final ProofContext context;
    private final JTree tree = new JTree(new DefaultMutableTreeNode("(no proof)"));
    private final Map<Node, DefaultMutableTreeNode> nodeToTreeNode = new HashMap<>();
    private boolean syncing;
    private @org.jspecify.annotations.Nullable Consumer<@org.jspecify.annotations.Nullable Node> hoverListener;
    private @org.jspecify.annotations.Nullable BiConsumer<Node, Boolean> openNodeListener;
    private @org.jspecify.annotations.Nullable Consumer<Node> pruneListener;

    // Hide filters (mirroring de.uka.ilkd.key.gui.prooftree.ProofTreeViewFilter).
    private boolean hideIntermediate;
    private boolean onlyInteractive;
    private boolean hideClosed;
    private boolean hideNonAutomaticGoals;

    public ProofTreePanel(ProofContext context) {
        super(new BorderLayout());
        this.context = context;
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(true);
        tree.addTreeSelectionListener(e -> {
            if (syncing) {
                return;
            }
            if (tree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode dmtn) {
                Object userObject = dmtn.getUserObject();
                if (userObject instanceof NodeRef ref) {
                    context.setSelectedNode(ref.node());
                } else if (userObject instanceof BranchRef branch) {
                    context.setSelectedNode(branch.node());
                }
            }
        });
        tree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (hoverListener != null) {
                    hoverListener.accept(nodeAt(e.getX(), e.getY()));
                }
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowOpenMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowOpenMenu(e);
            }
        });
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        // Let the split divider shrink this pane to whatever the tree needs, not to the width the
        // toolbar's controls would otherwise demand.
        setMinimumSize(new Dimension(120, 0));
        context.addListener(this);
    }

    /// Registers a listener notified about the node under the cursor as the user hovers the tree
    /// (used to feed the node-info view); `null` when no node is under the cursor.
    public void setHoverListener(Consumer<@org.jspecify.annotations.Nullable Node> listener) {
        this.hoverListener = listener;
    }

    /// Registers the action invoked from the node's right-click menu to open it in the editor. The
    /// boolean is `true` when the node should open to the side (a new editor split) rather than as
    /// a
    /// tab in the current group.
    public void setOpenNodeListener(BiConsumer<Node, Boolean> listener) {
        this.openNodeListener = listener;
    }

    /// Registers the action invoked from an inner node's right-click menu to prune the proof there.
    public void setPruneListener(Consumer<Node> listener) {
        this.pruneListener = listener;
    }

    private void maybeShowOpenMenu(MouseEvent e) {
        if (!e.isPopupTrigger() || openNodeListener == null) {
            return;
        }
        Node node = nodeAt(e.getX(), e.getY());
        if (node == null) {
            return;
        }
        context.setSelectedNode(node);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem open = new JMenuItem("Open node " + node.getSerialNr() + " in a tab");
        open.addActionListener(ev -> openNodeListener.accept(node, false));
        JMenuItem openSide = new JMenuItem("Open node " + node.getSerialNr() + " to the side");
        openSide.addActionListener(ev -> openNodeListener.accept(node, true));
        menu.add(open);
        menu.add(openSide);
        // Pruning only makes sense at an inner node (it drops the subtree below it).
        if (pruneListener != null && node.childrenCount() > 0) {
            menu.addSeparator();
            JMenuItem prune = new JMenuItem("Prune proof at node " + node.getSerialNr());
            prune.addActionListener(ev -> pruneListener.accept(node));
            menu.add(prune);
        }
        menu.show(tree, e.getX(), e.getY());
    }

    private @org.jspecify.annotations.Nullable Node nodeAt(int x, int y) {
        TreePath path = tree.getPathForLocation(x, y);
        if (path != null
                && path.getLastPathComponent() instanceof DefaultMutableTreeNode dmtn) {
            Object userObject = dmtn.getUserObject();
            if (userObject instanceof NodeRef ref) {
                return ref.node();
            }
            if (userObject instanceof BranchRef branch) {
                return branch.node();
            }
        }
        return null;
    }

    /// A compact toolbar grounded by a hairline: an icon row (magnifier + "Filter ▾"), with the
    /// search field and the filter checkboxes each in their own full-width row below, shown only
    /// when their button is toggled. The filters are plain checkboxes (you can flip several at
    /// once)
    /// rather than a popup menu that closes after each click.
    private JPanel buildToolbar() {
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.hairline()),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));

        JComponent searchRow = buildSearchRow();
        JComponent filterRow = buildFilterRow();

        JButton searchToggle = toolbarButton("🔍", "Search the proof tree");
        searchToggle.addActionListener(e -> toggleRow(top, searchRow));
        JButton filterToggle = toolbarButton("Filter ▾", "Hide filters");
        filterToggle.addActionListener(e -> toggleRow(top, filterRow));

        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        iconRow.setOpaque(false);
        iconRow.add(searchToggle);
        iconRow.add(filterToggle);

        JPanel expandable = new JPanel();
        expandable.setOpaque(false);
        expandable.setLayout(new BoxLayout(expandable, BoxLayout.PAGE_AXIS));
        expandable.add(searchRow);
        expandable.add(filterRow);

        top.add(iconRow, BorderLayout.NORTH);
        top.add(expandable, BorderLayout.CENTER);
        return top;
    }

    private void toggleRow(JComponent toolbar, JComponent row) {
        boolean show = !row.isVisible();
        row.setVisible(show);
        toolbar.revalidate();
        toolbar.repaint();
        if (show) {
            row.requestFocusInWindow();
        }
    }

    private JComponent buildSearchRow() {
        JTextField field = new JTextField();
        field.setToolTipText("Find a node by label");
        JButton prev = new JButton("▲");
        JButton next = new JButton("▼");
        prev.setToolTipText("Previous match");
        next.setToolTipText("Next match");
        for (JButton b : new JButton[] { prev, next }) {
            b.setFocusable(false);
            b.setMargin(new java.awt.Insets(1, 6, 1, 6));
        }
        field.addActionListener(e -> search(field.getText(), true)); // Enter = next match
        next.addActionListener(e -> search(field.getText(), true));
        prev.addActionListener(e -> search(field.getText(), false));

        JPanel matchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        matchButtons.setOpaque(false);
        matchButtons.add(prev);
        matchButtons.add(next);

        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER); // full width of the panel
        row.add(matchButtons, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.setVisible(false);
        return row;
    }

    /// A stacked list of filter checkboxes (mirrors
    /// de.uka.ilkd.key.gui.prooftree.ProofTreeViewFilter)
    /// that fits a narrow pane and lets several be toggled without re-opening.
    private JComponent buildFilterRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.PAGE_AXIS));
        row.add(filterCheck("Hide intermediate", v -> hideIntermediate = v));
        row.add(filterCheck("Only interactive", v -> onlyInteractive = v));
        row.add(filterCheck("Hide closed", v -> hideClosed = v));
        row.add(filterCheck("Hide automatic goals", v -> hideNonAutomaticGoals = v));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.setVisible(false);
        return row;
    }

    private JCheckBox filterCheck(String label, Consumer<Boolean> setter) {
        JCheckBox box = new JCheckBox(label);
        box.setOpaque(false);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.addActionListener(e -> {
            setter.accept(box.isSelected());
            rebuild();
        });
        return box;
    }

    private JButton toolbarButton(String label, String tooltip) {
        JButton button = new JButton(label);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new java.awt.Insets(2, 6, 2, 6));
        return button;
    }

    /// Selects the next/previous tree node (in display order, wrapping around) whose label contains
    /// `query` (case-insensitive). Returns whether a match was found.
    boolean search(String query, boolean forward) {
        String q = query.trim().toLowerCase();
        int rows = tree.getRowCount();
        if (q.isEmpty() || rows == 0) {
            return false;
        }
        int start = Math.max(tree.getLeadSelectionRow(), 0);
        for (int k = 1; k <= rows; k++) {
            int row = forward ? (start + k) % rows : ((start - k) % rows + rows) % rows;
            TreePath path = tree.getPathForRow(row);
            if (path != null
                    && path.getLastPathComponent().toString().toLowerCase().contains(q)) {
                tree.setSelectionRow(row);
                tree.scrollRowToVisible(row);
                return true;
            }
        }
        Toolkit.getDefaultToolkit().beep();
        return false;
    }

    /// Package-private access to the underlying tree, for tests.
    JTree getTree() {
        return tree;
    }

    /// Sets the tree's font (family + size).
    public void applyFont(java.awt.Font font) {
        tree.setFont(font);
        tree.setRowHeight(0); // recompute the row height for the new font
    }

    /// Package-private filter control, for tests.
    void setFilters(boolean intermediate, boolean interactive, boolean closed, boolean autoGoals) {
        this.hideIntermediate = intermediate;
        this.onlyInteractive = interactive;
        this.hideClosed = closed;
        this.hideNonAutomaticGoals = autoGoals;
        rebuild();
    }

    @Override
    public void proofLoaded() {
        rebuild();
    }

    @Override
    public void proofChanged() {
        rebuild();
    }

    @Override
    public void selectedNodeChanged() {
        Node node = context.getSelectedNode();
        DefaultMutableTreeNode tn = node == null ? null : nodeToTreeNode.get(node);
        if (tn == null) {
            return;
        }
        syncing = true;
        try {
            TreePath path = new TreePath(tn.getPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        } finally {
            syncing = false;
        }
    }

    private void rebuild() {
        nodeToTreeNode.clear();
        Proof proof = context.getProof();
        DefaultMutableTreeNode root = proof == null ? new DefaultMutableTreeNode("(no proof)")
                : buildBranch(proof.root(), "Proof Tree");
        tree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        selectedNodeChanged();
    }

    /// Builds one branch of the linearized tree: the maximal linear chain of proof nodes starting
    /// at `start` is added as flat leaves, and each child of the chain's final (splitting) node
    /// starts a new sub-branch. Nodes hidden by the local filters are dropped from the chain, and
    /// sub-branches hidden by the global filters are skipped entirely.
    private DefaultMutableTreeNode buildBranch(Node start, String label) {
        DefaultMutableTreeNode branch = new DefaultMutableTreeNode(new BranchRef(start, label));

        List<Node> chain = new ArrayList<>();
        Node n = start;
        while (true) {
            chain.add(n);
            if (n.childrenCount() == 1) {
                n = n.child(0);
            } else {
                break;
            }
        }
        Node endpoint = n;
        for (Node step : chain) {
            if (showChainNode(step, endpoint)) {
                DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(new NodeRef(step));
                leaf.setAllowsChildren(false);
                nodeToTreeNode.putIfAbsent(step, leaf);
                branch.add(leaf);
            } else {
                // keep the selection resolvable: a hidden step maps to its enclosing branch
                nodeToTreeNode.putIfAbsent(step, branch);
            }
        }

        for (int i = 0; i < endpoint.childrenCount(); i++) {
            Node child = endpoint.child(i);
            if (hiddenByGlobalFilters(child)) {
                continue;
            }
            String childLabel = child.getNodeInfo().getBranchLabel();
            branch.add(buildBranch(child, childLabel != null ? childLabel : "Case " + (i + 1)));
        }
        return branch;
    }

    /// Whether a node of a linear chain is shown (the chain endpoint is always shown).
    private boolean showChainNode(Node step, Node endpoint) {
        if (step == endpoint) {
            return true;
        }
        if (onlyInteractive) {
            return step.getNodeInfo().getInteractiveRuleApplication();
        }
        return !hideIntermediate;
    }

    /// Whether the subtree rooted at `node` is hidden by an active global filter.
    private boolean hiddenByGlobalFilters(Node node) {
        if (hideClosed && node.isClosed()) {
            return true;
        }
        return hideNonAutomaticGoals && !hasAutomaticGoal(node);
    }

    private static boolean hasAutomaticGoal(Node node) {
        Proof proof = node.proof();
        var goals = proof.getSubtreeGoals(node);
        if (goals.isEmpty()) {
            return true; // a closed subtree is still shown (matches KeY)
        }
        for (Goal goal : goals) {
            if (goal.isAutomatic()) {
                return true;
            }
        }
        return false;
    }

    /// Wraps a single proof [Node] (a leaf of the tree); the label is its (single-line) name.
    private record NodeRef(Node node) {
        @Override
        public String toString() {
            String name = node.name().replace('\n', ' ').trim();
            return node.getSerialNr() + ": " + name;
        }
    }

    /// Wraps a proof branch (its first node and a label); shown as an inner tree node. A closed
    /// branch is marked.
    private record BranchRef(Node node, String label) {
        @Override
        public String toString() {
            String shown = node.getNodeInfo().getBranchLabel();
            String base = shown != null ? shown : label;
            return node.isClosed() ? base + "  ✓" : base;
        }
    }
}
