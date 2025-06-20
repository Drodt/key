/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.match.vm.instructions;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.op.TermLabelSV;
import de.uka.ilkd.key.rule.MatchConditions;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.match.vm.TermNavigator;

import org.key_project.logic.LogicServices;
import org.key_project.util.collection.ImmutableArray;

/**
 * This match instruction implements the matching logic for term labels.
 */
public class MatchTermLabelInstruction implements MatchInstruction {

    private final ImmutableArray<TermLabel> labels;

    public MatchTermLabelInstruction(ImmutableArray<TermLabel> labels) {
        this.labels = labels;
    }

    private MatchConditions match(TermLabelSV sv, JTerm instantiationCandidate,
            MatchConditions matchCond, LogicServices services) {

        final SVInstantiations svInsts = matchCond.getInstantiations();
        final ImmutableArray<TermLabel> inst =
            (ImmutableArray<TermLabel>) svInsts.getInstantiation(sv);

        if (inst == null) {
            return matchCond.setInstantiations(
                svInsts.add(sv, instantiationCandidate.getLabels(), TermLabel.class, services));
        } else {
            for (TermLabel o : inst) {
                if (!instantiationCandidate.containsLabel(o)) {
                    return null;
                }
            }
            return matchCond;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatchConditions match(TermNavigator termPosition, MatchConditions matchConditions,
            LogicServices services) {
        final JTerm term = termPosition.getCurrentSubterm();
        MatchConditions result = matchConditions;
        // TODO: Define a sane version of taclet matching for term labels
        // at the moment any termlabbel SV matches on all labels (or no label) (i.e., t<l1,l2> will
        // match l1 and l2 against all labels and both will have
        // all labels of the concret term as instantiation)
        for (int i = 0; i < labels.size() && result != null; i++) {
            final TermLabel templateLabel = labels.get(i);
            // ignore all labels which are not schema variables
            // if intended to match concrete label, match against schema label
            // and use an appropriate variable condition
            if (templateLabel instanceof TermLabelSV) {
                result = match((TermLabelSV) templateLabel, term, result, services);
            }
        }
        return result;
    }

}
