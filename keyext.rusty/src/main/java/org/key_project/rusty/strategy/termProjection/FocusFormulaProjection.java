/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.termProjection;

import org.key_project.logic.Term;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.proof.Goal;

public class FocusFormulaProjection implements ProjectionToTerm<Goal> {
    public static final ProjectionToTerm<Goal> INSTANCE = new FocusFormulaProjection();

    private FocusFormulaProjection() {}

    @Override
    public Term toTerm(RuleApp app, PosInOccurrence pos, Goal goal, MutableState mutableState) {
        assert pos != null : "Projection is only applicable to rules with find";

        return pos.sequentFormula().formula();
    }
}
