/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.match.vm.instructions;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.rule.MatchConditions;
import de.uka.ilkd.key.rule.match.vm.TermNavigator;

import org.key_project.logic.LogicServices;
import org.key_project.logic.op.Modality;
import org.key_project.logic.op.Operator;

/**
 * The match instruction reports a success if the top level operator of the term to be matched is
 * the <strong>same</strong> modality like the one for which this instruction has been
 * instantiated
 */
public class MatchModalityInstruction extends Instruction<Modality>
        implements MatchOperatorInstruction {

    public MatchModalityInstruction(Modality op) {
        super(op);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MatchConditions match(JTerm t, MatchConditions matchConditions,
            LogicServices services) {
        return match(t.op(), matchConditions, services);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatchConditions match(Operator instantiationCandidate, MatchConditions matchConditions,
            LogicServices services) {
        if (instantiationCandidate instanceof Modality mod1 && mod1.kind() == op.kind()) {
            return matchConditions;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatchConditions match(TermNavigator termPosition, MatchConditions matchConditions,
            LogicServices services) {
        return match(termPosition.getCurrentSubterm(), matchConditions, services);
    }

}
