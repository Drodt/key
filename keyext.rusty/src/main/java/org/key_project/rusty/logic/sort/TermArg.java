/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;

import org.jspecify.annotations.NonNull;

public record TermArg(Term term) implements ParamSortArg {
    public TermArg {
        if (term == null) {
            throw new NullPointerException("term is null");
        }
    }

    @Override
    public String toString() {
        return term.toString();
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0) {
            return term;
        }
        throw new IndexOutOfBoundsException("Invalid child index: " + n);
    }
}
