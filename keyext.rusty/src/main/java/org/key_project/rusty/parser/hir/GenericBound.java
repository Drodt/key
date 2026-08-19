/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.jspecify.annotations.Nullable;

public sealed interface GenericBound {
    record Trait(PolyTraitRef traitRef) implements GenericBound {
    }

    record Outlives(Lifetime lifetime) implements GenericBound {
    }

    record Use(PreciseCapturingArg[] args, Span span) implements GenericBound {
    }

    class Adapter extends HirAdapter<GenericBound> {
        @Override
        public @Nullable Class<? extends GenericBound> getType(String tag) {
            return switch (tag) {
                case "Trait" -> Trait.class;
                case "Outlives" -> Outlives.class;
                case "Use" -> Use.class;
                default -> null;
            };
        }
    }
}
