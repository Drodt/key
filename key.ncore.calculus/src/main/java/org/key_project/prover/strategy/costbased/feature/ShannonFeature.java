/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.prover.strategy.costbased.feature;

import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;

import org.jspecify.annotations.NonNull;


/// A conditional feature, in which the condition itself is a (binary) feature. The general notion
/// is
/// a term <code>c ? f1 : f2</code>, whereby the condition <code>c</code> determines whether the
/// value of the whole expression is <code>f1</code> (if <code>c</code> returns zero, or more
/// general
/// if <code>c</code> returns a distinguished value <code>trueCost</code>) or <code>f2</code>
public class ShannonFeature implements Feature {

    /// The filter that decides which sub-feature is to be evaluated
    private final Feature cond;

    /// If the result of <code>cond</code> is this cost, then the condition is assumed to hold
    private final RuleAppCost trueCost;

    /// The feature for positive results of <code>filter</code>
    private final Feature thenFeature;

    /// The feature for negative results of <code>filter</code>
    private final Feature elseFeature;

    private ShannonFeature(Feature p_cond, RuleAppCost p_trueCost,
            Feature p_thenFeature, Feature p_elseFeature) {
        cond = p_cond;
        trueCost = p_trueCost;
        thenFeature = p_thenFeature;
        elseFeature = p_elseFeature;
    }

    @Override
    public <Goal extends ProofGoal<@NonNull Goal>> RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pos, Goal goal, MutableState mState) {
        if (cond.computeCost(app, pos, goal, mState).equals(trueCost)) {
            return thenFeature.computeCost(app, pos, goal, mState);
        } else {
            return elseFeature.computeCost(app, pos, goal, mState);
        }
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param trueCost the value of <code>cond</code> that is regarded as true-value
    /// @param thenValue the value of the feature, if <code>cond</code> returns
    /// <code>trueCost</code>
    /// @return <code>thenValue</code> if <code>cond</code> returns <code>trueCost</code>, zero
    /// otherwise
    public static Feature createConditional(Feature cond, RuleAppCost trueCost,
            RuleAppCost thenValue) {
        return createConditional(cond, trueCost, ConstFeature.createConst(thenValue));
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param trueCost the value of <code>cond</code> that is regarded as true-value
    /// @param thenValue the value of the feature if <code>cond</code> returns <code>trueCost</code>
    /// @param elseValue the value of the feature if <code>cond</code> does not return
    /// <code>trueCost</code>
    /// @return <code>thenValue</code> if <code>cond</code> returns <code>trueCost</code>,
    /// <code>elseValue</code> otherwise
    public static Feature createConditional(Feature cond, RuleAppCost trueCost,
            RuleAppCost thenValue, RuleAppCost elseValue) {
        return createConditional(cond, trueCost, ConstFeature.createConst(thenValue),
            ConstFeature.createConst(elseValue));
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param trueCost the value of <code>cond</code> that is regarded as true-value
    /// @param thenFeature the value of the feature if <code>cond</code> returns
    /// <code>trueCost</code>
    /// @return the value of <code>thenFeature</code> if <code>cond</code> returns
    /// <code>trueCost</code>, zero otherwise
    public static Feature createConditional(Feature cond, RuleAppCost trueCost,
            Feature thenFeature) {
        return createConditional(cond, trueCost, thenFeature, NumberRuleAppCost.getZeroCost());
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param trueCost the value of <code>cond</code> that is regarded as true-value
    /// @param thenFeature the value of the feature if <code>cond</code> returns
    /// <code>trueCost</code>
    /// @param elseValue the value of the feature if <code>cond</code> does not return
    /// <code>trueCost</code>
    /// @return the value of <code>thenFeature</code> if <code>cond</code> returns
    /// <code>trueCost</code>, <code>elseValue</code> otherwise
    public static Feature createConditional(Feature cond, RuleAppCost trueCost,
            Feature thenFeature, RuleAppCost elseValue) {
        return createConditional(cond, trueCost, thenFeature, ConstFeature.createConst(elseValue));
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param trueCost the value of <code>cond</code> that is regarded as true-value
    /// @param thenFeature the value of the feature if <code>cond</code> returns
    /// <code>trueCost</code>
    /// @param elseFeature the value of the feature if <code>cond</code> does not return
    /// <code>trueCost</code>
    /// @return the value of <code>thenFeature</code> if <code>cond</code> returns
    /// <code>trueCost</code>, the value of <code>elseFeature</code> otherwise
    public static Feature createConditional(Feature cond, RuleAppCost trueCost,
            Feature thenFeature, Feature elseFeature) {
        return new ShannonFeature(cond, trueCost, thenFeature, elseFeature);
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param thenValue the value of the feature if <code>cond</code> returns zero
    /// @return the value of <code>thenFeature</code> if <code>cond</code> returns zero, zero
    /// otherwise
    public static Feature createConditionalBinary(Feature cond, RuleAppCost thenValue) {
        return createConditionalBinary(cond, ConstFeature.createConst(thenValue));
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param thenValue the value of the feature if <code>cond</code> returns zero
    /// @param elseValue the value of the feature if <code>cond</code> does not return zero
    /// @return <code>thenValue</code> if <code>cond</code> returns zero, <code>elseValue</code>
    /// otherwise
    public static Feature createConditionalBinary(
            Feature cond, RuleAppCost thenValue, RuleAppCost elseValue) {
        return createConditionalBinary(cond, ConstFeature.createConst(thenValue),
            ConstFeature.createConst(elseValue));
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param thenFeature the value of the feature if <code>cond</code> returns zero
    /// @param elseValue the value of the feature if <code>cond</code> does not return zero
    /// @return the value of <code>thenFeature</code> if <code>cond</code> returns zero,
    /// <code>elseValue</code> otherwise
    public static Feature createConditionalBinary(
            Feature cond, Feature thenFeature, RuleAppCost elseValue) {
        return createConditionalBinary(cond, thenFeature, ConstFeature.createConst(elseValue));
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param thenFeature the value of the feature if <code>cond</code> returns zero
    /// @param elseFeature the value of the feature if <code>cond</code> does not return zero
    /// @return the value of <code>thenFeature</code> if <code>cond</code> returns zero, the value
    /// of
    /// <code>elseFeature</code> otherwise
    public static Feature createConditionalBinary(
            Feature cond, Feature thenFeature, Feature elseFeature) {
        return createConditional(cond, BinaryFeature.ZERO_COST, thenFeature, elseFeature);
    }

    /// @param cond the feature that decides which value is to be returned
    /// @param thenFeature the value of the feature if <code>cond</code> returns zero
    /// @return the value of <code>thenFeature</code> if <code>cond</code> returns zero, zero
    /// otherwise
    public static Feature createConditionalBinary(Feature cond, Feature thenFeature) {
        return createConditional(cond, BinaryFeature.ZERO_COST, thenFeature,
            NumberRuleAppCost.getZeroCost());
    }

}
