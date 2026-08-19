/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.item;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public sealed interface ItemKind permits Const, Enum, ExternCrate, Fn, Struct, Use {
    class Adapter extends HirAdapter<ItemKind> {
        @Override
        public @Nullable Class<? extends ItemKind> getType(String tag) {
            return switch (tag) {
                case "Use" -> Use.class;
                case "ExternCrate" -> ExternCrate.class;
                case "Fn" -> Fn.class;
                case "Const" -> Const.class;
                case "Struct" -> Struct.class;
                case "Enum" -> Enum.class;
                default -> null;
            };
        }
    }
}
