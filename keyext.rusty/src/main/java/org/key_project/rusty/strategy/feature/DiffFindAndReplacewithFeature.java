/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.feature;

import org.key_project.logic.Term;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.RewriteTaclet;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.rusty.rule.tacletbuilder.RewriteTacletGoalTemplate;

/// Binary feature that returns zero iff the replacewith- and find-parts of a Taclet are matched to
/// different terms.
public class DiffFindAndReplacewithFeature extends BinaryTacletAppFeature {
    /// the single instance of this feature
    public static final Feature INSTANCE = new DiffFindAndReplacewithFeature();

    private DiffFindAndReplacewithFeature() {}

    @Override
    protected boolean filter(TacletApp app, PosInOccurrence pos, Goal goal, MutableState mState) {
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
