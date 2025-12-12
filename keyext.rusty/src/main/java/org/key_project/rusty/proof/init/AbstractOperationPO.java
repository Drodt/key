/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.init;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.GenericConstParam;
import org.key_project.rusty.ast.abstraction.GenericTyParam;
import org.key_project.rusty.ast.expr.BlockExpression;
import org.key_project.rusty.ast.expr.PanicFrame;
import org.key_project.rusty.logic.RustyBlock;
import org.key_project.rusty.logic.op.ProgramFunction;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

///
/// This abstract implementation of [ProofOblInput] extends the functionality of
/// [AbstractPO] to execute some code.
///
///
/// The generated [Sequent] has the following form:
/// <pre>
///
/// `==><generalAssumptions>
/// &<preconditions>-><updatesToStoreInitialValues><modalityStart>panics=false;<customCode><modalityEnd>(<postconditions
/// > & <optionalUninterpretedPredicate>)`
/// </pre>
///
///
/// If [#isAddUninterpretedPredicate()] an uninterpreted predicate is added to the
/// postcondition which contains the heap and all parameters as argument. This predicate can be used
/// to filter out invalid execution paths because its branches are closed while still open branches
/// contains valid execution paths.
///
///
/// @author Martin Hentschel
public abstract class AbstractOperationPO extends AbstractPO {
    protected InitConfig proofConfig;

    public AbstractOperationPO(InitConfig proofConfig, Name name) {
        super(proofConfig, name);
    }

    protected Services postInit() {
        proofConfig = environmentConfig.deepCopy();
        final Services proofServices = proofConfig.getServices();
        tb = proofServices.getTermBuilder();
        return proofServices;
    }

    @Override
    public void readProblem() throws ProofInputException {
        assert proofConfig == null;
        final Services proofServices = postInit();
        final ProgramFunction fn = getProgramFunction();

        // prepare variables, program method
        boolean makeNamesUnique = isMakeNamesUnique();
        final ImmutableList<ProgramVariable> paramVars = tb.paramVars(fn, makeNamesUnique);
        final ProgramVariable resultVar = tb.resultVar(fn, makeNamesUnique);
        final ProgramVariable panicVar = tb.panicVar(makeNamesUnique);

        if (fn.getFunction().getGenericParams().length > 0) {
            for (int i = 0; i < fn.getFunction().getGenericParams().length; i++) {
                var p = fn.getFunction().getGenericParams()[i];
                if (p instanceof GenericTyParam) {
                    throw new UnsupportedOperationException("TODO: Generic types");
                } else if (p instanceof GenericConstParam gcp) {
                    proofServices.getNamespaces().functions().addSafely(gcp.fn());
                }
            }
        }

        register(paramVars, new ProgramVariable[] { resultVar, panicVar }, proofServices);

        final Term termPO = createPOTerm(fn, paramVars, resultVar, panicVar, proofServices);

        assignPOTerm(termPO);
    }

    protected abstract ProgramFunction getProgramFunction();

    @Override
    protected InitConfig getCreatedInitConfigForSingleProof() {
        return proofConfig;
    }

    /// Checks if result variable and call arguments should
    /// be renamed to make sure that their names are unique in the whole KeY application.
    ///
    /// @return `true` use unique names, `false` use original names even if they are not
    /// unique in whole KeY application.
    protected boolean isMakeNamesUnique() {
        // Changing this behaviour to fix #1552.
        // return true;
        return false;
    }

    /// Checks if a copy of the call arguments are used instead of the original
    /// arguments.
    ///
    /// @return `true` use copy of method call arguments, `false` use original method
    /// call arguments.
    protected boolean isCopyOfArgumentsUsed() {
        return true;
    }

    private void register(final ImmutableList<ProgramVariable> paramVars,
            final ProgramVariable[] vars,
            final Services proofServices) {
        // register the variables so they are declared in proof header
        // if the proof is saved to a file
        register(paramVars, proofServices);
        for (ProgramVariable var : vars) {
            register(var, proofServices);
        }
    }

    private Term createPOTerm(ProgramFunction fn, final ImmutableList<ProgramVariable> paramVars,
            final ProgramVariable resultVar, final ProgramVariable panicVar, final Services proofServices) {
        final ImmutableList<ProgramVariable> formalParamVars =
            createFormalParamVars(paramVars, proofServices);

        // build program block to execute
        // (must be done before pre-condition is created).
        final BlockExpression be = buildOperationBlock(formalParamVars, resultVar, proofServices);

        // build precondition
        Term pre = tb.and(buildFreePre(paramVars, proofServices),
            getPre(paramVars, proofServices));
        // build program term
        Term post = createPost(paramVars, formalParamVars, resultVar, panicVar, proofServices);

        final Term progPost =
            buildProgramTerm(paramVars, formalParamVars, post, be, panicVar, proofServices);
        final Term preImpliesProgPost = tb.imp(pre, progPost);

        return preImpliesProgPost;
    }

