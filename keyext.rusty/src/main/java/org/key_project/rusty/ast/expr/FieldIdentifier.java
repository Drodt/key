/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.Identifier;
import org.key_project.rusty.ast.abstraction.Field;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public record FieldIdentifier(Identifier identifier, Field field) implements IFieldIdentifier {
    @Override
    public @NonNull Name name() {
        return identifier.name();
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnFieldIdentifier(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return identifier;
        throw new IndexOutOfBoundsException("Invalid child number " + n);
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
