/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule;

import org.key_project.logic.ChoiceExpr;
import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.TacletAnnotation;
import org.key_project.prover.rules.TacletApplPart;
import org.key_project.prover.rules.TacletAttributes;
import org.key_project.prover.rules.TacletPrefix;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.prover.sequent.Sequent;
import org.key_project.rusty.rule.executor.rustydl.AntecTacletExecutor;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

/// An AntecTaclet represents a taclet whose find part has to match a top level formula in the
/// antecedent of the sequent.
public class AntecTaclet extends FindTaclet {
    /// creates a Schematic Theory Specific Rule (Taclet) with the given parameters.
    ///
    /// @param name the name of the Taclet
    /// @param applPart contains the application part of a Taclet that is the if-sequent, the
    /// variable conditions
    /// @param goalTemplates a list of goal descriptions.
    /// @param ruleSets a list of rule sets for the Taclet
    /// @param attrs attributes for the Taclet; these are boolean values indicating a
    /// non-interactive
    /// or recursive use of the Taclet.
    /// @param find the find sequent of the Taclet
    /// @param prefixMap a ImmutableMap that contains the prefix for each
    /// SchemaVariable in the Taclet
    public AntecTaclet(Name name, TacletApplPart applPart,
            ImmutableList<TacletGoalTemplate> goalTemplates, ImmutableList<RuleSet> ruleSets,
            TacletAttributes attrs, Sequent find,
            ImmutableMap<@NonNull SchemaVariable, TacletPrefix> prefixMap,
            ChoiceExpr choices,
            ImmutableSet<TacletAnnotation> tacletAnnotations) {
        super(name, applPart, goalTemplates, ruleSets, attrs, find, prefixMap, choices,
            tacletAnnotations);
    }

    @Override
    public Term find() {
        return (Term) ((Sequent) find).antecedent().getFirst().formula();
    }

    /// toString for the find part
    @Override
    protected StringBuffer toStringFind(StringBuffer sb) {
        return sb.append("\\find(").append(find().toString()).append("==>)\n");
    }

    @Override
    protected void createAndInitializeExecutor() {
        executor = new AntecTacletExecutor(this);
    }

    @Override
    public @NonNull AntecTaclet setName(@NonNull String s) {
        final TacletApplPart applPart =
            new TacletApplPart(assumesSequent(), applicationRestriction(), varsNew(),
                varsNotFreeIn(),
                varsNewDependingOn(), getVariableConditions());
        final TacletAttributes attrs = new TacletAttributes(displayName(), trigger);

        return new AntecTaclet(new Name(s), applPart, goalTemplates(), getRuleSets(), attrs,
            (Sequent) find, prefixMap, choices, tacletAnnotations);
    }
}
