/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.sequent.FormulaChangeInfo;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.sv.ModalOperatorSV;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.op.sv.VariableSV;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.io.IntermediateProofReplayer;
import org.key_project.rusty.proof.io.OutputStreamProofSaver;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.rusty.rule.TacletExecutor;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

/// A modal dialog to complete the open schema-variable instantiations of a taclet application
/// before
/// it is applied. On the left it shows the rule's definition and a typed input field per
/// uninstantiated schema variable; on the right it shows a *live result preview* of the sequents
/// the
/// application would produce (with added/removed/modified formulas diffed). The preview is computed
/// side-effect-free via [TacletExecutor#getResultSequentChanges] (never `Goal.apply`), so it never
/// touches the proof. On apply the completed application is returned (or `null` when cancelled).
final class TacletCompletionDialog extends JDialog {

    private static final Color ADDED = new Color(0x1D, 0x9E, 0x75);
    private static final Color REMOVED = new Color(0xC0, 0x39, 0x2B);

    private final TacletApp app;
    private final Goal goal;
    private final List<SchemaVariable> svs = new ArrayList<>();
    private final List<JTextField> fields = new ArrayList<>();
    private final JLabel status = new JLabel(" ");
    private final JButton apply = new JButton("Apply");
    private final JPanel previewBody = new JPanel();
    private final Timer debounce = new Timer(150, e -> validateInputs());
    private @Nullable TacletApp result;

    private TacletCompletionDialog(@Nullable Window owner, TacletApp app, Goal goal) {
        super(owner, "Complete taclet: " + app.taclet().name(), ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.goal = goal;
        debounce.setRepeats(false);

        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));
        left.add(buildRuleSection(), BorderLayout.NORTH);
        left.add(buildInstantiateSection(), BorderLayout.CENTER);

        apply.addActionListener(e -> onApply());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        status.setForeground(Theme.mutedText());
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        south.add(status, BorderLayout.WEST);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(apply);
        south.add(buttons, BorderLayout.EAST);

        javax.swing.JSplitPane split = new javax.swing.JSplitPane(
            javax.swing.JSplitPane.HORIZONTAL_SPLIT, left, buildPreviewSection());
        split.setResizeWeight(0.55);

