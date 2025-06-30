/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import java.util.List;

import org.key_project.rusty.rule.tacletbuilder.TacletBuilder;

import org.jspecify.annotations.NonNull;

/// This interface describes a commands that manipulate taclets during construction in the parser.
///
/// Currently, we use this interface to handle the construction of
/// [VariableCondition] (`\varcond`), but may be used in future for
/// other facilities.
///
/// @author Alexander Weigl
/// @version 1 (12/9/19)
public interface TacletBuilderCommand {
    /// Checks if this command is responsible for the given command name. For example, for
    /// `\varcond(\newType(t))` the name would be "newType".
    boolean isSuitableFor(@NonNull String name);

    /// Defines the amount and type of expected arguments. For example, if you want describe a
    /// sub-type test (instanceOf) you would need two sorts `new ArgumentType[]{SORT,SORT}` as
    /// arguments.
    ///
    /// The parse guarantees, that the types are suitable, when calling
    /// [#apply(TacletBuilder,Object[],List,boolean)].
    ///
    ///
    /// @see ArgumentType
    ArgumentType[] getArgumentTypes();

    /// Applying this command on the given taclet builder.
    ///
    /// During application, this method should alter, e.g., add a
    /// [VariableCondition], to the taclet builder.
    ///
    /// The given arguments are well-typed for supplied [#getArgumentTypes()].
    void apply(TacletBuilder<?> tacletBuilder, Object[] arguments, List<String> parameters,
            boolean negated);
}
