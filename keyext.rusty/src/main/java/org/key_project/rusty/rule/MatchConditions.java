/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule;

import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.logic.LogicVariableTable;
import org.key_project.rusty.logic.op.BoundVariable;
import org.key_project.rusty.rule.inst.SVInstantiations;

public class MatchConditions extends MatchResultInfo {
    public static final MatchConditions EMPTY_MATCHCONDITIONS =
        new MatchConditions(SVInstantiations.EMPTY_SVINSTANTIATIONS, LogicVariableTable.EMPTY);

    private final LogicVariableTable lvTable;

    public MatchConditions() {
        super(SVInstantiations.EMPTY_SVINSTANTIATIONS);
        lvTable = LogicVariableTable.EMPTY;
    }

    public MatchConditions(SVInstantiations p_instantiations, LogicVariableTable p_lvTable) {
        super(p_instantiations);
        lvTable = p_lvTable;
    }

    public SVInstantiations getInstantiations() {
        return (SVInstantiations) instantiations;
    }

    public MatchConditions setInstantiations(
            org.key_project.prover.rules.instantiation.SVInstantiations p_instantiations) {
        if (instantiations == p_instantiations) {
            return this;
        } else {
            return new MatchConditions((SVInstantiations) p_instantiations, lvTable);
        }
    }

    public MatchConditions extendLogicVarTable(BoundVariable bv) {
        return new MatchConditions((SVInstantiations) instantiations, lvTable.extend(bv));
    }

    public MatchConditions shrinkLogicVarTable() {
        return new MatchConditions((SVInstantiations) instantiations, lvTable.parent());
    }

    public LogicVariableTable logicVariableTable() {
        return lvTable;
    }
}
