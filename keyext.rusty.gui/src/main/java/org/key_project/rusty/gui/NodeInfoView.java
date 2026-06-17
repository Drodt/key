/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.key_project.prover.rules.RuleApp;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.rule.TacletApp;

import org.jspecify.annotations.Nullable;

/// A read-only view describing the rule applied at a proof node. It *pins* to the selected node and
/// *previews* whichever node the cursor hovers in the proof tree, reverting to the pinned node when
/// the cursor leaves. When the rule is a taclet, the full taclet definition is available under a
/// collapsible toggle that shows only the taclet name when collapsed and the whole taclet when
/// expanded.
public final class NodeInfoView extends JPanel implements ProofContext.Listener {

    private final ProofContext context;
    private final JTextArea info = new JTextArea();
    private final JButton toggle = new JButton();
    private final JTextArea tacletText = new JTextArea();
    private final JScrollPane tacletScroll;

    private boolean expanded;
    private @Nullable String tacletName;
    private @Nullable Node shown;
    private Font contentFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private final Color normalForeground;

    public NodeInfoView(ProofContext context) {
        super(new BorderLayout());
        this.context = context;

        info.setEditable(false);
        info.setFocusable(false);
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        info.setMargin(new java.awt.Insets(6, 8, 2, 8));
        normalForeground = info.getForeground();

        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        toggle.setBorderPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setFocusable(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setVisible(false);
        toggle.addActionListener(e -> {
            expanded = !expanded;
            updateTacletSection();
        });

        tacletText.setEditable(false);
        tacletText.setFocusable(false);
        tacletText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tacletText.setMargin(new java.awt.Insets(2, 14, 4, 6));
        tacletScroll = new JScrollPane(tacletText);
        tacletScroll.setBorder(BorderFactory.createEmptyBorder());
        tacletScroll.setVisible(false);

        JPanel top = new JPanel(new BorderLayout());
        top.add(info, BorderLayout.CENTER);
        top.add(toggle, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(tacletScroll, BorderLayout.CENTER);
        context.addListener(this);
    }

    /// Pins the view to the selected node.
    @Override
    public void selectedNodeChanged() {
        render(context.getSelectedNode());
    }

    @Override
    public void proofLoaded() {
        render(context.getSelectedNode());
    }

    @Override
    public void proofChanged() {
        render(context.getSelectedNode());
    }

    /// Previews `node` (e.g. while hovering the proof tree); reverts to the pinned, selected node
    /// once the cursor leaves (`node == null`).
    public void preview(@Nullable Node node) {
        render(node != null ? node : context.getSelectedNode());
    }

    private void render(@Nullable Node node) {
        shown = node;
        if (node == null) {
            info.setForeground(Theme.mutedText());
            info.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, contentFont.getSize()));
            info.setText(
                "No node selected.\nHover or select a proof node to see its applied rule.");
        } else {
            info.setForeground(normalForeground);
            info.setFont(contentFont);
            info.setText(describe(node));
        }
        info.setCaretPosition(0);

        RuleApp app = node == null ? null : node.getAppliedRuleApp();
        if (app instanceof TacletApp tacletApp) {
            tacletName = tacletApp.taclet().name().toString();
            tacletText.setText(tacletApp.taclet().toString());
            tacletText.setCaretPosition(0);
        } else {
            tacletName = null;
            tacletText.setText("");
        }
        updateTacletSection();
    }

    /// Sets the font (family + size) of both the info and the taclet text.
    public void applyFont(Font font) {
        contentFont = font;
        tacletText.setFont(font);
        render(shown);
    }

    private void updateTacletSection() {
        boolean hasTaclet = tacletName != null;
        toggle.setVisible(hasTaclet);
        if (hasTaclet) {
            toggle.setText((expanded ? "▼ " : "▶ ") + "Applied taclet: " + tacletName);
        }
        tacletScroll.setVisible(hasTaclet && expanded);
        revalidate();
        repaint();
    }

    private static String describe(@Nullable Node node) {
        if (node == null) {
            return "";
        }
        RuleApp app = node.getAppliedRuleApp();
        if (app == null) {
            return "Node " + node.getSerialNr() + "\n(no rule applied — open or closed leaf)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(node.getSerialNr()).append('\n');
        sb.append("Rule: ").append(app.rule().displayName());
        String internal = app.rule().name().toString();
        if (!internal.equals(app.rule().displayName())) {
            sb.append("  (").append(internal).append(')');
        }
        sb.append('\n');
        sb.append("Kind: ").append(app instanceof TacletApp ? "taclet" : "built-in rule");
        int children = node.childrenCount();
        if (children > 1) {
            sb.append('\n').append("Splits into ").append(children).append(" branches");
        }
        return sb.toString();
    }
}
