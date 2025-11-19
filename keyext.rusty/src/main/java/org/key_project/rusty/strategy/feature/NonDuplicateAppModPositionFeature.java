/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.feature;

import org.key_project.logic.Term;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;

/// Binary feature that returns zero iff a certain Taclet app has not already been performed
public class NonDuplicateAppModPositionFeature extends NonDuplicateAppFeature {
    public static final Feature INSTANCE = new NonDuplicateAppModPositionFeature();

    @Override
    protected boolean comparePio(TacletApp newApp, TacletApp oldApp,
            PosInOccurrence newPio,
            PosInOccurrence oldPio) {
        final Term newFocus = newPio.subTerm();
        final Term oldFocus = oldPio.subTerm();
        if (!newFocus.equals(oldFocus)) {
            return false;
        }

        if (newFocus.isRigid()) {
            return true;
        }

        final ImmutableList<Term> oldUpdateContext =
            oldApp.instantiations().getUpdateContext();
        final ImmutableList<Term> newUpdateContext =
            newApp.instantiations().getUpdateContext();
        return oldUpdateContext.equals(newUpdateContext);
    }
}
