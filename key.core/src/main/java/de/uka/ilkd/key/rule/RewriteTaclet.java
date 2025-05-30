/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.label.TermLabelState;
import de.uka.ilkd.key.logic.op.IfThenElse;
import de.uka.ilkd.key.logic.op.Junctor;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.executor.javadl.RewriteTacletExecutor;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.*;
import org.key_project.prover.rules.TacletPrefix;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.prover.sequent.PIOPathIterator;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

/**
 * A RewriteTaclet represents a taclet, whose find can be matched against any term in the sequent no
 * matter where it occurs. The only constraint to be fulfilled is that the term matches the
 * structure described by the term of the find-part.
 */
public class RewriteTaclet extends FindTaclet {

    /** does not pose state restrictions on valid matchings */
    public static final int NONE = 0;

    /**
     * all taclet constituents must appear in the same state (and not below a modality (for
     * efficiency reasons))
     */
    public static final int SAME_UPDATE_LEVEL = 1;

    /**
     * all taclet constituents must be in the same state as the sequent
     */
    public static final int IN_SEQUENT_STATE = 2;

    /**
     * If the surrounding formula has been decomposed completely, the find-term will NOT appear on
     * the SUCcedent. The formula {@code wellformed(h)} in {@code wellformed(h) ==>} or in
     * {@code ==> wellformed(h) ->
     * (inv(h) = inv(h2))} or in {@code ==> \if(b) \then(!wellformed(h)) \else(!wellformed(h2))} has
     * antecedent polarity. The formula {@code wellformed(h)} in
     * {@code wellformed(h) <-> wellformed(h2) ==>}
     * has NO antecedent polarity.
     */
    public static final int ANTECEDENT_POLARITY = 4;

    /**
     * If the surrounding formula has been decomposed completely, the find-term will NOT appear on
     * the ANTEcedent. The formula {@code wellformed(h)} in {@code ==> wellformed(h)} or in
     * {@code wellformed(h) ->
     * (inv(h) = inv(h2)) ==>} or in {@code \if(b) \then(!wellformed(h)) \else(!wellformed(h2)) ==>}
     * has
     * succedent polarity. The formula {@code wellformed(h)} in
     * {@code wellformed(h) <-> wellformed(h2) ==>} has
     * NO succedent polarity.
     */
    public static final int SUCCEDENT_POLARITY = 8;

    /**
     * encodes restrictions on the state where a rewrite taclet is applicable If the value is equal
     * to
     * <ul>
     * <li>{@link RewriteTaclet#NONE} no state restrictions are posed</li>
     * <li>{@link RewriteTaclet#SAME_UPDATE_LEVEL} then <code>\assumes</code> must match on a
     * formula within the same state as <code>\find</code> rsp. <code>\add</code>. For efficiency no
     * modalities are allowed above the <code>\find</code> position</li>
     * <li>{@link RewriteTaclet#IN_SEQUENT_STATE} the <code>\find</code> part is only allowed to
     * match on formulas which are evaluated in the same state as the sequent</li>
     * </ul>
     */
    private final int applicationRestriction;


    /**
     * creates a Schematic Theory Specific Rule (Taclet) with the given parameters that represents a
     * rewrite rule.
     *
     * @param name the Name of the Taclet
     * @param applPart the TacletApplPart that contains the application part of an Taclet that is
     *        the if-sequent, the variable conditions
     * @param goalTemplates a list of goal descriptions.
     * @param ruleSets a list of rule sets for the Taclet
     * @param attrs the TacletAttributes; these are boolean values indicating a noninteractive or
     *        recursive use of the Taclet.
     * @param find the find term of the Taclet
     * @param prefixMap an ImmutableMap that contains the prefix for each
     *        SchemaVariable in the Taclet
     * @param p_applicationRestriction an int defining state restrictions of the taclet (required
     *        for location check)
     * @param choices the SetOf<Choices> to which this taclet belongs to
     */
    public RewriteTaclet(Name name, TacletApplPart applPart,
            ImmutableList<TacletGoalTemplate> goalTemplates,
            ImmutableList<RuleSet> ruleSets,
            TacletAttributes attrs, Term find,
            ImmutableMap<@NonNull SchemaVariable, TacletPrefix> prefixMap,
            int p_applicationRestriction, ChoiceExpr choices,
            ImmutableSet<TacletAnnotation> tacletAnnotations) {
        this(name, applPart, goalTemplates, ruleSets, attrs, find, prefixMap,
            p_applicationRestriction, choices, false, tacletAnnotations);
    }

    public RewriteTaclet(Name name, TacletApplPart applPart,
            ImmutableList<TacletGoalTemplate> goalTemplates,
            ImmutableList<RuleSet> ruleSets,
            TacletAttributes attrs, Term find,
            ImmutableMap<@NonNull SchemaVariable, TacletPrefix> prefixMap,
            int p_applicationRestriction, ChoiceExpr choices,
            boolean surviveSymbExec,
            ImmutableSet<TacletAnnotation> tacletAnnotations) {
        super(name, applPart, goalTemplates, ruleSets, attrs, find, prefixMap, choices,
            surviveSymbExec, tacletAnnotations);
        applicationRestriction = p_applicationRestriction;
        createTacletServices();
    }

