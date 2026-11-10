/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.proof.init;

public class ProofInputException extends Exception {
    public ProofInputException(Exception e) {
        this(e.getMessage(), e);
    }

    public ProofInputException(String s) {
        this(s, null);
    }

    public ProofInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
