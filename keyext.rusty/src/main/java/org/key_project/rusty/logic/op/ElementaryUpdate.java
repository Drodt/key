/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.logic.op;


import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.util.collection.WeakValueInterner;

import org.jspecify.annotations.NonNull;

public class ElementaryUpdate extends AbstractSortedOperator {
    /// Interns elementary updates so that the same left-hand side yields the same operator object
    /// (the rest of the system relies on this identity). Thread-safe: two concurrent workers must
    /// not
    /// end up with two distinct operators for the same left-hand side.
    private static final WeakValueInterner<UpdateableOperator, ElementaryUpdate> instances =
        new WeakValueInterner<>();

    private final UpdateableOperator lhs;

    private ElementaryUpdate(UpdateableOperator lhs) {
        super(new Name("elem-update(" + lhs + ")"), new Sort[] { lhs.sort() }, RustyDLTheory.UPDATE,
            Modifier.NONE);
        this.lhs = lhs;
        assert lhs.arity() == 0;
    }

    /// Returns the elementary update operator for the passed left hand side.
    public static ElementaryUpdate getInstance(UpdateableOperator lhs) {
        return instances.intern(lhs, ElementaryUpdate::new);
    }

    /// Returns the left hand side of this elementary update operator.
    public UpdateableOperator lhs() {
        return lhs;
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return lhs;
        throw new IndexOutOfBoundsException("Elementary updates only contain 1 child");
    }
}
