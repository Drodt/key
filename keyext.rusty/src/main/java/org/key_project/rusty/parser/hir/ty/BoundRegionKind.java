/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.DefId;
import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public sealed interface BoundRegionKind {
    record Anon() implements BoundRegionKind {
    }
    record Named(DefId defId, String symbol) implements BoundRegionKind {
    }
    record ClosureEnv() implements BoundRegionKind {
    }

    class Adapter extends HirAdapter<BoundRegionKind> {
        @Override
        public @Nullable Class<? extends BoundRegionKind> getType(String tag) {
            return switch (tag) {
                case "Anon" -> Anon.class;
                case "Named" -> Named.class;
                case "ClosureEnv" -> ClosureEnv.class;
                default -> null;
            };
        }
    }
}
