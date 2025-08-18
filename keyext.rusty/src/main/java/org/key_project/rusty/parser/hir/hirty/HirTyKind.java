/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.hirty;

import org.key_project.rusty.parser.hir.ConstArg;
import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.QPath;
import org.key_project.rusty.parser.hir.item.Item;

import org.jspecify.annotations.Nullable;

public interface HirTyKind {
    // InferDelegation?

    record Slice(HirTy ty) implements HirTyKind {
    }

    record Array(HirTy ty, ConstArg len) implements HirTyKind {
    }

    record Ptr(HirTy ty) implements HirTyKind {
    }

    record Ref(MutHirTy ty) implements HirTyKind {
    }

    record BareFn(BareFnHirTy ty) implements HirTyKind {
    }

    record Never() implements HirTyKind {
    }

    record Tup(HirTy[] tys) implements HirTyKind {
    }

    record AnonAdt(Item item) implements HirTyKind {
    }

    record Path(QPath path) implements HirTyKind {
    }

    // TraitObject?

    class Adapter extends HirAdapter<HirTyKind> {
        @Override
        public @Nullable Class<? extends HirTyKind> getType(String tag) {
            return switch (tag) {
                case "Slice" -> Slice.class;
                case "Array" -> Array.class;
                case "Ptr" -> Ptr.class;
                case "Ref" -> Ref.class;
                case "BareFn" -> BareFn.class;
                case "Never" -> Never.class;
                case "Tup" -> Tup.class;
                case "AnonAdt" -> AnonAdt.class;
                case "Path" -> Path.class;
                default -> null;
            };
        }
    }
}
