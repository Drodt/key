/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.speclang;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.fn.FunctionParamPattern;
import org.key_project.rusty.ast.pat.BindingPattern;
import org.key_project.rusty.logic.op.*;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.item.Param;
import org.key_project.rusty.parser.hir.pat.PatKind;
import org.key_project.rusty.speclang.spec.*;
import org.key_project.util.collection.ImmutableList;

public class FnSpecConverter  extends AbstractSpecConverter {

    public FnSpecConverter(Services services) {
        super(services);
    }

    public List<FunctionalOperationContract> convert(FnSpec fnSpec, ProgramFunction target) {
        return Arrays.stream(fnSpec.cases()).flatMap(c -> convert(c, target)).toList();
    }

    public Stream<FunctionalOperationContract> convert(SpecCase specCase, ProgramFunction target) {
        final var kind = specCase.kind();
        final var name = specCase.name();
        final var result = new ProgramVariable(new Name("result"), target.getType());
        var pre = mapAndJoinTerms(specCase.pre(), target, result);
        var post = mapAndJoinTerms(specCase.post(), target, result);
        var variant = specCase.variant() == null ? null
                : convert(specCase.variant().value(),
                    params2PVs(specCase.variant().params(), target, result));
        var diverges = convert(specCase.diverges().value(),
            params2PVs(specCase.diverges().params(), target, result));
        var paramVars = ImmutableList.fromList(target.getFunction().params().stream().map(p -> {
            var fp = (FunctionParamPattern) p;
            var bp = (BindingPattern) fp.pattern();
            return bp.pv();
        }).toList());
        if (diverges == tb.ff()) {
            return Stream.of(new FunctionalOperationContractImpl(name, name, target,
                RModality.RustyModalityKind.DIA, pre, variant, post, null, paramVars, result,
                null, 0, true, services));
        }
        if (diverges == tb.tt()) {
            return Stream.of(new FunctionalOperationContractImpl(name, name, target,
                RModality.RustyModalityKind.BOX, pre, variant, post, null, paramVars, result,
                null, 0, true, services));
        }
        throw new UnsupportedOperationException("TODO: Unsupported diverges: " + diverges);
    }

    private Term mapAndJoinTerms(WithParams<org.key_project.rusty.speclang.spec.Term>[] terms,
            ProgramFunction target, ProgramVariable resultVar) {
        return Arrays.stream(terms)
                .map(wp -> convert(wp.value(), params2PVs(wp.params(), target, resultVar)))
                .reduce(tb.tt(), tb::and);
    }

    private Map<HirId, ProgramVariable> params2PVs(Param[] params, ProgramFunction target,
            ProgramVariable resultVar) {
        // TODO: Get same PVs as in target or create new ones? Ask RB!
        var map = new HashMap<HirId, ProgramVariable>();
        for (int i = 0; i < params.length; i++) {
            var param = params[i];
            if (param.pat().kind() instanceof PatKind.Binding bp) {
                if (i == target.getNumParams()) {
                    map.put(bp.hirId(), resultVar);
                } else {
                    map.put(bp.hirId(),
                        new ProgramVariable(new Name(bp.ident().name()), target.getParamType(i)));
                }
            }
        }
        return map;
    }

}
