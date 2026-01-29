/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.keyproject.key.api.data;

import org.key_project.rusty.proof.Node;


/**
 * @author Alexander Weigl
 * @version 1 (13.10.23)
 */
public record TreeNodeDesc(KeyIdentifications.NodeId id, String name)
        implements KeYDataTransferObject {
    public static TreeNodeDesc from(KeyIdentifications.ProofId proofId, Node root) {
        return new TreeNodeDesc(new KeyIdentifications.NodeId(proofId, "" + root.getSerialNr()),
            "");
    }
}
