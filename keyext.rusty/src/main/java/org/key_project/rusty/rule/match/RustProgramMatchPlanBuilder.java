/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.matcher.compiler.*;
import org.key_project.prover.rules.matcher.vm.MatchProgram;
import org.key_project.prover.rules.matcher.vm.VMProgramInterpreter;
import org.key_project.prover.rules.matcher.vm.instruction.MatchByEqualsInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.VMInstruction;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.Identifier;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.ast.abstraction.SchemaType;
import org.key_project.rusty.ast.expr.*;
import org.key_project.rusty.ast.pat.BindingPattern;
import org.key_project.rusty.ast.pat.SchemaVarPattern;
import org.key_project.rusty.ast.ty.SchemaRustType;
import org.key_project.rusty.logic.PosInProgram;
import org.key_project.rusty.logic.PossibleProgramPrefix;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.rusty.rule.match.instructions.MatchContextBlockExpressionInstruction;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet.getMatchIdentityInstruction;
import static org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet.getMatchInstructionForSV;

/// Builds the [ProgramMatchPlan] for one Java program (sub)element of a modality. A taclet's
/// find pattern may contain a piece of Java code inside a modality (`\\<{ ... }\\>`); matching
/// decides whether that pattern fits the program of a concrete formula, and binds the pattern's
/// _schema variables_ (placeholders such as `#se` or `#slist`) to the concrete
/// program parts they stand for (their _instantiations_).
///
/// A plan is a single description of how one pattern element is matched, from which both matcher
/// back-ends are derived:
/// [ProgramMatchPlan#emit] produces the interpreter's VM instructions and
/// [ProgramMatchPlan#compile] the compiled, cursor-free matcher. It is the program-AST
/// counterpart of [RustMatchPlanBuilder] (which does the same for the first-order term
/// skeleton). On the compiled back-end this dispatch is the _whole_ program matcher: it
/// reads only the structure of pattern and source and never calls an AST
/// `match(SourceData, MatchConditions)` method. (The interpreter back-end has its own
/// conversion, `SyntaxElementMatchProgramGenerator`, with the monolithic
/// `MatchProgramInstruction` beneath it.)
///
/// The plans form a small immutable tree that mirrors the pattern, with one node kind per way of
/// matching:
///
/// - [ProgramLeafPlan]: a plan-tree leaf matched by a single instruction (schema variables,
/// schema type references, and value-carrying leaves such as literals);
/// - [ProgramListSVPlan]: a list program schema variable, which matches a run of source children
/// and is driven by its enclosing [ProgramStructuralPlan];
/// - [ProgramStructuralPlan]: the default **generic match**, i.e. same concrete class as the
/// pattern, an optional extra field check (the `guard`, e.g. array dimensions), and the
/// children matched recursively;
/// - [ContextBlockPlan]: the `.. ...` context pattern of symbolic-execution
/// taclets.
///
/// "Generic" is used throughout in the sense of [#isGenericMatch]: the
/// class-identity-plus-children match that `JavaProgramElement` /
/// `JavaNonTerminalProgramElement` provide by default, for elements that do not override
/// `match`. It has nothing to do with Java generics or generic sorts. Each node's
/// `toString` renders its subtree, so a whole plan can be printed for debugging.
///
/// [#buildProgramPlan] returns `null` for an element this dispatch does not describe.
/// On the compiled back-end the modality then has no head and the taclet fails to load with the
/// framework's clear "no head for this find pattern" error (the same contract the term side has);
/// on the interpreter back-end the conversion falls back to the monolithic matcher. Every construct
/// expressible in the taclet language is described.
public final class RustProgramMatchPlanBuilder {
    private RustProgramMatchPlanBuilder() {}

