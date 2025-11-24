/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

public class BlockExpression extends AbstractBlockExpression {

    public BlockExpression(ImmutableList<org.key_project.rusty.ast.stmt.Statement> statements,
            @Nullable Expr value) {
        super(statements, value);
    }

    public BlockExpression(ExtList children) {
        super(children);
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnBlockExpression(this);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("{\n");
        for (var s : getStatements()) {
            sb.append("\t");
            sb.append(s.toString());
            sb.append("\n");
        }
        if (getValue() != null) {
            sb.append("\t");
            sb.append(getValue());
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
