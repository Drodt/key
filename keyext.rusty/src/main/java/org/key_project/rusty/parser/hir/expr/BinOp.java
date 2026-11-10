/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.Span;

public record BinOp(BinOpKind node, Span span) {
}
