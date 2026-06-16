/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

/// Entry point of KeYther, the minimal Rusty prover GUI.
public final class RustyMain {

    private RustyMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // A modern, flat cross-platform look (away from the native Aqua/Metal styling).
            try {
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("TabbedPane.tabType", "card");
                UIManager.put("ScrollBar.showButtons", false);
                FlatLightLaf.setup();
            } catch (Exception ex) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to the default look and feel
                }
            }
            new MainWindow().setVisible(true);
        });
    }
}
