/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.informationflow.macros;


import java.util.Map;

import de.uka.ilkd.key.control.UserInterfaceControl;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.macros.AbstractProofMacro;
import de.uka.ilkd.key.macros.ProofMacro;
import de.uka.ilkd.key.macros.ProofMacroFinishedInfo;
import de.uka.ilkd.key.macros.ProofMacroListener;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.prover.impl.DefaultTaskStartedInfo;

import org.key_project.logic.PosInTerm;
import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.engine.TaskStartedInfo.TaskKind;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

/**
 * The abstract class ExhaustiveProofMacro can be used to create compound macros which either apply
 * the macro given by {@link #getProofMacro()} directly, or --if not directly applicable-- search on
 * the sequent for any applicable posInOcc and apply it on the first applicable one or --if not
 * applicable anywhere on the sequent-- do not apply it.
 *
 * @author Michael Kirsten
 */
public abstract class ExhaustiveProofMacro extends AbstractProofMacro {

    private PosInOccurrence getApplicablePosInOcc(Proof proof,
            Goal goal, PosInOccurrence posInOcc,
            ProofMacro macro) {
        if (posInOcc == null || posInOcc.subTerm() == null) {
            return null;
        } else if (macro.canApplyTo(proof, ImmutableSLList.<Goal>nil().prepend(goal), posInOcc)) {
            return posInOcc;
        } else {
            final var subTerm = posInOcc.subTerm();
            PosInOccurrence res = null;
            for (int i = 0; i < subTerm.arity() && res == null; i++) {
                res = getApplicablePosInOcc(proof, goal, posInOcc.down(i), macro);
            }
            return res;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.uka.ilkd.key.gui.macros.ProofMacro#getName()
     */
    @Override
    public String getName() {
        return "Apply macro on first applicable position in the sequent.";
    }

    /*
     * (non-Javadoc)
     *
     * @see de.uka.ilkd.key.gui.macros.ProofMacro#getDescription()
     */
    @Override
    public String getDescription() {
        return "Applies specificed macro --if it is applicable anywhere on"
            + "the sequent-- either directly or on the first applicable" + "position found.";
    }

    @Override
    public boolean canApplyTo(Proof proof, ImmutableList<Goal> goals,
            PosInOccurrence posInOcc) {
        final Services services = proof.getServices();

        final Map<Node, PosInOccurrence> applicableOnNodeAtPos =
            services.getCaches().getExhaustiveMacroCache();

        Sequent seq = null;
        boolean applicable = false;
        final ProofMacro macro = getProofMacro();
        for (final Goal goal : goals) {
            seq = goal.sequent();
            synchronized (applicableOnNodeAtPos) {
                if (!applicableOnNodeAtPos.containsKey(goal.node())) {
                    // node has not been checked before, so do it
                    for (int i = 1; i <= seq.size()
                            && applicableOnNodeAtPos.get(goal.node()) == null; i++) {
                        PosInOccurrence searchPos =
                            PosInOccurrence.findInSequent(seq, i,
                                PosInTerm.getTopLevel());
                        PosInOccurrence applicableAt =
                            getApplicablePosInOcc(proof, goal, searchPos, macro);
                        applicableOnNodeAtPos.put(goal.node(), applicableAt);
                    }
                }
            }

            applicable = applicable || applicableOnNodeAtPos.get(goal.node()) != null;
        }
        return applicable;
    }

    @Override
    public ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Proof proof,
            ImmutableList<Goal> goals, PosInOccurrence posInOcc,
            ProverTaskListener listener)
            throws Exception {

        final Map<Node, PosInOccurrence> applicableOnNodeAtPos =
            proof.getServices().getCaches().getExhaustiveMacroCache();
        ProofMacroFinishedInfo info = new ProofMacroFinishedInfo(this, goals);
        final ProofMacro macro = getProofMacro();

        synchronized (applicableOnNodeAtPos) {
            for (final Goal goal : goals) {
                boolean isCached;
                isCached = applicableOnNodeAtPos.containsKey(goal.node());
                if (!isCached) {
                    // node has not been checked before, so do it
                    boolean canBeApplied =
                        canApplyTo(proof, ImmutableSLList.<Goal>nil().prepend(goal), posInOcc);
                    if (!canBeApplied) {
                        // canApplyTo checks all open goals. thus, if it returns
                        // false, then this macro is not applicable at all and
                        // we can return
                        return new ProofMacroFinishedInfo(this, goal);
                    }
                }

                final PosInOccurrence applicableAt;

                applicableAt = applicableOnNodeAtPos.get(goal.node());

                if (applicableAt != null) {
                    final ProverTaskListener pml =
                        new ProofMacroListener(macro.getName(), listener);
                    pml.taskStarted(new DefaultTaskStartedInfo(TaskKind.Macro, getName(), 0));
                    synchronized (macro) {
                        // wait for macro to terminate
                        info = macro.applyTo(uic, proof, ImmutableSLList.<Goal>nil().prepend(goal),
                            applicableAt, pml);
                    }
                    pml.taskFinished(info);
                    info = new ProofMacroFinishedInfo(this, info);
                }
            }
            applicableOnNodeAtPos.clear();
        }
        return info;
    }

    /**
     * Gets the proof macros.
     * <p/>
     *
     * @return the proofMacro.
     */
    abstract ProofMacro getProofMacro();
}
