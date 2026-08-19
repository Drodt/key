/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public sealed interface TyTerm {
    record Type(Ty ty) implements TyTerm {
    }
    record ConstTerm(TyConst c) implements TyTerm {
    }

    class Adapter extends HirAdapter<TyTerm> {
        @Override
        public @Nullable Class<? extends TyTerm> getType(String tag) {
            return switch (tag) {
                case "Type" -> Type.class;
                case "Const" -> ConstTerm.class;
                default -> null;
            };
        }
    }
}
