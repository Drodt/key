/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy;

import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.feature.SumFeature;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.strategy.feature.RuleSetDispatchFeature;
import org.key_project.rusty.strategy.quantifierHeuristics.ClausesSmallerThanFeature;

public class RFOLStrategy extends FOLStrategy {
    public RFOLStrategy(Proof proof, StrategyProperties strategyProperties) {
        super(proof, strategyProperties);
    }

    @Override
    protected void setupFormulaNormalisation(RuleSetDispatchFeature d) {
        super.setupFormulaNormalisation(d);
        var numbers = getServices().getLDTs().getIntLDT();
        bindRuleSet(d, "cnf_orComm",
            SumFeature.createSum(applyTF("commRight", ff.clause),
                applyTFNonStrict("commResidue", ff.clauseSet),
                or(applyTF("commLeft", ff.andF),
                    add(applyTF("commLeft", ff.literal),
                        literalsSmallerThan("commRight", "commLeft", numbers))),
                longConst(-100)));
        bindRuleSet(d, "cnf_andComm",
            SumFeature.createSum(applyTF("commLeft", ff.clause),
                applyTF("commRight", ff.clauseSet), applyTFNonStrict("commResidue", ff.clauseSet),
                // at least one of the subformulas has to be a literal;
                // otherwise, sorting is not likely to have any big effect
                ifZero(
                    add(applyTF("commLeft", not(ff.literal)),
                        applyTF("commRight", rec(ff.andF, not(ff.literal)))),
                    longConst(100), longConst(-60)),
                clausesSmallerThan("commRight", "commLeft", numbers)));
    }

    private Feature clausesSmallerThan(String smaller, String bigger, IntLDT numbers) {
        return ClausesSmallerThanFeature.create(instOf(smaller), instOf(bigger), numbers);
    }
}
