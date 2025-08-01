/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.proof.init;

import java.util.*;
import java.util.Map.Entry;

import de.uka.ilkd.key.java.Expression;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.Statement;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.expression.operator.New;
import de.uka.ilkd.key.java.reference.TypeRef;
import de.uka.ilkd.key.java.statement.MethodBodyStatement;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.label.OriginTermLabel.Origin;
import de.uka.ilkd.key.logic.label.OriginTermLabel.SpecType;
import de.uka.ilkd.key.logic.label.SymbolicExecutionTermLabel;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ConstructorCall;
import de.uka.ilkd.key.rule.metaconstruct.CreateObject;
import de.uka.ilkd.key.rule.metaconstruct.PostWork;
import de.uka.ilkd.key.settings.Configuration;
import de.uka.ilkd.key.speclang.FunctionalOperationContract;

import org.key_project.prover.sequent.Sequent;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static de.uka.ilkd.key.java.KeYJavaASTFactory.declare;

/**
 * <p>
 * The proof obligation for operation contracts.
 * </p>
 * <p>
 * The generated {@link Sequent} has the following form:
 *
 * <pre>
 * {@code
 * ==>
 * <generalAssumptions> &
 * <preconditions>
 * ->
 * <updatesToStoreInitialValues>
 * <modalityStart>
 * exc=null;try {<methodBodyExpand>}catch(java.lang.Throwable e) {exc = e}
 * <modalityEnd>
 * (exc = null & <postconditions > & <optionalUninterpretedPredicate>)
 * }
 * </pre>
 * </p>
 */
public class FunctionalOperationContractPO extends AbstractOperationPO implements ContractPO {
    public static final Map<Boolean, @NonNull String> TRANSACTION_TAGS =
        new LinkedHashMap<>();

    private final FunctionalOperationContract contract;

    protected JTerm mbyAtPre;

    static {
        TRANSACTION_TAGS.put(false, "transaction_inactive");
        TRANSACTION_TAGS.put(true, "transaction_active");
    }

    /**
     * Constructor.
     *
     * @param initConfig The {@link InitConfig} to use.
     * @param contract The {@link FunctionalOperationContractPO} to prove.
     */
    public FunctionalOperationContractPO(InitConfig initConfig,
            FunctionalOperationContract contract) {
        super(initConfig, contract.getName());
        this.contract = contract;
    }

