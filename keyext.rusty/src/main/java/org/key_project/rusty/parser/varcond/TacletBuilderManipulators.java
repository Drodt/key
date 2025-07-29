/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.rusty.ast.abstraction.KeYRustyType;
import org.key_project.rusty.logic.op.sv.OperatorSV;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.parser.varcond.TypeComparisonCondition.Mode;
import org.key_project.rusty.rule.tacletbuilder.TacletBuilder;

import org.jspecify.annotations.NonNull;

import static org.key_project.rusty.parser.varcond.ArgumentType.SORT;


/// This class manages the register of various factories for the different built-in
/// [VariableCondition]s.
///
/// @author Alexander Weigl
/// @version 1 (12/9/19)
public class TacletBuilderManipulators {
    // region Factories
    // Shortcut for argument types
    private static final ArgumentType TR = ArgumentType.TYPE_RESOLVER;
    private static final ArgumentType KRT = ArgumentType.RUST_TYPE;
    private static final ArgumentType PV = ArgumentType.VARIABLE;
    private static final ArgumentType USV = ArgumentType.VARIABLE;
    private static final ArgumentType TSV = ArgumentType.VARIABLE;
    private static final ArgumentType ASV = ArgumentType.VARIABLE;
    private static final ArgumentType FSV = ArgumentType.VARIABLE;
    private static final ArgumentType SV = ArgumentType.VARIABLE;
    private static final ArgumentType TLSV = ArgumentType.VARIABLE;
    private static final ArgumentType S = ArgumentType.STRING;
    private static final ArgumentType T = ArgumentType.TERM;

    public static final AbstractConditionBuilder DIFFERENT =
        new ConstructorBasedBuilder("different", DifferentInstantiationCondition.class, SV, SV);

    public static final AbstractConditionBuilder APPLY_UPDATE_ON_RIGID =
        new ConstructorBasedBuilder(
            "applyUpdateOnRigid", ApplyUpdateOnRigidCondition.class, USV, SV, SV);
    public static final AbstractTacletBuilderCommand NEW_DEPENDING_ON =
        new AbstractTacletBuilderCommand("newDependingOn", SV, SV) {
            @Override
            public void apply(TacletBuilder<?> tb, Object[] arguments, List<String> parameters,
                    boolean negated) {
                if (negated) {
                    throw new IllegalArgumentException("Negation is not supported");
                }
                tb.addVarsNewDependingOn((org.key_project.logic.op.sv.SchemaVariable) arguments[0],
                    (org.key_project.logic.op.sv.SchemaVariable) arguments[1]);
            }
        };
    public static final AbstractConditionBuilder DROP_EFFECTLESS_ELEMENTARIES =
        new ConstructorBasedBuilder("dropEffectlessElementaries",
            DropEffectlessElementariesCondition.class, USV, SV, SV);
    public static final AbstractConditionBuilder EQUAL_UNIQUE =
        new ConstructorBasedBuilder("equalUnique", EqualUniqueCondition.class, TSV, TSV, FSV);
    public static final AbstractConditionBuilder SIMPLIFY_ITE_UPDATE =
        new ConstructorBasedBuilder("simplifyIfThenElseUpdate",
            SimplifyIfThenElseUpdateCondition.class, FSV, USV, USV, FSV, SV);

