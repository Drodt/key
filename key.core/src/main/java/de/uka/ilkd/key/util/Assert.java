/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.util;

import de.uka.ilkd.key.logic.Sorted;
import de.uka.ilkd.key.speclang.FunctionalOperationContractImpl;

import org.key_project.logic.sort.Sort;

/**
 * Special assert statements intended for use with KeY. Raises
 * {@link AssertionFailure} (which is a subtype of {@link Exception})
 * instead of {@link AssertionError}.
 *
 * @author daniel
 *
 */
public final class Assert {
    private static boolean assertionsEnabled() {
        // This class is only used by FunctionalOperationContractImpl
        return FunctionalOperationContractImpl.class.desiredAssertionStatus();
    }

    /**
     * Check whether two terms (or other sorted objects) are of the same sort.
     *
     * @param t1
     * @param t2
     */
    public static void assertEqualSort(Sorted t1, Sorted t2) {
        if (!assertionsEnabled()) {
            return;
        }
        Sort s1 = t1.sort();
        Sort s2 = t2.sort();
        if (!s1.equals(s2)) {
            throw new AssertionFailure("Assertion failed: The sorts of " + t1 + " and " + t2
                + " were supposed to be equal," + " but are " + s1 + " and " + s2 + ".");
        }
    }

    /**
     * Check whether the sort of t1 is a subsort of the sort of t2. I.e., check whether a
     * substitution of t1 for t2 is legal.
     *
     * @param t1
     * @param t2
     */
    public static void assertSubSort(Sorted t1, Sorted t2) {
        if (!assertionsEnabled()) {
            return;
        }
        Sort s1 = t1.sort();
        Sort s2 = t2.sort();
        if (!s1.extendsTrans(s2)) {
            throw new AssertionFailure(
                "Assertion failed: The sort of " + t1 + " was supposed to be a subsort of " + t2
                    + "'s," + " but are " + s1 + " and " + s2 + ".");
        }
    }
}
