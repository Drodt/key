/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java.recoderext;

import recoder.java.ProgramElement;
import recoder.java.SourceVisitor;

/**
 * A "\Return" parameter of a ccatch clause.
 *
 * @author Dominic Steinhöfel
 */
public class CcatchReturnParameterDeclaration extends CcatchNonstandardParameterDeclaration {
    private static final long serialVersionUID = 1L;

    @Override
    public ProgramElement getChildAt(int arg0) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public int getChildPositionCode(ProgramElement arg0) {
        return 0;
    }

    @Override
    public boolean replaceChild(ProgramElement arg0,
            ProgramElement arg1) {
        return false;
    }

    @Override
    public void accept(SourceVisitor v) {
        if (v instanceof SourceVisitorExtended) {
            ((SourceVisitorExtended) v).visitCcatchReturnParameterDeclaration(this);
        } else {
            // throw new IllegalStateException(
            // "Method 'accept' not implemented in Ccatch");
        }
    }

    @Override
    public CcatchReturnParameterDeclaration deepClone() {
        return new CcatchReturnParameterDeclaration();
    }

}
