/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.strategy.AbstractFeatureStrategy;
import org.key_project.prover.strategy.Strategy;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;

import org.jspecify.annotations.NonNull;

public abstract class RustAbstractFeatureStrategy extends AbstractFeatureStrategy<Goal>
        implements Strategy<Goal> {
    private final Proof proof;

    protected RustAbstractFeatureStrategy(Proof proof) {
        this.proof = proof;
    }

    /// @return Returns the proof.
    protected Proof getProof() {
        return proof;
    }

    protected RuleSet getHeuristic(String p_name) {
        final NamespaceSet nss = getProof().getNamespaces();
        final Namespace<@NonNull RuleSet> ns = nss.ruleSets();
        final RuleSet h = ns.lookup(new Name(p_name));

        assert h != null : "Did not find the rule set " + p_name;

        return h;
    }

    /// returns the service instance for access to [LDT]s
    ///
    /// @return the services for access to the meta logic
    protected final Services getServices() {
        return getProof().getServices();
    }
}