    /// Dispatches on the kind of program element and returns the plan that matches it.
    ///
    /// @param pe a program (sub)element pattern
    /// @return a plan matching `pe`, or `null` if this dispatch does not describe
    /// `pe`'s kind (see the class comment for what the two back-ends do then)
    public static @Nullable ProgramMatchPlan buildProgramPlan(SyntaxElement pe) {
        // The dispatch order is significant: a subtype must be checked before its supertype
        // (SchemaTypeReference extends TypeReferenceImp), and ProgramElementName is a Name, not a
        // ProgramElement, so it must be handled before the ProgramElement gate below.
        if (pe instanceof ProgramSV psv) {
            return psv.isListSV() ? new ProgramListSVPlan(psv)
                    : new ProgramLeafPlan(psv, getMatchInstructionForSV(psv));
        }
        if (pe instanceof SchemaVariable) {
            return null; // other schema variables in programs are not describable
        }
        // value-carrying leaves matched by equals (class + payload): Literal (+ subclasses),
        // TransactionStatement, and ProgramElementName (a name identity). They recurse into no
        // children and bind no schema variables.
        if (pe instanceof LiteralExpression || pe instanceof Identifier
                || pe instanceof UnaryExpression.Operator
                || pe instanceof BinaryExpression.Operator) {
            return new ProgramLeafPlan(pe, new MatchByEqualsInstruction(pe));
        }
        // concrete program variables and methods occurring literally in a pattern (e.g.
        // "let x = 0;" in a handwritten taclet) match by reference identity, mirroring
        // ProgramVariable.match / ProgramMethod.match
        if (pe instanceof ProgramVariable) {
            return new ProgramLeafPlan(pe, getMatchIdentityInstruction(pe));
        }
        if (!(pe instanceof RustyProgramElement progEl)) {
            return null;
        }
        switch (progEl) {
            case ContextBlockExpression cbe -> {
                return ContextBlockPlan.of(cbe);
            }
            case BorrowExpression be -> {
                return new BorrowExpressionPlan(be);
            }
            case SchemaRustType(SchemaType type) -> {
                return buildProgramPlan(type.sv());
            }
            case SchemaVarPattern svp -> {
                return new SchemaVarPatternPlan(svp);
            }
            default -> {
            }
        }
        if (!isGenericMatch(progEl)) {
            return null; // overrides match() with logic not described here
        }
        return structuralPlan(progEl, null);
    }

    /// Caches, per program-element class, whether it uses the generic `match` (no override).
    private static final Map<Class<?>, Boolean> GENERIC_MATCH = new ConcurrentHashMap<>();

    /// @return whether the element's class uses the generic
    /// `match(SourceData, MatchConditions)` (declared in `JavaProgramElement` or
    /// `JavaNonTerminalProgramElement`: class equality + exact-size child recursion)
    /// rather than its own override.
    static boolean isGenericMatch(RustyProgramElement pe) {
        return GENERIC_MATCH.computeIfAbsent(pe.getClass(), c -> {
            try {
                final Class<?> declaring =
                    c.getMethod("match", SourceData.class, MatchConditions.class)
                            .getDeclaringClass();
                return declaring == RustyProgramElement.class;
            } catch (NoSuchMethodException e) {
                return false;
            }
        });
    }

    /// Builds the plans for `pe`'s children and combines them into a structural plan (same
    /// concrete class, optional `guard`, children matched recursively). Returns `null`
    /// if some child cannot be described by the dispatch.
    private static @Nullable ProgramMatchPlan structuralPlan(RustyProgramElement pe,
            @Nullable MatchInstruction guard) {
        final int childCount = pe.getChildCount();
        final ProgramMatchPlan[] children = new ProgramMatchPlan[childCount];
        for (int i = 0; i < childCount; i++) {
            final ProgramMatchPlan child = buildProgramPlan(pe.getChild(i));
            if (child == null) {
                return null;
            }
            children[i] = child;
        }
        return new ProgramStructuralPlan(pe, guard, children, RustListSVMatcher.INSTANCE);
    }


