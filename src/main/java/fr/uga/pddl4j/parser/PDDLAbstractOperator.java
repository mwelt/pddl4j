/*
 * Copyright (c) 2020 by Damien Pellier <Damien.Pellier@imag.fr>.
 *
 * This file is part of PDDL4J library.
 *
 * PDDL4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PDDL4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PDDL4J.  If not, see <http://www.gnu.org/licenses/>
 */

package fr.uga.pddl4j.parser;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class defines an abstract planning operator.
 *
 * @author D. Pellier
 * @version 1.0 - 25.03.2020
 */
public abstract class PDDLAbstractOperator extends AbstractParsedObject implements PDDLOperator {

    /**
     * The name of the operator.
     */
    private PDDLSymbol name;

    /**
     * The list of parameters of the operators.
     */
    private List<PDDLTypedSymbol> parameters;

    /**
     * The goal description that represents the preconditions of the operator.
     */
    private PDDLExpression preconditions;

    /**
     * The duration constraints of the operator.
     */
    private PDDLExpression duration;

    /**
     * Create a new operator from another.
     */
    private PDDLAbstractOperator() {
        super();
    }

    /**
     * Create a new operator from another.
     *
     * @param other the other operator.
     */
    protected PDDLAbstractOperator(final PDDLOperator other) {
        if (other == null) {
            throw new NullPointerException();
        }
        this.name = new PDDLSymbol(other.getName());
        this.parameters = new LinkedList<>();
        this.parameters.addAll(other.getParameters().stream().map(PDDLTypedSymbol::new).collect(Collectors.toList()));
        this.preconditions = new PDDLExpression(other.getPreconditions());
        if (this.duration != null) {
            this.duration = new PDDLExpression(other.getDuration());
        }
    }

    /**
     * Creates operator with a specified name, list of parameters, preconditions.
     *
     * @param name The name of the operator.
     * @param parameters The list of parameters of the operator.
     * @param preconditions The goal description that represents the preconditions of the operator.
     */
    protected PDDLAbstractOperator(final PDDLSymbol name, final List<PDDLTypedSymbol> parameters,
                                   final PDDLExpression preconditions) {
        this(name, parameters, preconditions, null);
    }

    /**
     * Creates operator with a specified name, list of parameters, preconditions and duration constraints.
     *
     * @param name The name of the operator.
     * @param parameters The list of parameters of the operator.
     * @param preconditions The goal description that represents the preconditions of the operator.
     * @param duration the duration constraint of the operator.
     */
    protected PDDLAbstractOperator(final PDDLSymbol name, final List<PDDLTypedSymbol> parameters,
                                   final PDDLExpression preconditions, final PDDLExpression duration) {
        this.name = name;
        this.parameters = parameters;
        this.preconditions = preconditions;
        this.duration = duration;
    }

    /**
     * Returns the name of the operator.
     *
     * @return the name of the operator.
     */
    public final PDDLSymbol getName() {
        return this.name;
    }

    /**
     * Sets a new name to the operator.
     *
     * @param name the name to set.
     */
    public final void setName(final PDDLSymbol name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }

    /**
     * Returns the list of parameters of the operator.
     *
     * @return the list of parameters of the operator.
     */
    public final List<PDDLTypedSymbol> getParameters() {
        return this.parameters;
    }

    /**
     * Returns the parameter of the operator that has a specified symbol.
     *
     * @param symbol The symbol.
     * @return the parameter of the operator that has a specified symbol or <code>null</code> if the
     *          operator has no such parameter.
     */
    public final PDDLTypedSymbol getParameter(final PDDLSymbol symbol) {
        final int index = this.parameters.indexOf(symbol);
        return (index == -1) ? null : this.parameters.get(index);
    }

    /**
     * Returns the task representaion of this operator.
     *
     * @return the task representaion of this operator.
     */
    public final PDDLNamedTypedList toTask() {
        PDDLNamedTypedList task = new PDDLNamedTypedList(this.getName());
        for (PDDLTypedSymbol p : this.getParameters()) {
            task.add(new PDDLTypedSymbol(p));
        }
        return task;
    }

