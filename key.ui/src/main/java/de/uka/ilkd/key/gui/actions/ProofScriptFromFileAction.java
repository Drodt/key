/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.*;

import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.gui.IssueDialog;
import de.uka.ilkd.key.gui.KeYFileChooser;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.ProofScriptWorker;
import de.uka.ilkd.key.nparser.ParsingFacade;
import de.uka.ilkd.key.proof.Proof;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ProofScriptFromFileAction.
 *
 * @author Mattias Ulbrich
 */
public class ProofScriptFromFileAction extends AbstractAction {
    private static final long serialVersionUID = -3181592516055470032L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProofScriptFromFileAction.class);

    private final KeYMediator mediator;

    private static Path lastDirectory;

    /**
     * Instantiates a new proof script from file action.
     *
     * @param mediator the mediator
     */
    public ProofScriptFromFileAction(KeYMediator mediator) {
        super("Run proof script from file...");
        this.mediator = mediator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Path dir = Paths.get(".");
        if (lastDirectory != null) {
            dir = lastDirectory;
        } else {
            Proof currentProof = mediator.getSelectedProof();
            if (currentProof != null) {
                Path currentFile = currentProof.getProofFile();
                if (currentFile != null) {
                    dir = currentFile.getParent();
                }
            }
        }

        try {
            MainWindow mainWindow = MainWindow.getInstance();

            KeYFileChooser fc = KeYFileChooser.getFileChooser("Select file to load");
            fc.setFileFilter(fc.getAcceptAllFileFilter());
            fc.setCurrentDirectory(dir.toFile());
            int res = fc.showOpenDialog(mainWindow);
            if (res == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fc.getSelectedFile();
                lastDirectory = selectedFile.getParentFile().toPath();
                var script = ParsingFacade.parseScript(selectedFile.toPath());
                ProofScriptWorker psw = new ProofScriptWorker(mediator, script);
                psw.init();
                psw.execute();
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
            IssueDialog.showExceptionDialog(MainWindow.getInstance(), ex);
        }
    }
}
