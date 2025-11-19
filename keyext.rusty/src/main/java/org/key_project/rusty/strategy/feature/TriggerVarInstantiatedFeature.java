/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.feature;

import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.rusty.strategy.termProjection.SVInstantiationProjection;

public class TriggerVarInstantiatedFeature extends BinaryTacletAppFeature {
    public static final TriggerVarInstantiatedFeature INSTANCE =
        new TriggerVarInstantiatedFeature();

    private TriggerVarInstantiatedFeature() {}

    @Override
    protected boolean filter(TacletApp app, PosInOccurrence pos, Goal goal,
            MutableState mState) {
        assert app.taclet().hasTrigger();

        var instProj =
            SVInstantiationProjection.create(app.taclet().getTrigger().triggerVar().name(), false);
        return instProj.toTerm(app, pos, goal, mState) != null;
    }
}
