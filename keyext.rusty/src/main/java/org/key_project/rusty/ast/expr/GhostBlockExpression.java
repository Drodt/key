/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableList;
import org.jspecify.annotations.Nullable;

public class GhostBlockExpression extends AbstractBlockExpression {

    public GhostBlockExpression(ImmutableList<org.key_project.rusty.ast.stmt.Statement> statements,
                                @Nullable Expr value) {
        super(statements, value);
    }

    public GhostBlockExpression(ExtList children) {
        super(children);
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnGhostBlockExpression(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ghost! {");
        for (int i = 0; i < getStatements().size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(getStatements().get(i));
        }
        if (getValue() != null) {
            if (!getStatements().isEmpty()) sb.append("; ");
            sb.append(getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
