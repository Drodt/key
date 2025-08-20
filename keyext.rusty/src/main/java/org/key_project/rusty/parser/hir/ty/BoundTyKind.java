/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.DefId;

public interface BoundTyKind {
    record Anon() implements BoundTyKind {
    }

    record Param(DefId defId, String symbol) implements BoundTyKind {
    }
}
