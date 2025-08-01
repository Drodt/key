/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.ldt;

import de.uka.ilkd.key.java.Expression;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.Type;
import de.uka.ilkd.key.java.expression.Literal;
import de.uka.ilkd.key.java.expression.literal.NullLiteral;
import de.uka.ilkd.key.java.reference.ExecutionContext;
import de.uka.ilkd.key.java.reference.FieldReference;
import de.uka.ilkd.key.java.reference.ReferencePrefix;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.proof.init.JavaProfile;
import de.uka.ilkd.key.proof.io.ProofSaver;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.Namespace;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.sort.Sort;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


/**
 * LDT responsible for the "Heap" sort, and the associated "Field" sort. Besides offering the usual
 * LDT functionality, this class is also responsible for creating and managing the constant symbols
 * representing fields.
 */
public final class HeapLDT extends LDT {

    public static final Name NAME = new Name("Heap");

    public static final Name SELECT_NAME = new Name("select");
    public static final Name STORE_NAME = new Name("store");
    public static final Name FINAL_NAME = new Name("final");
    public static final Name BASE_HEAP_NAME = new Name("heap");
    public static final Name SAVED_HEAP_NAME = new Name("savedHeap");
    public static final Name PERMISSION_HEAP_NAME = new Name("permissions");
    public static final Name[] VALID_HEAP_NAMES =
        { BASE_HEAP_NAME, SAVED_HEAP_NAME, PERMISSION_HEAP_NAME };



    // additional sorts
    private final Sort fieldSort;

    // select/store
    private final SortDependingFunction select;
    private final SortDependingFunction finalFunction;
    private final Function store;
    private final Function create;
    private final Function anon;
    private final Function memset;

    // fields
    private final Function arr;
    private final Function created;
    private final Function initialized;
    private final SortDependingFunction classPrepared;
    private final SortDependingFunction classInitialized;
    private final SortDependingFunction classInitializationInProgress;
    private final SortDependingFunction classErroneous;

    // length
    private final Function length;

    // null
    private final Function nullFunc;

    // predicates
    private final Function wellFormed;
    private final Function acc;
    private final Function reach;
    private final Function prec;

    // heap pv
    private ImmutableList<LocationVariable> heaps;



    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    public HeapLDT(TermServices services) {
        super(NAME, services);
        final Namespace<@NonNull Sort> sorts = services.getNamespaces().sorts();
        final Namespace<@NonNull IProgramVariable> progVars =
            services.getNamespaces().programVariables();

        fieldSort = sorts.lookup(new Name("Field"));
        select = addSortDependingFunction(services, SELECT_NAME.toString());
        finalFunction = addSortDependingFunction(services, FINAL_NAME.toString());
        store = addFunction(services, STORE_NAME.toString());
        create = addFunction(services, "create");
        anon = addFunction(services, "anon");
        memset = addFunction(services, "memset");
        arr = addFunction(services, "arr");
        created = addFunction(services, "java.lang.Object::<created>");
        initialized = addFunction(services, "java.lang.Object::<initialized>");
        classPrepared = addSortDependingFunction(services, "<classPrepared>");
        classInitialized = addSortDependingFunction(services, "<classInitialized>");
        classInitializationInProgress =
            addSortDependingFunction(services, "<classInitializationInProgress>");
        classErroneous = addSortDependingFunction(services, "<classErroneous>");
        length = addFunction(services, "length");
        nullFunc = addFunction(services, "null");
        acc = addFunction(services, "acc");
        reach = addFunction(services, "reach");
        prec = addFunction(services, "prec");
        heaps = ImmutableSLList.<LocationVariable>nil()
                .append((LocationVariable) progVars.lookup(BASE_HEAP_NAME))
                .append((LocationVariable) progVars.lookup(SAVED_HEAP_NAME));
        if (services instanceof Services s) {
            if (s.getProfile() instanceof JavaProfile) {
                if (((JavaProfile) s.getProfile()).withPermissions()) {
                    heaps = heaps.append((LocationVariable) progVars.lookup(PERMISSION_HEAP_NAME));
                }
            }
        }
        wellFormed = addFunction(services, "wellFormed");
    }

    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------

    private String getFieldSymbolName(LocationVariable fieldPV) {
        if (fieldPV.isImplicit()) {
            return fieldPV.name().toString();
        } else {
            String fieldPVName = fieldPV.name().toString();
            int index = fieldPV.toString().indexOf("::");
            assert index > 0;
            return fieldPVName.substring(0, index) + "::$" + fieldPVName.substring(index + 2);
        }
    }



    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    /**
     * Wrapper class
     *
     * @param className the class name
     * @param attributeName the attribute name
     */
    public record SplitFieldName(String className, String attributeName) {
    }

