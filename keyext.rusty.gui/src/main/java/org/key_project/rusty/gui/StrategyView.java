/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;

import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.settings.StrategySettings;
import org.key_project.rusty.strategy.StrategyProperties;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.strategy.StrategyProperties.FUNCTION_CONTRACT;
import static org.key_project.rusty.strategy.StrategyProperties.FUNCTION_EXPAND;
import static org.key_project.rusty.strategy.StrategyProperties.FUNCTION_NONE;
import static org.key_project.rusty.strategy.StrategyProperties.FUNCTION_OPTIONS_KEY;
import static org.key_project.rusty.strategy.StrategyProperties.NON_LIN_ARITH_COMPLETION;
import static org.key_project.rusty.strategy.StrategyProperties.NON_LIN_ARITH_DEF_OPS;
import static org.key_project.rusty.strategy.StrategyProperties.NON_LIN_ARITH_NONE;
import static org.key_project.rusty.strategy.StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY;
import static org.key_project.rusty.strategy.StrategyProperties.SPLITTING_DELAYED;
import static org.key_project.rusty.strategy.StrategyProperties.SPLITTING_NORMAL;
import static org.key_project.rusty.strategy.StrategyProperties.SPLITTING_OFF;
import static org.key_project.rusty.strategy.StrategyProperties.SPLITTING_OPTIONS_KEY;
import static org.key_project.rusty.strategy.StrategyProperties.STOPMODE_DEFAULT;
import static org.key_project.rusty.strategy.StrategyProperties.STOPMODE_NONCLOSE;
import static org.key_project.rusty.strategy.StrategyProperties.STOPMODE_OPTIONS_KEY;

/// An always-visible tool window for the automatic proof search of the current proof: it shows the
/// live strategy settings (max rule applications, timeout, and the main option groups) and writes
/// every edit straight back to the proof's settings, so there is no open/apply round-trip. It
/// repopulates whenever a proof is loaded and disables itself when none is open.
public final class StrategyView extends JPanel implements ProofContext.Listener {

    /// A single multiple-choice strategy option (a property key and its labelled values).
    private record Choice(String label, String key, String[] values, String[] valueLabels) {
    }

    private static final Choice[] CHOICES = {
        new Choice("Stop at", STOPMODE_OPTIONS_KEY,
            new String[] { STOPMODE_DEFAULT, STOPMODE_NONCLOSE },
            new String[] { "Default", "Unclosable goal" }),
        new Choice("Splitting", SPLITTING_OPTIONS_KEY,
            new String[] { SPLITTING_NORMAL, SPLITTING_DELAYED, SPLITTING_OFF },
            new String[] { "Normal", "Delayed", "Off" }),
        new Choice("Functions", FUNCTION_OPTIONS_KEY,
            new String[] { FUNCTION_EXPAND, FUNCTION_CONTRACT, FUNCTION_NONE },
            new String[] { "Expand", "Contract", "None" }),
        new Choice("Arithmetic", NON_LIN_ARITH_OPTIONS_KEY,
            new String[] { NON_LIN_ARITH_NONE, NON_LIN_ARITH_DEF_OPS, NON_LIN_ARITH_COMPLETION },
            new String[] { "Basic", "DefOps", "Model search" }),
    };

