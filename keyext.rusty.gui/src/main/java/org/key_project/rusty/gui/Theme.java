/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.awt.Color;
import javax.swing.UIManager;

/// Look-and-feel-derived colours for the GUI chrome, so the docked layout adapts to the active
/// theme (light or dark) instead of hardcoding a light palette. Each accessor falls back to a light
/// default when the look and feel does not define the key.
final class Theme {

    private Theme() {}

    /// Surface of the tool-window title bars and the sequent header: a shade nudged away from the
    /// plain panel background, so a header reads as a distinct band rather than melding into the
    /// flat panels and the split-divider arrows next to it.
    static Color surface() {
        return shade(color("Panel.background", new Color(0xEC, 0xEC, 0xEC)));
    }

    /// The hairline separating a title bar / header from its content.
    static Color hairline() {
        return color("Separator.foreground", color("controlShadow", new Color(0xCC, 0xCC, 0xCC)));
    }

    /// Muted text for title-bar labels.
    static Color mutedText() {
        return color("Label.disabledForeground", new Color(0x55, 0x55, 0x55));
    }

    /// Selection-style highlight for the hovered/targeted sequent term.
    static Color selection() {
        return color("textHighlight", new Color(0xBB, 0xD6, 0xFB));
    }

    /// Accent colour, used to mark the live (selection-following) editor tab.
    static Color accent() {
        return color("Component.accentColor", new Color(0x24, 0x75, 0xBF));
    }

    private static Color color(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    /// Nudges a colour toward higher contrast with itself: darker in a light theme, lighter in a
    /// dark one, so a header band stands slightly apart from the surrounding panels.
    private static Color shade(Color c) {
        boolean dark = c.getRed() + c.getGreen() + c.getBlue() < 384;
        double f = dark ? 1.16 : 0.92;
        return new Color(clamp(c.getRed() * f), clamp(c.getGreen() * f), clamp(c.getBlue() * f));
    }

    private static int clamp(double v) {
        return Math.max(0, Math.min(255, (int) Math.round(v)));
    }
}
