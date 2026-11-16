/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match;

import java.util.HashMap;
import java.util.Iterator;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.*;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.conditions.NotFreeIn;
import org.key_project.prover.rules.instantiation.*;
import org.key_project.prover.rules.matcher.compiler.PatternKeySource;
import org.key_project.prover.rules.matcher.vm.MatchProgram;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.op.UpdateApplication;
import org.key_project.rusty.rule.*;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.rusty.rule.Taclet;
import org.key_project.rusty.rule.match.instructions.MatchSchemaVariableInstruction;
import org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.collection.Pair;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.logic.equality.RenamingTermProperty.RENAMING_TERM_PROPERTY;

/// The matcher of one taclet (proof rule): it decides whether the taclet applies to a given term
/// and, if so, with which instantiations of the taclet's schema variables (its placeholders). At
/// construction, it builds a matcher for the taclet's find pattern and one per `\\assumes`
/// formula (the side formulas the rule additionally requires) through the match-plan framework.
/// Matching
/// itself is read-only: results are threaded through immutable [MatchConditions], so one
/// matcher instance serves concurrent proof-search threads.
public class VMTacletMatcher implements TacletMatcher {
    /// the matcher for the find expression of the taclet
    private final MatchProgram findMatchProgram;
    /// the matcher for the taclet's assumes formulas
    private final HashMap<Term, @NonNull MatchProgram> assumesMatchPrograms =
        new HashMap<>();
    /// per assumes formula, the summary of how it accepts terms, for indexing (see
    /// [#assumesKeySource]); computed once here so it comes from the same dispatch as the
    /// match programs above
    private final HashMap<Term, @NonNull PatternKeySource> assumesKeySources = new HashMap<>();

    /// the variable conditions of the taclet that need to be satisfied by found schema variable
    /// instantiations
    private final ImmutableList<? extends VariableCondition> varconditions;
    /// the built-in notFreeIn variable conditions
    private final ImmutableList<? extends NotFreeIn> varsNotFreeIn;

    /// the assumes sequent of the taclet
    private final Sequent assumesSequent;
    /// the bound variables
    private final ImmutableSet<QuantifiableVariable> boundVars;

    /// flag indicating if preceding updates of the term to be matched should be ignored this
    /// requires the taclet to ignore updates and that the find term does not start with an
    /// [UpdateApplication] operator
    private final boolean ignoreTopLevelUpdates;
    /// the find expression of the taclet or `null` if it is a [NoFindTaclet]
    private final @Nullable Term findExp;

    /// @param taclet the Taclet matched by this matcher
    public VMTacletMatcher(Taclet taclet) {
        varconditions = taclet.getVariableConditions();
        assumesSequent = taclet.assumesSequent();
        boundVars = taclet.getBoundVariables();
        varsNotFreeIn = taclet.varsNotFreeIn();

        if (taclet instanceof final FindTaclet findTaclet) {
            findExp = findTaclet.find();
            ignoreTopLevelUpdates = taclet.ignoreTopLevelUpdates()
                    && !(findExp.op() instanceof UpdateApplication);
            findMatchProgram = matchProgramFor(findExp);
        } else {
            ignoreTopLevelUpdates = false;
            findExp = null;
            findMatchProgram = null;
        }

        for (final SequentFormula sf : assumesSequent) {
            assumesMatchPrograms.put(sf.formula(), matchProgramFor(sf.formula()));
            assumesKeySources.put(sf.formula(), keySourceFor(sf.formula()));
        }
    }

    /// The key source of an assumes pattern. Almost always the summary the plan framework
    /// derives; the one Java-DL correction made here: a pattern whose top is an update
    /// application gets no key. The top of a candidate is observed with its leading updates
    /// removed (the find's update context, see [#matchUpdateContext]), so a top key
    /// naming an update application could never meet a candidate's key. Deriving the summary
    /// of the pattern's stripped core instead would let such a pattern park; it is not keyed
    /// at all, so the base simply stays in the queue.
    private static PatternKeySource keySourceFor(Term pattern) {
        if (pattern.op() instanceof UpdateApplication) {
            return PatternKeySource.Unkeyable.INSTANCE;
        }
        return RustMatchPlanBuilder.keySource(pattern);
    }

    /// Summarizes how the given `\\assumes` formula of this taclet accepts terms, for
    /// indexing candidates that wait for such a formula (see [PatternKeySource]). The
    /// summary is derived from the same dispatch as the formula's matcher, so it cannot drift
    /// from what [#matchAssumes] accepts.
    ///
    /// @param assumesFormula a formula of the taclet's `\\assumes` sequent
    /// @return the formula's key source
    /// @throws IllegalArgumentException if the formula is not an assumes formula of this taclet
    public PatternKeySource assumesKeySource(Term assumesFormula) {
        final PatternKeySource source = assumesKeySources.get(assumesFormula);
        if (source == null) {
            throw new IllegalArgumentException(
                "not an assumes formula of this taclet: " + assumesFormula);
        }
        return source;
    }

