/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.GenericParam;
import org.key_project.rusty.parser.hir.LocalDefId;
import org.key_project.rusty.parser.hir.Span;
import org.key_project.rusty.parser.hir.item.Body;
import org.key_project.rusty.parser.hir.item.FnDecl;

import org.jspecify.annotations.Nullable;

public record ClosureExpr(LocalDefId defId, ClosureBinder binder, boolean constness,
        CaptureBy captureClause, GenericParam[] boundGenericParams,
        FnDecl fnDecl, Body body, Span fnDeclSpan, @Nullable Span fnArgSpan) {
}
