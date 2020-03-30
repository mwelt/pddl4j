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
public abstract class AbstractOperator implements Operator {

    /**
     * The name of the operator.
     */
    private Symbol name;

    /**
     * The list of parameters of the operators.
     */
    private List<TypedSymbol> parameters;

    /**
     * The goal description that represents the preconditions of the operator.
     */
    private Exp preconditions;

    /**
     * Create a new operator from another.
     *
     */
    private AbstractOperator() {
        super();
    }

    /**
     * Create a new operator from another.
     *
     * @param other the other operator.
     */
    protected AbstractOperator(final Operator other) {
        if (other == null) {
            throw new NullPointerException();
        }
        this.name = new Symbol(other.getName());
        this.parameters = new LinkedList<>();
        this.parameters.addAll(other.getParameters().stream().map(TypedSymbol::new).collect(Collectors.toList()));
        this.preconditions = new Exp(other.getPreconditions());
    }

    /**
     * Creates operator with a specified name, list of parameters, preconditions and effects.
     *
     * @param name The name of the operator.
     * @param parameters The list of parameters of the operator.
     * @param preconditions The goal description that represents the preconditions of the operator.
     */
    protected AbstractOperator(final Symbol name, final List<TypedSymbol> parameters, final Exp preconditions) {
        this.name = name;
        this.parameters = parameters;
        this.preconditions = preconditions;
    }

    /**
     * Returns the name of the operator.
     *
     * @return the name of the operator.
     */
    public final Symbol getName() {
        return this.name;
    }

    /**
     * Sets a new name to the operator.
     *
     * @param name the name to set.
     */
    public final void setName(final Symbol name) {
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
    public final List<TypedSymbol> getParameters() {
        return this.parameters;
    }

    /**
     * Returns the parameter of the operator that has a specified symbol.
     *
     * @param symbol The symbol.
     * @return the parameter of the operator that has a specified symbol or <code>null</code> if the
     *          operator has no such parameter.
     */
    public final TypedSymbol getParameter(final Symbol symbol) {
        final int index = this.parameters.indexOf(symbol);
        return (index == -1) ? null : this.parameters.get(index);
    }

    /**
     * Returns the task representaion of this operator.
     *
     * @return the task representaion of this operator.
     */
    public final NamedTypedList toTask() {
        NamedTypedList task = new NamedTypedList(this.getName());
        for (TypedSymbol p : this.getParameters()) {
            task.add(new TypedSymbol(p));
        }
        return task;
    }

    /**
     * Sets a new list of parameters to this operator.
     *
     * @param parameters The list of parameters to set.
     * @throws NullPointerException if the specified parameters is null.
     */
    public final void setParameters(final List<TypedSymbol> parameters) {
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
    public final Exp getPreconditions() {
        return this.preconditions;
    }

    /**
     * Sets new preconditions to the operator.
     *
     * @param preconditions The new goal description that represents the preconditions of the
     *                      operator to set.
     * @throws NullPointerException if the specified preconditions is null.
     */
    public final void setPreconditions(final Exp preconditions) {
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
     * Normalizes the operators.
     *
     * @see Exp#renameVariables()
     * @see Exp#moveNegationInward()
     */
    public final void normalize() {
        this.normalize(0);
    }

    /**
     * Normalizes the operators.
     *
     * @param index the index of the first variable, index, i.e., ?Xi.
     * @return the renamed variable.
     * @see Exp#renameVariables()
     * @see Exp#moveNegationInward()
     */
    protected Map<String, String> normalize(int index) {
        int i = index;
        // Rename the parameters
        final Map<String, String> context = new LinkedHashMap<>();
        final List<TypedSymbol> parameters = this.getParameters();
        for (final TypedSymbol params : parameters) {
            final String image = params.renameVariables(i);
            context.put(image, params.getImage());
            i++;
        }
        // A hack to remove single atom in precondition
        if (this.preconditions.isLiteral()) {
            Exp atom = this.preconditions;
            this.preconditions = new Exp(Connective.AND);
            this.preconditions.addChild(atom);
        }
        // Rename the preconditions
        this.getPreconditions().renameVariables(context);
        this.getPreconditions().moveNegationInward();
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
        if (object != null && object instanceof AbstractOperator) {
            final AbstractOperator other = (Action) object;
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

