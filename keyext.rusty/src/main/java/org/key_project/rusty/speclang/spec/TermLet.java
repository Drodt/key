/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.speclang.spec;

import org.key_project.rusty.parser.hir.Span;
import org.key_project.rusty.parser.hir.pat.Pat;
import org.key_project.rusty.parser.hir.ty.Ty;

import org.jspecify.annotations.Nullable;

public record TermLet(Span span, Pat pat, @Nullable Ty ty, Term init) {
}
