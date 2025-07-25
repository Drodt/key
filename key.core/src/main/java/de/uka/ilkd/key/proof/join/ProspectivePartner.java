/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.proof.join;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.proof.Node;

import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;

/**
 * Represents the partners of a join operation.
 *
 * @author Benjamin Niedermann
 */
public class ProspectivePartner {
    private final JTerm[] updates = new JTerm[2];
    private final JTerm commonFormula;
    private final SequentFormula[] formula =
        new SequentFormula[2];
    private final Node[] nodes = new Node[2];
    private JTerm commonPredicate = null;
    private Node commonParent = null;
    private SequentFormula formulaForHiding = null;

    /**
     * Constructs a new prospective partner object, i.e. a structure comprising the information
     * about two partners of a join operation.
     *
     * @param commonFormula The common formula of a join operation, i.e. the "symbolic state -
     *        program counter" part of the join.
     * @param node1 The first node of the join.
     * @param formula1 The first join formula.
     * @param update1 The first symbolic state.
     * @param node2 The second node of the join.
     * @param formula2 The second join formula.
     * @param update2 The second symbolic state.
     */
    public ProspectivePartner(JTerm commonFormula, Node node1,
            SequentFormula formula1, JTerm update1,
            Node node2, SequentFormula formula2, JTerm update2) {
        super();
        this.commonFormula = commonFormula;
        formula[0] = formula1;
        formula[1] = formula2;
        updates[0] = update1;
        updates[1] = update2;
        nodes[0] = node1;
        nodes[1] = node2;
    }

    public JTerm getCommonFormula() {
        return commonFormula;
    }

    public Node getNode(int index) {
        return nodes[index];
    }

    public JTerm getUpdate(int index) {
        return updates[index];
    }

    public void setCommonPredicate(JTerm commonPredicate) {
        this.commonPredicate = commonPredicate;
    }

    public JTerm getCommonPredicate() {
        return commonPredicate;
    }

    public void setCommonParent(Node commonParent) {
        this.commonParent = commonParent;
        if (commonParent.getAppliedRuleApp() != null
                && commonParent.getAppliedRuleApp().posInOccurrence() != null) {
            setFormulaForHiding(
                commonParent.getAppliedRuleApp().posInOccurrence()
                        .sequentFormula());
        }
    }

    private void setFormulaForHiding(
            SequentFormula formulaForHiding) {
        this.formulaForHiding = formulaForHiding;
    }

    public SequentFormula getFormulaForHiding() {
        return formulaForHiding;
    }

    public Node getCommonParent() {
        return commonParent;
    }

    public Sequent getSequent(int index) {
        return getNode(index).sequent();
    }

    public SequentFormula getFormula(int i) {
        return formula[i];
    }

}
