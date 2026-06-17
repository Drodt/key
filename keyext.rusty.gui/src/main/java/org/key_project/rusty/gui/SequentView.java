/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.rusty.Services;
import org.key_project.rusty.pp.IdentitySequentPrintFilter;
import org.key_project.rusty.pp.InitialPositionTable;
import org.key_project.rusty.pp.LogicPrinter;
import org.key_project.rusty.pp.NotationInfo;
import org.key_project.rusty.pp.PosInSequent;
import org.key_project.rusty.pp.PosTableLayouter;
import org.key_project.rusty.pp.Range;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.rule.FindTaclet;
import org.key_project.rusty.rule.NoPosTacletApp;
import org.key_project.rusty.rule.TacletApp;

import org.jspecify.annotations.Nullable;

/// Shows the sequent of the selected node (inner or leaf). A small header carries the node/rule
/// info; the sequent itself is rendered with the position-table printer so a left-click maps to the
/// term under the cursor and offers the applicable taclets of an open goal in a popup. Taclets with
/// open schema variables are completed via the [TacletCompletionDialog] before being applied.
public final class SequentView extends JPanel implements ProofContext.Listener {

    private final ProofContext context;
    private final JLabel header = new JLabel(" ");
    private final JTextArea text = new JTextArea();
    private final IdentitySequentPrintFilter filter = new IdentitySequentPrintFilter();

    private final Highlighter.HighlightPainter painter =
        new DefaultHighlighter.DefaultHighlightPainter(Theme.selection());
    private @Nullable Object highlightTag;

    private @Nullable InitialPositionTable positionTable;
    private @Nullable Node node;
    private @Nullable Runnable runProverAction;

    /// When pinned to a node this view shows that node; otherwise it follows the selection.
    private @Nullable Node pinnedNode;
    private boolean follow = true;
    private Font contentFont = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    private final Color normalForeground;

