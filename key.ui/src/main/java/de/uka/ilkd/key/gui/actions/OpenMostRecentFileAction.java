/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;

import de.uka.ilkd.key.core.KeYSelectionEvent;
import de.uka.ilkd.key.core.KeYSelectionListener;
import de.uka.ilkd.key.gui.KeYFileChooser;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.ProofSelectionDialog;
import de.uka.ilkd.key.gui.fonticons.IconFactory;
import de.uka.ilkd.key.proof.Proof;

/**
 * Loads the last opened file
 */
public final class OpenMostRecentFileAction extends MainWindowAction
        implements KeYSelectionListener {
    private static final long serialVersionUID = 4855372503837208313L;

    public OpenMostRecentFileAction(MainWindow mainWindow) {
        super(mainWindow);
        setName("Reload");
        setIcon(IconFactory.openMostRecent(MainWindow.TOOLBAR_ICON_SIZE));
        setTooltip("Reload last opened file.");
        setEnabled(mainWindow.getRecentFiles() != null
                && mainWindow.getRecentFiles().getMostRecent() != null);
        mainWindow.getMediator().addKeYSelectionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (mainWindow.getRecentFiles() != null
                && mainWindow.getRecentFiles().getMostRecent() != null) {
            final String recentFile = mainWindow.getRecentFiles().getMostRecent();
            if (recentFile != null) {
                Path file = new File(recentFile).toPath();
                KeYFileChooser.getFileChooser("Select file to load").setSelectedFile(file.toFile());

                if (ProofSelectionDialog.isProofBundle(file)) {
                    Path proofPath = ProofSelectionDialog.chooseProofToLoad(file);
                    if (proofPath == null) {
                        // canceled by user
                    } else {
                        mainWindow.loadProofFromBundle(file, proofPath);
                    }
                } else {
                    mainWindow.loadProblem(file);
                }
            }
        }
    }

    @Override
    public void selectedProofChanged(KeYSelectionEvent<Proof> e) {
        setEnabled(true);
    }
}
