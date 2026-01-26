/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;

import org.jspecify.annotations.NonNull;

public record SortArg(Sort sort) implements GenericArgument, SyntaxElement {
    @Override
    public @NonNull String toString() {
        return sort.toString();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0 && sort instanceof ParametricSortInstance psi)
            return psi;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return sort instanceof ParametricSortInstance ? 1 : 0;
    }
}
