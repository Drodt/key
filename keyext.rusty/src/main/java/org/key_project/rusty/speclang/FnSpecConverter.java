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

public class FnSpecConverter extends AbstractSpecConverter {
    public FnSpecConverter(Services services) {
        super(services);
    }

    public List<FunctionalOperationContract> convert(FnSpec fnSpec, ProgramFunction target) {
        setLocalParams(target.getFunction().getLocalIdsToGenericParams());
        List<FunctionalOperationContract> contracts =
            Arrays.stream(fnSpec.cases()).flatMap(c -> convert(c, target)).toList();
        clearLocalParams();
        return contracts;
    }

    public Stream<FunctionalOperationContract> convert(SpecCase specCase, ProgramFunction target) {
        final var kind = specCase.kind();
        final var name = specCase.name();
        final var result = new ProgramVariable(new Name("result"), target.getType());
        var paramVars = ImmutableList.fromList(target.getFunction().params().stream().map(p -> {
            var fp = (FunctionParamPattern) p;
            var bp = (BindingPattern) fp.pattern();
            return bp.pv();
        }).toList());
        var pre = mapAndJoinTerms(specCase.pre(), target, paramVars, result);
        var post = mapAndJoinTerms(specCase.post(), target, paramVars, result);
        Term variant;
        if (specCase.variant() == null)
            variant = null;
        else {
            setCtx(new ConversionCtx(
                params2PVs(specCase.variant().params(), target, paramVars, result)));
            variant = convert(specCase.variant().value());
        }
        setCtx(
            new ConversionCtx(params2PVs(specCase.diverges().params(), target, paramVars, result)));
        var diverges = convert(specCase.diverges().value());
        clearCtx();
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
            ProgramFunction target, ImmutableList<ProgramVariable> paramVars,
            ProgramVariable resultVar) {
        return Arrays.stream(terms)
                .map(wp -> {
                    setCtx(
                        new ConversionCtx(params2PVs(wp.params(), target, paramVars, resultVar)));
                    var c = convert(wp.value());
                    clearCtx();
                    return c;
                })
                .reduce(tb.tt(), tb::and);
    }

    private Map<HirId, ProgramVariable> params2PVs(Param[] params, ProgramFunction target,
            ImmutableList<ProgramVariable> paramVars, ProgramVariable resultVar) {
        // TODO: Get same PVs as in target or create new ones? Ask RB!
        var map = new HashMap<HirId, ProgramVariable>();
        for (int i = 0; i < paramVars.size(); i++) {
            var param = params[i];
            if (param.pat().kind() instanceof PatKind.Binding bp) {
                map.put(bp.hirId(), paramVars.get(i));
            }
        }
        if (params.length > paramVars.size()) {
            final PatKind.Binding bp = (PatKind.Binding) params[params.length - 1].pat().kind();
            map.put(bp.hirId(), resultVar);
        }
        return map;
    }
}
