/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.key_project.logic.*;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.ParsableVariable;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.RuleSet;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.*;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.logic.op.AbstractTermTransformer;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.sort.*;
import org.key_project.rusty.parser.KeYRustyParser;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class DefaultBuilder extends AbstractBuilder<@Nullable Object> {
    protected final Services services;
    protected final NamespaceSet nss;
    private Namespace<@NonNull SchemaVariable> schemaVariablesNamespace = new Namespace<>();


    public DefaultBuilder(Services services, NamespaceSet nss) {
        this.services = services;
        this.nss = nss;
    }

    @Override
    public List<String> visitPvset(KeYRustyParser.PvsetContext ctx) {
        return mapOf(ctx.varId());
    }

    @Override
    public List<RuleSet> visitRulesets(KeYRustyParser.RulesetsContext ctx) {
        return mapOf(ctx.ruleset());
    }

    @Override
    public RuleSet visitRuleset(KeYRustyParser.RulesetContext ctx) {
        String id = ctx.IDENT().getText();
        Name name = new Name(id);
        RuleSet h = ruleSets().lookup(name);
        if (h == null) {
            semanticError(ctx, String.format("Rule set %s was not defined.", name));
        }
        return h;
    }

    protected Named lookup(Name n) {
        final Namespace<?>[] lookups =
            { programVariables(), variables(), functions() };
        return doLookup(n, lookups);
    }

    protected <T> T doLookup(Name n, Namespace<?>... lookups) {
        for (Namespace<?> lookup : lookups) {
            Object l;
            if (lookup != null && (l = lookup.lookup(n)) != null) {
                try {
                    return (T) l;
                } catch (ClassCastException e) {
                }
            }
        }
        return null;
    }

    public NamespaceSet namespaces() {
        return nss;
    }

    protected Namespace<@NonNull QuantifiableVariable> variables() {
        return namespaces().variables();
    }

    protected Namespace<@NonNull Sort> sorts() {
        return namespaces().sorts();
    }

    protected Namespace<@NonNull Function> functions() {
        return namespaces().functions();
    }

    protected Namespace<@NonNull RuleSet> ruleSets() {
        return namespaces().ruleSets();
    }

    protected Namespace<@NonNull Choice> choices() {
        return namespaces().choices();
    }

    protected Namespace<@NonNull ProgramVariable> programVariables() {
        return namespaces().programVariables();
    }

    protected <T> T withSortAndConsts(Namespace<@NonNull Sort> sorts,
            Namespace<@NonNull Function> consts, Supplier<T> fn) {
        var oldSorts = nss.sorts();
        var oldFns = nss.functions();
        nss.setSorts(sorts);
        nss.setFunctions(consts);
        var res = fn.get();
        nss.setSorts(oldSorts);
        nss.setFunctions(oldFns);
        return res;
    }

    public String visitSimple_ident_dots(KeYRustyParser.Simple_ident_dotsContext ctx) {
        return ctx.getText();
    }

    public List<Sort> visitArg_sorts_or_formula(KeYRustyParser.Arg_sorts_or_formulaContext ctx) {
        return mapOf(ctx.arg_sorts_or_formula_helper());
    }

    public Sort visitArg_sorts_or_formula_helper(
            KeYRustyParser.Arg_sorts_or_formula_helperContext ctx) {
        if (ctx.FORMULA() != null) {
            return RustyDLTheory.FORMULA;
        } else {
            return accept(ctx.sortId());
        }
    }

    protected void unbindVars(Namespace<@NonNull QuantifiableVariable> orig) {
        namespaces().setVariables(orig);
    }

    /// looks up and returns the sort of the given name or null if none has been found
    protected Sort lookupSort(String name) {
        return sorts().lookup(new Name(name));
    }

    /// looks up a function, (program) variable or static query of the given name varfunc_id and the
    /// argument terms args in the namespaces and Rust info.
    ///
    /// @param varfuncName the String with the symbols name
    /// @param genericArgsCtxt
    protected Operator lookupVarfuncId(ParserRuleContext ctx, String varfuncName,
            KeYRustyParser.Formal_sort_argsContext genericArgsCtxt) {
        Name name = new Name(varfuncName);
        Operator[] operators =
            { schemaVariables().lookup(name), variables().lookup(name),
                programVariables().lookup(new Name(varfuncName)),
                functions().lookup(name), AbstractTermTransformer.name2metaop(varfuncName) };

        for (Operator op : operators) {
            if (op != null) {
                return op;
            }
        }

        if (genericArgsCtxt != null) {
            var d = nss.parametricFunctions().lookup(name);
            if (d == null) {
                semanticError(ctx, "Could not find parametric function: %s", name);
                return null;
            }
            var args = getParamSortArgs(genericArgsCtxt, d.getParameters());
            return ParametricFunctionInstance.get(d, args);
        }
        semanticError(ctx, "Could not find (program) variable or constant %s", varfuncName);
        return null;
    }

    public String visitString_value(KeYRustyParser.String_valueContext ctx) {
        return ctx.getText().substring(1, ctx.getText().length() - 1);
    }

    public Services getServices() {
        return services;
    }

    public Namespace<SchemaVariable> schemaVariables() {
        return schemaVariablesNamespace;
    }

    public void setSchemaVariables(Namespace<SchemaVariable> ns) {
        this.schemaVariablesNamespace = ns;
    }

    @Override
    public Object visitVarIds(KeYRustyParser.VarIdsContext ctx) {
        Collection<String> ids = accept(ctx.simple_ident_comma_list());
        List<ParsableVariable> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            ParsableVariable v = (ParsableVariable) lookup(new Name(id));
            if (v == null) {
                semanticError(ctx, "Variable " + id + " not declared.");
            }
            list.add(v);
        }
        return list;
    }

    @Override
    public Object visitSimple_ident_dots_comma_list(
            KeYRustyParser.Simple_ident_dots_comma_listContext ctx) {
        return mapOf(ctx.simple_ident_dots());
    }

    @Override
    public String visitSimple_ident(KeYRustyParser.Simple_identContext ctx) {
        return ctx.IDENT().getText();
    }

    @Override
    public List<String> visitSimple_ident_comma_list(
            KeYRustyParser.Simple_ident_comma_listContext ctx) {
        return mapOf(ctx.simple_ident());
    }

    @Override
    public List<Boolean> visitWhere_to_bind(KeYRustyParser.Where_to_bindContext ctx) {
        List<Boolean> list = new ArrayList<>(ctx.children.size());
        ctx.b.forEach(it -> list.add(it.getText().equalsIgnoreCase("true")));
        return list;
    }

    @Override
    public List<Sort> visitArg_sorts(KeYRustyParser.Arg_sortsContext ctx) {
        return mapOf(ctx.sortId());
    }

    @Override
    public Sort visitSortId(KeYRustyParser.SortIdContext ctx) {
        String name = ctx.id.getText();
        Sort s;
        if (ctx.formal_sort_args() != null) {
            // parametric sorts should be instantiated
            ParametricSortDecl sortDecl = nss.parametricSorts().lookup(name);
            if (sortDecl == null) {
                semanticError(ctx, "Could not find polymorphic sort: %s", name);
            }
            ImmutableList<GenericArgument> parameters =
                getParamSortArgs(ctx.formal_sort_args(), sortDecl.getParameters());
            s = ParametricSortInstance.get(sortDecl, parameters);
        } else {
            s = lookupSort(name);
            if (s == null) {
                semanticError(ctx, "Could not find sort: %s", ctx.getText());
            }
        }
        return s;
    }

    protected ImmutableList<GenericArgument> getParamSortArgs(
            KeYRustyParser.Formal_sort_argsContext ctx, ImmutableList<GenericParameter> params) {
        if (ctx.formal_sort_arg().size() != params.size()) {
            semanticError(ctx, "Expected %d sort arguments, got only %d",
                params.size(), ctx.formal_sort_arg().size());
        }
        ImmutableList<GenericArgument> args = ImmutableSLList.nil();
        for (int i = params.size() - 1; i >= 0; i--) {
            var expectConst = params.get(i) instanceof ConstParam;
            var arg = ctx.formal_sort_arg(i);
            var isConst = arg.CONST() != null;
            if (isConst && !expectConst) {
                semanticError(arg, "Expected argument %s to be a sort argument but got const %s",
                    params.get(i), arg.getText());
            }
            if (!isConst && expectConst) {
                semanticError(arg, "Expected argument %s to be a const argument but got sort %s",
                    params.get(i), arg.getText());
            }
            if (isConst) {
                var t = visitTerm(arg.term());
                Term c;
                if (t instanceof String s) {
                    var op = nss.functions().lookup(s);
                    if (op == null) {
                        semanticError(arg, "Could not find constant: %s", s);
                    }
                    c = services.getTermBuilder().func(op);
                } else {
                    c = (Term) t;
                }

                Sort expectedSort = ((ConstParam) params.get(i)).sort();
                if (!c.sort().extendsTrans(expectedSort) && !(c.op() instanceof SchemaVariable)) {
                    semanticError(arg, "Constant %s is sort %s, which does not extend %s", c,
                        c.sort(), expectedSort);
                }
                args = args.prepend(new TermArg(c));
            } else {
                var sort = visitSortId(arg.sortId());
                args = args.prepend(new SortArg(sort));
            }
        }
        return args;
    }

    public KeYRustyType visitTypemapping(KeYRustyParser.TypemappingContext ctx) {
        if (ctx.typemapping() == null) {
            String type = visitSimple_ident(ctx.simple_ident());
            KeYRustyType krt = services.getRustInfo().getKeYRustyType(type);
            if (krt == null) {
                Sort sort = lookupSort(type);
                if (sort != null) {
                    krt = new KeYRustyType(null, sort);
                }
            }
            if (krt == null) {
                semanticError(ctx, "Unknown type: " + type);
            }

            return krt;
        }

        if (ctx.AND() != null) {
            boolean mut = ctx.MUT() != null;
            KeYRustyType krt = accept(ctx.typemapping());
            Type ty = ReferenceType.get(krt.getRustyType(), mut);
            return services.getRustInfo().getKeYRustyType(ty);
        } else {
            // Array
            KeYRustyType krt = accept(ctx.typemapping());
            ArrayLen len;
            if (ctx.simple_ident() != null) {
                String l = visitSimple_ident(ctx.simple_ident());
                Function fn = services.getNamespaces().functions().lookup(l);
                if (fn == null) {
                    semanticError(ctx.simple_ident(), "Unknown function: " + l);
                }
                if (!fn.argSorts().isEmpty()) {
                    semanticError(ctx.simple_ident(), "Expected constant, got: " + fn);
                }
                if (fn.sort() == services.getLDTs().getIntLDT().targetSort()) {
                    semanticError(ctx.simple_ident(), "Expected int constant, got: " + fn);
                }
                len = new ConstArrayLen(fn);
            } else {
                int l = Integer.parseInt(ctx.INT_LITERAL().getText());
                len = new IntArrayLen(l);
            }
            Type ty = ArrayType.getInstance(krt.getRustyType(), len, services);
            return services.getRustInfo().getKeYRustyType(ty);
        }
    }

    public Object visitFuncpred_name(KeYRustyParser.Funcpred_nameContext ctx) {
        return ctx.getText();
    }

    @Override
    public @Nullable List<GenericParameter> visitFormal_sort_param_decls(
            KeYRustyParser.Formal_sort_param_declsContext ctx) {
        return mapOf(ctx.formal_sort_param_decl());
    }

    @Override
    public GenericParameter visitFormal_sort_param_decl(
            KeYRustyParser.Formal_sort_param_declContext ctx) {
        if (ctx.simple_ident() != null) {
            var name = ctx.simple_ident().getText();

            return new GenericSortParam(new GenericSort(new Name(name)));
        }
        var name = new Name(ctx.const_param_decl().simple_ident().getText());
        var sort = visitSortId(ctx.const_param_decl().sortId());
        return new ConstParam(name, sort);
    }
}
