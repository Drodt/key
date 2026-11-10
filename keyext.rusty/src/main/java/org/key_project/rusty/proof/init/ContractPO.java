/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.proof.init;

import org.key_project.logic.Term;
import org.key_project.rusty.speclang.Contract;

/// An obligation for some kind of contract.
public interface ContractPO extends ProofOblInput {
    Contract getContract();

    Term getMbyAtPre();
}