    public SequentView(ProofContext context) {
        super(new BorderLayout());
        this.context = context;

        // Cut from the same cloth as the tool-window title bars (same surface + hairline) so the
        // editor reads as part of the family, but carries node/rule data rather than a label.
        header.setOpaque(true);
        header.setBackground(Theme.surface());
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.hairline()),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        text.setEditable(false);
        // No text caret: the hover highlight already shows the focused term, and a blinking caret
        // next to it is just noise. Making the area non-focusable suppresses the caret while mouse
        // highlighting and the popup keep working.
        text.setFocusable(false);
        text.setFont(contentFont);
        text.setMargin(new java.awt.Insets(6, 8, 6, 8));
        normalForeground = text.getForeground();
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showTacletPopup(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                highlightAt(text.viewToModel2D(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearHighlight();
            }
        };
        text.addMouseListener(mouse);
        text.addMouseMotionListener(mouse);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(text), BorderLayout.CENTER);
        context.addListener(this);
        render(); // show the empty-state hint until a proof is loaded
    }

    /// Highlights the term/formula whose printed range contains `offset` (the lowest such subterm),
    /// or clears the highlight when `offset` does not point inside a term.
    private void highlightAt(int offset) {
        if (positionTable == null) {
            clearHighlight();
            return;
        }
        PosInSequent pis = positionTable.getPosInSequent(offset, filter);
        if (pis == null || pis.isSequent() || pis.getBounds() == null) {
            clearHighlight();
            return;
        }
        var bounds = pis.getBounds();
        try {
            clearHighlight();
            highlightTag =
                text.getHighlighter().addHighlight(bounds.start(), bounds.end(), painter);
        } catch (BadLocationException ignored) {
            // range no longer valid (e.g. just re-rendered); ignore
        }
    }

    private void clearHighlight() {
        if (highlightTag != null) {
            text.getHighlighter().removeHighlight(highlightTag);
            highlightTag = null;
        }
    }

    @Override
    public void selectedNodeChanged() {
        render();
    }

    @Override
    public void proofLoaded() {
        render();
    }

    @Override
    public void proofChanged() {
        render();
    }

    /// Pins this view to a specific node (it stops following the selection).
    public void pinTo(Node node) {
        this.pinnedNode = node;
        this.follow = false;
        render();
    }

    /// A short title for this view's editor tab: the pinned node, or the live selection.
    public String tabTitle() {
        return pinnedNode != null ? "Node " + pinnedNode.getSerialNr() : "Selection";
    }

    /// The node this view is pinned to, or `null` when it follows the selection.
    public @Nullable Node pinnedNode() {
        return pinnedNode;
    }

    private void render() {
        highlightTag = null; // setText below drops all highlights
        node = follow ? context.getSelectedNode() : pinnedNode;
        if (node == null) {
            header.setText(" ");
            // A quiet, non-monospace hint — clearly chrome, not a sequent to be read.
            text.setForeground(Theme.mutedText());
            text.setFont(
                new Font(Font.SANS_SERIF, Font.ITALIC, Math.max(contentFont.getSize(), 13)));
            text.setText("\n    Open a .key problem or .proof to begin.\n\n"
                + "    Use File ▸ Open, or the Open button in the toolbar.");
            text.setCaretPosition(0);
            positionTable = null;
            return;
        }
        // Real content: prominent, in the configured font and normal colour.
        text.setForeground(normalForeground);
        text.setFont(contentFont);
        boolean goal = goalFor(node) != null;
        StringBuilder head = new StringBuilder("Node ").append(node.getSerialNr())
                .append("   •   ").append(goal ? "open goal" : "inner node");
        if (node.getAppliedRuleApp() != null) {
            head.append("   •   rule: ").append(node.getAppliedRuleApp().rule().name());
        }
        header.setText(head.toString());

        Services services = node.proof().getServices();
        PosTableLayouter layouter = PosTableLayouter.positionTable(80);
        LogicPrinter printer = new LogicPrinter(new NotationInfo(), services, layouter);
        printer.printSequent(node.sequent());
        text.setText(printer.result());
        positionTable = layouter.getInitialPositionTable();
        filter.setSequent(node.sequent());
        text.setCaretPosition(0);
    }

    /// Left-click popup: the taclets applicable to the formula/term under the cursor.
    private void showTacletPopup(MouseEvent e) {
        if (positionTable == null || node == null) {
            return;
        }
        Goal goal = goalFor(node);
        JPopupMenu menu = new JPopupMenu();
        if (goal == null) {
            JMenuItem item = new JMenuItem("(not an open goal)");
            item.setEnabled(false);
            menu.add(item);
            menu.show(text, e.getX(), e.getY());
            return;
        }

        int offset = text.viewToModel2D(e.getPoint());
        highlightAt(offset); // show what the popup targets
        PosInSequent pis = positionTable.getPosInSequent(offset, filter);
        PosInOccurrence occ = pis == null ? null : pis.getPosInOccurrence();

        // Rules that apply to the clicked formula/term go first, most-specific match (deepest find
        // pattern) on top; the sequent-wide (no-find) rules are tucked away in a submenu so they do
        // not bury the term-specific ones.
        List<TacletApp> termApps = applicableTaclets(goal, occ);
        termApps.sort(BY_SPECIFICITY);
        List<TacletApp> sequentApps = noFindTaclets(goal);
        sequentApps.sort(BY_SPECIFICITY);
        if (termApps.isEmpty() && sequentApps.isEmpty()) {
            JMenuItem item = new JMenuItem("(no applicable rules here)");
            item.setEnabled(false);
            menu.add(item);
        } else if (termApps.isEmpty()) {
            // Whole-sequent selection: no term-specific rules to bury, so show the sequent rules
            // directly (chunked into submenus only when there are too many).
            addTacletItems(menu, sequentApps, goal, null);
        } else {
            addTacletItems(menu, termApps, goal, occ);
            if (!sequentApps.isEmpty()) {
                // A term was clicked: tuck the sequent-wide rules away so they do not bury the
                // term-specific ones.
                menu.addSeparator();
                JMenu sequentMenu = new JMenu("Sequent rules");
                addTacletItems(sequentMenu, sequentApps, goal, null);
                menu.add(sequentMenu);
            }
        }
        menu.show(text, e.getX(), e.getY());
    }

    /// Orders taclet applications by most specific match first (deepest find pattern), breaking
    /// ties
    /// alphabetically by display name.
    private static final Comparator<TacletApp> BY_SPECIFICITY =
        Comparator.comparingInt(SequentView::specificity).reversed()
                .thenComparing(app -> app.rule().displayName());

    /// A specificity score for ordering: the depth of the taclet's find pattern (no-find taclets,
    /// which match the whole sequent, score 0 — the least specific).
    private static int specificity(TacletApp app) {
        return app.taclet() instanceof FindTaclet find ? find.find().depth() : 0;
    }

    /// Adds the apps to `parent`, never showing more than six taclets in one menu: when there are
    /// more, the five most specific stay and the rest nest under a "More rules…" submenu
    /// (recursively
    /// chunked the same way).
    private void addTacletItems(JComponent parent, List<TacletApp> apps, Goal goal,
            @Nullable PosInOccurrence occ) {
        if (apps.size() <= 6) {
            for (TacletApp app : apps) {
                parent.add(tacletItem(app, goal, occ));
            }
            return;
        }
        for (int i = 0; i < 5; i++) {
            parent.add(tacletItem(apps.get(i), goal, occ));
        }
        JMenu more = new JMenu("More rules…");
        addTacletItems(more, apps.subList(5, apps.size()), goal, occ);
        parent.add(more);
    }

    private JMenuItem tacletItem(TacletApp app, Goal goal, @Nullable PosInOccurrence occ) {
        JMenuItem item = new JMenuItem(app.rule().displayName());
        item.addActionListener(ev -> applyTaclet(app, goal, occ));
        return item;
    }

    /// Right-click context menu: general actions (run the prover, copy the term under the cursor).
    private void showContextMenu(MouseEvent e) {
        if (node == null) {
            return;
        }
        int offset = text.viewToModel2D(e.getPoint());
        highlightAt(offset);
        JPopupMenu menu = new JPopupMenu();

        JMenuItem run = new JMenuItem("Run prover");
        run.setEnabled(runProverAction != null && context.getProof() != null);
        run.addActionListener(ev -> {
            if (runProverAction != null) {
                runProverAction.run();
            }
        });
        menu.add(run);

        JMenuItem copy = new JMenuItem("Copy term");
        copy.addActionListener(ev -> copyTerm(offset));
        menu.add(copy);

        menu.show(text, e.getX(), e.getY());
    }

    /// Copies the printed text of the term/formula under `offset` (or the whole sequent if the
    /// offset is not inside a term) to the system clipboard.
    private void copyTerm(int offset) {
        String content = text.getText();
        if (positionTable != null) {
            PosInSequent pis = positionTable.getPosInSequent(offset, filter);
            if (pis != null && pis.getBounds() != null) {
                Range b = pis.getBounds();
                content = content.substring(b.start(), b.end());
            }
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(content), null);
    }

    /// Sets the action invoked by the context menu's "Run prover" entry (wired by the main window).
    public void setRunProverAction(@Nullable Runnable runProverAction) {
        this.runProverAction = runProverAction;
    }

    /// Sets the font (family + size) of the sequent text.
    public void applyFont(Font font) {
        this.contentFont = font;
        render(); // re-applies the right font/colour for content vs. the empty-state hint
    }

    /// The open goal sitting on `node`, or `null` if `node` is not an open goal.
    private @Nullable Goal goalFor(Node node) {
        Proof proof = context.getProof();
        if (proof == null) {
            return null;
        }
        for (Goal g : proof.openGoals()) {
            if (g.getNode() == node) {
                return g;
            }
        }
        return null;
    }

    /// The find-taclet applications at `occ` (the rules that apply to the formula/term under the
    /// cursor). Applications with open schema variables are included; they are completed via the
    /// taclet-completion dialog when chosen.
    List<TacletApp> applicableTaclets(Goal goal, @Nullable PosInOccurrence occ) {
        List<TacletApp> result = new ArrayList<>();
        if (occ != null) {
            Services services = goal.getOverlayServices();
            for (TacletApp app : goal.ruleAppIndex().getTacletAppAt(occ, services)) {
                result.add(app);
            }
        }
        return result;
    }

    /// The no-find (sequent-wide) taclet applications, e.g. cut (which is completed via the
    /// dialog).
    List<TacletApp> noFindTaclets(Goal goal) {
        List<TacletApp> result = new ArrayList<>();
        for (NoPosTacletApp app : goal.ruleAppIndex().getNoFindTaclet(goal.getOverlayServices())) {
            result.add(app);
        }
        return result;
    }

    /// Applies a taclet to the goal, refreshing the views. If the application still has open schema
    /// variables, the taclet-completion dialog is shown first; cancelling it aborts the
    /// application.
    void applyTaclet(TacletApp app, Goal goal, @Nullable PosInOccurrence occ) {
        try {
            TacletApp toApply = occ != null ? app.setPosInOccurrence(occ, goal.getOverlayServices())
                    : app;
            if (!toApply.complete()) {
                TacletApp completed = TacletCompletionDialog.completeApp(this, toApply, goal);
                if (completed == null) {
                    return; // cancelled or invalid
                }
                toApply = completed;
            }
            goal.apply(toApply);
            context.fireProofChanged();
            context.setSelectedNode(goal.getNode());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not apply " + app.rule().displayName() + ":\n"
                    + ex.getMessage(),
                "Rule application failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /// Package-private accessors for tests.
    String renderedText() {
        return text.getText();
    }

    @Nullable
    PosInSequent posAt(int offset) {
        return positionTable == null ? null : positionTable.getPosInSequent(offset, filter);
    }
}
