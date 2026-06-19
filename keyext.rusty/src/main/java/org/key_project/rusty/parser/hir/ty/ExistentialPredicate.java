/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.DefId;
import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public sealed interface ExistentialPredicate {
    record Trait(ExistentialTraitRef pred) implements ExistentialPredicate {

    }

    record Projection(ExistentialProjection pred) implements ExistentialPredicate {

    }

    record AutoTrait(DefId defId) implements ExistentialPredicate {
    }

    class Adapter extends HirAdapter<ExistentialPredicate> {
        @Override
        public @Nullable Class<? extends ExistentialPredicate> getType(String tag) {
            return switch (tag) {
                case "Trait" -> Trait.class;
                case "Projection" -> Projection.class;
                case "AutoTrait" -> AutoTrait.class;
                default -> null;
            };
        }
    }
}
