/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.pat;

import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Ident;
import org.key_project.rusty.parser.hir.QPath;
import org.key_project.rusty.parser.hir.expr.Expr;

import org.jspecify.annotations.Nullable;

public interface PatKind {
    record Wild() implements PatKind {
    }

    record Binding(BindingMode mode, HirId hirId, Ident ident, @Nullable Pat pat)
            implements PatKind {
    }

    record Struct(QPath path, PatField[] fields, boolean rest) implements PatKind {
    }

    record TupleStruct(QPath path, Pat[] pats, int dotDotPos) implements PatKind {
    }

    record Or(Pat[] pats) implements PatKind {
    }

    record Never() implements PatKind {
    }

    record Path(QPath path) implements PatKind {
    }

    record Tuple(Pat[] pats, int dotDotPos) implements PatKind {
    }

    record Box(Pat pat) implements PatKind {
    }

    record Deref(Pat pat) implements PatKind {
    }

    record Ref(Pat pat, boolean mut) implements PatKind {
    }

    record Lit(Expr expr) implements PatKind {
    }

    record Range(@Nullable PatExpr lhs, @Nullable PatExpr rhs, boolean inclusive)
            implements PatKind {
    }

    record Slice(Pat[] start, @Nullable Pat mid, Pat[] end) implements PatKind {
    }

    record Expr(PatExpr expr) implements PatKind {
    }

    record Guard(Pat pat, Expr guard) implements PatKind {
    }

    class Adapter extends HirAdapter<PatKind> {
        @Override
        public @Nullable Class<? extends PatKind> getType(String tag) {
            return switch (tag) {
                case "Wild" -> Wild.class;
                case "Binding" -> Binding.class;
                case "Struct" -> Struct.class;
                case "TupleStruct" -> TupleStruct.class;
                case "Or" -> Or.class;
                case "Never" -> Never.class;
                case "Path" -> Path.class;
                case "Tuple" -> Tuple.class;
                case "Box" -> Box.class;
                case "Deref" -> Deref.class;
                case "Ref" -> Ref.class;
                case "Lit" -> Lit.class;
                case "Range" -> Range.class;
                case "Slice" -> Slice.class;
                case "Expr" -> Expr.class;
                case "Guard" -> Guard.class;
                default -> null;
            };
        }
    }
}
