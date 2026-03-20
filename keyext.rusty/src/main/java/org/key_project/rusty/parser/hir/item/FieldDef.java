/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.item;

import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Ident;
import org.key_project.rusty.parser.hir.LocalDefId;
import org.key_project.rusty.parser.hir.Span;
import org.key_project.rusty.parser.hir.hirty.HirTy;

public record FieldDef(Span span, Span visSpan, Ident ident, HirId hirId, LocalDefId defId,
        HirTy ty) {
}
