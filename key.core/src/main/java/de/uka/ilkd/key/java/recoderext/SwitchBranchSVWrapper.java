/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java.recoderext;

import de.uka.ilkd.key.logic.op.ProgramSV;

import de.uka.ilkd.key.logic.op.SchemaVariable;
import recoder.java.ProgramElement;
import recoder.java.SourceVisitor;
import recoder.java.Statement;
import recoder.java.statement.SwitchBranch;

public class SwitchBranchSVWrapper extends SwitchBranch implements KeYRecoderExtension, SVWrapper {
    private static final long serialVersionUID = -1;
    protected ProgramSV sv;

    public SwitchBranchSVWrapper(ProgramSV sv) {
        this.sv = sv;
    }

    /**
     * sets the schema variable of sort statement
     *
     * @param sv the SchemaVariable
     */
    @Override
    public void setSV(SchemaVariable sv) {
        this.sv = (ProgramSV) sv;
    }

    /**
     * returns a String name of this meta construct.
     */
    @Override
    public ProgramSV getSV() {
        return sv;
    }

    @Override
    public void accept(SourceVisitor v) {

    }

    public SwitchBranchSVWrapper deepClone() {
        return new SwitchBranchSVWrapper(sv);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public ProgramElement getChildAt(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildPositionCode(ProgramElement child) {
        return 0;
    }

    @Override
    public boolean replaceChild(ProgramElement p, ProgramElement q) {
        return false;
    }

    @Override
    public int getStatementCount() {
        return 0;
    }

    @Override
    public Statement getStatementAt(int index) {
        throw new IndexOutOfBoundsException();
    }

}