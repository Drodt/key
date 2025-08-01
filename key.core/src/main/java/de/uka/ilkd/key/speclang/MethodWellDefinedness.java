/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.speclang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.declaration.modifier.VisibilityModifier;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.rule.RewriteTaclet;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletGoalTemplate;
import de.uka.ilkd.key.speclang.jml.JMLInfoExtractor;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

/**
 * A contract for checking the well-definedness of a specification for a method or model field.
 * Additionally to the general well-definedness contract, it consists of other definitions for the
 * contract.
 *
 * @author Michael Kirsten
 */
public final class MethodWellDefinedness extends WellDefinednessCheck {

    private final Contract contract;

    private JTerm globalDefs;
    private JTerm axiom;
    private final boolean modelField;

    private MethodWellDefinedness(String name, int id, Type type, IObserverFunction target,
            LocationVariable heap, OriginalVariables origVars, Condition requires, JTerm modifiable,
            JTerm accessible, Condition ensures, JTerm mby, JTerm rep, Contract contract,
            JTerm globalDefs, JTerm axiom, boolean model, TermBuilder tb) {
        super(name, id, type, target, heap, origVars, requires, modifiable, accessible, ensures,
            mby, rep, tb);
        this.contract = contract;
        this.globalDefs = globalDefs;
        this.axiom = axiom;
        this.modelField = model;
    }

    public MethodWellDefinedness(FunctionalOperationContract contract, Services services) {
        super(contract.getTypeName(), contract.id(), contract.getTarget(), contract.getOrigVars(),
            Type.OPERATION_CONTRACT, services);
        assert contract != null;
        this.contract = contract;
        this.modelField = false;
        final OriginalVariables origVars = contract.getOrigVars();
        final LocationVariable h = getHeap();
        final LocationVariable hPre = origVars.atPres.get(h);

        setRequires(contract.getRequires(h));
        setModifiable(
            contract.hasModifiable(h) ? contract.getModifiable(h) : TB.strictlyNothing(),
            services);
        combineAccessible(contract.getAccessible(h),
            hPre != null ? contract.getAccessible(hPre) : null, services);
        setEnsures(contract.getEnsures(h));
        setMby(contract.getMby());
        this.axiom = contract.getRepresentsAxiom(h, origVars.self, origVars.params, origVars.result,
            origVars.atPres, services);
        assert this.axiom == null || contract.getTarget().isModel();
        this.globalDefs = contract.getGlobalDefs();
    }

    public MethodWellDefinedness(DependencyContract contract, Services services) {
        super(
            ContractFactory.generateContractTypeName("JML model field", contract.getKJT(),
                contract.getTarget(), contract.getTarget().getContainerType()),
            contract.id(), contract.getTarget(), contract.getOrigVars(), Type.OPERATION_CONTRACT,
            services);
        assert contract != null;
        this.contract = contract;
        this.modelField = true;
        final LocationVariable h = getHeap();
        final LocationVariable hPre = contract.getOrigVars().atPres.get(h);

        setRequires(contract.getRequires(h));
        setModifiable(TB.strictlyNothing(), services);
        combineAccessible(contract.getAccessible(h),
            hPre != null ? contract.getAccessible(hPre) : null, services);
        setEnsures(TB.tt());
        setMby(contract.getMby());
        this.globalDefs = contract.getGlobalDefs();
        this.axiom = null;
    }

    public MethodWellDefinedness(RepresentsAxiom rep, Services services) {
        super(
            ContractFactory.generateContractTypeName("JML model field", rep.getKJT(),
                rep.getTarget(), rep.getTarget().getContainerType()),
            0, rep.getTarget(), rep.getOrigVars(), Type.OPERATION_CONTRACT, services);
        Map<LocationVariable, JTerm> pres = new LinkedHashMap<>();
        pres.put(services.getTypeConverter().getHeapLDT().getHeap(),
            rep.getOrigVars().self == null ? TB.tt() : TB.inv(TB.var(rep.getOrigVars().self)));
        Map<LocationVariable, JTerm> deps = new LinkedHashMap<>();
        for (LocationVariable heap : HeapContext.getModifiableHeaps(services, false)) {
            deps.put(heap, TB.allLocs());
        }
        this.contract = new DependencyContractImpl("JML model field",
            ContractFactory.generateContractName("JML model field", rep.getKJT(), rep.getTarget(),
                rep.getTarget().getContainerType(), 0),
            rep.getKJT(), rep.getTarget(), rep.getTarget().getContainerType(), pres, null, deps,
            rep.getOrigVars().self, rep.getOrigVars().params, rep.getOrigVars().atPres, null, 0);
        this.modelField = true;
        final OriginalVariables origVars = contract.getOrigVars();
        final LocationVariable h = getHeap();
        final LocationVariable hPre = origVars.atPres.get(h);

        setRequires(contract.getRequires(h));
        setModifiable(TB.strictlyNothing(), services);
        combineAccessible(contract.getAccessible(h),
            hPre != null ? contract.getAccessible(hPre) : null, services);
        setEnsures(TB.tt());
        setMby(contract.getMby());
        this.globalDefs = contract.getGlobalDefs();
        this.axiom = null;
        addRepresents(rep.getAxiom(getHeap(), rep.getOrigVars().self, services));
    }

