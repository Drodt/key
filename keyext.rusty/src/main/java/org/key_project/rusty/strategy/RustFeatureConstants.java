/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import org.key_project.logic.LogicServices;
import org.key_project.logic.op.Operator;
import org.key_project.prover.strategy.FeatureConstants;
import org.key_project.prover.strategy.costbased.feature.CountOccurrencesFeature;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.quantifierHeuristics.TriggerTreatment;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.prover.strategy.costbased.termProjection.TermBuffer;
import org.key_project.prover.strategy.costbased.termfeature.EqTermFeature;
import org.key_project.prover.strategy.costbased.termfeature.TermFeature;
import org.key_project.prover.strategy.costbased.termgenerator.TermGenerator;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.equality.RenamingTermProperty;
import org.key_project.rusty.strategy.feature.*;
import org.key_project.rusty.strategy.quantifierHeuristics.HeuristicInstantiation;
import org.key_project.rusty.strategy.quantifierHeuristics.InstantiationCost;
import org.key_project.rusty.strategy.quantifierHeuristics.InstantiationTieBreakFeature;
import org.key_project.rusty.strategy.quantifierHeuristics.SplittableQuantifiedFormulaFeature;
import org.key_project.rusty.strategy.termProjection.TermConstructionProjection;
import org.key_project.rusty.strategy.termfeature.ContainsExecutableCodeTermFeature;
import org.key_project.rusty.strategy.termgenerator.AllowedCutPositionsGenerator;
import org.key_project.rusty.strategy.termgenerator.SuperTermGenerator;
import org.key_project.rusty.strategy.termgenerator.TriggeredInstantiations;

import static org.key_project.prover.strategy.StaticFeatureCollection.inftyTermConst;
import static org.key_project.prover.strategy.StaticFeatureCollection.op;

public final class RustFeatureConstants implements FeatureConstants {
    @Override
    public Feature nonDuplicateAppModPositionFeature() {
        return NonDuplicateAppModPositionFeature.INSTANCE;
    }

    @Override
    public Feature eqNonDuplicateAppFeature() {
        return EqNonDuplicateAppFeature.INSTANCE;
    }

    @Override
    public Feature notInScopeOfModalityFeature() {
        return NotInScopeOfModalityFeature.INSTANCE;
    }

    @Override
    public Feature topLevelFindAntecOrSuccWithUpdate() {
        return TopLevelFindFeature.ANTEC_OR_SUCC_WITH_UPDATE;
    }

    @Override
    public Feature topLevelFindAntec() {
        return TopLevelFindFeature.ANTEC;
    }

    @Override
    public Feature topLevelFindSucc() {
        return TopLevelFindFeature.SUCC;
    }

    @Override
    public Feature checkApplyEqFeature() {
        return CheckApplyEqFeature.INSTANCE;
    }

    @Override
    public TermFeature createEqTermFeature(TermBuffer t) {
        return EqTermFeature.create(t, RenamingTermProperty.RENAMING_TERM_PROPERTY);
    }

    @Override
    public Feature directlyBelowSymbolAtIndex(Operator symbol, int index) {
        return RustStaticFeatureCollection.directlyBelowSymbolAtIndex(op(symbol), index);
    }

    @Override
    public ProjectionToTerm opTerm(Operator op, ProjectionToTerm subTerm0,
            ProjectionToTerm subTerm1) {
        return TermConstructionProjection.create(op, new ProjectionToTerm[] { subTerm0, subTerm1 });
    }

    @Override
    public TermGenerator upwardsWithIndexSuperGenerator(TermFeature cond,
            LogicServices services) {
        return SuperTermGenerator.SuperTermWithIndexGenerator.upwardsWithIndex(cond,
            (Services) services);
    }

    @Override
    public TermFeature createEliminableQuantifier() {
        return EliminableQuantifierTF.INSTANCE;
    }

    @Override
    public Feature splittableQuantifiedFormula() {
        return SplittableQuantifiedFormulaFeature.INSTANCE;
    }

    @Override
    public TermGenerator createTriggeredInstantiations(boolean skipConditions) {
        return TriggeredInstantiations.create(skipConditions);
    }

    @Override
    public Feature diffFindAndReplacewithFeature() {
        return DiffFindAndReplacewithFeature.INSTANCE;
    }

    @Override
    public Feature countMaxDPath() {
        return CountMaxDPathFeature.INSTANCE;
    }

    @Override
    public Feature countPosDPath() {
        return CountPosDPathFeature.INSTANCE;
    }

    @Override
    public Feature purePosDPath() {
        return PurePosDPathFeature.INSTANCE;
    }

    @Override
    public TermGenerator allowedCutPositionsGenerator() {
        return AllowedCutPositionsGenerator.INSTANCE;
    }

    @Override
    public Feature allowedCutPositions() {
        return AllowedCutPositionFeature.INSTANCE;
    }

    @Override
    public Feature countOccurrences(ProjectionToTerm target) {
        return CountOccurrencesFeature.create(target, RenamingTermProperty.RENAMING_TERM_PROPERTY);
    }

    @Override
    public TermFeature containsExecutableCodeTF() {
        return ContainsExecutableCodeTermFeature.PROGRAMS;
    }

    @Override
    public TermGenerator heuristicGenerator(TriggerTreatment option) {
        return HeuristicInstantiation.forOption(option);
    }

    @Override
    public Feature computeInstantiationCostOf(ProjectionToTerm instOf_t,
            TriggerTreatment option) {
        return InstantiationCost.create(instOf_t, option);
    }

    @Override
    public Feature getInstantiationTieBreakFeature(TermBuffer varInst, String s) {
        return InstantiationTieBreakFeature.create(varInst, s);
    }

    @Override
    public TermFeature selectSkolemConstant() {
        return inftyTermConst();
    }
}
