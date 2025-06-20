/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.informationflow.po;

import java.util.List;
import java.util.Map;

import de.uka.ilkd.key.informationflow.po.snippet.BasicPOSnippetFactory;
import de.uka.ilkd.key.informationflow.po.snippet.POSnippetFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.reference.ExecutionContext;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.*;
import de.uka.ilkd.key.settings.Configuration;
import de.uka.ilkd.key.speclang.BlockContract;
import de.uka.ilkd.key.speclang.ContractFactory;

import org.key_project.logic.Named;
import org.key_project.util.collection.ImmutableList;


/**
 *
 * @author christoph
 */
public class BlockExecutionPO extends AbstractInfFlowPO implements InfFlowCompositePO {

    private final BlockContract contract;
    private final ProofObligationVars symbExecVars;
    private final Goal initiatingGoal;
    private final ExecutionContext context;

    /**
     * For saving and loading Information-Flow proofs, we need to remember the according taclets,
     * program variables, functions and such.
     */
    private InfFlowProofSymbols infFlowSymbols = new InfFlowProofSymbols();

    /**
     * To be used only for auxiliary proofs where the services object of the actual proof has to be
     * used instead of the initial services form the InitConfig.
     */
    public BlockExecutionPO(InitConfig initConfig, BlockContract contract,
            ProofObligationVars symbExecVars, Goal initiatingGoal, ExecutionContext context,
            Services services) {
        this(initConfig, contract, symbExecVars, initiatingGoal, context);
        this.environmentServices = services;
    }

    public BlockExecutionPO(InitConfig initConfig, BlockContract contract,
            ProofObligationVars symbExecVars, Goal initiatingGoal, ExecutionContext context) {
        super(initConfig,
            ContractFactory.generateContractName(contract.getName(), contract.getKJT(),
                contract.getTarget(), contract.getTarget().getContainerType(),
                contract.getBlock().getStartPosition().line()));
        this.contract = contract;
        this.symbExecVars = symbExecVars;
        this.initiatingGoal = initiatingGoal;
        this.context = context;
    }

    @Override
    public void readProblem() throws ProofInputException {
        postInit();

        // generate snippet factory for symbolic execution
        BasicPOSnippetFactory symbExecFactory =
            POSnippetFactory.getBasicFactory(contract, symbExecVars, context, environmentServices);

        // symbolic execution
        final JTerm symExec =
            symbExecFactory.create(BasicPOSnippetFactory.Snippet.BLOCK_EXEC_WITH_PRE);

        // final symbolic execution term
        final JTerm finalTerm = tb.applyElementary(symbExecVars.pre.heap, tb.not(symExec));

        // register final term
        assignPOTerms(finalTerm);

        // add class axioms
        final Proof initiatingProof = initiatingGoal.proof();
        if (initiatingProof != null) {
            // proof is not loaded
            final AbstractOperationPO initiatingPO =
                (AbstractOperationPO) specRepos.getProofOblInput(initiatingProof);
            taclets = initiatingPO.getInitialTaclets();
        }
    }

    @Override
    public boolean implies(ProofOblInput po) {
        if (!(po instanceof BlockExecutionPO cPO)) {
            return false;
        }
        return contract.equals(cPO.contract);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected String buildPOName(boolean transactionFlag) {
        return contract.getName();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected IProgramMethod getProgramMethod() {
        return contract.getTarget();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isTransactionApplicable() {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected KeYJavaType getCalleeKeYJavaType() {
        return contract.getKJT();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected JModality.JavaModalityKind getTerminationMarker() {
        return contract.getModalityKind();
    }


    public Goal getInitiatingGoal() {
        return initiatingGoal;
    }


    public ExecutionContext getExecutionContext() {
        return context;
    }


    // public IFProofObligationVars getIFProofObligationVars() {
    // return if
    // }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Configuration createLoaderConfig() {
        var c = super.createLoaderConfig();
        c.set("Non-interference contract", contract.getUniqueName());
        return c;
    }

    @Override
    public InfFlowProofSymbols getIFSymbols() {
        assert infFlowSymbols != null;
        return infFlowSymbols;
    }

    @Override
    public void addIFSymbol(JTerm t) {
        assert t != null;
        infFlowSymbols.add(t);
    }

    @Override
    public void addIFSymbol(Named n) {
        assert n != null;
        infFlowSymbols.add(n);
    }

    @Override
    public void addLabeledIFSymbol(JTerm t) {
        assert t != null;
        infFlowSymbols.addLabeled(t);
    }

    @Override
    public void addLabeledIFSymbol(Named n) {
        assert n != null;
        infFlowSymbols.addLabeled(n);
    }

    @Override
    public void unionLabeledIFSymbols(InfFlowProofSymbols symbols) {
        assert symbols != null;
        infFlowSymbols = infFlowSymbols.unionLabeled(symbols);
    }

    @Override
    protected JTerm getGlobalDefs(LocationVariable heap, JTerm heapTerm, JTerm selfTerm,
            ImmutableList<JTerm> paramTerms, Services services) {
        // information flow contracts do not have global defs
        return null;
    }



    @Override
    public AbstractInfFlowPO getChildPO() {
        Proof initiatingProof = getInitiatingGoal().proof();
        Services initiatingServices = initiatingProof.getServices();
        ProofOblInput initiatingPO =
            initiatingServices.getSpecificationRepository().getProofOblInput(initiatingProof);
        assert initiatingPO instanceof AbstractInfFlowPO : "Information flow auxiliary "
            + "proof started from within non-information flow proof!?!";
        return (AbstractInfFlowPO) initiatingPO;
    }


    @Override
    public IFProofObligationVars getLeafIFVars() {
        return getChildPO().getLeafIFVars();
    }


    // the following code is legacy code
    @Override
    @Deprecated
    protected ImmutableList<StatementBlock> buildOperationBlocks(
            ImmutableList<LocationVariable> formalParVars, ProgramVariable selfVar,
            ProgramVariable resultVar, Services services) {
        throw new UnsupportedOperationException(
            "Not supported any more. " + "Please use the POSnippetFactory instead.");
    }


    @Override
    @Deprecated
    protected JTerm getPre(List<LocationVariable> modHeaps, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars,
            Map<LocationVariable, LocationVariable> atPreVars, Services services) {
        throw new UnsupportedOperationException(
            "Not supported any more. " + "Please use the POSnippetFactory instead.");
    }


    @Override
    @Deprecated
    protected JTerm getPost(List<LocationVariable> modHeaps, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars, LocationVariable resultVar,
            LocationVariable exceptionVar, Map<LocationVariable, LocationVariable> atPreVars,
            Services services) {
        throw new UnsupportedOperationException(
            "Not supported any more. " + "Please use the POSnippetFactory instead.");
    }


    @Override
    @Deprecated
    protected JTerm buildFrameClause(List<LocationVariable> modHeaps, Map<JTerm, JTerm> heapToAtPre,
            LocationVariable selfVar, ImmutableList<LocationVariable> paramVars,
            Services services) {
        throw new UnsupportedOperationException(
            "Not supported any more. " + "Please use the POSnippetFactory instead.");
    }


    @Override
    @Deprecated
    protected JTerm generateMbyAtPreDef(LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars, Services services) {
        throw new UnsupportedOperationException(
            "Not supported any more. " + "Please use the POSnippetFactory instead.");
    }
}
