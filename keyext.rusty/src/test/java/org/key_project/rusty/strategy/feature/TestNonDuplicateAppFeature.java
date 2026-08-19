/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.feature;

import org.key_project.logic.PosInTerm;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.rusty.proof.*;
import org.key_project.rusty.proof.calculus.RustySequentKit;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.rusty.util.TacletForTests;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/// Tests duplicate detection of [NonDuplicateAppFeature] and [EqNonDuplicateAppFeature],
/// which go through the fingerprint-bucketed [AppliedRuleAppsNameCache]. The two key
/// properties: an application already performed on the branch is rejected (cost
/// [TopRuleAppCost]), and the fingerprint bucketing never hides a real duplicate nor invents
/// one for the same taclet applied at a _different_ position (a different bucket).
class TestNonDuplicateAppFeature {
    @BeforeAll
    public static void setUp() {
        TacletForTests.parse();
    }

    /// Root sequent `==> A -> B, B -> A` (two distinct succedent formulas).
    private static Sequent twoImplications() {
        ImmutableList<SequentFormula> succ = ImmutableSLList.<SequentFormula>nil()
                .append(new SequentFormula(TacletForTests.parseTerm("A -> B")))
                .append(new SequentFormula(TacletForTests.parseTerm("B -> A")));
        return RustySequentKit.createSequent(ImmutableSLList.nil(), succ);
    }

    private static Goal goalFor(Node n, TacletIndex idx) {
        return new Goal(n, idx, new BuiltInRuleAppIndex(new BuiltInRuleIndex()),
            n.proof().getServices());
    }

    private static PosInOccurrence topSucc(Node n, int idx) {
        return new PosInOccurrence(n.sequent().succedent().get(idx), PosInTerm.getTopLevel(),
            false);
    }

    private static TacletApp appAt(Goal goal, PosInOccurrence pos) {
        return goal.ruleAppIndex().getTacletAppAt(pos, null).head();
    }

    /// Build `root --(imp_right at succedent #applyIdx)--> child(leaf)` and return the feature
    /// cost of applying imp\_right at succedent #candIdx on the child leaf.
    private static RuleAppCost cost(Feature feature, int applyIdx, int candIdx) {
        Proof proof = new Proof("TestNonDuplicateAppFeature", TacletForTests.initConfig());
        Node root = new Node(proof, twoImplications());
        proof.setRoot(root);

        TacletIndex idx = new TacletIndex();
        idx.add(TacletForTests.lookupTaclet("imp_right"));
        Goal rootGoal = goalFor(root, idx);

        final PosInOccurrence applyPos = topSucc(root, applyIdx);
        final PosInOccurrence candPos = topSucc(root, candIdx);
        final TacletApp applied = appAt(rootGoal, applyPos);
        final TacletApp candidate = candIdx == applyIdx ? applied : appAt(rootGoal, candPos);

        root.setAppliedRuleApp(applied);
        Node child = new Node(proof, root.sequent(), root);
        root.add(child);

        return feature.computeCost(candidate, candPos, goalFor(child, idx), new MutableState());
    }

    @Test
    public void nonDuplicateRejectsAlreadyAppliedApp() {
        assertSame(TopRuleAppCost.INSTANCE, cost(NonDuplicateAppFeature.INSTANCE, 0, 0),
            "re-applying the same taclet at the same position must be rejected as a duplicate");
    }

    @Test
    public void nonDuplicateAcceptsAppAtDifferentPosition() {
        assertNotSame(TopRuleAppCost.INSTANCE, cost(NonDuplicateAppFeature.INSTANCE, 1, 0),
            "the same taclet at a different position is not a duplicate (different fingerprint bucket)");
    }

    @Test
    public void eqNonDuplicateRejectsAlreadyAppliedApp() {
        assertSame(TopRuleAppCost.INSTANCE, cost(EqNonDuplicateAppFeature.INSTANCE, 0, 0),
            "eqEquals variant must also reject an already-applied application");
    }

    @Test
    public void eqNonDuplicateAcceptsAppAtDifferentPosition() {
        assertNotSame(TopRuleAppCost.INSTANCE, cost(EqNonDuplicateAppFeature.INSTANCE, 1, 0),
            "eqEquals variant must accept the same taclet at a different position");
    }

    /// Apply a rewrite (TestApplyTaclet\_contradiction, `find(b->c)`) at one occurrence of
    /// `A -> B` in `==> (A -> B) & (A -> B)`, then return the cost of applying it at the
    /// _other_ occurrence: same focus term, different sequent position. This is the case the
    /// fingerprint must not get wrong -- the modulo-position variant treats it as a duplicate, the
    /// plain variant does not.
    private static RuleAppCost crossPositionCost(Feature feature) {
        Proof proof = new Proof("TestNonDuplicateAppFeature", TacletForTests.initConfig());
        SequentFormula conj = new SequentFormula(TacletForTests.parseTerm("(A -> B) & (A -> B)"));
        Node root = new Node(proof,
            RustySequentKit.createSequent(ImmutableSLList.nil(), ImmutableSLList.singleton(conj)));
        proof.setRoot(root);

        TacletIndex idx = new TacletIndex();
        idx.add(TacletForTests.lookupTaclet("TestApplyTaclet_contradiction"));
        Goal rootGoal = goalFor(root, idx);

        final SequentFormula f = root.sequent().succedent().get(0);
        final PosInOccurrence leftConjunct =
            new PosInOccurrence(f, PosInTerm.getTopLevel().down(0), false);
        final PosInOccurrence rightConjunct =
            new PosInOccurrence(f, PosInTerm.getTopLevel().down(1), false);

        root.setAppliedRuleApp(appAt(rootGoal, leftConjunct));
        Node child = new Node(proof, root.sequent(), root);
        root.add(child);

        return feature.computeCost(appAt(rootGoal, rightConjunct), rightConjunct,
            goalFor(child, idx),
            new MutableState());
    }

    @Test
    public void modPositionRejectsSameFocusAtDifferentPosition() {
        assertSame(TopRuleAppCost.INSTANCE,
            crossPositionCost(NonDuplicateAppModPositionFeature.INSTANCE),
            "modulo-position variant must treat the same focus term at a different position as a duplicate");
    }

    @Test
    public void nonDuplicateAllowsSameFocusAtDifferentPosition() {
        assertNotSame(TopRuleAppCost.INSTANCE, crossPositionCost(NonDuplicateAppFeature.INSTANCE),
            "plain variant must not treat a different position as a duplicate even with equal focus term");
    }
}
