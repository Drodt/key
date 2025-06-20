/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.gui.nodeviews;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.pp.PosInSequent;
import de.uka.ilkd.key.proof.io.ProofSaver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class in an implementation of the {@link Transferable} interface and allows to transfer a
 * {@link PosInSequent} object. It supports to data flavors:
 * <ul>
 * <li>{@link PosInSequentTransferable#POS_IN_SEQUENT_TRANSFER} flavor which is of mime type
 * {@link DataFlavor#javaJVMLocalObjectMimeType}</li>
 * <li>{@link DataFlavor#stringFlavor} which returns the term described by the
 * {@link de.uka.ilkd.key.pp.PosInSequent} as a parsable string</li>
 * </ul>
 */
public class PosInSequentTransferable implements Transferable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PosInSequentTransferable.class);

    public static DataFlavor POS_IN_SEQUENT_TRANSFER;
    static {
        try {
            POS_IN_SEQUENT_TRANSFER = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
        } catch (ClassNotFoundException e) {
            LOGGER.info("POS_IN_SEQUENT_TRANSFER not supported, using string flavor behaviour", e);
        }
    }

    /** the highlighted position in the sequentview to be transferred */
    private final PosInSequent pis;

    /** the highlighted term as parseable string */
    private String stringSelection;


    /**
     * creates an instance of this transferable
     *
     * @param pis the PosInSequent to be transfered (string flavor only supported if pis denotes a
     *        term or formula, not the complete sequent)
     */
    public PosInSequentTransferable(PosInSequent pis, Services serv) {
        this.pis = pis;
        if (!pis.isSequent()) {
            this.stringSelection =
                ProofSaver.printTerm((JTerm) pis.getPosInOccurrence().subTerm(), serv);
        }
    }

    /**
     * returns the supported flavors of this transferable. These are currently
     * {@link DataFlavor#stringFlavor} and {@link PosInSequentTransferable#POS_IN_SEQUENT_TRANSFER}
     *
     * @see java.awt.datatransfer.Transferable#getTransferDataFlavors()
     */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { POS_IN_SEQUENT_TRANSFER, DataFlavor.stringFlavor };
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * java.awt.datatransfer.Transferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor != null && (flavor.equals(POS_IN_SEQUENT_TRANSFER)
                || flavor.equals(DataFlavor.stringFlavor));
    }

    /**
     * if the flavor is equal to the {@link PosInSequentTransferable#POS_IN_SEQUENT_TRANSFER} the
     * return data is of kind {@link PosInSequent}. If the flavor equals
     * {@link DataFlavor#stringFlavor} the highlighted term is returned as parsable string.
     *
     * @throws UnsupportedFlavorException if the flavor is not supported
     */
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (flavor != null) {
            if (flavor.equals(POS_IN_SEQUENT_TRANSFER)) {
                return pis;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return stringSelection;
            }
        }
        throw new UnsupportedFlavorException(flavor);
    }

}
