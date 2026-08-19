/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.jspecify.annotations.Nullable;

public sealed interface AssocItemConstraintKind {
    record Equality() implements AssocItemConstraintKind {
    }

    record Bound(GenericBound[] bounds) implements AssocItemConstraintKind {
    }

    class Adapter extends HirAdapter<AssocItemConstraintKind> {
        @Override
        public @Nullable Class<? extends AssocItemConstraintKind> getType(String tag) {
            return switch (tag) {
                case "Equality" -> Equality.class;
                case "Bound" -> Bound.class;
                default -> null;
            };
        }
    }
}
