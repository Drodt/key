/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.HirAdapter;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

public sealed interface GenericTyArgKind {
    record Lifetime() implements GenericTyArgKind {
    }

    record Type(Ty ty) implements GenericTyArgKind {
    }

    record Const(@SerializedName("const") TyConst _const) implements GenericTyArgKind {
    }

    class Adapter extends HirAdapter<GenericTyArgKind> {
        @Override
        public @Nullable Class<? extends GenericTyArgKind> getType(String tag) {
            return switch (tag) {
                case "Lifetime" -> Lifetime.class;
                case "Type" -> Type.class;
                case "Const" -> Const.class;
                default -> null;
            };
        }
    }
}