    /// A [ContextBlockExpression], the `.. ...` pattern with which taclets match a
    /// program at its current execution point. Such a pattern matches statements (the _active
    /// statements_, written between `..` and `...`) that sit inside an arbitrary
    /// nesting of blocks, try-catch statements, labels and method frames. The leading `..`
    /// stands for that surrounding nesting, called the _prefix_; the trailing `...`
    /// stands for whatever follows the active statements, called the _suffix_.
    ///
    /// The compiled matcher performs the whole context match by reading the structure of the
    /// source, in four steps:
    ///
    /// 1. **locate the active statements**: walk down the source's chain of nested prefix
    /// elements until the remaining nesting is exactly as deep as the pattern's own. This yields
    /// the parent element and the child index at which the source's active statements begin (index
    /// `-1` means the located element itself is the match target, e.g. an empty
    /// block);
    /// 2. **match the execution context**: the execution context says in which class, method
    /// and on which object the active statements run, needed, for example, to resolve field
    /// accesses and method calls during matching. It is taken from the source's innermost method
    /// frame (the statement KeY wraps around an inlined method body), or is the default context if
    /// there is none. If the pattern names an execution context of its own (`.#ex..` or
    /// `.#pm@#t(#v)..`), it is matched against the found one. The found context is then
    /// recorded with the instantiations;
    /// 3. **match the active statements**: each active-statement sub-plan consumes exactly one
    /// source child (a list schema variable consumes a run of children);
    /// 4. **record the context**: store the position where the prefix ends and the position
    /// where the suffix starts (as paths into the source program) in the _context
    /// instantiation_: when the taclet is applied, the new program is assembled around these
    /// two positions.
    ///
    /// The order of the instantiation updates (execution-context match, recording the context,
    /// active statements, final completion) must not be changed: it is observable through the
    /// iteration order of the instantiation map. The interpreter back-end ([#emit]) does not
    /// use these steps; it delegates to `ContextStatementBlock.match`, which must produce
    /// identical results.
    private static final class ContextBlockPlan implements ProgramMatchPlan {
        private final ContextBlockExpression cbe;
        /// the pattern's own prefix length (a per-pattern constant, `>= 1`: the block itself
        /// counts as one prefix element)
        private final int patternPrefixLength;
        /// plan for the pattern's execution context (child 0), or `null` if it has none.
        private final @Nullable ProgramMatchPlan ecPlan;
        private final ProgramMatchPlan[] active;
        private final boolean interpretable;

        private ContextBlockPlan(ContextBlockExpression cbe, @Nullable ProgramMatchPlan ecPlan,
                ProgramMatchPlan[] active, boolean interpretable) {
            this.cbe = cbe;
            this.patternPrefixLength = cbe.getPrefixLength();
            this.ecPlan = ecPlan;
            this.active = active;
            this.interpretable = interpretable;
        }

        /// @return the plan matching `cbe`, or `null` if the execution context or an
        /// active statement cannot be described by the dispatch
        static @Nullable ContextBlockPlan of(ContextBlockExpression cbe) {
            assert cbe.getPrefixLength() > 0;
            ProgramMatchPlan ecPlan = null;
            final int childCount = cbe.getChildCount();
            final ProgramMatchPlan[] active = new ProgramMatchPlan[childCount];
            boolean interpretable = true;
            for (int i = 0; i < childCount; i++) {
                final ProgramMatchPlan p = buildProgramPlan(cbe.getChild(i));
                if (p == null) {
                    return null;
                }
                active[i] = p;
                // an active statement that is itself a list SV makes the block variable-arity;
                // like every list SV it is compiled-only
                interpretable &= p.interpretable();
            }
            return new ContextBlockPlan(cbe, ecPlan, active, interpretable);
        }

        @Override
        public boolean interpretable() {
            return interpretable;
        }

        @Override
        public void emit(List<VMInstruction> out) {
            final List<VMInstruction> activeInstr = new ArrayList<>();
            for (final ProgramMatchPlan p : active) {
                p.emit(activeInstr);
            }
            out.add(new MatchContextBlockExpressionInstruction(cbe,
                new VMProgramInterpreter(activeInstr.toArray(new VMInstruction[0]))));
        }

        @Override
        public MatchProgram compile() {
            final ProgramChildSequence activeSequence =
                ProgramChildSequence.compile(active, RustListSVMatcher.INSTANCE);
            final MatchProgram ecMatch = ecPlan == null ? null : ecPlan.compile();
            final int prefixLength = patternPrefixLength;
            return (actual, mc, services) -> matchContext((RustyProgramElement) actual,
                (MatchConditions) mc, (Services) services, prefixLength, ecMatch,
                activeSequence);
        }