    private final ProofContext context;
    private final JSpinner maxSteps =
        new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 100));
    private final JSpinner timeout =
        new JSpinner(new SpinnerNumberModel(-1.0, -1.0, Long.MAX_VALUE, 1000));
    private final List<JComboBox<String>> combos = new ArrayList<>();
    private final List<JComponent> controls = new ArrayList<>();

    /// Guards the change listeners while the controls are repopulated from the settings.
    private boolean loading;

    /// While an auto-mode run is in progress the controls are locked, so a live edit can never
    /// mutate the settings the prover thread is reading.
    private boolean running;

    public StrategyView(ProofContext context) {
        super(new BorderLayout());
        this.context = context;

        // Stack the label above its control so each row only needs the width of one control: the
        // pane stays usable when narrow instead of forcing a horizontal scrollbar. The form tracks
        // the viewport width (see ScrollableForm) so the controls shrink to fit and are never
        // clipped on the right.
        JPanel form = new ScrollableForm();
        form.setLayout(new BoxLayout(form, BoxLayout.PAGE_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        maxSteps.addChangeListener(e -> applyMaxSteps());
        addGroup(form, "Max. rule apps", maxSteps);
        controls.add(maxSteps);

        timeout.addChangeListener(e -> applyTimeout());
        timeout.setToolTipText("Timeout in ms; -1 disables it");
        addGroup(form, "Timeout (ms)", timeout);
        controls.add(timeout);

        for (Choice c : CHOICES) {
            JComboBox<String> combo = new JComboBox<>(c.valueLabels());
            int index = combos.size();
            combo.addActionListener(e -> applyChoice(index));
            combos.add(combo);
            controls.add(combo);
            addGroup(form, c.label(), combo);
        }

        form.add(Box.createVerticalGlue()); // keep the groups pinned to the top

        // No horizontal scrollbar: groups always fit the pane width.
        add(new JScrollPane(form, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

        // Let the divider drag this pane narrow; it does not demand a wide minimum.
        setMinimumSize(new Dimension(150, 0));

        context.addListener(this);
        load();
    }

    @Override
    public void proofLoaded() {
        load();
    }

    /// Locks the controls while an auto-mode run is in progress and restores them afterwards.
    public void setRunning(boolean running) {
        this.running = running;
        updateEnabled();
    }

    private void addGroup(JPanel form, String label, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 2));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(new JLabel(label), BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        // Let the group grow horizontally but never stretch vertically.
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, group.getPreferredSize().height));
        form.add(group);
        form.add(Box.createVerticalStrut(10));
    }

    private @Nullable StrategySettings settings() {
        Proof proof = context.getProof();
        return proof == null ? null : proof.getSettings().getStrategySettings();
    }

    /// Repopulates the controls from the current proof's settings (or disables them if none).
    private void load() {
        StrategySettings settings = settings();
        if (settings != null) {
            loading = true;
            try {
                maxSteps.setValue(Math.max(settings.getMaxSteps(), 0));
                timeout.setValue((double) settings.getTimeout());
                StrategyProperties props = settings.getActiveStrategyProperties();
                for (int i = 0; i < CHOICES.length; i++) {
                    combos.get(i).setSelectedIndex(
                        indexOf(CHOICES[i], props.getProperty(CHOICES[i].key())));
                }
            } finally {
                loading = false;
            }
        }
        updateEnabled();
    }

    private void applyMaxSteps() {
        StrategySettings settings = liveSettings();
        if (settings != null) {
            settings.setMaxSteps((Integer) maxSteps.getValue());
        }
    }

    private void applyTimeout() {
        StrategySettings settings = liveSettings();
        if (settings != null) {
            settings.setTimeout(((Number) timeout.getValue()).longValue());
        }
    }

    private void applyChoice(int i) {
        StrategySettings settings = liveSettings();
        if (settings == null) {
            return;
        }
        StrategyProperties props = settings.getActiveStrategyProperties();
        props.setProperty(CHOICES[i].key(), CHOICES[i].values()[combos.get(i).getSelectedIndex()]);
        settings.setActiveStrategyProperties(props);
    }

    /// The settings to write to from a user edit, or `null` while repopulating or with no proof.
    private @Nullable StrategySettings liveSettings() {
        return loading ? null : settings();
    }

    /// Controls are usable only when a proof is open and no run is in progress.
    private void updateEnabled() {
        boolean enabled = !running && settings() != null;
        for (JComponent control : controls) {
            control.setEnabled(enabled);
        }
    }

    private static int indexOf(Choice c, String value) {
        for (int i = 0; i < c.values().length; i++) {
            if (c.values()[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    /// A form panel that is as wide as the scroll viewport (never wider), so its controls shrink to
    /// fit the pane and are never clipped on the right; it still scrolls vertically when too tall.
    private static final class ScrollableForm extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(visible.height - 16, 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
