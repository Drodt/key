/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.tacletbuilder;

import de.uka.ilkd.key.logic.BoundVarsVisitor;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.proof.calculus.JavaDLSequentKit;
import de.uka.ilkd.key.rule.Taclet;

import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.sequent.Sequent;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;

/**
 * this class inherits from TacletGoalTemplate. It is used if there is a replacewith in the
 * ruleGoals that replaces a term by another term. For a sequent {@link AntecSuccTacletGoalTemplate}
 */
public class RewriteTacletGoalTemplate extends TacletGoalTemplate {

    /** term that replaces another one */
    private final JTerm replacewith;

    /**
     * creates new Goaldescription
     *
     * @param addedSeq new Sequent to be added
     * @param addedRules IList<Taclet> contains the new allowed rules at this branch
     * @param replacewith the Term that replaces another one
     * @param pvs the set of schema variables
     */
    public RewriteTacletGoalTemplate(Sequent addedSeq, ImmutableList<Taclet> addedRules,
            JTerm replacewith, ImmutableSet<SchemaVariable> pvs) {
        super(addedSeq, addedRules, pvs);
        TacletBuilder.checkContainsFreeVarSV(replacewith, null, "replacewith term");
        this.replacewith = replacewith;
    }

    public RewriteTacletGoalTemplate(Sequent addedSeq, ImmutableList<Taclet> addedRules,
            JTerm replacewith) {
        this(addedSeq, addedRules, replacewith, DefaultImmutableSet.nil());
    }


    public RewriteTacletGoalTemplate(JTerm replacewith) {
        this(JavaDLSequentKit.getInstance().getEmptySequent(), ImmutableSLList.nil(), replacewith);
    }


    /**
     * a Taclet may replace a Term by another. The new Term is returned.
     *
     * @return Term being paramter in the rule goal replacewith(Seq)
     */
    public JTerm replaceWith() {
        return replacewith;
    }

    /**
     * rertieves and returns all variables that are bound in the goal template
     *
     * @return all variables that occur bound in this goal template
     */
    @Override
    public ImmutableSet<QuantifiableVariable> getBoundVariables() {
        final BoundVarsVisitor bvv = new BoundVarsVisitor();
        bvv.visit(replaceWith());
        return bvv.getBoundVariables().union(super.getBoundVariables());
    }

    /**
     * @return Term being paramter in the rule goal replacewith(term)
     */
    @Override
    public Object replaceWithExpressionAsObject() {
        return replacewith;
    }


    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        final RewriteTacletGoalTemplate other = (RewriteTacletGoalTemplate) o;
        return replacewith.equals(other.replacewith);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result * super.hashCode();
        result = 37 * result * replacewith.hashCode();
        return result;
    }


    /** toString */
    @Override
    public String toString() {
        String result = super.toString();
        result += "\\replacewith(" + replaceWith() + ") ";
        return result;
    }

}
