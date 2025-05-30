/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.symbolic_execution.strategy.breakpoint;

import java.util.HashMap;
import java.util.Map;

import de.uka.ilkd.key.java.SourceElement;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;

import org.key_project.prover.rules.RuleApp;

/**
 * Adds the hit count functionality to an {@link AbstractBreakpoint}.
 *
 * @author Martin Hentschel
 */
public abstract class AbstractHitCountBreakpoint extends AbstractBreakpoint {
    /**
     * The HitCount of the Breakpoint (set by user).
     */
    private int hitCount;

    /**
     * Counter for how often the Breakpoint was hit.
     */
    private int hitted = 0;

    /**
     * Map to save the nodes that already have been reached, so nodes are not counted twice for the
     * hitcount
     */
    private final Map<Integer, Boolean> hittedNodes = new HashMap<>();

    /**
     * Creates a new {@link AbstractHitCountBreakpoint}.
     *
     * @param hitCount the number of hits after which the execution should hold at this breakpoint
     * @param proof the {@link Proof} that will be executed and should stop
     * @param enabled flag if the Breakpoint is enabled
     */
    protected AbstractHitCountBreakpoint(int hitCount, Proof proof, boolean enabled) {
        super(proof, enabled);
        this.hitCount = hitCount;
    }

    /**
     * Checks if the hitcount is exceeded for the given {@link LineBreakpoint}. If the Hitcount
     * is not exceeded the hitted counter is incremented, otherwise its set to 0.
     *
     * @return true if the Hitcount is exceeded or the {@link LineBreakpoint} has no Hitcount.
     */
    protected boolean hitcountExceeded(Node node) {
        if (!(hitCount == -1)) {
            if (!hittedNodes.containsKey(node.serialNr())) {
                if (hitCount == hitted + 1) {
                    hitted = 0;
                    hittedNodes.put(node.serialNr(), Boolean.TRUE);
                    return true;
                } else {
                    hittedNodes.put(node.serialNr(), Boolean.FALSE);
                    hitted++;
                }
            } else {
                return hittedNodes.get(node.serialNr());
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBreakpointHit(SourceElement activeStatement, RuleApp ruleApp, Node node) {
        return hitcountExceeded(node);
    }

    /**
     * Returns the hitCount of the associated Breakpoint.
     *
     * @return the hitCount of the associated Breakpoint
     */
    public int getHitCount() {
        return hitCount;
    }

    /**
     * Set the hitCount to the new value
     *
     * @param hitCount the new value
     */
    public void setHitCount(int hitCount) {
        if (this.hitCount != hitCount) {
            this.hitCount = hitCount;
            this.hitted = 0;
            hittedNodes.clear();
        }
    }
}
