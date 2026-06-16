/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;

import org.jspecify.annotations.Nullable;

/// The central editor: a tabbed area of sequent views. One non-closable main tab follows the
/// selected node (its title says whether that node is an open goal or an inner node); further nodes
/// open as closable, pinned tabs (from the proof tree's right-click menu). A node can open *to the
/// side*, which splits the editor into two tab groups, and any pinned view can be moved between the
/// group and the split (or closed) from its tab menu.
public final class EditorArea extends JPanel implements ProofContext.Listener {

    private final ProofContext context;
    private final Runnable runProverAction;
    private final List<SequentView> views = new ArrayList<>();
    private final SequentView mainView;
    private final JLabel mainTabLabel = new JLabel("Sequent");

    private final JTabbedPane leftTabs = new JTabbedPane();
    private @Nullable JTabbedPane rightTabs;
    private @Nullable JSplitPane split;
    private JTabbedPane activeTabs;
    private Font contentFont = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    public EditorArea(ProofContext context, Runnable runProverAction) {
        super(new BorderLayout());
        this.context = context;
        this.runProverAction = runProverAction;
        // A hairline on the editor's left edge so its tab strip does not meld into the proof-tree
        // column across the thin split divider.
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.hairline()));

        // Mark the live (selection-following) tab with the accent colour + bold + a dot, so it is
        // obvious which pane tracks the proof-tree selection and which are pinned to a fixed node.
        mainTabLabel.setForeground(Theme.accent());
        mainTabLabel.setFont(mainTabLabel.getFont().deriveFont(Font.BOLD));
        mainTabLabel.setToolTipText("This pane follows the selected proof node");

        mainView = newView();
        leftTabs.addTab("", mainView);
        leftTabs.setTabComponentAt(0, tabComponent(mainTabLabel, null));
        wireGroup(leftTabs);
        activeTabs = leftTabs;
        add(leftTabs, BorderLayout.CENTER);

        context.addListener(this);
        updateMainTab();
    }

    @Override
    public void proofLoaded() {
        updateMainTab();
    }

    @Override
    public void proofChanged() {
        updateMainTab();
    }

    @Override
    public void selectedNodeChanged() {
        updateMainTab();
    }

    /// Labels the main tab by what it currently shows: an open goal or an inner node. A leading dot
    /// marks it as the live, selection-following pane.
    private void updateMainTab() {
        Node node = context.getSelectedNode();
        mainTabLabel.setText("● " + (node == null ? "Sequent" : nodeLabel(node)));
    }

    /// A node's tab/title label, using the same vocabulary everywhere (goal vs. inner node).
    private String nodeLabel(Node node) {
        return (isGoal(node) ? "Open goal " : "Inner node ") + node.getSerialNr();
    }

    private boolean isGoal(Node node) {
        Proof proof = context.getProof();
        if (proof != null) {
            for (Goal goal : proof.openGoals()) {
                if (goal.getNode() == node) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Opens `node` in a pinned editor tab — in a new split to the side when `toSide`, otherwise as
    /// a tab in the active group.
    public void openNode(Node node, boolean toSide) {
        JTabbedPane target;
        if (toSide) {
            ensureSplit();
            target = rightTabs;
        } else {
            target = activeTabs;
        }
        SequentView view = newView();
        view.pinTo(node);
        addClosableTab(target, view);
        target.setSelectedComponent(view);
        activeTabs = target;
    }

    /// Sets the font (family + size) of every editor view.
    public void applyFont(Font font) {
        this.contentFont = font;
        for (SequentView view : views) {
            view.applyFont(font);
        }
    }

    private SequentView newView() {
        SequentView view = new SequentView(context);
        view.setRunProverAction(runProverAction);
        view.applyFont(contentFont);
        views.add(view);
        return view;
    }

    // ── tab groups ──────────────────────────────────────────────────────────

    private void wireGroup(JTabbedPane tabs) {
        // Left-align tabs (Aqua centres a lone tab) and scroll, rather than wrap, when there are
        // many.
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        // A slimmer tab strip (FlatLaf's default is fairly tall).
        tabs.putClientProperty("JTabbedPane.tabHeight", 26);
        tabs.addChangeListener(e -> activeTabs = tabs);
        tabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onTabMouse(tabs, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                onTabMouse(tabs, e);
            }
        });
    }

    private void onTabMouse(JTabbedPane tabs, MouseEvent e) {
        activeTabs = tabs;
        if (!e.isPopupTrigger()) {
            return;
        }
        int idx = tabs.indexAtLocation(e.getX(), e.getY());
        if (idx < 0 || !(tabs.getComponentAt(idx) instanceof SequentView view)
                || view == mainView) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem move = new JMenuItem(tabs == leftTabs && split == null
                ? "Move to a new split"
                : "Move to other split");
        move.addActionListener(ev -> moveToOtherSide(tabs, view));
        JMenuItem close = new JMenuItem("Close");
        close.addActionListener(ev -> closeTab(tabs, view));
        menu.add(move);
        menu.add(close);
        menu.show(tabs, e.getX(), e.getY());
    }

    private void addClosableTab(JTabbedPane tabs, SequentView view) {
        String title = titleFor(view);
        tabs.addTab(title, view);
        int idx = tabs.indexOfComponent(view);
        tabs.setTabComponentAt(idx, tabComponent(new JLabel(title), () -> closeTab(tabs, view)));
    }

    private String titleFor(SequentView view) {
        Node node = view.pinnedNode();
        return node != null ? nodeLabel(node) : view.tabTitle();
    }

    /// A uniform tab component (used for both the main and the pinned tabs so their heights line
    /// up):
    /// a title label and, when `onClose` is given, a small ✕ close affordance.
    private JComponent tabComponent(JLabel title, @Nullable Runnable onClose) {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        tab.setOpaque(false);
        tab.add(title);
        if (onClose != null) {
            JLabel close = new JLabel("✕");
            close.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            close.setToolTipText("Close");
            close.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClose.run();
                }
            });
            tab.add(close);
        }
        return tab;
    }

    private void closeTab(JTabbedPane tabs, SequentView view) {
        if (view == mainView) {
            return;
        }
        tabs.remove(view);
        views.remove(view);
        if (tabs == rightTabs && rightTabs.getTabCount() == 0) {
            collapseSplit();
        }
    }

    private void moveToOtherSide(JTabbedPane from, SequentView view) {
        JTabbedPane to;
        if (from == leftTabs) {
            ensureSplit();
            to = rightTabs;
        } else {
            to = leftTabs;
        }
        from.remove(view);
        addClosableTab(to, view);
        to.setSelectedComponent(view);
        activeTabs = to;
        if (from == rightTabs && rightTabs.getTabCount() == 0) {
            collapseSplit();
        }
    }

    private void ensureSplit() {
        if (split != null) {
            return;
        }
        JTabbedPane right = new JTabbedPane();
        wireGroup(right);
        rightTabs = right;
        remove(leftTabs);
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabs, right);
        sp.setResizeWeight(0.5);
        sp.setOneTouchExpandable(true);
        split = sp;
        add(sp, BorderLayout.CENTER);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> sp.setDividerLocation(0.5));
    }

    private void collapseSplit() {
        if (split == null) {
            return;
        }
        remove(split);
        split = null;
        rightTabs = null;
        add(leftTabs, BorderLayout.CENTER);
        activeTabs = leftTabs;
        revalidate();
        repaint();
    }
}
