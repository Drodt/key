/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.item;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface WherePredicateKind {
    record Bound(WhereBoundPredicate pred) implements WherePredicateKind {
    }
    record Region(WhereRegionPredicate pred) implements WherePredicateKind {
    }
    record Eq(WhereEqPredicate pred) implements WherePredicateKind {
    }

    class Adapter extends HirAdapter<WherePredicateKind> {
        @Override
        public @Nullable Class<? extends WherePredicateKind> getType(String tag) {
            return switch (tag) {
                case "Bound" -> Bound.class;
                case "Region" -> Region.class;
                case "Eq" -> Eq.class;
                default -> null;
            };
        }
    }
}
