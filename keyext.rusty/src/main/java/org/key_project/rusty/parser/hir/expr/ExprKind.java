/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.QPath;
import org.key_project.rusty.parser.hir.Span;

public interface ExprKind {
    record Binary(BinOp op, Expr left, Expr right) implements ExprKind {}

    record Unary(UnOp op, Expr expr) implements ExprKind {}

    record LitExpr(Lit lit) implements ExprKind {}

    record BlockExpr(Block block) implements ExprKind {}

    record Assign(Expr left, Expr right, Span span) implements ExprKind {}

    record Path(QPath path) implements ExprKind {}

    record AddrOf(boolean raw, boolean mut, Expr expr) implements ExprKind {}

    class Adapter extends HirAdapter<ExprKind> {
        @Override
        public Class<? extends ExprKind> getType(String tag) {
            return switch (tag) {
                case "Binary" -> Binary.class;
                case "Unary" -> Unary.class;
                case "Lit" -> LitExpr.class;
                case "Block" -> BlockExpr.class;
                case "Assign" -> Assign.class;
                case "Path" -> Path.class;
                case "AddrOf" -> AddrOf.class;
                default -> null;
            };
        }
    }
}