        add(split, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(apply);
        validateInputs();
        setSize(880, 560);
        setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.55));
    }

    /// Opens the dialog for `app` (which still has uninstantiated schema variables) and returns the
    /// completed application, or `null` if the user cancelled or the input was invalid.
    static @Nullable TacletApp completeApp(Component parent, TacletApp app, Goal goal) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        TacletCompletionDialog dialog = new TacletCompletionDialog(owner, app, goal);
        dialog.setVisible(true);
        return dialog.result;
    }

    // ── left column ───────────────────────────────────────────────────────────

    private JPanel buildRuleSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(sectionLabel("Rule: " + app.taclet().name()), BorderLayout.NORTH);
        JTextArea body = new JTextArea(app.taclet().toString());
        body.setEditable(false);
        body.setFocusable(false);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        body.setMargin(new Insets(4, 6, 4, 6));
        JScrollPane scroll = new JScrollPane(body);
        scroll.setPreferredSize(new Dimension(420, 150));
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildInstantiateSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(sectionLabel("Instantiate schema variables"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        int row = 0;
        DocumentListener live = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onEdit();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onEdit();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onEdit();
            }
        };
        for (SchemaVariable sv : app.uninstantiatedVars()) {
            svs.add(sv);
            JTextField field = new JTextField(20);
            field.getDocument().addDocumentListener(live);
            fields.add(field);
            g.gridy = row;
            g.gridx = 0;
            form.add(new JLabel(sv.name() + "  (" + kind(sv) + ")"), g);
            g.gridx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            form.add(field, g);
            g.fill = GridBagConstraints.NONE;
            g.weightx = 0;
            row++;
        }
        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.NORTH);
        section.add(new JScrollPane(top), BorderLayout.CENTER);
        return section;
    }

    // ── right column (preview) ────────────────────────────────────────────────

    private JPanel buildPreviewSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 12));
        section.add(sectionLabel("Result preview"), BorderLayout.NORTH);
        previewBody.setLayout(new BoxLayout(previewBody, BoxLayout.Y_AXIS));
        JPanel top = new JPanel(new BorderLayout());
        top.add(previewBody, BorderLayout.NORTH);
        section.add(new JScrollPane(top), BorderLayout.CENTER);
        return section;
    }

    /// Single source of truth for the dialog state: parses the current inputs and sets the status
    /// line, the Apply button and the preview together. Apply is enabled only when every field
    /// parses and the application is complete.
    private void validateInputs() {
        previewBody.removeAll();
        if (hasEmptyField()) {
            setState(Theme.mutedText(), "Enter an instantiation for every schema variable.", false);
            previewMessage("Complete the instantiations to preview the result.");
        } else {
            try {
                TacletApp built = parseApp();
                if (!built.complete()) {
                    setState(Theme.mutedText(), "Some schema variables are still open.", false);
                    previewMessage("Complete the instantiations to preview the result.");
                } else {
                    setState(Theme.mutedText(), "Ready to apply.", true);
                    TacletExecutor exec = (TacletExecutor) built.taclet().getExecutor();
                    ImmutableList<SequentChangeInfo> changes =
                        exec.getResultSequentChanges(goal, built);
                    if (changes.isEmpty()) {
                        previewMessage("No preview available for this rule.");
                    } else {
                        renderGoals(changes);
                    }
                }
            } catch (Exception ex) {
                setState(REMOVED, "The instantiation cannot be parsed.", false);
                previewMessage("Fix the instantiation to preview the result.");
            }
        }
        previewBody.revalidate();
        previewBody.repaint();
    }

    private boolean hasEmptyField() {
        for (JTextField field : fields) {
            if (field.getText().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void setState(Color colour, String text, boolean ready) {
        status.setForeground(colour);
        status.setText(text);
        apply.setEnabled(ready);
    }

    private void renderGoals(ImmutableList<SequentChangeInfo> changes) {
        Services services = goal.proof().getServices();
        int n = changes.size();
        int i = 1;
        for (SequentChangeInfo sci : changes) {
            if (i > 1) {
                previewBody.add(new JSeparator());
            }
            if (n > 1) {
                JLabel head = new JLabel("Goal " + i + " of " + n);
                head.setForeground(Theme.mutedText());
                head.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
                previewBody.add(head);
            }
            renderSide(sci, true, services);
            renderSide(sci, false, services);
            i++;
        }
    }

    private void renderSide(SequentChangeInfo sci, boolean antec, Services services) {
        ImmutableList<SequentFormula> removed = sci.removedFormulas(antec);
        ImmutableList<SequentFormula> added = sci.addedFormulas(antec);
        ImmutableList<FormulaChangeInfo> modified = sci.modifiedFormulas(antec);
        if (removed.isEmpty() && added.isEmpty() && modified.isEmpty()) {
            return;
        }
        JLabel head = new JLabel(antec ? "antecedent" : "succedent");
        head.setForeground(Theme.mutedText());
        head.setBorder(BorderFactory.createEmptyBorder(4, 0, 1, 0));
        previewBody.add(head);
        for (SequentFormula f : removed) {
            previewBody.add(changeRow("−", REMOVED, f.formula(), services));
        }
        for (FormulaChangeInfo m : modified) {
            previewBody.add(changeRow("−", REMOVED, m.getOriginalFormula().formula(), services));
            previewBody.add(changeRow("+", ADDED, m.newFormula().formula(), services));
        }
        for (SequentFormula f : added) {
            previewBody.add(changeRow("+", ADDED, f.formula(), services));
        }
    }

    private JComponent changeRow(String marker, Color color,
            org.key_project.logic.Term formula, Services services) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        JLabel m = new JLabel(marker);
        m.setForeground(color);
        m.setFont(m.getFont().deriveFont(Font.BOLD));
        m.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        p.add(m, BorderLayout.WEST);
        String printed = OutputStreamProofSaver.printTerm(formula, services).replace('\n', ' ');
        JLabel term = new JLabel(printed);
        term.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        term.setToolTipText(printed);
        p.add(term, BorderLayout.CENTER);
        return p;
    }

    private void previewMessage(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.mutedText());
        previewBody.add(l);
    }

    // ── shared ────────────────────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private static String kind(SchemaVariable sv) {
        if (sv.isFormula()) {
            return "formula";
        }
        if (sv.isVariable()) {
            return "variable";
        }
        if (sv.isSkolemTerm()) {
            return "skolem term";
        }
        if (sv instanceof ProgramSV) {
            return "program";
        }
        if (sv instanceof ModalOperatorSV) {
            return "modality";
        }
        return "term";
    }

    private void onEdit() {
        // Disable Apply until the (debounced) validation confirms the new input parses.
        apply.setEnabled(false);
        debounce.restart();
    }

    /// Builds the (possibly completed) application from the current field inputs, reusing the
    /// proof-replayer's parsing. Throws when an input is missing or cannot be parsed.
    private TacletApp parseApp() throws Exception {
        Services services = goal.proof().getServices();
        TacletApp current = app;
        for (int i = 0; i < svs.size(); i++) {
            if (svs.get(i) instanceof VariableSV vsv) {
                current = IntermediateProofReplayer.parseSV1(current, vsv,
                    fields.get(i).getText().trim(), services);
            }
        }
        for (int i = 0; i < svs.size(); i++) {
            if (!(svs.get(i) instanceof VariableSV)) {
                current = IntermediateProofReplayer.parseSV2(current, svs.get(i),
                    fields.get(i).getText().trim(), goal);
            }
        }
        return current;
    }

    private void onApply() {
        try {
            TacletApp completed = parseApp();
            if (!completed.complete()) {
                status.setForeground(REMOVED);
                status.setText("The taclet is still not completely instantiated.");
                return;
            }
            result = completed;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not complete the taclet:\n" + ex.getMessage(), "Invalid instantiation",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
