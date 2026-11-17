/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.feature;

import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.ITacletApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.BinaryTacletAppFeature;
import org.key_project.prover.strategy.costbased.termProjection.SVInstantiationProjection;

public class TriggerVarInstantiatedFeature extends BinaryTacletAppFeature {
    public static final TriggerVarInstantiatedFeature INSTANCE =
        new TriggerVarInstantiatedFeature();

    private TriggerVarInstantiatedFeature() {}

    @Override
    protected boolean filter(ITacletApp app, PosInOccurrence pos, ProofGoal<?> goal,
            MutableState mState) {
        assert app.taclet().hasTrigger();

        var instProj =
            SVInstantiationProjection.create(app.taclet().getTrigger().triggerVar().name(), false);
        return instProj.toTerm(app, pos, goal, mState) != null;
    }
}