    /// Builds the matcher for a find / assumes `pattern` on the selected back-end. Both
    /// back-ends are derived from the same match plan, so a pattern outside the plan framework
    /// fails at taclet-load time on either one.
    private static MatchProgram matchProgramFor(Term pattern) {
        return RustMatchPlanBuilder.getCompiledProgram(pattern);
    }

    @Override
    public final @NonNull AssumesMatchResult matchAssumes(
            @NonNull Iterable<@NonNull AssumesFormulaInstantiation> p_toMatch,
            @NonNull Term p_template,
            @NonNull MatchResultInfo p_matchCond,
            @NonNull LogicServices p_services) {
        final MatchProgram program = assumesMatchPrograms.get(p_template);
        if (program == null) {
            throw new IllegalArgumentException(
                "template is not an assumes formula of this taclet: " + p_template);
        }
        final var mc = (MatchConditions) p_matchCond;

        ImmutableList<AssumesFormulaInstantiation> resFormulas = ImmutableList.nil();
        ImmutableList<MatchResultInfo> resMC =
            ImmutableList.nil();

        final boolean updateContextPresent = !mc.getInstantiations().getUpdateContext().isEmpty();
        ImmutableList<Term> context = ImmutableList.nil();

        if (updateContextPresent) {
            context = mc.getInstantiations().getUpdateContext();
        }

        for (var cf : p_toMatch) {
            Term formula = cf.getSequentFormula().formula();

            if (updateContextPresent) {
                formula = matchUpdateContext(context, formula);
            }
            if (formula != null) {// update context not present or update context match succeeded
                final MatchResultInfo newMC =
                    checkConditions(program.match(formula, mc, p_services), p_services);

                if (newMC != null) {
                    resFormulas = resFormulas.prepend(cf);
                    resMC = resMC.prepend(newMC);
                }
            }
        }
        return new AssumesMatchResult(resFormulas, resMC);
    }

    /// the formula ensures that the update context described the update of the given formula.
    /// If it does not then `null` is returned, otherwise the formula without the update
    /// context.
    ///
    /// @param context the list of updates describing the update context
    /// @param formula the formula whose own update context must be equal (modulo renaming) to the
    /// given one
    /// @return `null` if the update context does not match the one of the formula or the
    /// formula without the update context
    private Term matchUpdateContext(ImmutableList<Term> context, Term formula) {
        ImmutableList<Term> curContext = context;
        for (int i = 0, size = context.size(); i < size; i++) {
            if (formula.op() instanceof UpdateApplication) {
                final Term update = UpdateApplication.getUpdate(formula);
                final Term u = curContext.head();
                if (RENAMING_TERM_PROPERTY.equalsModThisProperty(u, update)) {
                    curContext = curContext.tail();
                    formula = UpdateApplication.getTarget(formula);
                    continue;
                }
            }
            // update context does not match update prefix of formula
            return null;
        }
        return formula;
    }


    /// @see TacletMatcher#matchAssumes(Iterable, MatchResultInfo, LogicServices)
    @Override
    public final @Nullable MatchResultInfo matchAssumes(
            @NonNull Iterable<AssumesFormulaInstantiation> p_toMatch,
            @NonNull MatchResultInfo p_matchCond,
            @NonNull LogicServices p_services) {

        final Iterator<SequentFormula> anteIterator = assumesSequent.antecedent().iterator();
        final Iterator<SequentFormula> succIterator = assumesSequent.succedent().iterator();

        ImmutableList<MatchResultInfo> newMC;

        for (final AssumesFormulaInstantiation candidateInst : p_toMatch) {
            // Part of fix for #1716: match antecedent with antecedent, succ with succ
            boolean candidateInAntec =
                (candidateInst instanceof final AssumesFormulaInstSeq candidateInSeq)
                        // Only if AssumesFormulaInstSeq has inAntec() property ...
                        && (candidateInSeq.inAntecedent())
                        || !(candidateInst instanceof AssumesFormulaInstSeq)
                                // ... and it seems we don't need the check for other
                                // implementations.
                                // Default: just take the next ante formula, else succ formula
                                && anteIterator.hasNext();

            final var assumesSequentIterator = candidateInAntec ? anteIterator : succIterator;
            // Fix end

            assert assumesSequentIterator.hasNext()
                    : "p_toMatch and assumes sequent must have same number of elements";
            newMC = matchAssumes(
                ImmutableList.singleton(candidateInst),
                assumesSequentIterator.next().formula(), p_matchCond, p_services).matchConditions();

            if (newMC.isEmpty()) {
                return null;
            }

            p_matchCond = newMC.head();
        }
        assert !anteIterator.hasNext() && !succIterator.hasNext()
                : "p_toMatch and assumes sequent must have same number of elements";

        return p_matchCond;
    }

