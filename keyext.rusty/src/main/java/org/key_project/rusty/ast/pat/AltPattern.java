/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.pat;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

public record AltPattern(ImmutableArray<Pattern> alternatives) implements Pattern {
    @Override
    public SyntaxElement getChild(int n) {
        return Objects.requireNonNull(alternatives.get(n));
    }

    @Override
    public int getChildCount() {
        return alternatives.size();
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnAltPattern(this);
    }
}