    public static final AbstractConditionBuilder STORE_TERM_IN =
        new AbstractConditionBuilder("storeTermIn", SV, T) {
            @Override
            public VariableCondition build(Object[] arguments,
                    List<String> parameters,
                    boolean negated) {
                return new StoreTermInCondition((SchemaVariable) arguments[0], (Term) arguments[1]);
            }
        };
    public static final AbstractConditionBuilder STORE_EXPR_IN =
        new ConstructorBasedBuilder("storeExprIn", StoreExprInCondition.class, SV, T);
    public static final AbstractConditionBuilder HAS_INVARIANT =
        new ConstructorBasedBuilder("\\hasInvariant", HasLoopInvariantCondition.class, PV, SV);
    public static final AbstractConditionBuilder GET_INVARIANT =
        new ConstructorBasedBuilder("\\getInvariant", LoopInvariantCondition.class, PV, SV, SV);
    public static final AbstractConditionBuilder GET_VARIANT =
        new AbstractConditionBuilder("\\getVariant", PV, SV) {
            @Override
            public VariableCondition build(Object[] arguments,
                    List<String> parameters,
                    boolean negated) {
                return new LoopVariantCondition((ProgramSV) arguments[0],
                    (SchemaVariable) arguments[1]);
            }
        };
    public static final AbstractConditionBuilder IS_LABELED =
        new AbstractConditionBuilder("isLabeled", PV) {
            @Override
            public IsLabeledCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                return new IsLabeledCondition((ProgramSV) arguments[0], negated);
            }
        };

    public static AbstractTacletBuilderCommand NEW_TYPE_OF =
        new AbstractTacletBuilderCommand("newTypeOf", SV, SV) {
            @Override
            public void apply(TacletBuilder<?> tacletBuilder, Object[] arguments,
                    List<String> parameters, boolean negated) {
                if (negated) {
                    throw new IllegalArgumentException("Negation is not supported");
                }
                tacletBuilder.addVarsNew((org.key_project.logic.op.sv.SchemaVariable) arguments[0],
                    (org.key_project.logic.op.sv.SchemaVariable) arguments[1]);

            }
        };
    public static final AbstractTacletBuilderCommand NEW_RUSTY_TYPE =
        new AbstractTacletBuilderCommand("new", SV, KRT) {
            @Override
            public void apply(TacletBuilder<?> tacletBuilder, Object[] arguments,
                    List<String> parameters, boolean negated) {
                if (negated) {
                    throw new IllegalArgumentException("Negation is not supported");
                }
                var krt = (KeYRustyType) arguments[1];
                tacletBuilder.addVarsNew((SchemaVariable) arguments[0], krt);
            }
        };

    public static final AbstractConditionBuilder SAME =
        new AbstractConditionBuilder("same", TR, TR) {
            @Override
            public TypeComparisonCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1],
                    negated ? Mode.NOT_SAME : Mode.SAME);
            }
        };

    public static final AbstractConditionBuilder IS_SUBTYPE =
        new AbstractConditionBuilder("sub", TR, TR) {
            @Override
            public TypeComparisonCondition build(Object[] arguments, List<String> parameters,
                    boolean negated) {
                return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1],
                    negated ? Mode.NOT_IS_SUBTYPE : Mode.IS_SUBTYPE);
            }
        };

    static class RustTypeToSortConditionBuilder extends AbstractConditionBuilder {
        public RustTypeToSortConditionBuilder(@NonNull String triggerName) {
            super(triggerName, SV, SORT);
        }

        @Override
        public VariableCondition build(Object[] arguments, List<String> parameters,
                boolean negated) {
            var v = (OperatorSV) arguments[0];
            Sort s = (Sort) arguments[1];
            if (!RustTypeToSortCondition.checkSortedSV(v)) {
                throw new IllegalArgumentException(
                    "Expected schema variable of kind EXPRESSION or TYPE, but is " + v);
            } else if (s instanceof GenericSort gs) {
                return new RustTypeToSortCondition(v, gs);
            } else if (s instanceof ParametricSortInstance psi) {
                return new RustTypeToParametricSortCondition(v, psi);
            } else {
                throw new IllegalArgumentException(
                    "Generic or parametric sort is expected. Got: " + s);
            }
        }
    }

    public static final AbstractConditionBuilder HAS_SORT =
        new RustTypeToSortConditionBuilder("hasSort");

    public static final TacletBuilderCommand NEW_LOCAL_VARS =
        new ConstructorBasedBuilder("newLocalVars", NewLocalVarsCondition.class, SV, SV, SV, SV);

    private static final List<TacletBuilderCommand> tacletBuilderCommands = new ArrayList<>(2);

    static {
        register(DIFFERENT, APPLY_UPDATE_ON_RIGID, NEW_DEPENDING_ON, EQUAL_UNIQUE,
            DROP_EFFECTLESS_ELEMENTARIES, SIMPLIFY_ITE_UPDATE, NEW_TYPE_OF, NEW_RUSTY_TYPE,
            IS_SUBTYPE, SAME, HAS_SORT,
            NEW_LOCAL_VARS, STORE_EXPR_IN, STORE_TERM_IN, HAS_INVARIANT, GET_INVARIANT,
            GET_VARIANT, IS_LABELED);
    }

    /// Announce a [TacletBuilderCommand] for the use during the interpretation of asts. This
    /// affects every following interpretation of rule contextes in
    /// [TacletPBuilder].
    public static void register(TacletBuilderCommand... cb) {
        for (TacletBuilderCommand a : cb) {
            register(a);
        }
    }

    /// @see #register(TacletBuilderCommand...)
    public static void register(TacletBuilderCommand cb) {
        tacletBuilderCommands.add(cb);
    }


    /// Returns all available [TacletBuilderCommand]s that response on the given name.
    ///
    /// @see TacletBuilderCommand#isSuitableFor(String)
    public static List<TacletBuilderCommand> getConditionBuildersFor(String name) {
        return tacletBuilderCommands.stream().filter(it -> it.isSuitableFor(name))
                .collect(Collectors.toList());
    }
}
