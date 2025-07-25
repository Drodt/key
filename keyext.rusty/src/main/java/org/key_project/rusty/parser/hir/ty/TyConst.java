/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface TyConst {
    record ValueConst(Value value) implements TyConst {
    }

    class Adapter extends HirAdapter<TyConst> {
        @Override
        public @Nullable Class<? extends TyConst> getType(String tag) {
            return switch (tag) {
                case "Value" -> ValueConst.class;
                default -> null;
            };
        }
    }
}
