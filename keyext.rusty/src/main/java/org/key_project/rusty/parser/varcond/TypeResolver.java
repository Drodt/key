/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.rule.inst.SVInstantiations;

/// Several variable conditions deal with types. The type resolver provides a unique interface to
/// access types, e.g. the type of a schemavariable instantiation, the instantiated type of a
/// generic
/// sort or the type an attribute is declared in.
public abstract class TypeResolver {
    public static TypeResolver createElementTypeResolver(SchemaVariable s) {
        return new ElementTypeResolverForSV(s);
    }

    public static TypeResolver createGenericSortResolver(GenericSort gs) {
        return new GenericSortResolver(gs);
    }

    public static TypeResolver createNonGenericSortResolver(Sort s) {
        return new NonGenericSortResolver(s);
    }

    public static TypeResolver createParametricSortResolver(ParametricSortInstance psi) {
        return new ParametricSortResolver(psi);
    }

    public abstract boolean isComplete(SchemaVariable sv, SyntaxElement instCandidate,
            SVInstantiations instMap, Services services);

    public abstract Sort resolveSort(SchemaVariable sv, SyntaxElement instCandidate,
            SVInstantiations instMap, Services services);

    public static class GenericSortResolver extends TypeResolver {

        private final GenericSort gs;

        public GenericSortResolver(GenericSort gs) {
            this.gs = gs;
        }

        public GenericSort getGenericSort() {
            return gs;
        }

        @Override
        public boolean isComplete(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {
            return instMap.getGenericSortInstantiations().getInstantiation(gs) != null;
        }

        @Override
        public Sort resolveSort(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {
            return instMap.getGenericSortInstantiations().getInstantiation(gs);
        }

        @Override
        public String toString() {
            return gs.toString();
        }
    }

    public static class NonGenericSortResolver extends TypeResolver {
        private final Sort s;

        public NonGenericSortResolver(Sort s) {
            this.s = s;
        }

        @Override
        public boolean isComplete(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {
            return true;
        }

        @Override
        public Sort resolveSort(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {
            return s;
        }

        public Sort getSort() {
            return s;
        }

        @Override
        public String toString() {
            return s.toString();
        }
    }

    public static class ElementTypeResolverForSV extends TypeResolver {
        private final SchemaVariable resolveSV;

        public ElementTypeResolverForSV(SchemaVariable sv) {
            this.resolveSV = sv;
        }

        @Override
        public boolean isComplete(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {
            return resolveSV == sv || instMap.getInstantiation(resolveSV) != null;
        }

        @Override
        public Sort resolveSort(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {

            final Sort s;

            final SyntaxElement inst = (SyntaxElement) (resolveSV == sv ? instCandidate
                    : instMap.getInstantiation(resolveSV));

            if (inst instanceof ProgramVariable pv) {
                s = pv.sort();
            } else {
                Term gsTerm;
                if (inst instanceof Term t) {
                    gsTerm = t;
                } else if (inst instanceof RustyProgramElement pe) {
                    gsTerm = Services.convertToLogicElement(
                        pe, services);
                } else {
                    return null;
                }
                s = gsTerm.sort();
            }
            return s;
        }

        @Override
        public String toString() {
            return "\\typeof(" + resolveSV + ")";
        }
    }

    private static class ParametricSortResolver extends TypeResolver {
        private final ParametricSortInstance psi;

        public ParametricSortResolver(ParametricSortInstance psi) {
            this.psi = psi;
        }

        @Override
        public boolean isComplete(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {
            return psi.isComplete(instMap);
        }

        @Override
        public Sort resolveSort(SchemaVariable sv, SyntaxElement instCandidate,
                SVInstantiations instMap, Services services) {
            return psi.resolveSort(sv, instCandidate, instMap);
        }

        @Override
        public String toString() {
            return psi.toString();
        }
    }
}
