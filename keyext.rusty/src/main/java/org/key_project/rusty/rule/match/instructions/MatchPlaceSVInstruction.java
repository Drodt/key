/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.PVPlace;
import org.key_project.rusty.logic.SVPlace;
import org.key_project.rusty.logic.op.MutRef;

import org.jspecify.annotations.Nullable;


public class MatchPlaceSVInstruction extends MatchSchemaVariableInstruction {
    public MatchPlaceSVInstruction(SVPlace place) {
        super(place.getSchemaVariable());
    }

    public MatchResultInfo match(MutRef mr, MatchResultInfo mc, LogicServices services) {
        var place = mr.getPlace();
        if (place instanceof PVPlace pvp) {
            return addInstantiation(
                ((Services) services).getTermBuilder().var(pvp.getProgramVariable()), mc,
                services);
        }
        return null;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo matchConditions, LogicServices services) {
        if (!(actualElement instanceof MutRef mr))
            return null;
        return match(mr, matchConditions, services);
    }
}
