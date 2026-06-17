/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/// A modal dialog to configure the fonts of the GUI: a global font family + size applied to every
/// pane, and an optional per-pane override (proof tree, sequent, node info, open goals). Settings
/// are stored in [Preferences] under `font.*`; `onApply` re-applies them to the live panes.
final class PreferencesDialog extends JDialog {

    /// The panes whose font can be overridden: a preferences key and a display label.
    private static final String[][] PANES = {
        { "tree", "Proof tree" },
        { "sequent", "Sequent" },
        { "info", "Node info" },
        { "goals", "Open goals" },
    };

    private final Preferences prefs;
    private final Runnable onApply;

    private final JComboBox<String> globalFamily;
    private final JSpinner globalSize;
    private final List<JCheckBox> override = new ArrayList<>();
    private final List<JComboBox<String>> paneFamily = new ArrayList<>();
    private final List<JSpinner> paneSize = new ArrayList<>();

    PreferencesDialog(Frame owner, Preferences prefs, Runnable onApply) {
        super(owner, "Preferences", true);
        this.prefs = prefs;
        this.onApply = onApply;

        String[] families = fontFamilies();
        String globalFamilyValue = prefs.get("font.family", Font.MONOSPACED);
        int globalSizeValue = prefs.getInt("font.size", 13);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        int row = 0;

        row = header(form, g, row, "Global font (applies to every pane)");
        globalFamily = new JComboBox<>(families);
        globalFamily.setSelectedItem(globalFamilyValue);
        globalSize = new JSpinner(new SpinnerNumberModel(globalSizeValue, 6, 48, 1));
        row = fontRow(form, g, row, new JLabel("Font"), globalFamily, globalSize);

        row = separator(form, g, row);
        row = header(form, g, row, "Per-pane overrides");

        for (String[] pane : PANES) {
            String key = pane[0];
            boolean has = prefs.get("font." + key + ".family", null) != null;
            JCheckBox check = new JCheckBox(pane[1]);
            check.setSelected(has);
            JComboBox<String> family = new JComboBox<>(families);
            family.setSelectedItem(prefs.get("font." + key + ".family", globalFamilyValue));
            JSpinner size = new JSpinner(new SpinnerNumberModel(
                prefs.getInt("font." + key + ".size", globalSizeValue), 6, 48, 1));
            override.add(check);
            paneFamily.add(family);
            paneSize.add(size);
            int idx = override.size() - 1;
            check.addActionListener(e -> updateEnabled(idx));
            row = fontRow(form, g, row, check, family, size);
            updateEnabled(idx);
        }

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            apply();
            dispose();
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(ok);

        add(new JScrollPane(form), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private int header(JPanel form, GridBagConstraints g, int row, String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 3;
        form.add(label, g);
        g.gridwidth = 1;
        return row + 1;
    }

    private int separator(JPanel form, GridBagConstraints g, int row) {
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 3;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JSeparator(), g);
        g.fill = GridBagConstraints.NONE;
        g.gridwidth = 1;
        return row + 1;
    }

    private int fontRow(JPanel form, GridBagConstraints g, int row, JLabel label,
            JComboBox<String> family, JSpinner size) {
        g.gridy = row;
        g.gridx = 0;
        form.add(label, g);
        g.gridx = 1;
        form.add(family, g);
        g.gridx = 2;
        form.add(size, g);
        return row + 1;
    }

    private int fontRow(JPanel form, GridBagConstraints g, int row, JCheckBox check,
            JComboBox<String> family, JSpinner size) {
        g.gridy = row;
        g.gridx = 0;
        form.add(check, g);
        g.gridx = 1;
        form.add(family, g);
        g.gridx = 2;
        form.add(size, g);
        return row + 1;
    }

    private void updateEnabled(int idx) {
        boolean on = override.get(idx).isSelected();
        paneFamily.get(idx).setEnabled(on);
        paneSize.get(idx).setEnabled(on);
    }

    private void apply() {
        prefs.put("font.family", (String) globalFamily.getSelectedItem());
        prefs.putInt("font.size", (Integer) globalSize.getValue());
        for (int i = 0; i < PANES.length; i++) {
            String key = PANES[i][0];
            if (override.get(i).isSelected()) {
                prefs.put("font." + key + ".family", (String) paneFamily.get(i).getSelectedItem());
                prefs.putInt("font." + key + ".size", (Integer) paneSize.get(i).getValue());
            } else {
                prefs.remove("font." + key + ".family");
                prefs.remove("font." + key + ".size");
            }
        }
        onApply.run();
    }

    private static String[] fontFamilies() {
        List<String> all = new ArrayList<>(List.of(Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF));
        all.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()));
        return all.toArray(new String[0]);
    }
}
