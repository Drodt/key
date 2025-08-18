/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.*;
import org.key_project.rusty.parser.hir.hirty.HirTy;

import org.jspecify.annotations.Nullable;

public interface ExprKind {
    record ConstBlock(ConstBlockExpr block) implements ExprKind {
    }

    record Array(Expr[] exprs) implements ExprKind {
    }

    record Call(Expr callee, Expr[] args) implements ExprKind {
    }

    record MethodCall(PathSegment segment, Expr callee, Expr[] args, Span span)
            implements ExprKind {
    }

    record Tup(Expr[] exprs) implements ExprKind {
    }

    record Binary(BinOp op, Expr left, Expr right) implements ExprKind {
    }

    record Unary(UnOp op, Expr expr) implements ExprKind {
    }

    record LitExpr(Lit lit) implements ExprKind {
    }

    record CastExpr(Expr expr, HirTy ty) implements ExprKind {
    }

    record DropTemps(Expr expr) implements ExprKind {
    }

    record Let(LetExpr let) implements ExprKind {
    }

    record If(Expr cond, Expr then, @Nullable Expr els) implements ExprKind {
    }

    record Loop(Block block, @Nullable Label label, Span span) implements ExprKind {
    }

    record Match(Expr expr, Arm[] arms, MatchSource src) implements ExprKind {
    }

    record Closure(ClosureExpr closure) implements ExprKind {
    }

    record BlockExpr(Block block) implements ExprKind {
    }

    record Assign(Expr left, Expr right, Span span) implements ExprKind {
    }

    record AssignOp(AssignOperator op, Expr left, Expr right) implements ExprKind {
    }

    record Field(Expr expr, Ident field) implements ExprKind {
    }

    record Path(QPath path) implements ExprKind {
    }

    record AddrOf(boolean raw, boolean mut, Expr expr) implements ExprKind {
    }

    record Break(Destination dest, @Nullable Expr expr) implements ExprKind {
    }

    record Continue(Destination dest) implements ExprKind {
    }

    record Ret(@Nullable Expr expr) implements ExprKind {
    }

    record Struct(QPath path, ExprField[] fields, StructTailExpr tail) implements ExprKind {
    }

    record Repeat(Expr expr, ConstArg len) implements ExprKind {
    }

    record Yield(Expr expr, YieldSource src) implements ExprKind {
    }

    record Index(Expr base, Expr idx, Span span) implements ExprKind {
    }

    class Adapter extends HirAdapter<ExprKind> {
        @Override
        public @Nullable Class<? extends ExprKind> getType(String tag) {
            return switch (tag) {
                case "ConstBlock" -> ConstBlock.class;
                case "Array" -> Array.class;
                case "MethodCall" -> MethodCall.class;
                case "Tup" -> Tup.class;
                case "Call" -> Call.class;
                case "Binary" -> Binary.class;
                case "Unary" -> Unary.class;
                case "Lit" -> LitExpr.class;
                case "Cast" -> CastExpr.class;
                case "DropTemps" -> DropTemps.class;
                case "Let" -> Let.class;
                case "If" -> If.class;
                case "Loop" -> Loop.class;
                case "Match" -> Match.class;
                case "Closure" -> Closure.class;
                case "Block" -> BlockExpr.class;
                case "Assign" -> Assign.class;
                case "AssignOp" -> AssignOp.class;
                case "Field" -> Field.class;
                case "Index" -> Index.class;
                case "Path" -> Path.class;
                case "AddrOf" -> AddrOf.class;
                case "Break" -> Break.class;
                case "Continue" -> Continue.class;
                case "Ret" -> Ret.class;
                case "Struct" -> Struct.class;
                case "Repeat" -> Repeat.class;
                case "Yield" -> Yield.class;
                default -> null;
            };
        }
    }
}
