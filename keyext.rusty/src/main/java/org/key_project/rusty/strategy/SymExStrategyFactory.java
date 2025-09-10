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

public class SymExStrategyFactory implements StrategyFactory {
    public static final String TOOL_TIP_LOOP_INVARIANT =
        "<html>" + "Use the loop scope-based invariant taclet.<br>"
            + "Three properties have to be shown:<br>"
            + "<ul><li>Validity of invariant of a loop is preserved by the<br>"
            + "loop guard and loop body (initially valid).</li>"
            + "<li>If the invariant was valid at the start of the loop, it holds <br>"
            + "after arbitrarily many loop iterations (body preserves invariant).</li>"
            + "<li>Invariant holds after the loop terminates (use case).</li>" + "</ul>"
            + "<p>The last two are combined into a single goal or split into two<br>"
            + "goals based on the 'rustLoopTreatment' strategy option.</p>" + "</html>";
    public static final String TOOL_TIP_LOOP_EXPAND =
        "<html>" + "Unroll loop body, but with the loop scope technology.<br>"
            + "This requires less program transformation for irregular<br>"
            + "termination behavior." + "</html>";
    public static final String TOOL_TIP_LOOP_NONE = "<html>" + "Leave loops untouched." + "</html>";
    public static final String TOOL_TIP_FUNCTION_CONTRACT =
        "<html>Replace method calls by contracts. In some cases<br>"
            + "a method call may also be replaced by its method body.<br>"
            + "If query treatment is activated, this behavior applies<br>"
            + "to queries as well.</html>";
    public static final String TOOL_TIP_FUNCTION_EXPAND =
        "<html>Replace method calls by their bodies, i.e. by their<br>"
            + "implementation. Method contracts are strictly deactivated.</html>";
    public static final String TOOL_TIP_FUNCTION_NONE =
        "<html>" + "Stop when encountering a method" + "</html>";

    @Override
    public SymExStrategy create(Proof proof, StrategyProperties strategyProperties) {
        return new SymExStrategy(proof, strategyProperties);
    }

    private static OneOfStrategyPropertyDefinition getLoopTreatment() {
        return new OneOfStrategyPropertyDefinition(StrategyProperties.LOOP_OPTIONS_KEY,
            "Loop treatment", 2,
            new StrategyPropertyValueDefinition(StrategyProperties.LOOP_INVARIANT,
                "Invariant", TOOL_TIP_LOOP_INVARIANT),
            new StrategyPropertyValueDefinition(StrategyProperties.LOOP_EXPAND,
                "Expand", TOOL_TIP_LOOP_EXPAND),
            new StrategyPropertyValueDefinition(StrategyProperties.LOOP_NONE, "None",
                TOOL_TIP_LOOP_NONE));
    }

    private static OneOfStrategyPropertyDefinition getFunctionTreatment() {
        return new OneOfStrategyPropertyDefinition(StrategyProperties.FUNCTION_OPTIONS_KEY,
            "Method treatment",
            new StrategyPropertyValueDefinition(StrategyProperties.FUNCTION_CONTRACT, "Contract",
                TOOL_TIP_FUNCTION_CONTRACT),
            new StrategyPropertyValueDefinition(StrategyProperties.FUNCTION_EXPAND, "Expand",
                TOOL_TIP_FUNCTION_EXPAND),
            new StrategyPropertyValueDefinition(StrategyProperties.FUNCTION_NONE, "None",
                TOOL_TIP_FUNCTION_NONE));
    }

    @Override
    public StrategySettingsDefinition getSettingsDefinition() {
        final OneOfStrategyPropertyDefinition loopTreatment = getLoopTreatment();
        final OneOfStrategyPropertyDefinition methodTreatment = getFunctionTreatment();
        return new StrategySettingsDefinition("Symbolic Execution Options",
            loopTreatment, methodTreatment);
    }

    @Override
    public @NonNull Name name() {
        return SymExStrategy.NAME;
    }
}
