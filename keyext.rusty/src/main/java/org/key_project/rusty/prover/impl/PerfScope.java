/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.prover.impl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class PerfScope {
    private static final DecimalFormat DECIMAL_FORMAT =
        new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    public static String formatTime(long dt) {
        String unit;
        double time;
        if (dt < 1000000) {
            time = dt / 1e3;
            unit = "ns";
        } else if (dt < 1000000000) {
            time = dt / 1e6;
            unit = "ms";
        } else {
            time = dt / 1e9;
            unit = "s";
        }

        return DECIMAL_FORMAT.format(time) + unit;
    }
}
