/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match.instructions;

import java.util.Set;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.rusty.logic.op.sv.ModalOperatorSV;
import org.key_project.rusty.rule.inst.SVInstantiations;

/// Matches a modal-operator schema variable against a concrete Java modality kind: the source kind
/// must be one of the kinds the schema variable ranges over, and the schema variable is
/// instantiated with it.
public final class MatchModalOperatorSVInstruction implements MatchInstruction {
    private final Set<RModality.RustyModalityKind> modalityKinds;
    private final ModalOperatorSV modalitySV;

    public MatchModalOperatorSVInstruction(ModalOperatorSV mod) {
        this.modalitySV = mod;
        this.modalityKinds = modalitySV.getModalities().toSet();
    }

    @Override
    public MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo mc, LogicServices services) {
        if (actualElement instanceof RModality.RustyModalityKind kind
                && modalityKinds.contains(kind)) {
            final SVInstantiations instantiations = (SVInstantiations) mc.getInstantiations();
            // instantiate or agree: a schema variable that is already instantiated matches only
            // the same modality kind again (SVInstantiations.add would silently overwrite)
            final Object inst = instantiations.getInstantiation(modalitySV);
            if (inst == null) {
                return mc.setInstantiations(instantiations.add(modalitySV, kind, services));
            }
            return inst.equals(kind) ? mc : null;
        } else {
            return null;
        }
    }
}
