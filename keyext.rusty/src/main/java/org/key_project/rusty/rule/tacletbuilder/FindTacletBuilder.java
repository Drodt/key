/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.tacletbuilder;

import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.ApplicationRestriction;
import org.key_project.rusty.rule.BoundUniquenessChecker;
import org.key_project.rusty.rule.FindTaclet;

public abstract class FindTacletBuilder<T extends FindTaclet> extends TacletBuilder<T> {
    protected SyntaxElement find = null;

    /**
     * encodes restrictions on the state where a rewrite taclet is applicable If the value is equal
     * to
     * <ul>
     * <li>{@link ApplicationRestriction#NONE} no state restrictions are posed</li>
     * <li>{@link ApplicationRestriction#SAME_UPDATE_LEVEL} then <code>\assumes</code> must
     * match on
     * a
     * formula within the same state as <code>\find</code> rsp. <code>\add</code>. For efficiency no
     * modalities are allowed above the <code>\find</code> position</li>
     * <li>{@link ApplicationRestriction#IN_SEQUENT_STATE} the <code>\find</code> part is
     * only
     * allowed to
     * match on formulas which are evaluated in the same state as the sequent</li>
     * </ul>
     */
    protected ApplicationRestriction applicationRestriction =
        ApplicationRestriction.NONE;

    /**
     * checks that a SchemaVariable that is used to match pure variables (this means bound
     * variables) occurs at most once in a quantifier of the assumes and finds and throws an
     * exception otherwise
     */
    protected void checkBoundInIfAndFind() {
        final BoundUniquenessChecker ch = new BoundUniquenessChecker(getFind(), ifSequent());
        if (!ch.correct()) {
            throw new TacletBuilder.TacletBuilderException(this,
                "A bound SchemaVariable variables occurs both " + "in assumes and find clauses.");
        }
    }

    /**
     * Get the {@code find} term. This could be a term or a formula for a RewriteTaclet, but only a
     * formula for an Antec/Succ Taclet.
     */
    public SyntaxElement getFind() {
        return find;
    }

    public FindTacletBuilder<T> setApplicationRestriction(
            ApplicationRestriction p_applicationRestriction) {
        applicationRestriction = p_applicationRestriction;
        return this;
    }
}
