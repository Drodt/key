/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.exploration.actions;

import java.awt.event.ActionEvent;

import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.pp.PosInSequent;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;

import org.key_project.exploration.ProofExplorationService;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;

/**
 * Action to edit formulas in the actions mode
 *
 * @author Alexander Weigl
 * @author Sarah Grebing
 * @version 2 (25.05.18)
 */
public class EditFormulaAction extends ExplorationAction {
    private final transient PosInSequent posInSeq;

    public EditFormulaAction(PosInSequent pis) {
        this(pis, MainWindow.getInstance());
    }

    public EditFormulaAction(PosInSequent pis, MainWindow mainWindow) {
        super(mainWindow);
        setName("Edit formula");
        this.posInSeq = pis;
        // enable only if position is in sequent
        setEnabled(!pis.isSequent());
    }

    /**
     * If action is chosen in context menu
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (posInSeq.isSequent()) {
            return;
        }

        TermBuilder tb = getMediator().getServices().getTermBuilder();
        PosInOccurrence pio = posInSeq.getPosInOccurrence();
        JTerm term = (JTerm) pio.subTerm();
        SequentFormula sf = pio.sequentFormula();
        Goal g = getMediator().getSelectedGoal();
        JTerm newTerm = promptForTerm(mainWindow, term);

        if (newTerm.equals(term)) {
            return;
        }

        ProofExplorationService api = ProofExplorationService.get(getMediator());
        final JTerm formula = (JTerm) sf.formula();
        Node toBeSelected = api.applyChangeFormula(g, pio, formula,
            tb.replace(formula, pio.posInTerm(), newTerm));
        getMediator().getSelectionModel().setSelectedNode(toBeSelected);
    }
}