    // -------------------------------------------------------------------------
    // Internal Methods
    // -------------------------------------------------------------------------

    /**
     * Gets the argument list for the operator of the method
     *
     * @param sv schema variable for self
     * @param heap schema variable for the heap
     * @param isStatic information whether this is a static method
     * @param params schema variables for the parameters
     * @return the term array of arguments used to construct the method term
     */
    private JTerm[] getArgs(JOperatorSV sv, JOperatorSV heap, JOperatorSV heapAtPre,
            boolean isStatic, boolean twoState, ImmutableList<JOperatorSV> params) {
        JTerm[] args = new JTerm[params.size() + (isStatic ? 1 : 2) + (twoState ? 1 : 0)];
        int i = 0;
        args[i++] = TB.var(heap);
        if (twoState) {
            args[i++] = TB.var(heapAtPre);
        }
        if (!isStatic) {
            args[i++] = TB.var(sv);
        }
        for (var arg : params) {
            args[i++] = TB.var(arg);
        }
        return args;
    }

    /**
     * Finds an -on top level- conjuncted term of the form (exc = null) in the given term.
     *
     * @param t the term to be searched in
     * @param exc the exception variable
     * @return true if the term guarantees exc to be equal to null
     */
    private boolean findExcNull(JTerm t, ProgramVariable exc) {
        assert t != null;
        if (t.op().equals(Junctor.AND)) {
            assert t.arity() == 2;
            return findExcNull(t.sub(0), exc) || findExcNull(t.sub(1), exc);
        } else if (t.op().equals(Equality.EQUALS)) {
            assert t.arity() == 2;
            return t.sub(1).equals(TB.NULL()) && t.sub(0).op().equals(exc);
        }
        return false;
    }

    /**
     * Converts the original parameters into schema variables
     *
     * @return a list of schema variables
     */
    private ImmutableList<JOperatorSV> paramsSV() {
        ImmutableList<JOperatorSV> paramsSV = ImmutableSLList.nil();
        for (var pv : getOrigVars().params) {
            paramsSV = paramsSV.append(
                SchemaVariableFactory.createTermSV(pv.name(), pv.getKeYJavaType().getSort()));
        }
        return paramsSV;
    }

    @Override
    Function generateMbyAtPreFunc(Services services) {
        return hasMby()
                ? new JFunction(new Name(TB.newName("mbyAtPre")),
                    services.getTypeConverter().getIntegerLDT().targetSort())
                : null;
    }

    /**
     * Generates a term of the form (mbyAtPre = mby) if mby is specified.
     *
     * @param self the self variable
     * @param params the list of parameters
     * @param mbyAtPreFunc the measured-by function
     * @param services
     * @return the measured by at pre equation for the precondition
     */
    JTerm generateMbyAtPreDef(LocationVariable self, ImmutableList<LocationVariable> params,
            Function mbyAtPreFunc, Services services) {
        final JTerm mbyAtPreDef;
        if (hasMby()) {
            final JTerm mbyAtPre = TB.func(mbyAtPreFunc);
            assert params != null;
            ImmutableList<LocationVariable> paramVars = ImmutableSLList.nil();
            for (var pv : params) {
                paramVars = paramVars.append(pv);
            }
            final JTerm mby = contract.getMby(self, paramVars, services);
            mbyAtPreDef = TB.equals(mbyAtPre, mby);
        } else {
            mbyAtPreDef = TB.tt();
        }
        return mbyAtPreDef;
    }

    @Override
    ImmutableList<JTerm> getRest() {
        ImmutableList<JTerm> rest = super.getRest();
        final JTerm globalDefs = getGlobalDefs();
        if (globalDefs != null) {
            rest = rest.append(globalDefs);
        }
        final JTerm axiom = getAxiom();
        if (axiom != null) {
            rest = rest.append(axiom);
        }
        return rest;
    }

