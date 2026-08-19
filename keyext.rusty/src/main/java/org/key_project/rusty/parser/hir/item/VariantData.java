/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.item;

import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.LocalDefId;

import org.jspecify.annotations.Nullable;

public sealed interface VariantData {
    record Struct(FieldDef[] fields, boolean recovered) implements VariantData {
    }

    record Tuple(FieldDef[] def, HirId hirId, LocalDefId localDefId) implements VariantData {
    }

    record Unit(HirId hirId, LocalDefId localDefId) implements VariantData {
    }

    class Adapter extends HirAdapter<VariantData> {
        @Override
        public @Nullable Class<? extends VariantData> getType(String tag) {
            return switch (tag) {
                case "Struct" -> Struct.class;
                case "Tuple" -> Tuple.class;
                case "Unit" -> Unit.class;
                default -> null;
            };
        }
    }
}
