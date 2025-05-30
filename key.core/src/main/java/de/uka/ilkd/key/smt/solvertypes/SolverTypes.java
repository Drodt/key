/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.smt.solvertypes;

import java.util.*;

import org.jspecify.annotations.NonNull;

/**
 * Facade for the management of {@link SolverType}. This class holds references to common and known
 * solver type instances.
 * <p>
 * To add a new solver type, use the service loader with the {@link SolverType} interface.
 *
 * @author Alexander Weigl
 * @version 1 (9/29/21)
 */
public final class SolverTypes {

    /**
     * All available solver types, including experimental solvers. The objects in this map are
     * identically returned whenever {@link #getSolverTypes()} is called.
     */
    private static final Collection<SolverType> SOLVERS = new ArrayList<>(5);
    /**
     * The available experimental solvers out of the {@link #SOLVERS} list. The objects in this map
     * are identically returned whenever {@link #getExperimentalSolvers()} is called.
     */
    private static final Collection<SolverType> EXPERIMENTAL_SOLVERS = new ArrayList<>(1);

    private SolverTypes() {

    }

    /**
     * Loads and returns the available solver types using the {@link SolverPropertiesLoader}. The
     * returned SolverType objects don't change (singletons).
     *
     * @return the available solver types, including experimental solvers
     */
    public static @NonNull Collection<SolverType> getSolverTypes() {
        if (SOLVERS.isEmpty()) {
            SolverPropertiesLoader solverLoader = new SolverPropertiesLoader();
            SOLVERS.addAll(solverLoader.getSolvers());
            EXPERIMENTAL_SOLVERS.addAll(solverLoader.getExperimentalSolvers());
        }
        return new ArrayList<>(SOLVERS);
    }

    /**
     * Returns the available experimental solver types according to the
     * {@link SolverPropertiesLoader}.
     *
     * @return the available experimental solver types
     */
    public static @NonNull Collection<SolverType> getExperimentalSolvers() {
        if (SOLVERS.isEmpty()) {
            getSolverTypes();
        }
        return new ArrayList<>(EXPERIMENTAL_SOLVERS);
    }

    /**
     * Z3 counterexample solver.
     */
    public static final SolverType Z3_CE_SOLVER = getSolverTypes().stream().filter(
        it -> it.getClass().equals(SolverTypeImplementation.class) && it.getName().equals("Z3_CE"))
            .findFirst().orElse(null);

}
