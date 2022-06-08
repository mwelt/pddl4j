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

package fr.uga.pddl4j.problem.operator;

import fr.uga.pddl4j.parser.Connector;
import fr.uga.pddl4j.parser.Symbol;
import fr.uga.pddl4j.util.BitMatrix;
import fr.uga.pddl4j.util.BitSet;
import fr.uga.pddl4j.util.SquareBitMatrix;

import java.util.LinkedList;
import java.util.List;

/**
 * This class implements a set orderings constraints between tasks.
 *
 * @author D. Pellier
 * @version 1.0 - 09.10.2020
 */
public class OrderingConstraintNetwork extends SquareBitMatrix {

    /**
     * Creates a deep copy from an others set of ordering constraints.
     *
     * @param other The other ordering constraints.
     */
    public OrderingConstraintNetwork(final OrderingConstraintNetwork other) {
        super(other);
    }

    /**
     * Creates a new set of ordering constraints.
     *
     * @param size the size of ordering constraints.
     */
    public OrderingConstraintNetwork(final int size) {
        super(size);
    }

    /**
     * Returns <code>true</code> if the orderings constraints is totally ordered. A ordering constraints set with
     * strictly less than 2 constraints is totally ordered.
     *
     * @return <code>true</code> if the ordering constraints set is totally ordered; <code>false</code> otherwise.
     */
    public final boolean isTotallyOrdered() {
        if (this.rows() < 2) {
            return true;
        }
        final OrderingConstraintNetwork ordering = new OrderingConstraintNetwork(this);
        boolean ordered = true;
        int index = 0;
        while (ordering.rows() > 1 && ordered) {
            List<Integer> tasks = this.getTasksWithNoPredecessors(ordering);
            ordered = tasks.size() == 1;
            if (ordered) {
                ordering.removeRow(tasks.get(0));
                ordering.removeColumn(tasks.get(0));
            }
            index++;
        }
        return ordered;
    }

    /**
     * Returns the list of tasks with no successors. The method works if only if the method
     * <code>transitiveClosure()</code> was previously called.
     *
     *
     * @return the  list of tasks with no successors.
     */
    public final List<Integer> getTasksWithNoSuccessors() {
        return this.getTasksWithNoSuccessors(this);
    }

    /**
     * Returns the list of tasks with no successors.  The method works if only if the method
     * <code>transitiveClosure()</code> was previously called.
     *
     * @return the  list of tasks with no successors.
     */
    private final List<Integer> getTasksWithNoSuccessors(OrderingConstraintNetwork matrix) {
        final List<Integer> tasks = new LinkedList<>();
        for (int i = 0; i < matrix.columns(); i++) {
            if (matrix.getRow(i).cardinality() == 0) {
                tasks.add(i);
            }
        }
        return tasks;
    }

    /**
     * Returns the list of tasks with no predecessors.  The method works if only if the method
     * <code>transitiveClosure()</code> was previously called.
     *
     * @return the  list of tasks with no predecessors.
     */
    public List<Integer> getTasksWithNoPredecessors() {
        return this.getTasksWithNoPredecessors(this);
    }

    /**
     * Returns the list of tasks with no predecessor. The method works if only if the method
     * <code>transitiveClosure()</code> was previously called.
     *
     * @return the  list of tasks with no predecessor.
     */
    private final List<Integer> getTasksWithNoPredecessors(OrderingConstraintNetwork matrix) {
        final List<Integer> tasks = new LinkedList<>();
        for (int i = 0; i < matrix.columns(); i++) {
            if (matrix.getColumn(i).cardinality() == 0) {
                tasks.add(i);
            }
        }
        return tasks;
    }

    /**
     * Returns if this orderings constraint set is cyclic.
     *
     * @return <code>true</code> if the task network contains acyclic ordering constraints, <code>false</code>
     *      otherwise.
     */
    public final boolean isAcyclic() {
        this.transitiveClosure();
        final int size = this.rows();
        boolean acyclic = true;
        int i = 0;
        while (i < size && acyclic) {
            acyclic &= !this.get(i, i);
            i++;
        }
        return acyclic;
    }


    /**
     * Returns a string representation of this ordering constraints.
     *
     * @return a string representation of the ordering constraints.
     */
    public final String toString() {
        final StringBuilder str = new StringBuilder();
        if (this.cardinality() == 0) {
            str.append(" ()");
        } else {
            int index = 0;
            for (int r = 0; r < this.rows(); r++) {
                BitSet row = this.getRow(r);
                for (int c = row.nextSetBit(0); c >= 0; c = row.nextSetBit(c + 1)) {
                    str.append(" C");
                    str.append(index);
                    str.append(": ");
                    str.append(Symbol.DEFAULT_TASK_ID_SYMBOL + r);
                    str.append(" ");
                    str.append(Connector.LESS_ORDERING_CONSTRAINT.getImage());
                    str.append(" ");
                    str.append(Symbol.DEFAULT_TASK_ID_SYMBOL + c);
                    str.append("\n");
                    index++;
                }
            }
        }
        return str.toString();
    }


}
