/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.RustyDLTheory;

import org.jspecify.annotations.NonNull;


/// This class defines the logic equality operator `=`. It is a binary predicate accepting
/// arbitrary terms (of sort "any") as arguments.
/// It also defines the formula equivalence operator `<->` (which could alternatively be seen
/// as a Junctor).
public final class Equality extends AbstractSortedOperator {

    /// the usual 'equality' operator '='
    public static final Equality EQUALS = new Equality(new Name("equals"), RustyDLTheory.ANY);

    /// the usual 'equivalence' operator `<->` (be `A, B` formulae then `A <-> B`
    /// is true
    /// if and only if `A` and `B` have the same truth value
    public static final Equality EQV = new Equality(new Name("equiv"), RustyDLTheory.FORMULA);


    private Equality(Name name, Sort targetSort) {
        super(name, new Sort[] { targetSort, targetSort }, RustyDLTheory.FORMULA, Modifier.RIGID);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException(name() + " has no children");
    }
}
