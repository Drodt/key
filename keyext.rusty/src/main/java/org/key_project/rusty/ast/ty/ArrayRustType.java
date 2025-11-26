/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.Def;
import org.key_project.rusty.ast.ResDef;
import org.key_project.rusty.ast.abstraction.*;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.expr.IntegerLiteralExpression;
import org.key_project.rusty.ast.expr.PathExpr;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public class ArrayRustType implements RustType {
    private final RustType elemTy;
    private final Expr len;
    private final Type ty;

    public ArrayRustType(RustType elemTy, Expr len, Services services) {
        this.elemTy = elemTy;
        this.len = len;
        ArrayLen length = switch (len) {
            case IntegerLiteralExpression le -> new IntArrayLen(le.getValue().intValue());
            case PathExpr p -> {
                if (p.path().res() instanceof ResDef(Def def)
                        && def instanceof GenericConstParam cp) {
                    yield new ConstArrayLen(cp.fn());
                } else {
                    throw new IllegalArgumentException("Unexpected path: " + p.path());
                }
            }
            default -> throw new IllegalStateException(
                "Unexpected value: " + len + " (" + len.getClass() + ")");
        };
        ty = ArrayType.getInstance(elemTy.type(), length, services);
    }

    public RustType getElemTy() {
        return elemTy;
    }

    public Expr getLen() {
        return len;
    }

    @Override
    public Type type() {
        return ty;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnArrayRustType(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> elemTy;
            case 1 -> len;
            default -> throw new IndexOutOfBoundsException();
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }
}