    /// {@inheritDoc}
    @Override
    public final @Nullable MatchResultInfo checkConditions(
            @Nullable MatchResultInfo cond,
            @NonNull LogicServices services) {
        var result = (MatchConditions) cond;
        if (result != null) {
            final var svIterator = result.getInstantiations().svIterator();

            if (!svIterator.hasNext()) {
                // for example SimplifyIfThenElseUpdateCondition
                // rewrite these conditions and avoid null; conditions that do not involve matched
                // variables
                return checkVariableConditions(null, null, cond, services);// XXX
            }

            while (result != null && svIterator.hasNext()) {
                final SchemaVariable sv = svIterator.next();
                final Object o = result.getInstantiations().getInstantiation(sv);
                if (o instanceof SyntaxElement se) {
                    result = (MatchConditions) checkVariableConditions(sv, se, result, services);
                }
            }
        }

        return result;
    }

    /// looks if a variable is declared as not free in
    ///
    /// @param var the SchemaVariable to look for
    /// @return true iff declared not free
    private boolean varDeclaredNotFree(SchemaVariable var) {
        for (final NotFreeIn nfi : varsNotFreeIn) {
            if (nfi.first() == var) {
                return true;
            }
        }
        return false;
    }


    /// returns true iff the given variable is bound either in the ifSequent or in any part of the
    /// TacletGoalTemplates
    ///
    /// @param v the bound variable to be searched
    private boolean varIsBound(SchemaVariable v) {
        return (v instanceof QuantifiableVariable) && boundVars.contains(v);
    }

    /// {@inheritDoc}
    @Override
    public final @Nullable MatchResultInfo checkVariableConditions(@Nullable SchemaVariable var,
            @Nullable SyntaxElement instantiationCandidate,
            @Nullable MatchResultInfo matchCond,
            @NonNull LogicServices services) {
        if (matchCond != null) {
            if (instantiationCandidate instanceof Term term) {
                if (!(term.op() instanceof QuantifiableVariable)) {
                    if (varIsBound(var) || varDeclaredNotFree(var)) {
                        // match(x) is not a variable, but the corresponding template variable is
                        // bound or declared non free (so it has to be matched to a variable)
                        return null; // FAILED
                    }
                }
            }
            // check generic conditions
            for (final VariableCondition vc : varconditions) {
                matchCond = vc.check(var, instantiationCandidate, matchCond, services);
                if (matchCond == null) {
                    return null; // FAILED
                }
            }
        }
        return matchCond;
    }

    /// Ignores a possible update prefix. This method assumes that the taclet allows to ignore
    /// updates
    /// and the find expression does not start with an update application operator
    ///
    /// @param source the term to be matched
    /// @param matchResult the accumulated match conditions for a successful match
    /// @return a pair of updated match conditions and the unwrapped term without the ignored
    /// updates
    /// (Which have been added to the update context in the match conditions)
    private Pair<Term, MatchResultInfo> matchAndIgnoreUpdatePrefix(
            Term source, final MatchResultInfo matchResult) {
        final MatchConditions matchCond = (MatchConditions) matchResult;
        final var instantiations = matchCond.getInstantiations();
        ImmutableList<Term> updates = instantiations.getUpdateContext();
        while (source.op() instanceof UpdateApplication) {
            final Term update = UpdateApplication.getUpdate(source);
            updates = updates.append(update);
            source = UpdateApplication.getTarget(source);
        }
        return new Pair<>(source,
            matchCond.setInstantiations(instantiations.addUpdateList(updates)));
    }

    /// {@inheritDoc}
    @Override
    public final @Nullable MatchResultInfo matchFind(
            @NonNull Term term,
            @NonNull MatchResultInfo p_matchCond,
            @NonNull LogicServices services) {
        if (findMatchProgram == null) {
            return null;
        }
        Term source = term;
        if (ignoreTopLevelUpdates) {
            Pair</* term below updates */Term, MatchResultInfo> resultUpdateMatch =
                matchAndIgnoreUpdatePrefix(source, p_matchCond);
            source = resultUpdateMatch.first;
            p_matchCond = resultUpdateMatch.second;
        }
        final MatchResultInfo matchResult = findMatchProgram.match(source, p_matchCond, services);
        return matchResult == null ? null : checkConditions(matchResult, services);
    }


    /// {@inheritDoc}
    @Override
    public @Nullable MatchResultInfo matchSV(@NonNull SchemaVariable sv,
            @NonNull SyntaxElement syntaxElement,
            @NonNull MatchResultInfo matchCond,
            @NonNull LogicServices services) {
        final MatchSchemaVariableInstruction instr =
            RustyDLMatchVMInstructionSet.getMatchInstructionForSV(sv);

        // the instruction routes the candidate by kind (term or program element) itself
        matchCond = instr.match(syntaxElement, matchCond, services);
        if (syntaxElement instanceof Term) {
            matchCond = checkVariableConditions(sv, syntaxElement, matchCond, services);
        } else if (syntaxElement instanceof RustyProgramElement) {
            matchCond = checkConditions(matchCond, services);
        }
        return matchCond;
    }
}
