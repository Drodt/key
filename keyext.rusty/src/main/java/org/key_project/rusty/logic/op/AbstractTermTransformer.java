/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.rusty.logic.sort.SortImpl;
import org.key_project.rusty.rule.inst.ProgramListInstantiation;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.rusty.rule.metaconstruct.CreateFrameCond;
import org.key_project.rusty.rule.metaconstruct.CreateLocalAnonUpdate;
import org.key_project.rusty.rule.metaconstruct.IntroAtPreDefs;
import org.key_project.rusty.rule.metaconstruct.arith.*;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

/// Abstract class factoring out commonalities of typical term transformer implementations. The
/// available singletons of term transformers are kept here.
public abstract class AbstractTermTransformer extends AbstractSortedOperator
        implements TermTransformer {
    // must be first
    /// The metasort sort
    public static final Sort METASORT = new SortImpl(new Name("Meta"));

    /// A map from String names to meta operators
    public static final Map<String, AbstractTermTransformer> NAME_TO_META_OP =
        new LinkedHashMap<>(17);

    public static final AbstractTermTransformer META_SHIFTRIGHT = new MetaShiftRight();
    public static final AbstractTermTransformer META_SHIFTLEFT = new MetaShiftLeft();
    public static final AbstractTermTransformer META_AND = new MetaBinaryAnd();
    public static final AbstractTermTransformer META_OR = new MetaBinaryOr();
    public static final AbstractTermTransformer META_XOR = new MetaBinaryXor();
    public static final AbstractTermTransformer META_ADD = new MetaAdd();
    public static final AbstractTermTransformer META_SUB = new MetaSub();
    public static final AbstractTermTransformer META_MUL = new MetaMul();
    public static final AbstractTermTransformer META_DIV = new MetaDiv();
    public static final AbstractTermTransformer META_POW = new MetaPow();
    public static final AbstractTermTransformer META_LESS = new MetaLess();
    public static final AbstractTermTransformer META_GREATER = new MetaGreater();
    public static final AbstractTermTransformer META_LEQ = new MetaLeq();
    public static final AbstractTermTransformer META_GEQ = new MetaGeq();
    public static final AbstractTermTransformer META_EQ = new MetaEqual();

    public static final AbstractTermTransformer DIVIDE_MONOMIALS = new DivideMonomials();
    public static final AbstractTermTransformer DIVIDE_LCR_MONOMIALS = new DivideLCRMonomials();

    public static final AbstractTermTransformer PV_TO_PLACE = new PVToPlace();
    public static final AbstractTermTransformer PLACE_TO_UPDATE = new PlaceToUpdate();

    public static final AbstractTermTransformer CREATE_LOCAL_ANON_UPDATE =
        new CreateLocalAnonUpdate();
    public static final AbstractTermTransformer CREATE_FRAME_COND = new CreateFrameCond();

    public static final AbstractTermTransformer INTRODUCE_AT_PRE_DEFINITIONS = new IntroAtPreDefs();

    public static final AbstractTermTransformer TO_TUPLE = new ToTuple();

    @SuppressWarnings("argument.type.incompatible")
    protected AbstractTermTransformer(Name name, int arity, Sort sort) {
        super(name, createMetaSortArray(arity), sort, Modifier.NONE);
        NAME_TO_META_OP.put(name.toString(), this);
    }

    protected AbstractTermTransformer(Name name, int arity) {
        this(name, arity, METASORT);
    }

    private static Sort[] createMetaSortArray(int arity) {
        Sort[] result = new Sort[arity];
        Arrays.fill(result, METASORT);
        return result;
    }

    public static @Nullable TermTransformer name2metaop(String s) {
        return NAME_TO_META_OP.get(s);
    }

    private static class PVToPlace extends AbstractTermTransformer {
        public PVToPlace() {
            super(new Name("pvToPlace"), 1);
        }

        @Override
        public Term transform(Term term, SVInstantiations svInst, Services services) {
            return getPlace(term.sub(0), services);
        }
    }

    private static class PlaceToUpdate extends AbstractTermTransformer {
        public PlaceToUpdate() {
            super(new Name("placeToUpdate"), 2);
        }

        @Override
        public Term transform(Term term, SVInstantiations svInst, Services services) {
            var place = term.sub(0);
            var t = term.sub(1);
            var placeName = place.op().name().toString();
            var pvName = placeName.substring(1, place.op().name().toString().length() - 1);
            var pv = services.getNamespaces().programVariables().lookup(pvName);
            return services.getTermBuilder().elementary(pv, t);
        }
    }

    private static class ToTuple extends AbstractTermTransformer {
        public ToTuple() {
            super(new Name("toTuple"), 1);
        }

        @Override
        public Term transform(Term term, SVInstantiations svInst, Services services) {
            var sv = term.sub(0);
            var pes =
                (ProgramListInstantiation) svInst.getInstantiationEntry((SchemaVariable) sv.op());

            var terms = new Term[pes.getInstantiation().size()];
            for (int i = 0; i < terms.length; i++) {
                terms[i] = services.convertToLogicElement(pes.getInstantiation().get(i));
            }
            return services.getTermBuilder().tuple(terms);
        }
    }

    public static Term getPlace(Term t, Services services) {
        if (t.op() instanceof ProgramVariable pv) {
            var name = pv.name();
            var tb = services.getTermBuilder();
            var c = services.getNamespaces().functions().lookup("/" + name + "/");
            if (c == null) {
                var innerSort = t.sort();
                var pSort = services.getNamespaces().parametricSorts().lookup("Place");
                assert pSort != null;
                var sort =
                    ParametricSortInstance.get(pSort, ImmutableList.of(new SortArg(innerSort)));
                c = new RFunction(new Name("/" + name + "/"), sort);
                services.getNamespaces().functions().add(c);
            }

            return tb.func(c);
        }
        throw new IllegalArgumentException("Cannot convert " + t + " to a Place");
    }

    /// @return String representing a logical integer literal in decimal representation
    public static String convertToDecimalString(Term term, Services services) {
        StringBuilder result = new StringBuilder();
        boolean neg = false;

        Operator top = term.op();
        IntLDT intModel = services.getLDTs().getIntLDT();
        final Operator numbers = intModel.getNumberSymbol();
        final Operator base = intModel.getNumberTerminator();
        final Operator minus = intModel.getNegativeNumberSign();
        // check whether term is really a "literal"

        // skip any updates that have snuck in (int lits are rigid)
        while (top == UpdateApplication.UPDATE_APPLICATION) {
            term = term.sub(1);
            top = term.op();
        }

        if (top != numbers) {
            // LOGGER.debug("abstractmetaoperator: Cannot convert to number: {}", term);
            throw new NumberFormatException();
        }

        term = term.sub(0);
        top = term.op();

        // skip any updates that have snuck in (int lits are rigid)
        while (top == UpdateApplication.UPDATE_APPLICATION) {
            term = term.sub(1);
            top = term.op();
        }

        while (top == minus) {
            neg = !neg;
            term = term.sub(0);
            top = term.op();

            // skip any updates that have snuck in (int lits are rigid)
            while (top == UpdateApplication.UPDATE_APPLICATION) {
                term = term.sub(1);
                top = term.op();
            }
        }

        while (top != base) {
            result.insert(0, top.name());
            term = term.sub(0);
            top = term.op();

            // skip any updates that have snuck in (int lits are rigid)
            while (top == UpdateApplication.UPDATE_APPLICATION) {
                term = term.sub(1);
                top = term.op();
            }
        }

        if (neg) {
            result.insert(0, "-");
        }

        return result.toString();
    }
}
