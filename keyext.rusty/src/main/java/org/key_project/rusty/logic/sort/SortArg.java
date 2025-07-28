/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import org.key_project.logic.TerminalSyntaxElement;
import org.key_project.logic.sort.Sort;

public record SortArg(Sort sort) implements ParamSortArg, TerminalSyntaxElement {
    @Override
    public String toString() {
        return sort.toString();
    }
}