        /// Matches the whole context pattern against the source program `src`, in the four
        /// steps described in the class comment. The pattern enters only through its prefix length,
        /// its execution-context matcher and its active-statement matchers.
        private static @Nullable MatchConditions matchContext(RustyProgramElement src,
                MatchConditions matchCond, Services services, int patternPrefixLength,
                @Nullable MatchProgram ecMatch, ProgramChildSequence activeSequence) {
            if (matchCond.getInstantiations().getContextInstantiation() != null) {
                // several context statement block occurrences in find or assumes clauses are not
                // allowed
                return null;
            }

            final PossibleProgramPrefix prefix;
            int pos = -1;
            PosInProgram relPos = PosInProgram.TOP;
            // the element whose children are the source's active statements, and the child index
            // of the first one (-1: the element itself is the match target, e.g. an empty block)
            RustyProgramElement activeParent = src;
            int start = -1;

            // step 1: locate the active statements under the source's prefix nesting
            if (src instanceof PossibleProgramPrefix) {
                prefix = (PossibleProgramPrefix) src;
                final int srcPrefixLength = prefix.getPrefixLength();
                if (patternPrefixLength > srcPrefixLength) {
                    return null;
                }
                pos = srcPrefixLength - patternPrefixLength;
                PossibleProgramPrefix firstActiveStatement = getPrefixElementAt(prefix, pos);
                relPos = firstActiveStatement.getFirstActiveChildPos();
                if (relPos != PosInProgram.TOP) {
                    start = relPos.get(relPos.depth() - 1);
                    if (relPos.depth() > 1) {
                        firstActiveStatement = (PossibleProgramPrefix) PosInProgram
                                .getProgramAt(relPos.up(), firstActiveStatement);
                    }
                }
                activeParent = firstActiveStatement;
            } else {
                prefix = null;
            }

            // step 2: match the active statements
            final ProgramChildCursor newSource = new ProgramChildCursor(activeParent, start);
            matchCond = matchActiveStatements(newSource, matchCond, services, activeSequence);
            if (matchCond == null) {
                return null;
            }

            // step 3: record the prefix-end and suffix-start positions
            return makeContextInfoComplete(matchCond, newSource, prefix, pos, relPos, src,
                services);
        }

        /// Step 2: matches the active statements. In the common case (no list schema variable
        /// among them, and the match starts at a regular child index) each sub-matcher is applied
        /// directly to its source child, and the cursor is then advanced past the matched children.
        /// Otherwise, a cursor walks the source children one by one: an active statement that is a
        /// list schema variable consumes a greedy run of children, and a start index of `-1`
        /// means the first sub-matcher consumes the located element itself (an empty block or empty
        /// try). The pattern need not consume all source children; whatever remains belongs to the
        /// context's suffix.
        ///
        /// When the source runs out of children before the pattern's active statements are
        /// exhausted, the match simply fails. (The interpreter back-end instead crashes with a
        /// `NullPointerException` if the leftover pattern statement is a childless terminal;
        /// its AST matcher does not null-check; failing cleanly here is deliberate. No shipped
        /// taclet has such a shape.)
        private static @Nullable MatchConditions matchActiveStatements(
                ProgramChildCursor newSource, MatchConditions matchCond, Services services,
                ProgramChildSequence activeSequence) {
            final int n = activeSequence.size();
            final int start = newSource.getChildPos();
            if (!activeSequence.isVarArity() && start >= 0) {
                if (newSource.getElement().getChildCount() < start + n) {
                    // not enough source children -> matching would fail on a null source child
                    return null;
                }
                matchCond =
                    (MatchConditions) activeSequence.matchOneToOneFrom(newSource.getElement(),
                        start,
                        matchCond, services);
                if (matchCond == null) {
                    return null;
                }
                newSource.setChildPos(start + n);
            } else {
                matchCond =
                    (MatchConditions) activeSequence.match(newSource, matchCond, services);
            }
            return matchCond;
        }

        /// Step 4: computes the position where the prefix ends and the position where the suffix
        /// starts, and completes the context instantiation with them. The execution context is
        /// re-read from the instantiations because step 2 may have bound it there.
        private static MatchConditions makeContextInfoComplete(MatchConditions matchCond,
                ProgramChildCursor newSource, @Nullable PossibleProgramPrefix prefix, int pos,
                PosInProgram relPos, RustyProgramElement src, Services services) {
            final SVInstantiations instantiations = matchCond.getInstantiations();
            final PosInProgram prefixEnd = matchPrefixEnd(prefix, pos, relPos);
            // position of the first source element that is not matched (start of the suffix)
            final int lastMatchedPos = newSource.getChildPos();
            final PosInProgram suffixStart = prefixEnd.up().down(lastMatchedPos);
            return matchCond.setInstantiations(instantiations.replace(prefixEnd, suffixStart,
                src, services));
        }

