package org.key_project.rusty.parser.hir.ty;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.expr.BinOpKind;
import org.key_project.rusty.parser.hir.expr.UnOp;

public interface ConstExprKind {
    record Binop(BinOpKind kind) implements ConstExprKind {}

    record Unop(UnOp kind) implements ConstExprKind {}

    record FunctionCall() implements ConstExprKind {}

    record Cast(CastKind kind) implements ConstExprKind {}

    class Adapter extends HirAdapter<ConstExprKind> {
        @Override
        public @Nullable Class<? extends ConstExprKind> getType(String tag) {
            return switch (tag) {
                case "Binop" -> Binop.class;
                case "Unop" -> Unop.class;
                case "FunctionCall" -> FunctionCall.class;
                case "Cast" -> Cast.class;
                default -> null;
            };
        }
    }
}
