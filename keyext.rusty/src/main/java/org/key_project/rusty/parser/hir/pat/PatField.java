/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.pat;

import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Ident;
import org.key_project.rusty.parser.hir.Span;

public record PatField(HirId hirId, Ident ident, Pat pat, boolean isShorthand, Span span)
        implements PatKind {
}
