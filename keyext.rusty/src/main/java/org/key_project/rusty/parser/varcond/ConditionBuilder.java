/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import java.util.List;

import org.key_project.prover.rules.VariableCondition;
import org.key_project.rusty.rule.tacletbuilder.TacletBuilder;

/// A specilized [TacletBuilderCommand] for handling `\varcond`s.
///
/// @author Alexander Weigl
/// @version 1 (12/10/19)
public interface ConditionBuilder extends TacletBuilderCommand {
    /// Should construct a variable condition for the given arguments and parameters. The arguments
    /// are the adhering the type specified in [#getArgumentTypes()].
    ///
    /// For a varcond `\varcond(\abc[p1,p2](a1, a2))` the arguments are a1 and a2, the
    /// parameters are p1 and p2. `negated` is true if `\not` is used.
    VariableCondition build(Object[] arguments,
            List<String> parameters, boolean negated);

    /// This method will add the contructed [VariableCondition] to given `tacletBuilder`.
    ///
    /// @see TacletBuilderCommand#apply(TacletBuilder, Object[], List, boolean)
    @Override
    default void apply(TacletBuilder<?> tacletBuilder, Object[] arguments, List<String> parameters,
            boolean negated) {
        VariableCondition condition =
            build(arguments, parameters, negated);
        tacletBuilder.addVariableCondition(condition);
    }
}
