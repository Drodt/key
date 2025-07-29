/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.rule.VariableConditionAdapter;
import org.key_project.rusty.rule.inst.SVInstantiations;

/// General varcond for checking relationships between types of schema variables.
public final class TypeComparisonCondition extends VariableConditionAdapter {
    public enum Mode {
        NOT_SAME, /* checks if sorts are not same */
        SAME, /* checks if sorts are same */
        IS_SUBTYPE, /* checks subtype relationship */
        NOT_IS_SUBTYPE, /* checks subtype relationship */
        STRICT_SUBTYPE, /* checks for strict subtype */
    } /* checks if sorts are disjoint */

    private final Mode mode;
    private final TypeResolver fst;
    private final TypeResolver snd;


    /// creates a condition that checks if the declaration types of the schemavariable's
    /// instantiations are unequal
    ///
    /// @param fst one of the SchemaVariable whose type is checked
    /// @param snd one of the SchemaVariable whose type is checked
    /// @param mode an int encoding if testing of not same or not compatible
    public TypeComparisonCondition(TypeResolver fst, TypeResolver snd, Mode mode) {
        this.fst = fst;
        this.snd = snd;
        this.mode = mode;
    }

    public TypeResolver getFirstResolver() {
        return fst;
    }

    public TypeResolver getSecondResolver() {
        return snd;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement subst, SVInstantiations svInst,
            Services services) {

        if (!fst.isComplete(var, subst, svInst, services)
                || !snd.isComplete(var, subst, svInst, services)) {
            // not yet complete
            return true;
        }
        Sort fstSort = fst.resolveSort(var, subst, svInst, services);
        Sort sndSort = snd.resolveSort(var, subst, svInst, services);

        return checkSorts(fstSort, sndSort, services);
    }

    private boolean checkSorts(final Sort fstSort, final Sort sndSort, final Services services) {
        // This is the standard case where no proxy sorts are involved
        return switch (mode) {
            case SAME -> fstSort == sndSort;
            case NOT_SAME -> fstSort != sndSort;
            case IS_SUBTYPE -> fstSort.extendsTrans(sndSort);
            case STRICT_SUBTYPE -> fstSort != sndSort && fstSort.extendsTrans(sndSort);
            case NOT_IS_SUBTYPE -> !fstSort.extendsTrans(sndSort);
        };

    }


    @Override
    public String toString() {
        return switch (mode) {
            case SAME -> "\\same(" + fst + ", " + snd + ")";
            case NOT_SAME -> "\\not\\same(" + fst + ", " + snd + ")";
            case IS_SUBTYPE -> "\\sub(" + fst + ", " + snd + ")";
            case STRICT_SUBTYPE -> "\\strict\\sub(" + fst + ", " + snd + ")";
            case NOT_IS_SUBTYPE -> "\\not\\sub(" + fst + ", " + snd + ")";
            default -> "invalid type comparison mode";
        };
    }
}
