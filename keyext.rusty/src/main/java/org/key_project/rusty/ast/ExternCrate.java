/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.Nullable;

/// An `extern crate` item, with optional original crate name if the crate was renamed.
/// E.g., `extern crate foo` or `extern crate foo_bar as foo`.
///
/// @param ident
/// @param origIdent
// spotless:off
public record ExternCrate(String ident, @Nullable String origIdent) implements Item {
    @Override
    public void visit(Visitor v) {

    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
//spotless:on
