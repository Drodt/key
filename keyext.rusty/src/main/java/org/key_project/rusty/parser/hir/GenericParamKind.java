/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.key_project.rusty.parser.hir.hirty.HirTy;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

public interface GenericParamKind {
    record Lifetime(LifetimeParamKind kind) implements GenericParamKind {
    }

    record Type(@SerializedName("default") @Nullable HirTy _default, boolean synthetic)
            implements GenericParamKind {
    }

    record Const(HirTy ty, @SerializedName("default") @Nullable ConstArg _default,
            boolean synthetic) implements GenericParamKind {
    }

    class Adapter extends HirAdapter<GenericParamKind> {
        @Override
        public @Nullable Class<? extends GenericParamKind> getType(String tag) {
            return switch (tag) {
                case "Lifetime" -> Lifetime.class;
                case "Type" -> Type.class;
                case "Const" -> Const.class;
                default -> null;
            };
        }
    }
}
