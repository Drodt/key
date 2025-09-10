/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy;

import org.key_project.logic.Name;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.strategy.definition.OneOfStrategyPropertyDefinition;
import org.key_project.rusty.strategy.definition.StrategyPropertyValueDefinition;
import org.key_project.rusty.strategy.definition.StrategySettingsDefinition;

import org.jspecify.annotations.NonNull;

public class RustyDLStrategyFactory implements StrategyFactory {
    /// The unique [Name] of this [StrategyFactory].
    public static final Name NAME = new Name(RustyDLStrategy.RUSTY_DL_STRATEGY);

    public static final String TOOL_TIP_PROOF_SPLITTING_FREE =
        "<html>" + "Split formulas (if-then-else expressions,<br>"
            + "disjunctions in the antecedent, conjunctions in<br>"
            + "the succedent) freely without restrictions." + "</html>";
    public static final String TOOL_TIP_PROOF_SPLITTING_DELAYED =
        "<html>" + "Do not split formulas (if-then-else expressions,<br>"
            + "disjunctions in the antecedent, conjunctions in<br>"
            + "the succedent) as long as programs are present in<br>" + "the sequent.<br>"
            + "NB: This does not affect the splitting of formulas<br>"
            + "that themselves contain programs.<br>"
            + "NB2: Delaying splits often prevents KeY from finding<br>"
            + "short proofs, but in some cases it can significantly<br>"
            + "improve the performance." + "</html>";
    public static final String TOOL_TIP_PROOF_SPLITTING_OFF = "<html>"
        + "Do never split formulas (if-then-else expressions,<br>"
        + "disjunctions in the antecedent, conjunctions in<br>" + "the succedent).<br>"
        + "NB: This does not affect the splitting of formulas<br>" + "that contain programs.<br>"
        + "NB2: Without splitting, KeY is often unable to find<br>"
        + "proofs even for simple problems. This option can,<br>"
        + "nevertheless, be meaningful to keep the complexity<br>"
        + "of proofs small and support interactive proving." + "</html>";
    public static final String TOOL_TIP_AUTO_INDUCTION_ON =
        "<html>" + "Create an inductive proof for formulas of the form:<br>"
            + "      ==>  \\forall int i; 0&lt;=i->phi <br>"
            + "and certain other forms. The induction hypothesis<br>"
            + "is the subformula phi. The rule is applied before<br>"
            + "beta rules are applied.<br>" + "<br>" + "When encountering a formula of the form<br>"
            + "      ==>  (\\forall int i; 0&lt;=i->phi) & psi <br>"
            + "and certain similar forms, then the quantified formula<br>"
            + "is used in the Use Case branch as a lemma for psi,<br>"
            + "i.e., the sequent in the Use Case has the form:<br>"
            + "      (\\forall int i; 0&lt;=i->phi) ==>  psi <br>" + "</html>";
    public static final String TOOL_TIP_AUTO_INDUCTION_RESTRICTED =
        "<html>" + "Performs auto induction only on quantified formulas that<br>"
            + "(a) fullfill a certain pattern (as described for the \"on\"option)<br>"
            + "and (b) whose quantified variable has the suffix \"Ind\" or \"IND\".<br>"
            + "For instance, auto induction will be applied on:<br>"
            + "      ==>  \\forall int iIND; 0&lt;=iIND->phi <br>" + "but not on: <br>"
            + "      ==>  \\forall int i; 0&lt;=i->phi <br>" + "</html>";
    public static final String TOOL_TIP_AUTO_INDUCTION_OFF =
        "<html>" + "Deactivates automatic creation of inductive proofs.<br>"
            + "In order to make use of auto induction, activate <br>"
            + "auto induction early in proofs before the <br>"
            + "quantified formula that is to be proven inductively<br>"
            + "is Skolemized (using the delta rule). Auto induction<br>"
            + "is not applied on Skolemized formulas in order to<br>"
            + "limit the number of inductive proofs." + "</html>";

    public RustyDLStrategyFactory() {
    }

    private static OneOfStrategyPropertyDefinition getProofSplitting() {
        return new OneOfStrategyPropertyDefinition(StrategyProperties.SPLITTING_OPTIONS_KEY,
            "Proof splitting",
            new StrategyPropertyValueDefinition(StrategyProperties.SPLITTING_NORMAL, "Free",
                TOOL_TIP_PROOF_SPLITTING_FREE),
            new StrategyPropertyValueDefinition(StrategyProperties.SPLITTING_DELAYED, "Delayed",
                TOOL_TIP_PROOF_SPLITTING_DELAYED),
            new StrategyPropertyValueDefinition(StrategyProperties.SPLITTING_OFF, "Off",
                TOOL_TIP_PROOF_SPLITTING_OFF));
    }

    private static OneOfStrategyPropertyDefinition getAutoInduction() {
        return new OneOfStrategyPropertyDefinition(StrategyProperties.AUTO_INDUCTION_OPTIONS_KEY,
            "Auto Induction",
            new StrategyPropertyValueDefinition(StrategyProperties.AUTO_INDUCTION_LEMMA_ON, "On",
                TOOL_TIP_AUTO_INDUCTION_ON),
            new StrategyPropertyValueDefinition(StrategyProperties.AUTO_INDUCTION_RESTRICTED,
                "Restricted", TOOL_TIP_AUTO_INDUCTION_RESTRICTED),
            new StrategyPropertyValueDefinition(StrategyProperties.AUTO_INDUCTION_OFF, "Off",
                TOOL_TIP_AUTO_INDUCTION_OFF));
    }

    @Override
    public RustyDLStrategy create(Proof proof, StrategyProperties strategyProperties) {
        return new RustyDLStrategy(proof, strategyProperties);
    }

    @Override
    public @NonNull Name name() {
        return NAME;
    }

    @Override
    public StrategySettingsDefinition getSettingsDefinition() {
        // Properties
        final OneOfStrategyPropertyDefinition proofSplitting = getProofSplitting();
        final OneOfStrategyPropertyDefinition autoInduction = getAutoInduction();
        // Model
        return new StrategySettingsDefinition("Rusty DL Options", proofSplitting,
            autoInduction);
    }
}
