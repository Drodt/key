/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.logic.op;

import java.util.ArrayList;

import de.uka.ilkd.key.java.*;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.declaration.*;
import de.uka.ilkd.key.java.reference.ExecutionContext;
import de.uka.ilkd.key.java.reference.PackageReference;
import de.uka.ilkd.key.java.reference.ReferencePrefix;
import de.uka.ilkd.key.java.reference.TypeReference;
import de.uka.ilkd.key.java.visitor.Visitor;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.ProgramConstruct;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.rule.MatchConditions;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.speclang.HeapContext;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Objects of this class are schema variables matching program constructs within modal operators.
 * The particular construct being matched is determined by the ProgramSVSort of the schema variable.
 */
public final class ProgramSV extends JOperatorSV
        implements ProgramConstruct, UpdateableOperator {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProgramSV.class);

    private static final ImmutableArray<ProgramElement> EMPTY_LIST_INSTANTIATION =
        new ImmutableArray<>(new ProgramElement[0]);

    private final boolean isListSV;

    /**
     * creates a new SchemaVariable used as a placeholder for program constructs
     *
     * @param name the Name of the SchemaVariable allowed to match a list of program constructs
     */
    ProgramSV(Name name, ProgramSVSort s, boolean isListSV) {
        super(name, s, false, false);
        this.isListSV = isListSV;
    }

    public boolean isListSV() {
        return isListSV;
    }

    /**
     * @return comments if the schemavariable stands for programm construct and has comments
     *         attached to it (not supported yet)
     */
    @Override
    public Comment[] getComments() {
        return new Comment[0];
    }

    @Override
    public SourceElement getFirstElement() {
        return this;
    }

    @Override
    public SourceElement getFirstElementIncludingBlocks() {
        return getFirstElement();
    }

    @Override
    public SourceElement getLastElement() {
        return this;
    }

    @Override
    public Position getStartPosition() {
        return Position.UNDEFINED;
    }

    @Override
    public Position getEndPosition() {
        return Position.UNDEFINED;
    }

    @Override
    public recoder.java.SourceElement.Position getRelativePosition() {
        return recoder.java.SourceElement.Position.UNDEFINED;
    }

    @Override
    public PositionInfo getPositionInfo() {
        return PositionInfo.UNDEFINED;
    }

    @Override
    public ReferencePrefix getReferencePrefix() {
        return null;
    }

    @Override
    public int getDimensions() {
        return 0;
    }

    @Override
    public int getTypeReferenceCount() {
        return 0;
    }

    @Override
    public TypeReference getTypeReferenceAt(int index) {
        return this;
    }

    @Override
    public PackageReference getPackageReference() {
        return null;
    }

    @Override
    public int getExpressionCount() {
        return 0;
    }

    @Override
    public Expression getExpressionAt(int index) {
        return null;
    }

    @Override
    public ProgramElement getChildAt(int index) {
        return this;
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("ProgramSV " + this + " has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public int getStatementCount() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public ImmutableArray<Expression> getUpdates() {
        return null;
    }

    @Override
    public ImmutableArray<LoopInitializer> getInits() {
        return null;
    }

    @Override
    public Statement getStatementAt(int i) {
        return this;
    }

    @Override
    public ProgramElementName getProgramElementName() {
        return new ProgramElementName(toString());
    }

    @Override
    public String getName() {
        return name().toString();
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnSchemaVariable(this);
    }

    @Override
    public KeYJavaType getKeYJavaType() {
        return null;
    }

    @Override
    public KeYJavaType getKeYJavaType(Services javaServ) {
        return null;
    }

    @Override
    public KeYJavaType getKeYJavaType(Services javaServ, ExecutionContext ec) {
        return null;
    }

    /**
     * adds a found mapping from schema variable <code>var</code> to program element <code>pe</code>
     * and returns the updated match conditions or null if mapping is not possible because of
     * violating some variable condition
     *
     * @param pe the ProgramElement <code>var</code> is mapped to
     * @param matchCond the MatchConditions to be updated
     * @param services the Services provide access to the Java model
     * @return the updated match conditions including mapping <code>var</code> to <code>pe</code> or
     *         null if some variable condition would be hurt by the mapping
     */
    private MatchConditions addProgramInstantiation(ProgramElement pe, MatchConditions matchCond,
            Services services) {
        if (matchCond == null) {
            return null;
        }

        SVInstantiations insts = matchCond.getInstantiations();

        final Object foundInst = insts.getInstantiation(this);

        if (foundInst != null) {
            final Object newInst;
            if (foundInst instanceof JTerm) {
                newInst = services.getTypeConverter().convertToLogicElement(pe,
                    insts.getExecutionContext());
            } else {
                newInst = pe;
            }

            if (foundInst.equals(newInst)) {
                return matchCond;
            } else {
                return null;
            }
        }

        insts = insts.add(this, pe, services);
        return insts == null ? null : matchCond.setInstantiations(insts);
    }

    /**
     * adds a found mapping from schema variable <code>var</code> to the list of program elements
     * <code>list</code> and returns the updated match conditions or null if mapping is not possible
     * because of violating some variable condition
     *
     * @param list the ProgramList <code>var</code> is mapped to
     * @param matchCond the MatchConditions to be updated
     * @param services the Services provide access to the Java model
     * @return the updated match conditions including mapping <code>var</code> to <code>list</code>
     *         or null if some variable condition would be hurt by the mapping
     */
    private MatchConditions addProgramInstantiation(ImmutableArray<ProgramElement> list,
            MatchConditions matchCond,
            Services services) {
        if (matchCond == null) {
            return null;
        }

        SVInstantiations insts = matchCond.getInstantiations();
        final var pl = (ImmutableArray<ProgramElement>) insts.getInstantiation(this);
        if (pl != null) {
            if (pl.equals(list)) {
                return matchCond;
            } else {
                return null;
            }
        }

        insts = insts.add(this, list, ProgramElement.class, services);
        return insts == null ? null : matchCond.setInstantiations(insts);
    }

    private MatchConditions matchListSV(SourceData source, MatchConditions matchCond) {
        final Services services = source.getServices();
        ProgramElement src = source.getSource();

        if (src == null) {
            return addProgramInstantiation(EMPTY_LIST_INSTANTIATION, matchCond, services);
        }

        SVInstantiations instantiations = matchCond.getInstantiations();

        final ExecutionContext ec = instantiations.getExecutionContext();

        final ArrayList<ProgramElement> matchedElements =
            new ArrayList<>();

        while (src != null) {
            if (!check(src, ec, services)) {
                break;
            }
            matchedElements.add(src);
            source.next();
            src = source.getSource();
        }

        return addProgramInstantiation(new ImmutableArray<>(matchedElements), matchCond, services);
    }

    /**
     * returns true, if the given SchemaVariable can stand for the ProgramElement
     *
     * @param match the ProgramElement to be matched
     * @param services the Services object encapsulating information about the java datastructures
     *        like (static)types etc.
     * @return true if the SchemaVariable can stand for the given element
     */
    private boolean check(ProgramElement match, ExecutionContext ec, Services services) {
        if (match == null) {
            return false;
        }
        return ((ProgramSVSort) sort()).canStandFor(match, ec, services);
    }

    @Override
    public MatchConditions match(SourceData source, MatchConditions matchCond) {
        if (isListSV()) {
            return matchListSV(source, matchCond);
        }

        final Services services = source.getServices();
        final ProgramElement src = source.getSource();

        final SVInstantiations instantiations = matchCond.getInstantiations();

        final ExecutionContext ec = instantiations.getExecutionContext();

        if (!check(src, ec, services)) {
            return null;
        }

        final Object instant = instantiations.getInstantiation(this);
        if (instant == null || instant.equals(src)
                || (instant instanceof JTerm && ((JTerm) instant).op().equals(src))) {

            matchCond = addProgramInstantiation(src, matchCond, services);

            if (matchCond == null) {
                // FAILED due to incompatibility with already found matchings
                // (e.g. generic sorts)
                return null;
            }
        } else {
            LOGGER.debug("Match failed: Former match of "
                + " SchemaVariable incompatible with " + " the current match.");
            return null; // FAILED mismatch
        }
        source.next();
        return matchCond;
    }

    @Override
    public String toString() {
        return toString("program " + sort().name());
    }

    @Override
    public MethodDeclaration getMethodDeclaration() {
        return null;
    }

    @Override
    public KeYJavaType getParameterType(int i) {
        return null;
    }

    @Override
    public StatementBlock getBody() {
        return null;
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public boolean isModel() {
        return false;
    }

    @Override
    public int getStateCount() {
        return 1;
    }

    @Override
    public int getHeapCount(Services services) {
        return HeapContext.getModifiableHeaps(services, false).size();
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public KeYJavaType getReturnType() {
        return null;
    }

    @Override
    public String getUniqueName() {
        return sort().declarationString() + " " + name();
    }

    @Override
    public String getFullName() {
        return null;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isSynchronized() {
        return false;
    }

    @Override
    public Throws getThrown() {
        return null;
    }

    @Override
    public ParameterDeclaration getParameterDeclarationAt(int index) {
        return null;
    }

    @Override
    public VariableSpecification getVariableSpecification(int index) {
        return null;
    }

    @Override
    public int getParameterDeclarationCount() {
        return 0;
    }

    @Override
    public ImmutableArray<ParameterDeclaration> getParameters() {
        return null;
    }

    @Override
    public ImmutableList<LocationVariable> collectParameters() {
        return null;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isStrictFp() {
        return false;
    }

    @Override
    public ImmutableArray<Modifier> getModifiers() {
        return null;
    }

    @Override
    public ImmutableArray<KeYJavaType> getParamTypes() {
        return null;
    }

    @Override
    public KeYJavaType getType() {
        return null;
    }

    @Override
    public KeYJavaType getContainerType() {
        return null;
    }

    @Override
    public int getNumParams() {
        return 0;
    }

    @Override
    public KeYJavaType getParamType(int i) {
        return null;
    }

    @Override
    public TypeReference getTypeReference() {
        return null;
    }

    @Override
    public IProgramMethod getMethodContext() {
        return null;
    }

    @Override
    public ReferencePrefix getRuntimeInstance() {
        return null;
    }
}
