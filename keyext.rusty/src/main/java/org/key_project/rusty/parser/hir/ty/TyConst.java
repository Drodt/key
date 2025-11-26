/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface TyConst {
    record Param(ParamConst pc) implements TyConst {}

    record Infer() implements TyConst {}

    record Bound(int idx, int boundVar) implements TyConst {}

    record Placholder() implements TyConst {}

    record Unevaluated(UnevaluatedConst uc) implements TyConst {}

    record ValueConst(Value value) implements TyConst {
    }

    record Expr(ConstExpr expr) implements TyConst{}

    class Adapter extends HirAdapter<TyConst> {
        @Override
        public @Nullable Class<? extends TyConst> getType(String tag) {
            return switch (tag) {
                case "Param" -> Param.class;
                case "Infer" -> Infer.class;
                case "Bound" -> Bound.class;
                case "Placholder" -> Placholder.class;
                case "Unevaluated" -> Unevaluated.class;
                case "Value" -> ValueConst.class;
                case "Expr" -> Expr.class;
                default -> null;
            };
        }
    }
}