    /**
     * Splits a field name.
     *
     * @param symbol the field name to split.
     * @return the split field name
     */
    public static @Nullable SplitFieldName trySplitFieldName(Named symbol) {
        var name = symbol.name().toString();
        // check for normal attribute
        int endOfClassName = name.indexOf("::$");

        int startAttributeName = endOfClassName + 3;


        if (endOfClassName < 0) {
            // not a normal attribute, maybe an implicit attribute like <created>?
            endOfClassName = name.indexOf("::<");
            startAttributeName = endOfClassName + 2;
        }

        if (endOfClassName < 0) {
            return null;
        }

        String className = name.substring(0, endOfClassName);
        String attributeName = name.substring(startAttributeName);
        return new SplitFieldName(className, attributeName);
    }

    /**
     * Given a constant symbol representing a field, this method returns a simplified name of the
     * constant symbol to be used for pretty printing.
     */
    public static String getPrettyFieldName(Named fieldSymbol) {
        String name = fieldSymbol.name().toString();
        int index = name.indexOf("::");
        if (index == -1) {
            return name;
        } else {
            String result = name.substring(index + 2);
            if (result.charAt(0) == '$') {
                result = result.substring(1);
            }
            return result;
        }
    }


    /**
     * Extracts the name of the enclosing class from the name of a constant symbol representing a
     * field.
     */
    public static String getClassName(Function fieldSymbol) {
        String name = fieldSymbol.name().toString();
        int index = name.indexOf("::");
        if (index == -1) {
            return null;
        } else {
            return name.substring(0, index);
        }
    }


    /**
     * Returns the sort "Field".
     */
    public Sort getFieldSort() {
        return fieldSort;
    }


    /**
     * Returns the select function for the given sort.
     */
    public SortDependingFunction getSelect(Sort instanceSort, TermServices services) {
        return select.getInstanceFor(instanceSort, services);
    }

    /**
     * Returns the function symbol to access final fields for the given instance sort.
     *
     * @param instanceSort the sort of the value to be read
     * @param services the services to find/create the sort-depending function
     * @return the function symbol to access final fields for the given instance sort
     */
    public @NonNull SortDependingFunction getFinal(@NonNull Sort instanceSort,
            @NonNull Services services) {
        return finalFunction.getInstanceFor(instanceSort, services);
    }

    /**
     * Check if the given operator is an instance of the "final" function to access final fields.
     *
     * @param op the operator to check
     * @return true if the operator is an instance of the "X::final" srot-depending function
     */
    public boolean isFinalOp(Operator op) {
        return op instanceof SortDependingFunction
                && ((SortDependingFunction) op).isSimilar(finalFunction);
    }


    /**
     * If the passed operator is an instance of "select", this method returns the sort of the
     * function (identical to its return type); otherwise, returns null.
     */
    public Sort getSortOfSelect(Operator op) {
        if (isSelectOp(op)) {
            return ((SortDependingFunction) op).getSortDependingOn();
        } else {
            return null;
        }
    }

    public boolean isSelectOp(Operator op) {
        return op instanceof SortDependingFunction
                && ((SortDependingFunction) op).isSimilar(select);
    }


    public Function getStore() {
        return store;
    }


    public Function getCreate() {
        return create;
    }


    public Function getAnon() {
        return anon;
    }


    public Function getMemset() {
        return memset;
    }


    public Function getArr() {
        return arr;
    }


    public Function getCreated() {
        return created;
    }


    public Function getInitialized() {
        return initialized;
    }


    public Function getClassPrepared(Sort instanceSort, TermServices services) {
        return classPrepared.getInstanceFor(instanceSort, services);
    }


    public Function getClassInitialized(Sort instanceSort, TermServices services) {
        return classInitialized.getInstanceFor(instanceSort, services);
    }


    public Function getClassInitializationInProgress(Sort instanceSort,
            TermServices services) {
        return classInitializationInProgress.getInstanceFor(instanceSort, services);
    }


    public Function getClassErroneous(Sort instanceSort, TermServices services) {
        return classErroneous.getInstanceFor(instanceSort, services);
    }


    public Function getLength() {
        return length;
    }


    public Function getNull() {
        return nullFunc;
    }


    public Function getWellFormed() {
        return wellFormed;
    }


    public Function getAcc() {
        return acc;
    }


    public Function getReach() {
        return reach;
    }

    public Function getPrec() {
        return prec;
    }


    public LocationVariable getHeap() {
        return heaps.head();
    }

    public LocationVariable getSavedHeap() {
        return heaps.tail().head();
    }


    public ImmutableList<LocationVariable> getAllHeaps() {
        return heaps;
    }

