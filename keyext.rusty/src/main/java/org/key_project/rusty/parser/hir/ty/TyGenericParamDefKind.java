/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface TyGenericParamDefKind {
    record Lifetime() implements TyGenericParamDefKind {
    }

    record Type(boolean hasDefault, boolean synthetic) implements TyGenericParamDefKind {
    }

    record Const(boolean hasDefault, boolean synthetic) implements TyGenericParamDefKind {
    }

    class Adapter extends HirAdapter<TyGenericParamDefKind> {
        @Override
        public @Nullable Class<? extends TyGenericParamDefKind> getType(String tag) {
            return switch (tag) {
                case "Lifetime" -> Lifetime.class;
                case "Type" -> Type.class;
                case "Const" -> Const.class;
                default -> null;
            };
        }
    }

}
