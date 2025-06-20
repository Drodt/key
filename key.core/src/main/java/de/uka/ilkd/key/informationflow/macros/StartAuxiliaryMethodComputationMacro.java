/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.informationflow.macros;

import de.uka.ilkd.key.control.UserInterfaceControl;
import de.uka.ilkd.key.informationflow.po.InfFlowContractPO;
import de.uka.ilkd.key.informationflow.po.SymbolicExecutionPO;
import de.uka.ilkd.key.informationflow.po.snippet.InfFlowPOSnippetFactory;
import de.uka.ilkd.key.informationflow.po.snippet.POSnippetFactory;
import de.uka.ilkd.key.informationflow.proof.InfFlowProof;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.macros.AbstractProofMacro;
import de.uka.ilkd.key.macros.ProofMacroFinishedInfo;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.ProofOblInput;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.ImmutableList;

import static de.uka.ilkd.key.logic.equality.RenamingTermProperty.RENAMING_TERM_PROPERTY;

/**
 *
 * @author christoph
 */
public class StartAuxiliaryMethodComputationMacro extends AbstractProofMacro
        implements StartSideProofMacro {

    @Override
    public String getName() {
        return "Start auxiliary computation for self-composition proofs";
    }

    @Override
    public String getCategory() {
        return "Information Flow";
    }

    @Override
    public String getDescription() {
        return "In order to increase the efficiency of self-composition "
            + "proofs, this macro starts a side calculation which does "
            + "the symbolic execution only once. The result is "
            + "instantiated twice with the variable to be used in the "
            + "two executions of the self-composition.";
    }

    @Override
    public boolean canApplyTo(Proof proof, ImmutableList<Goal> goals,
            PosInOccurrence posInOcc) {
        if (goals == null || goals.isEmpty()) {
            return false;
        }
        if (posInOcc == null || posInOcc.subTerm() == null) {
            return false;
        }
        final Services services = proof.getServices();
        ProofOblInput poForProof = services.getSpecificationRepository().getProofOblInput(proof);
        if (!(poForProof instanceof InfFlowContractPO po)) {
            return false;
        }

        final InfFlowPOSnippetFactory f = POSnippetFactory.getInfFlowFactory(po.getContract(),
            po.getIFVars().c1, po.getIFVars().c2, services);
        final JTerm selfComposedExec =
            f.create(InfFlowPOSnippetFactory.Snippet.SELFCOMPOSED_EXECUTION_WITH_PRE_RELATION);

        return RENAMING_TERM_PROPERTY.equalsModThisProperty(posInOcc.subTerm(), selfComposedExec);
    }

    @Override
    public ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Proof proof,
            ImmutableList<Goal> goals, PosInOccurrence posInOcc, ProverTaskListener listener)
            throws Exception {
        final Services services = proof.getServices();
        final InfFlowContractPO po =
            (InfFlowContractPO) services.getSpecificationRepository().getProofOblInput(proof);

        final InitConfig initConfig = proof.getEnv().getInitConfigForEnvironment();

        final SymbolicExecutionPO symbExecPO = new SymbolicExecutionPO(initConfig, po.getContract(),
            po.getIFVars().symbExecVars.labelHeapAtPreAsAnonHeapFunc(), goals.head(),
            proof.getServices());

        final InfFlowProof p;
        synchronized (symbExecPO) {
            p = (InfFlowProof) uic.createProof(initConfig, symbExecPO);
        }
        p.unionIFSymbols(((InfFlowProof) proof).getIFSymbols());

        ProofMacroFinishedInfo info = new ProofMacroFinishedInfo(this, p);
        info.addInfo(PROOF_MACRO_FINISHED_INFO_KEY_ORIGINAL_PROOF, proof);
        return info;
    }
}