    /**
     * Sets a new list of parameters to this operator.
     *
     * @param parameters The list of parameters to set.
     * @throws NullPointerException if the specified parameters is null.
     */
    public final void setParameters(final List<PDDLTypedSymbol> parameters) {
        if (parameters == null) {
            throw new NullPointerException();
        }
        this.parameters = parameters;
    }

    /**
     * Returns the goal description that represents the preconditions of the operator.
     *
     * @return The goal description that represents the preconditions of the operator.
     */
    public final PDDLExpression getPreconditions() {
        return this.preconditions;
    }

    /**
     * Sets new preconditions to the operator.
     *
     * @param preconditions The new goal description that represents the preconditions of the
     *                      operator to set.
     * @throws NullPointerException if the specified preconditions is null.
     */
    public final void setPreconditions(final PDDLExpression preconditions) {
        if (preconditions == null) {
            throw new NullPointerException();
        }
        this.preconditions = preconditions;
    }

    /**
     * Return the arity of the operator, i.e., the number of parameters of the operator.
     *
     * @return the arity of the operator.
     */
    public final int getArity() {
        return this.parameters.size();
    }

    /**
     * Returns the duration constraints of the operator.
     *
     * @return the duration constraints of the operator.
     */
    public final PDDLExpression getDuration() {
        return this.duration;
    }

    /**
     * Sets new duration constraints to the operator.
     *
     * @param duration the duration constraint to set
     */
    public final void setDuration(final PDDLExpression duration) {
        this.duration = duration;
    }

    /**
     * Returns if this action is durative operator.
     *
     * @return <code>true</code> if this operator is a durative, <code>false</code> otherwise.
     */
    public final boolean isDurative() {
        return this.getDuration() != null;
    }

    /**
     * Normalizes the operators. This method renames the parameters of the operator used in its preconditions, its
     * effects and its durative constraints. It also simplifies all the logical expression and converts it into it
     * negative normal form. Not that imply expression are also replace by their disjunctive equivalence.
     *
     * @see PDDLExpression#renameVariables()
     * @see PDDLExpression#simplify()
     * @see PDDLExpression#toNNF() ()
     */
    public final void normalize() {
        this.normalize(0);
    }

    /**
     * Normalizes the operators. This method renames the parameters of the operator used in its preconditions, its
     * effects and its durative constraints. It also simplifies all the logical expression and converts it into it
     * negative normal form. Not that imply expression are also replace by their disjunctive equivalence.
     *
     * @param index the index of the first variable, index, i.e., ?Xi.
     * @return the renamed variable.
     * @see PDDLExpression#renameVariables()
     * @see PDDLExpression#simplify()
     * @see PDDLExpression#toNNF() ()
     */
    protected Map<String, String> normalize(int index) {
        int i = index;
        // Rename the parameters
        final Map<String, String> context = new LinkedHashMap<>();
        final List<PDDLTypedSymbol> parameters = this.getParameters();
        for (final PDDLTypedSymbol params : parameters) {
            final String image = AbstractExpression.renameVariables(params, i);
            context.put(image, params.getImage());
            i++;
        }
        // Rename the preconditions
        this.getPreconditions().renameVariables(context);
        this.getPreconditions().simplify();
        this.getPreconditions().toNNF();
        return context;
    }

    /**
     * Return if this operator is equals to another object.
     *
     * @param object the other object.
     * @return <code>true</code> if <code>object</code> is not <code>null</code>, is an instance of
     *          the class <code>Action</code>, and has the same name; otherwise it returns <code>false</code>.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object object) {
        if (object != null && object instanceof PDDLAbstractOperator) {
            final PDDLAbstractOperator other = (PDDLAction) object;
            return this.name.equals(other.name);
        }
        return false;
    }

    /**
     * Returns the hash code value of the operator.
     *
     * @return the hash code value of the operator.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        return this.name.hashCode();
    }
}