        /// Computes the position of the first element that is not part of the source's prefix, by
        /// concatenating the active-child positions of the first `pos + 1` prefix elements.
        private static PosInProgram matchPrefixEnd(@Nullable PossibleProgramPrefix prefix, int pos,
                PosInProgram relPos) {
            PosInProgram prefixEnd = PosInProgram.TOP;
            if (prefix != null) {
                PossibleProgramPrefix currentPrefix = prefix;
                int i = 0;
                while (i <= pos) {
                    prefixEnd = prefixEnd.append(currentPrefix.getFirstActiveChildPos());
                    i++;
                    if (i <= pos) {
                        currentPrefix = currentPrefix.getNextPrefixElement();
                    }
                }
            } else {
                prefixEnd = relPos;
            }
            return prefixEnd;
        }

        /// the `i`-th element of the source's prefix chain (element `0` = the start)
        private static PossibleProgramPrefix getPrefixElementAt(PossibleProgramPrefix prefix,
                int i) {
            PossibleProgramPrefix current = prefix;
            for (int pos = 0; pos < i; pos++) {
                current = current.getNextPrefixElement();
            }
            return current;
        }

        @Override
        public String toString() {
            return "context" + (ecPlan == null ? "" : "(ec " + ecPlan + ")")
                + Arrays.toString(active);
        }
    }

    private static final class BorrowExpressionPlan implements ProgramMatchPlan {
        private final ProgramMatchPlan inner;
        boolean mut;

        BorrowExpressionPlan(BorrowExpression be) {
            this.inner = buildProgramPlan(be.getExpr());
            this.mut = be.isMut();
        }

        @Override
        public void emit(List<VMInstruction> out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MatchProgram compile() {
            var ic = inner.compile();
            return (actual, mc, services) -> {
                if (!(actual instanceof BorrowExpression be))
                    return null;
                if (mut != be.isMut())
                    return null;
                return ic.match(be.getExpr(), mc, services);
            };
        }

        @Override
        public String toString() {
            return "borrow(mut=" + mut + "," + inner + ")";
        }
    }

    private static class SchemaVarPatternPlan implements ProgramMatchPlan {
        private final SchemaVarPattern svp;
        private final ProgramMatchPlan inner;

        SchemaVarPatternPlan(SchemaVarPattern svp) {
            this.svp = svp;
            this.inner = buildProgramPlan(svp.operatorSV());
        }

        @Override
        public void emit(List<VMInstruction> out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MatchProgram compile() {
            var inner = this.inner.compile();
            if (svp.operatorSV().sort() == ProgramSVSort.VARIABLE) {
                return (actual, mc, services) -> {
                    if (!(actual instanceof RustyProgramElement rpe))
                        return null;
                    if (!ProgramSVSort.PATTERN.canStandFor(rpe, (Services) services))
                        return null;
                    var bp = (BindingPattern) rpe;
                    if (bp.mut() != svp.mut() || bp.mutRef() && (!svp.mut() || !svp.reference())
                            || bp.ref() && (svp.mut() || !svp.reference())) {
                        return null;
                    }
                    return inner.match(bp.pv(), mc, services);
                };
            } else {
                return (actual, mc, services) -> {
                    if (!(actual instanceof RustyProgramElement rpe))
                        return null;
                    if (!ProgramSVSort.PATTERN.canStandFor(rpe, (Services) services))
                        return null;
                    var bp = (BindingPattern) rpe;
                    if (bp.mut() != svp.mut() || bp.mutRef() && (!svp.mut() || !svp.reference())
                            || bp.ref() && (svp.mut() || !svp.reference())) {
                        return null;
                    }
                    final var instantiations = (SVInstantiations) mc.getInstantiations();
                    final Object inst = instantiations.getInstantiation(svp.operatorSV());
                    if (inst == null) {
                        var insts = instantiations.add(svp.operatorSV(), bp, services);
                        return insts == null ? null : mc.setInstantiations(insts);
                    } else {
                        final Object newInst;
                        if (inst instanceof Term) {
                            newInst = ((Services) services).convertToLogicElement(bp);
                        } else {
                            newInst = bp;
                        }
                        if (inst.equals(newInst)) {
                            return mc;
                        } else {
                            return null;
                        }
                    }
                };
            }
        }

        @Override
        public String toString() {
            return "schemaVarPattern(" + svp + "," + inner + ")";
        }
    }
}