    // -------------------------------------------------------------------------
    // Public Interface
    // -------------------------------------------------------------------------

    @Override
    public MethodWellDefinedness map(UnaryOperator<JTerm> op, Services services) {
        // TODO Auto-generated method stub
        return new MethodWellDefinedness(getName(), id(), type(), getTarget(), getHeap(),
            getOrigVars(), getRequires().map(op), op.apply(getModifiable()),
            op.apply(getAccessible()), getEnsures().map(op), op.apply(getMby()),
            op.apply(getRepresents()), contract.map(op, services), op.apply(globalDefs),
            op.apply(axiom), modelField, services.getTermBuilder());
    }

    public Contract getMethodContract() {
        return this.contract;
    }

    /**
     * Creates a well-definedness for a (pure) method invocation of this method.
     *
     * @param services
     * @return the taclet
     */
    public RewriteTaclet createOperationTaclet(Services services) {
        final String prefix;
        final IObserverFunction target = getTarget();
        final String methodName = target.name().toString();
        final String tName = getKJT().getJavaType().getFullName() + " "
            + methodName.substring(methodName.indexOf("::") + 2).replace("$", "");
        final boolean isStatic = target.isStatic();
        final boolean twoState = target.getStateCount() == 2;
        final LocationVariable heap = getHeap();
        final LocationVariable heapAtPre;
        if (getOrigVars().atPres != null && getOrigVars().atPres.get(heap) != null) {
            heapAtPre = getOrigVars().atPres.get(heap);
        } else {
            heapAtPre = heap;
        }
        final var heapSV = SchemaVariableFactory.createTermSV(heap.name(), heap.sort());
        final var heapAtPreSV =
            SchemaVariableFactory.createTermSV(heapAtPre.name(), heapAtPre.sort());
        final var selfSV =
            SchemaVariableFactory.createTermSV(new Name("callee"), getKJT().getSort());
        final ImmutableList<JOperatorSV> paramsSV = paramsSV();
        StringBuilder ps = new StringBuilder();
        for (ProgramVariable pv : getOrigVars().params) {
            ps.append(" ").append(pv.getKeYJavaType().getFullName());
        }
        final JTerm[] args = getArgs(selfSV, heapSV, heapAtPreSV, isStatic, twoState, paramsSV);
        if (isNormal(services)) {
            prefix = OP_TACLET;
            final boolean isConstructor =
                target instanceof IProgramMethod && ((IProgramMethod) target).isConstructor();
            final JTerm pre =
                getPreForTaclet(replaceSV(getRequires(), selfSV, paramsSV), selfSV, heapSV,
                    paramsSV, services).term();
            final JTerm wdArgs = TB.and(TB.wd(getArgs(selfSV, heapSV, heapAtPreSV,
                isStatic || isConstructor, twoState, paramsSV)));
            return createTaclet(prefix + (isStatic ? " Static " : " ") + tName + ps, TB.var(selfSV),
                TB.func(target, args), TB.and(wdArgs, pre), isStatic || isConstructor, services);
        } else {
            prefix = OP_EXC_TACLET;
            return createExcTaclet(prefix + (isStatic ? " Static " : " ") + tName + ps,
                TB.func(target, args), services);
        }
    }

    /**
     * Combines two well-definedness taclets for (pure) method invocations.
     *
     * @param t1 taclet one
     * @param t2 taclet two
     * @param services
     * @return the combined taclet
     */
    public RewriteTaclet combineTaclets(RewriteTaclet t1, RewriteTaclet t2, TermServices services) {
        assert t1.goalTemplates().size() == 1;
        assert t2.goalTemplates().size() == 1;
        final JTerm rw1 = ((RewriteTacletGoalTemplate) t1.goalTemplates().head()).replaceWith();
        final JTerm rw2 = ((RewriteTacletGoalTemplate) t2.goalTemplates().head()).replaceWith();
        final String n1 = t1.name().toString();
        final String n2 = t2.name().toString();
        final String n;
        if (n1.equals(n2)) {
            n = n1;
        } else if (n1.startsWith(OP_EXC_TACLET)) {
            n = n2;
        } else {
            n = n1;
        }
        return createTaclet(n, t1.find(), t2.find(), rw1, rw2, services);
    }

