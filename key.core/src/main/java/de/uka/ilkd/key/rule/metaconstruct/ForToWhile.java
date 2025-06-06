/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.metaconstruct;

import de.uka.ilkd.key.java.ProgramElement;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.Statement;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

/**
 * converts a for-loop to a while loop. Invariant and other rules cannot be performed on for but
 * only on while loops.
 *
 * It makes uses of the {@link ForToWhileTransformation} to create a transformed loop body which is
 * then put into the corresponding context.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * for (int i = 0; i &lt; 100; i++) {
 *     if (i == 2)
 *         continue;
 *     if (i == 3)
 *         break;
 * }
 * </pre>
 *
 * is translated to
 *
 * <pre>
 * _label1: {
 *     int i = 0;
 *     while (i &lt; 100) {
 *         _label0: {
 *             if (i == 2)
 *                 break _label0;
 *             if (i == 3)
 *                 break _label1;
 *         }
 *         i++;
 *     }
 * }
 * </pre>
 *
 * @see ForToWhileTransformation
 * @author MU
 */

public class ForToWhile extends ProgramTransformer {

    /**
     * the outer label that is used to leave the while loop ('l1')
     */
    private final SchemaVariable outerLabel;

    /**
     * the inner label ('l2')
     */
    private final SchemaVariable innerLabel;

    /**
     * creates an loop to while - ProgramTransformer
     *
     * @param loop the LoopStatement contained by the meta construct
     * @param innerLabel the label used to handle continue
     * @param outerLabel the label used to handle break (only needed for do-while-loops)
     */
    public ForToWhile(SchemaVariable innerLabel, SchemaVariable outerLabel, Statement loop) {
        super("#for-to-while", loop);
        this.innerLabel = innerLabel;
        this.outerLabel = outerLabel;

    }

    @Override
    public ProgramElement[] transform(ProgramElement pe, Services services,
            SVInstantiations svInst) {

        WhileLoopTransformation w = new ForToWhileTransformation(pe,
            (ProgramElementName) svInst.getInstantiation(outerLabel),
            (ProgramElementName) svInst.getInstantiation(innerLabel), services);

        w.start();
        return new ProgramElement[] { w.result() };
    }

    /**
     * return a list of the SV that are relevant to this UnwindLoop
     *
     * @param svInst the instantiations so far - ignored
     * @return a list of 0 to 2 schema variables (outer/inner label)
     */
    @Override
    public ImmutableList<SchemaVariable> neededInstantiations(SVInstantiations svInst) {
        ImmutableList<SchemaVariable> ret = ImmutableSLList.nil();

        if (innerLabel != null) {
            ret = ret.prepend(innerLabel);
        }

        if (outerLabel != null) {
            ret = ret.prepend(outerLabel);
        }

        return ret;
    }
}
