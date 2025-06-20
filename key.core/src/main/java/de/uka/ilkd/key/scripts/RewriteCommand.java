/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.uka.ilkd.key.control.AbstractUserInterfaceControl;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.RuleAppIndex;
import de.uka.ilkd.key.rule.PosTacletApp;
import de.uka.ilkd.key.rule.RewriteTaclet;
import de.uka.ilkd.key.rule.TacletApp;
import de.uka.ilkd.key.rule.executor.javadl.RewriteTacletExecutor;
import de.uka.ilkd.key.scripts.meta.Option;

import org.key_project.logic.IntIterator;
import org.key_project.logic.PosInTerm;
import org.key_project.prover.proof.rulefilter.TacletFilter;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import static de.uka.ilkd.key.logic.equality.RenamingTermProperty.RENAMING_TERM_PROPERTY;

/**
 * This class provides the command <code>rewrite</code>.
 * <p>
 * This command takes two parameters. A term to find, and a term as the substitutent. Parameter
 * class is {@link RewriteCommand.Parameters}.
 * <p>
 *
 * <p>
 * Usage:
 *
 * <pre>
 *     rewrite find="x+y" replace="y+x"; //(mulbrich script syntax)
 *     rewrite find=`y+x` replace=`y+x`; //(psdbg)
 * </pre>
 * </p>
 *
 * @author lulong, grebing, weigl
 */
public class RewriteCommand extends AbstractCommand<RewriteCommand.Parameters> {

    /**
     * List of PosInOcc that haven't been successfully replaced
     */
    private final List<PosInOccurrence> failposInOccs =
        new ArrayList<>();

    /**
     * List of PosInOcc that successfully replaced
     */
    private final List<PosInOccurrence> succposInOccs =
        new ArrayList<>();

    /**
     * Constructs this rewrite command.
     */
    public RewriteCommand() {
        super(Parameters.class);
    }

    @Override
    public String getName() {
        return "rewrite";
    }


    @Override
    public Parameters evaluateArguments(EngineState state, Map<String, Object> arguments)
            throws Exception {
        return state.getValueInjector().inject(this, new Parameters(), arguments);
    }

    @Override
    public void execute(AbstractUserInterfaceControl uiControl, Parameters args, EngineState state)
            throws ScriptException, InterruptedException {
        Proof proof = state.getProof();
        assert proof != null;

        ImmutableList<TacletApp> allApps = findAllTacletApps(args, state);

        // filter all taclets for being applicable on the find term
        List<PosInOccurrence> failposInOccs =
            findAndExecReplacement(args, allApps, state);

        // if not all find terms successfully replaced, apply cut
        if (!failposInOccs.isEmpty()) {

            CutCommand cut = new CutCommand();
            CutCommand.Parameters param = new CutCommand.Parameters();
            param.formula = args.replace;

            cut.execute(uiControl, param, state);
        }

    }

    /**
     * get all TacletApps that are applicable on the formula term
     */
    private ImmutableList<TacletApp> findAllTacletApps(Parameters p, EngineState state)
            throws ScriptException {
        Services services = state.getProof().getServices();
        TacletFilter filter = TacletFilter.TRUE;
        Goal g = state.getFirstOpenAutomaticGoal();
        RuleAppIndex index = g.ruleAppIndex();
        index.autoModeStopped();

        ImmutableList<TacletApp> allApps = ImmutableSLList.nil();

        // filter taclets that are applicable on the given formula
        // filter taclets that are applicable on the given formula in the antecedent
        for (SequentFormula sf : g.node().sequent().antecedent()) {

            if (p.formula != null) {
                JTerm term = (JTerm) sf.formula();
                if (!RENAMING_TERM_PROPERTY.equalsModThisProperty(term, p.formula)) {
                    continue;
                }
            }
            allApps = allApps.append(index.getTacletAppAtAndBelow(filter,
                new PosInOccurrence(sf, PosInTerm.getTopLevel(), true), services));
        }

        // filter taclets that are applicable on the given formula in the succedent
        for (SequentFormula sf : g.node().sequent().succedent()) {
            if (p.formula != null) {
                JTerm term = (JTerm) sf.formula();
                if (!RENAMING_TERM_PROPERTY.equalsModThisProperty(term, p.formula)) {
                    continue;
                }
            }
            allApps = allApps.append(index.getTacletAppAtAndBelow(filter,
                new PosInOccurrence(sf, PosInTerm.getTopLevel(), false), services));
        }

        return allApps;
    }