    @Override
    public String getBehaviour() {
        if (getMethodContract().getName().contains("normal_behavior")) {
            return "normal";
        } else if (getMethodContract().getName().contains("exceptional_behavior")) {
            return "exc";
        } else if (getMethodContract().getName().contains("model_behavior")) {
            return "model";
        } else if (getMethodContract().getName().contains("break_behavior")) {
            return "break";
        } else if (getMethodContract().getName().contains("continue_behavior")) {
            return "cont";
        } else if (getMethodContract().getName().contains("return_behavior")) {
            return "return";
        } else {
            return "";
        }
    }

    /**
     * Used to determine if the contract of this method has normal behaviour or is a model
     * field/method and can thus not throw any exception.
     *
     * @return true for either normal behaviour or model fields
     */
    public boolean isNormal(TermServices services) {
        if (modelField() || isModel()) {
            return true;
        }
        final JTerm post =
            getEnsures().implicit().equals(TB.tt()) ? getEnsures().explicit()
                    : getEnsures().implicit();
        final ProgramVariable exc = getOrigVars().exception;
        return findExcNull(post, exc);
    }

    /**
     * Tells whether the method is pure or not.
     *
     * @return true for pure methods and pure (model) fields
     */
    public boolean isPure() {
        IObserverFunction target = getTarget();
        if (target instanceof IProgramMethod pm) {
            return JMLInfoExtractor.isPure(pm);
        } else {
            return false;
        }
    }

    /**
     * Tells whether the method is a model method, i.e. has model behaviour or not.
     *
     * @return true for model methods
     */
    public boolean isModel() {
        if (getMethodContract() instanceof FunctionalOperationContract) {
            final IProgramMethod pm = (IProgramMethod) getTarget();
            return pm.isModel();
        }
        return false;
    }

    @Override
    public boolean modelField() {
        return this.modelField;
    }

    @Override
    public MethodWellDefinedness combine(WellDefinednessCheck wdc, TermServices services) {
        assert wdc instanceof MethodWellDefinedness;
        final MethodWellDefinedness mwd = (MethodWellDefinedness) wdc;
        assert !(getMethodContract() instanceof FunctionalOperationContract)
                || getMethodContract().getName().equals(mwd.getMethodContract().getName());
        assert this.getMethodContract().id() == mwd.getMethodContract().id();
        assert this.getMethodContract().getTarget().equals(mwd.getMethodContract().getTarget());
        assert this.getMethodContract().getKJT().equals(mwd.getMethodContract().getKJT());

        super.combine(mwd, services);
        if (this.getGlobalDefs() != null && mwd.getGlobalDefs() != null) {
            final JTerm defs = mwd.replace(mwd.getGlobalDefs(), this.getOrigVars());
            this.globalDefs = TB.andSC(defs, this.getGlobalDefs());
        } else if (mwd.getGlobalDefs() != null) {
            final JTerm defs = mwd.replace(mwd.getGlobalDefs(), this.getOrigVars());
            this.globalDefs = defs;
        }
        if (this.getAxiom() != null && mwd.getAxiom() != null) {
            final JTerm ax = mwd.replace(mwd.getAxiom(), this.getOrigVars());
            this.axiom = TB.andSC(ax, this.getAxiom());
        } else if (mwd.getGlobalDefs() != null) {
            final JTerm ax = mwd.replace(mwd.getAxiom(), this.getOrigVars());
            this.axiom = ax;
        }
        return this;
    }

    @Override
    public JTerm getGlobalDefs() {
        return this.globalDefs;
    }

    @Override
    public JTerm getAxiom() {
        return this.axiom;
    }

    @Override
    public boolean transactionApplicableContract() {
        return contract.transactionApplicableContract();
    }

    @Override
    public MethodWellDefinedness setID(int newId) {
        return new MethodWellDefinedness(getName(), newId, type(), getTarget(), getHeap(),
            getOrigVars(), getRequires(), getModifiable(), getAccessible(), getEnsures(), getMby(),
            getRepresents(), contract, globalDefs, axiom, modelField(), TB);
    }

    @Override
    public MethodWellDefinedness setTarget(KeYJavaType newKJT, IObserverFunction newPM) {
        return new MethodWellDefinedness(getName(), id(), type(), newPM, getHeap(), getOrigVars(),
            getRequires(), getModifiable(), getAccessible(), getEnsures(), getMby(),
            getRepresents(), contract.setTarget(newKJT, newPM), globalDefs, axiom, modelField(),
            TB);
    }

    @Override
    public String getTypeName() {
        return "Well-Definedness of " + (modelField() ? "JML model field" : contract.getTypeName());
    }

    @Override
    public VisibilityModifier getVisibility() {
        return contract.getVisibility();
    }

    @Override
    public KeYJavaType getKJT() {
        return contract.getKJT();
    }
}
