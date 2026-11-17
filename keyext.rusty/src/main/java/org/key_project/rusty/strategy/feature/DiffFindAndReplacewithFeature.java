/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.feature;

import org.key_project.logic.Term;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.ITacletApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.BinaryTacletAppFeature;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.feature.StableCost;
import org.key_project.rusty.rule.RewriteTaclet;
import org.key_project.rusty.rule.tacletbuilder.RewriteTacletGoalTemplate;

/// Binary feature that returns zero iff the replacewith- and find-parts of a Taclet are matched to
/// different terms.
// Stable: compares the find term against the taclet's replacewith template, both fixed for a given
// rule application.
@StableCost
public class DiffFindAndReplacewithFeature extends BinaryTacletAppFeature {
    /// the single instance of this feature
    public static final Feature INSTANCE = new DiffFindAndReplacewithFeature();

    private DiffFindAndReplacewithFeature() {}

    @Override
    protected boolean filter(ITacletApp app, PosInOccurrence pos, ProofGoal<?> goal,
            MutableState mState) {
        assert pos != null && app.rule() instanceof RewriteTaclet
                : "Feature is only applicable to rewrite taclets";

        var rwt = (RewriteTaclet) app.rule();
        for (var template : rwt.goalTemplates()) {
            final Term replaceWith = ((RewriteTacletGoalTemplate) template).replaceWith();
            if (replaceWith.equals(pos.subTerm())) {
                return false;
            }
        }
        return true;
    }
}