    /**
     * Filter tacletapps: term = find && result = replace and execute taclet that matches the
     * conditions
     **/
    private List<PosInOccurrence> findAndExecReplacement(Parameters p,
            ImmutableList<TacletApp> list, EngineState state) {

        // Find taclet that transforms find term to replace term, when applied on find term
        for (TacletApp tacletApp : list) {
            if (tacletApp instanceof PosTacletApp pta) {
                if (pta.taclet() instanceof RewriteTaclet) {
                    if (pta.taclet().displayName().equals("cut_direct")) {
                        continue;
                    }
                    if (pta.posInOccurrence().subTerm().equals(p.find) && pta.complete()) {
                        // if Term already succ replaced, then skip
                        if (succposInOccs.contains(pta.posInOccurrence())) {
                            continue;
                        }

                        try { // Term not already successfully replaced
                            Goal goalold = state.getFirstOpenAutomaticGoal();

                            RewriteTaclet rw = (RewriteTaclet) pta.taclet();
                            if (pta.complete()) {
                                SequentFormula rewriteResult =
                                    ((RewriteTacletExecutor) rw.getExecutor()).getRewriteResult(
                                        goalold, null, goalold.proof().getServices(), pta);

                                executeRewriteTaclet(p, pta, goalold, rewriteResult);
                                break;
                            }
                        } catch (Exception e) {
                            if (!failposInOccs.contains(pta.posInOccurrence())) {
                                failposInOccs.add(pta.posInOccurrence());
                            }
                        }
                    }
                }
            }
        }
        return failposInOccs;
    }

    /**
     * Execute taclet pta if after application p.find term is replaced by p.replace throws
     * IllegalArgumentException on not successfully applicable pta
     *
     * @param p
     * @param pta
     * @param goalold
     * @param rewriteResult
     */
    private void executeRewriteTaclet(Parameters p, PosTacletApp pta, Goal goalold,
            SequentFormula rewriteResult) {
        if (rewriteResult.formula().equals(p.replace)
                || getTermAtPos(rewriteResult, pta.posInOccurrence()).equals(p.replace)) {
            failposInOccs.remove(pta.posInOccurrence());
            succposInOccs.add(pta.posInOccurrence());
            goalold.apply(pta);
        } else {
            throw new IllegalArgumentException(
                "Unsuccessful application of rewrite taclet " + pta.taclet().displayName());
        }
    }


    /**
     * Calculates term at the PosInOccurrence pio
     *
     * @param sf top-level formula
     * @param pio PosInOccurrence of the to be returned term
     * @return term at pio
     */
    public JTerm getTermAtPos(SequentFormula sf, PosInOccurrence pio) {
        if (pio.isTopLevel()) {
            return (JTerm) sf.formula();

        } else {
            PosInTerm pit = pio.posInTerm();
            return getSubTerm((JTerm) sf.formula(), pit.iterator());
        }

    }

    /**
     * Gets subterm of t at the postion of pit
     *
     * @param t
     * @param pit
     * @return subterm
     */
    private JTerm getSubTerm(JTerm t, IntIterator pit) {
        if (pit.hasNext()) {
            int i = pit.next();
            return getSubTerm(t.sub(i), pit);
        } else {
            return t;
        }
    }

    /**
     * Parameters for the {@link RewriteCommand}
     *
     * @author luong, grebing, weigl
     */
    public static class Parameters {
        /**
         * Term, which should be replaced
         */
        @Option(value = "find")
        public JTerm find;
        /**
         * Substitutent
         */
        @Option(value = "replace")
        public JTerm replace;
        /**
         * Formula, where to find {@see find}.
         */
        @Option(value = "formula", required = false)
        public JTerm formula;
    }
}