    /**
     * Constructor.
     *
     * @param initConfig The {@link InitConfig} to use.
     * @param contract The {@link FunctionalOperationContractPO} to prove.
     * @param addUninterpretedPredicate {@code true} postcondition contains uninterpreted predicate,
     *        {@code false} uninterpreted predicate is not contained in postcondition.
     * @param addSymbolicExecutionLabel {@code true} to add the {@link SymbolicExecutionTermLabel}
     *        to the modality, {@code false} to not label the modality.
     */
    public FunctionalOperationContractPO(InitConfig initConfig,
            FunctionalOperationContract contract, boolean addUninterpretedPredicate,
            boolean addSymbolicExecutionLabel) {
        super(initConfig, contract.getName(), addUninterpretedPredicate, addSymbolicExecutionLabel);
        this.contract = contract;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IProgramMethod getProgramMethod() {
        return getContract().getTarget();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isTransactionApplicable() {
        return getContract().transactionApplicableContract();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected KeYJavaType getCalleeKeYJavaType() {
        return getContract().getKJT();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImmutableList<StatementBlock> buildOperationBlocks(
            ImmutableList<LocationVariable> formalParVars, ProgramVariable selfVar,
            ProgramVariable resultVar, Services services) {
        final StatementBlock[] result = new StatementBlock[4];
        final ImmutableArray<Expression> formalArray = new ImmutableArray<>(
            formalParVars.toArray(new ProgramVariable[formalParVars.size()]));

        if (getContract().getTarget().isConstructor()) {
            assert selfVar != null;
            assert resultVar == null;
            final KeYJavaType type = getContract().getKJT();

            final Expression[] formalArray2 =
                formalArray.toArray(new Expression[formalArray.size()]);
            final New n = new New(formalArray2, new TypeRef(type), null);
            final SVInstantiations svInst = SVInstantiations.EMPTY_SVINSTANTIATIONS;

            // construct what would be produced from rule instanceCreationAssignment
            final Expression init =
                (Expression) (new CreateObject(n)).transform(n, services, svInst)[0];
            final Statement assignTmp = declare(selfVar, init, type);
            result[0] = new StatementBlock(assignTmp);

            // try block
            final Statement constructorCall =
                (Statement) (new ConstructorCall(selfVar, n)).transform(n, services, svInst)[0];
            final Statement setInitialized =
                (Statement) (new PostWork(selfVar)).transform(selfVar, services, svInst)[0];
            result[1] = new StatementBlock(constructorCall, setInitialized);
        } else {
            final MethodBodyStatement call =
                new MethodBodyStatement(getContract().getTarget(), selfVar, resultVar, formalArray);
            result[1] = new StatementBlock(call);
        }
        assert result[1] != null : "null body in method";
        return ImmutableSLList.<StatementBlock>nil().prepend(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JTerm generateMbyAtPreDef(LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars, Services services) {
        final JTerm mbyAtPreDef;
        if (contract.hasMby()) {
            /*
             * final Function mbyAtPreFunc = new Function(new Name(TB.newName(services,
             * "mbyAtPre")), Sort.ANY); //
             * services.getTypeConverter().getIntegerLDT().targetSort()); register(mbyAtPreFunc);
             * mbyAtPre = TB.func(mbyAtPreFunc);
             */
            final JTerm mby = contract.getMby(selfVar, paramVars, services);
            // mbyAtPreDef = TB.equals(mbyAtPre, mby);
            mbyAtPreDef = tb.measuredBy(mby);
        } else {
            // mbyAtPreDef = TB.tt();
            mbyAtPreDef = tb.measuredByEmpty();
        }
        return mbyAtPreDef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JTerm getPre(List<LocationVariable> modifiableHeaps, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars,
            Map<LocationVariable, LocationVariable> atPreVars, Services services) {
        final JTerm freePre =
            contract.getFreePre(modifiableHeaps, selfVar, paramVars, atPreVars, services);
        final JTerm pre = contract.getPre(modifiableHeaps, selfVar, paramVars, atPreVars, services);
        return freePre != null ? services.getTermBuilder().and(pre, freePre) : pre;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JTerm getPost(List<LocationVariable> modifiableHeaps, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars, LocationVariable resultVar,
            LocationVariable exceptionVar, Map<LocationVariable, LocationVariable> atPreVars,
            Services services) {
        return contract.getPost(modifiableHeaps, selfVar, paramVars, resultVar, exceptionVar,
            atPreVars,
            services);
    }

    @Override
    protected JTerm getGlobalDefs(LocationVariable heap, JTerm heapTerm, JTerm selfTerm,
            ImmutableList<JTerm> paramTerms, Services services) {
        return contract.getGlobalDefs(heap, heapTerm, selfTerm, paramTerms, services);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @Nullable JTerm buildFrameClause(List<LocationVariable> modifiableHeaps,
            Map<JTerm, JTerm> heapToAtPre, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars, Services services) {
        JTerm frameTerm = null;
        for (LocationVariable heap : modifiableHeaps) {
            final JTerm ft;
            if (!getContract().hasModifiable(heap)) {
                if (!getContract().hasFreeModifiable(heap)) {
                    ft = tb.frameStrictlyEmpty(tb.var(heap), heapToAtPre);
                } else {
                    ft = tb.frame(tb.var(heap), heapToAtPre,
                        getContract().getFreeModifiable(heap, selfVar, paramVars, services));
                }
            } else {
                if (!getContract().hasFreeModifiable(heap)) {
                    ft = tb.frame(tb.var(heap), heapToAtPre,
                        getContract().getModifiable(heap, selfVar, paramVars, services));
                } else {
                    ft = tb.frame(tb.var(heap), heapToAtPre, tb.union(
                        getContract().getModifiable(heap, selfVar, paramVars, services),
                        getContract().getFreeModifiable(heap, selfVar, paramVars, services)));
                }
            }

            if (frameTerm == null) {
                frameTerm = ft;
            } else {
                frameTerm = tb.and(frameTerm, ft);
            }
        }

        return tb.addLabelToAllSubs(frameTerm, new Origin(SpecType.ASSIGNABLE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JModality.JavaModalityKind getTerminationMarker() {
        return getContract().getModalityKind();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JTerm buildUpdate(ImmutableList<LocationVariable> paramVars,
            ImmutableList<LocationVariable> formalParamVars,
            Map<LocationVariable, LocationVariable> atPreVars, Services services) {
        JTerm update = null;
        for (Entry<LocationVariable, LocationVariable> atPreEntry : atPreVars.entrySet()) {
            final LocationVariable heap = atPreEntry.getKey();
            final JTerm u = tb.elementary(atPreEntry.getValue(),
                heap == getSavedHeap(services) ? tb.getBaseHeap() : tb.var(heap));
            if (update == null) {
                update = u;
            } else {
                update = tb.parallel(update, u);
            }
        }
        Iterator<LocationVariable> formalParamIt = formalParamVars.iterator();
        Iterator<LocationVariable> paramIt = paramVars.iterator();
        while (formalParamIt.hasNext()) {
            JTerm paramUpdate = tb.elementary(formalParamIt.next(), tb.var(paramIt.next()));
            update = tb.parallel(update, paramUpdate);
        }
        return update;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String buildPOName(boolean transactionFlag) {
        return getContract().getName() + "." + TRANSACTION_TAGS.get(transactionFlag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctionalOperationContract getContract() {
        return contract;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JTerm getMbyAtPre() {
        return mbyAtPre;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean implies(ProofOblInput po) {
        if (!(po instanceof FunctionalOperationContractPO cPO)) {
            return false;
        }
        return specRepos.splitContract(cPO.contract).subset(specRepos.splitContract(contract));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof FunctionalOperationContractPO) {
            return contract.equals(((FunctionalOperationContractPO) o).contract);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return contract.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Configuration createLoaderConfig() {
        var c = super.createLoaderConfig();
        c.set("contract", contract.getName());
        return c;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public KeYJavaType getContainerType() {
        return getContract().getKJT();
    }
}