    protected Term buildProgramTerm(ImmutableList<ProgramVariable> paramVars,
                                    ImmutableList<ProgramVariable> formalParamVars, Term post,
                                    BlockExpression be, @Nullable ProgramVariable panicVar, Services proofServices) {
        // create rusty block
        final RustyBlock rb = buildRustyBlock(be, panicVar);

        // create program term
        Term programTerm = tb.prog(getTerminationMarker(), rb, post);

        // create update
        Term update = buildUpdate(paramVars, formalParamVars, proofServices);

        return tb.apply(update, programTerm);
    }

    protected RustyBlock buildRustyBlock(BlockExpression be, @Nullable ProgramVariable panicVar) {
        if (panicVar == null)
            return new RustyBlock(be);
        return new RustyBlock(new BlockExpression(ImmutableList.of(new PanicFrame(panicVar, be)), null));
    }

    /// Returns the [RModality.RustyModalityKind] to use as termination
    /// marker.
    ///
    /// @return The [RModality.RustyModalityKind] to use as termination
    /// marker.
    protected abstract RModality.RustyModalityKind getTerminationMarker();

    /// Builds the initial updates.
    ///
    /// @param paramVars Formal parameters of method call.
    /// @param formalParamVars Arguments from formal parameters for method call.
    /// @param services The services instance.
    /// @return The [Term] representing the initial updates.
    protected Term buildUpdate(ImmutableList<ProgramVariable> paramVars,
            ImmutableList<ProgramVariable> formalParamVars, Services services) {
        Term update = null;
        if (isCopyOfArgumentsUsed()) {
            var formalParamIt = formalParamVars.iterator();
            var paramIt = paramVars.iterator();
            while (formalParamIt.hasNext()) {
                Term paramUpdate = tb.elementary(formalParamIt.next(), tb.var(paramIt.next()));
                if (update == null)
                    update = paramUpdate;
                else
                    update = tb.parallel(update, paramUpdate);
            }
        }
        if (update == null)
            update = tb.skip();
        return update;
    }

    protected abstract Term getPre(ImmutableList<ProgramVariable> paramVars,
            Services proofServices);

    /// Builds the "general assumption".
    ///
    /// @param paramVars The parameters [ProgramVariable]s.
    /// @param services The services instance.
    /// @return The [Term] containing the general assumptions.
    private Term buildFreePre(ImmutableList<ProgramVariable> paramVars, Services services) {
        // conjunction of...
        // - "inBounds(p_i)" for integer parameters
        final Term paramsOk = generateParamsOk(paramVars);

        // initial value of measured_by clause
        final Term mbyAtPreDef = generateMbyAtPreDef(paramVars, services);

        return tb.and(paramsOk, mbyAtPreDef);
    }

    protected abstract Term generateMbyAtPreDef(ImmutableList<ProgramVariable> paramVars,
            Services services);

    /// Generates the general assumption that all parameter arguments are valid.
    ///
    /// @param paramVars The parameters [ProgramVariable]s.
    /// @return The term representing the general assumption.
    protected Term generateParamsOk(ImmutableList<ProgramVariable> paramVars) {
        Term paramsOK = tb.tt();
        for (var paramVar : paramVars) {
            paramsOK = tb.and(paramsOK, tb.reachableValue(paramVar));
        }
        return paramsOK;
    }

    private ImmutableList<ProgramVariable> createFormalParamVars(
            ImmutableList<ProgramVariable> paramVars, Services proofServices) {
        // create arguments from formal parameters for call
        ImmutableList<ProgramVariable> formalParamVars = ImmutableList.of();
        for (final var paramVar : paramVars) {
            if (isCopyOfArgumentsUsed()) {
                var pen = new Name("_" + paramVar.name());
                var formalParamVar = new ProgramVariable(pen, paramVar.getKeYRustyType());
                formalParamVars = formalParamVars.append(formalParamVar);
                register(formalParamVar, proofServices);
            } else {
                formalParamVars = formalParamVars.append(paramVar);
            }
        }
        return formalParamVars;
    }

    private Term createPost(final ImmutableList<ProgramVariable> paramVars,
            final ImmutableList<ProgramVariable> formalParamVars,
            final ProgramVariable resultVar, final @Nullable ProgramVariable panicVar,
            final Services proofServices) {
        Term postTerm = getPost(paramVars, resultVar,panicVar, proofServices);
        return postTerm;
    }

    protected abstract Term getPost(ImmutableList<ProgramVariable> paramVars,
            ProgramVariable resultVar, @Nullable ProgramVariable panicVar,Services proofServices);

    protected abstract BlockExpression buildOperationBlock(
            ImmutableList<ProgramVariable> formalParamVars, ProgramVariable resultVar,
            Services proofServices);
}
