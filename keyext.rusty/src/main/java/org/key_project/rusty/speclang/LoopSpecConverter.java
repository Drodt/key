/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.speclang;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.LoopExpression;
import org.key_project.rusty.ast.fn.Function;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.op.ProgramFunction;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.speclang.spec.LoopSpec;
import org.key_project.rusty.util.MiscTools;
import org.key_project.util.collection.ImmutableList;

public class LoopSpecConverter extends AbstractSpecConverter {
    public LoopSpecConverter(Services services) {
        super(services);
    }

    public LoopSpecification convert(LoopSpec loopSpec, Function fn, LoopExpression target,
            Map<HirId, ProgramVariable> pvs) {
        var invariant = Arrays.stream(loopSpec.invariants()).map(wp -> convert(wp.value(), pvs))
                .reduce(tb.tt(), tb::and);
        var variant = loopSpec.variant() == null ? null : convert(loopSpec.variant().value(), pvs);
        var localIns = tb.var(MiscTools.getLocalIns(target, services));
        var localOuts = tb.var(MiscTools.getLocalOuts(target, services));
        var atPres = createAtPres(ProgramFunction.collectParameters(fn), tb);
        return new LoopSpecImpl(target, invariant, variant, localIns, localOuts, atPres);
    }

    private static Map<ProgramVariable, Term> createAtPres(
            final ImmutableList<ProgramVariable> paramVars, final TermBuilder tb) {
        Map<ProgramVariable, Term> atPres = new LinkedHashMap<>();
        for (var param : paramVars) {
            atPres.put(param,
                tb.var(tb.atPreVar(param.toString(), param.getKeYRustyType(), false)));
        }
        return atPres;
    }
}
