/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.item;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.Ident;
import org.key_project.rusty.parser.hir.Path;
import org.key_project.rusty.parser.hir.Res;

public record Use(Path<Res[]> path, UseKind useKind) implements ItemKind {

    public interface UseKind {
        record Single(Ident ident) implements UseKind {}
        record Glob() implements UseKind {}
        record ListStem()  implements UseKind {}

        class Adapter extends HirAdapter<UseKind> {
            @Override
            public @Nullable Class<? extends UseKind> getType(String tag) {
                return switch (tag) {
                    case "Single" -> Single.class;
                    case "Glob" -> Glob.class;
                    case "ListStem" -> ListStem.class;
                    default -> null;
                };
            }
        }
    }
}
