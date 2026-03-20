/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.pp;

import org.key_project.logic.Name;
import org.key_project.rusty.ast.abstraction.KeYRustyType;
import org.key_project.rusty.ast.abstraction.TupleType;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.logic.op.ProgramVariable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrettyPrinterTest {
    @Test
    void printPV() {
        var pp = PrettyPrinter.purePrinter();
        var pv =
            new ProgramVariable(new Name("x"), new KeYRustyType(TupleType.UNIT, RustyDLTheory.ANY));
        pp.printFragment(pv);
        assertEquals("x", pp.result());
    }
}