    @Override
    protected void createAndInitializeExecutor() {
        this.executor = new RewriteTacletExecutor(this);
    }

    /**
     * this method is used to determine if top level updates are allowed to be ignored. This is the
     * case if we have an Antec or SuccTaclet but not for a RewriteTaclet
     *
     * @return true if top level updates shall be ignored
     */
    @Override
    public boolean ignoreTopLevelUpdates() {
        return false;
    }

    /**
     * returns the int encoding the kind of state restriction this rewrite taclet must obey
     *
     * @return the int encoding the kind of state restriction this rewrite taclet must obey
     */
    public int getApplicationRestriction() {
        return applicationRestriction;
    }


    /**
     * the top level operator has to be a simultaneous update. This method checks if the assignment
     * pairs of the update contain free logic variables and gives a veto if positive
     *
     * @param t the Term to check
     * @return false if vetoing
     */
    private boolean veto(Term t) {
        return !t.freeVars().isEmpty();
    }

    /**
     * For taclets with <code>getSameUpdatePrefix ()</code>, collect the updates above
     * <code>p_pos</code> and add them to the update context of the instantiations object
     * <code>p_mc</code>.
     *
     * @return the new instantiations with the additional updates, or <code>null</code>, if program
     *         modalities appear above <code>p_pos</code>
     */
    public MatchConditions checkPrefix(
            PosInOccurrence p_pos,
            MatchConditions p_mc) {
        int polarity = p_pos.isInAntec() ? -1 : 1; // init polarity
        SVInstantiations svi = p_mc.getInstantiations();
        // this is assumed to hold
        assert p_pos.posInTerm() != null;

        PIOPathIterator it = p_pos.iterator();
        while (it.next() != -1) {
            final Term t = (Term) it.getSubTerm();
            var op = t.op();
            if (op instanceof Transformer) {
                return null;
            } else if (op instanceof UpdateApplication
                    && it.getChild() == UpdateApplication.targetPos()
                    && getApplicationRestriction() != NONE) {
                if ((getApplicationRestriction() & IN_SEQUENT_STATE) != 0 || veto(t)) {
                    return null;
                } else {
                    Term update = UpdateApplication.getUpdate(t);
                    svi = svi.addUpdate(update, t.getLabels());
                }
            } else if (getApplicationRestriction() != NONE
                    && (op instanceof Modality)) {
                return null;
            }

            if (polarity != 0) {
                polarity = polarity(op, it, polarity);
            }
        }

        if (getApplicationRestriction() == NONE) {
            return p_mc;
        }
        if (((getApplicationRestriction() & ANTECEDENT_POLARITY) != 0 && polarity != -1)
                || ((getApplicationRestriction() & SUCCEDENT_POLARITY) != 0 && polarity != 1)) {
            return null;
        }
        return p_mc.setInstantiations(svi);
    }

    /**
     * Compute polarity
     *
     * (the {@code AntecSuccPrefixChecker} seems to reimplement this.
     */
    private int polarity(final Operator op, final PIOPathIterator it, int polarity) {
        // toggle polarity if find term is
        // subterm of
        if ((op == Junctor.NOT) || // not
                (op == Junctor.IMP && it.getChild() == 0)) { // left hand side of implication
            polarity = polarity * -1;
            // do not change polarity if find term
            // is subterm of
        } else if ((op == Junctor.AND) || // and
                (op == Junctor.OR) || // or
                (op == Junctor.IMP && it.getChild() != 0) || // right hand side of implication
                (op == IfThenElse.IF_THEN_ELSE && it.getChild() != 0)) { // then or else part of
                                                                         // if-then-else
            // do nothing
        } else { // find term has no polarity in any
                 // other case
            polarity = 0;
        }
        return polarity;
    }


    @Override
    protected StringBuffer toStringFind(StringBuffer sb) {
        StringBuffer res = super.toStringFind(sb);
        if ((getApplicationRestriction() & SAME_UPDATE_LEVEL) != 0) {
            res.append("\\sameUpdateLevel");
        }
        if ((getApplicationRestriction() & IN_SEQUENT_STATE) != 0) {
            res.append("\\inSequentState");
        }
        if ((getApplicationRestriction() & ANTECEDENT_POLARITY) != 0) {
            res.append("\\antecedentPolarity");
        }
        if ((getApplicationRestriction() & SUCCEDENT_POLARITY) != 0) {
            res.append("\\succedentPolarity");
        }
        return res;
    }

    public SequentFormula getRewriteResult(Goal goal, TermLabelState termLabelState,
            Services services, TacletApp app) {
        return ((RewriteTacletExecutor) getExecutor()).getRewriteResult(goal, termLabelState,
            services, app);
    }

    @Override
    public RewriteTaclet setName(String s) {
        final TacletApplPart applPart =
            new TacletApplPart(assumesSequent(), varsNew(), varsNotFreeIn(),
                varsNewDependingOn(), getVariableConditions());
        final TacletAttributes attrs = new TacletAttributes(displayName(), trigger);

        return new RewriteTaclet(new Name(s), applPart, goalTemplates(), getRuleSets(), attrs, find,
            prefixMap, applicationRestriction, choices, getSurviveSymbExec(), tacletAnnotations);
    }
}
