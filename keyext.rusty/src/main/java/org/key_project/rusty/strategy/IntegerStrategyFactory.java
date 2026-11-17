/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import org.key_project.logic.Name;
import org.key_project.prover.strategy.ArithTermFeatures;
import org.key_project.prover.strategy.IntegerStrategy;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.rule.metaconstruct.arith.RMonomial;
import org.key_project.rusty.strategy.definition.OneOfStrategyPropertyDefinition;
import org.key_project.rusty.strategy.definition.StrategyPropertyValueDefinition;
import org.key_project.rusty.strategy.definition.StrategySettingsDefinition;
import org.key_project.rusty.strategy.feature.PolynomialValuesCmpFeature;
import org.key_project.rusty.strategy.termProjection.CoeffGcdProjection;
import org.key_project.rusty.strategy.termProjection.MonomialColumnOp;
import org.key_project.rusty.strategy.termProjection.ReduceMonomialsProjection;
import org.key_project.rusty.strategy.termgenerator.DividePolynomialsProjection;
import org.key_project.rusty.strategy.termgenerator.MultiplesModEquationsGenerator;
import org.key_project.rusty.strategy.termgenerator.RootsGenerator;

import org.jspecify.annotations.NonNull;

public class IntegerStrategyFactory implements StrategyFactory {
    public static final String TOOL_TIP_ARITHMETIC_BASE = "<html>" + "Basic arithmetic support:"
        + "<ul>" + "<li>Simplification of polynomial expressions</li>"
        + "<li>Computation of Gr&ouml;bner Bases for polynomials in the antecedent</li>"
        + "<li>(Partial) Omega procedure for handling linear inequations</li>" + "</ul>"
        + "</html>";
    public static final String TOOL_TIP_ARITHMETIC_DEF_OPS =
        "<html>" + "Automatically expand defined symbols like:" + "<ul>"
            + "<li><tt>/</tt>, <tt>%</tt>, <tt>jdiv</tt>, <tt>jmod</tt>, ...</li>"
            + "<li><tt>int_RANGE</tt>, <tt>short_MIN</tt>, ...</li>"
            + "<li><tt>inInt</tt>, <tt>inByte</tt>, ...</li>"
            + "<li><tt>addJint</tt>, <tt>mulJshort</tt>, ...</li>" + "</ul>" + "</html>";
    public static final String TOOL_TIP_ARITHMETIC_MODEL_SEARCH = "<html>"
        + "Support for non-linear inequations and model search.<br>" + "In addition, this performs:"
        + "<ul>" + "<li>Multiplication of inequations with each other</li>"
        + "<li>Systematic case distinctions (cuts)</li>" + "</ul>"
        + "This method is guaranteed to find counterexamples for<br>"
        + "invalid goals that only contain polynomial (in)equations.<br>"
        + "Such counterexamples turn up as trivially unprovable goals.<br>"
        + "It is also able to prove many more valid goals involving<br>"
        + "(in)equations, but will in general not terminate on such goals." + "</html>";

    private static OneOfStrategyPropertyDefinition getArithmeticTreatment() {
        return new OneOfStrategyPropertyDefinition(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY,
            "Arithmetic treatment",
            new StrategyPropertyValueDefinition(StrategyProperties.NON_LIN_ARITH_NONE, "Basic",
                TOOL_TIP_ARITHMETIC_BASE),
            new StrategyPropertyValueDefinition(StrategyProperties.NON_LIN_ARITH_DEF_OPS, "DefOps",
                TOOL_TIP_ARITHMETIC_DEF_OPS),
            new StrategyPropertyValueDefinition(StrategyProperties.NON_LIN_ARITH_COMPLETION,
                "Model Search", TOOL_TIP_ARITHMETIC_MODEL_SEARCH));
    }

    @Override
    public RustIntegerStrategy create(Proof proof, StrategyProperties strategyProperties) {
        var tf = new ArithTermFeatures(proof.getServices().getLDTs().getIntLDT(), Equality.EQUALS);
        var ff = new FormulaTermFeatures(tf);
        final String nonLin =
            strategyProperties.getProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY);
        final IntegerStrategy.ArithTreatment arith =
            StrategyProperties.NON_LIN_ARITH_COMPLETION.equals(nonLin)
                    ? IntegerStrategy.ArithTreatment.MODEL_SEARCH
                    : StrategyProperties.NON_LIN_ARITH_DEF_OPS.equals(nonLin)
                            ? IntegerStrategy.ArithTreatment.DEF_OPS
                            : IntegerStrategy.ArithTreatment.BASIC;
        var cfg = new IntegerStrategy.Config(tf, arith,
            strategyProperties.getProperty(StrategyProperties.STOPMODE_OPTIONS_KEY)
                    .equals(StrategyProperties.STOPMODE_NONCLOSE),
            proof.getServices().getLDTs().getIntLDT(),
            RMonomial::create, ReduceMonomialsProjection::create, MonomialColumnOp::create,
            DividePolynomialsProjection::createRoundingUp,
            DividePolynomialsProjection::createRoundingDown, CoeffGcdProjection::create,
            PolynomialValuesCmpFeature::lt, PolynomialValuesCmpFeature::lt,
            PolynomialValuesCmpFeature::leq,
            PolynomialValuesCmpFeature::leq, PolynomialValuesCmpFeature::eq,
            PolynomialValuesCmpFeature::divides, MultiplesModEquationsGenerator::create,
            RootsGenerator::create);
        return new RustIntegerStrategy(cfg, ff, proof);
    }

    @Override
    public StrategySettingsDefinition getSettingsDefinition() {
        final OneOfStrategyPropertyDefinition arithmeticTreatment = getArithmeticTreatment();
        return new StrategySettingsDefinition("Integer Options", arithmeticTreatment);
    }

    @Override
    public @NonNull Name name() {
        return RustIntegerStrategy.NAME;
    }
}