    public LocationVariable getHeapForName(Name name) {
        for (LocationVariable h : getAllHeaps()) {
            if (h.name().equals(name)) {
                return h;
            }
        }
        return null;
    }

    public LocationVariable getPermissionHeap() {
        return heaps.size() > 2 ? heaps.tail().tail().head() : null;
    }

    /**
     * Given a "program variable" representing a field or a model field, returns the function symbol
     * representing the same field. For normal fields (Java or ghost fields), this function symbol
     * is a constant symbol of type "Field". For model fields, it is an observer function symbol. If
     * the appropriate symbol does not yet exist in the namespace, this method creates and adds it
     * to the namespace as a side effect.
     */
    public Function getFieldSymbolForPV(LocationVariable fieldPV, Services services) {
        assert fieldPV.isMember();
        assert fieldPV != services.getJavaInfo().getArrayLength();

        final Name name = new Name(getFieldSymbolName(fieldPV));
        Function result = services.getNamespaces().functions().lookup(name);
        if (result == null) {
            int index = name.toString().indexOf("::");
            assert index > 0;
            final Name kind = new Name(name.toString().substring(index + 2));

            SortDependingFunction firstInstance =
                SortDependingFunction.getFirstInstance(kind, services);
            if (firstInstance != null) {
                Sort sortDependingOn = fieldPV.getContainerType().getSort();
                result = firstInstance.getInstanceFor(sortDependingOn, services);
            } else {
                if (fieldPV.isModel()) {
                    int heapCount = 0;
                    for (LocationVariable heap : getAllHeaps()) {
                        if (heap == getSavedHeap()) {
                            continue;
                        }
                        heapCount++;
                    }
                    result = new ObserverFunction(kind.toString(), fieldPV.sort(),
                        fieldPV.getKeYJavaType(), targetSort(), fieldPV.getContainerType(),
                        fieldPV.isStatic(), new ImmutableArray<>(), heapCount, 1);
                } else {
                    result = new JFunction(name, fieldSort, new Sort[0], null, true);
                }
                services.getNamespaces().functions().addSafely(result);
            }
        }

        // sanity check
        if (fieldPV.isModel()) {
            assert result instanceof ObserverFunction;
        } else {
            assert !(result instanceof ObserverFunction);
            assert result.isUnique() : "field symbol is not unique: " + result;
        }

        return result;
    }

    @Override
    public boolean containsFunction(Function op) {
        if (super.containsFunction(op)) {
            return true;
        }
        if (op instanceof SortDependingFunction) {
            return ((SortDependingFunction) op).isSimilar(select);
        }
        return op.isUnique() && op.sort() == getFieldSort();
    }

    @Override
    public boolean isResponsible(de.uka.ilkd.key.java.expression.Operator op, JTerm[] subs,
            Services services, ExecutionContext ec) {
        return false;
    }


    @Override
    public boolean isResponsible(de.uka.ilkd.key.java.expression.Operator op, JTerm left,
            JTerm right,
            Services services, ExecutionContext ec) {
        return false;
    }


    @Override
    public boolean isResponsible(de.uka.ilkd.key.java.expression.Operator op, JTerm sub,
            TermServices services, ExecutionContext ec) {
        return false;
    }


    @Override
    public JTerm translateLiteral(Literal lit, Services services) {
        assert false;
        return null;
    }


    @Override
    public Function getFunctionFor(de.uka.ilkd.key.java.expression.Operator op, Services serv,
            ExecutionContext ec) {
        assert false;
        return null;
    }


    @Override
    public boolean hasLiteralFunction(Function f) {
        return false;
    }


    @Override
    public Expression translateTerm(JTerm t, ExtList children, Services services) {
        if (t.op() instanceof SortDependingFunction
                && ((SortDependingFunction) t.op()).isSimilar(select)) {
            ProgramVariable heap = (ProgramVariable) children.removeFirst();
            if (heap != getHeap()) {
                throw new IllegalArgumentException("Can only translate field access to base heap.");
            }
            ReferencePrefix prefix = (ReferencePrefix) children.removeFirst();
            ProgramVariable field = (ProgramVariable) children.removeFirst();

            if (prefix instanceof NullLiteral) {
                return new FieldReference(field, null);
            }
            return new FieldReference(field, prefix);
        } else if (t.sort() == getFieldSort() && t.op() instanceof Function
                && ((Function) t.op()).isUnique()) {
            return services.getJavaInfo().getAttribute(getPrettyFieldName(t.op()),
                getClassName((Function) t.op()));
        }
        throw new IllegalArgumentException(
            "Could not translate " + ProofSaver.printTerm(t, null) + " to program.");
    }


    @Override
    public Type getType(JTerm t) {
        assert false;
        return null;
    }


}
