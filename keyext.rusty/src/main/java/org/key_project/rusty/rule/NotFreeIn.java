/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule;

import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.rusty.logic.op.sv.SchemaVariable;
import org.key_project.rusty.logic.op.sv.VariableSV;

/**
 * Class contains a pair of SchemaVariables.
 * The first part has to match a {@link QuantifiableVariable},
 * the second one has to match a Term in order to model a pair of the not-free-in relation of a
 * Taclet.
 */
public record NotFreeIn(SchemaVariable first, SchemaVariable second) {
    /**
     * constructs a pair of variables given two SchemaVariables. The first SchemaVariable has to
     * occur bound in the Taclet, while the second one can stand for an arbitrary term of formula,
     * in order to model a pair of the not-free-in relation of a Taclet.
     */
    public NotFreeIn {
        if (!(first instanceof VariableSV)) {
            throw new RuntimeException("Expected a SchemaVariable "
                    + "that has been only allowed to match " + "variables");
        }
    }

    /**
     * returns the first SchemaVariable of the pair. This SchemaVariable has to be matched to a
     * QuantifiableVariable
     */
    @Override
    public SchemaVariable first() {
        return first;
    }

    /**
     * returns the second SchemaVariable of the pair.
     */
    @Override
    public SchemaVariable second() {
        return second;
    }

    public String toString() {
        return "\\notFreeIn(" + first() + "," + second() + ")";
    }

    public boolean equals(Object o) {
        if (!(o instanceof NotFreeIn nfi)) {
            return false;
        }
        return nfi.first == first() && nfi.second == second();
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + first().hashCode();
        result = 37 * result + second().hashCode();
        return result;
    }
}