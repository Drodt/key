/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.logic.op;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.ParsableVariable;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.util.collection.WeakValueInterner;

import org.jspecify.annotations.NonNull;


/// The objects of this class represent logical variables, used e.g. for quantification.
/// It uses De Brujin indices instead of names.
///
/// This class is for occurrences of logic variables in formulas/terms. For the class
/// for definition of logical variables {@see BoundVariable}.
public final class LogicVariable extends AbstractSortedOperator
        implements QuantifiableVariable, ParsableVariable {
    private static final WeakValueInterner<LogicVariable, LogicVariable> CACHE =
        new WeakValueInterner<>();

    private final int index;

    public static LogicVariable create(int index, Sort sort) {
        var lv = new LogicVariable(index, sort);
        return CACHE.intern(lv, l -> l);
    }

    private LogicVariable(int index, Sort sort) {
        super(new Name("@" + index), sort, Modifier.RIGID);
        assert sort != RustyDLTheory.FORMULA;
        assert sort != RustyDLTheory.UPDATE;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public @NonNull String toString() {
        return name() + ":" + sort();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Logic variable " + name() + " does not have children");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        LogicVariable that = (LogicVariable) o;
        return index == that.index && sort().equals(that.sort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, sort());
    }
}
