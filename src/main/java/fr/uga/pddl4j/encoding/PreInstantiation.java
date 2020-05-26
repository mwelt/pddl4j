/*
 * Copyright (c) 2010 by Damien Pellier <Damien.Pellier@imag.fr>.
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

package fr.uga.pddl4j.encoding;

import fr.uga.pddl4j.parser.PDDLConnective;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class contains the methods for the pre instantiation step. In other words, it contains methods in order to:
 * <ul>
 * <li> extract inertia information from the action and method encoded into integer representation.</li>
 * <li> create predicates tables used to count the occurrences of a specified predicates in the initial state.</li>
 * <li> infer types from unary inertia information.</li>
 * <li> simplify actions and methods with infer types information.</li>
 * </ul>
 * <p>
 * Revisions:
 * <ul>
 *     <li> 19/04/13: Add a hack in method simplifiedActionWithInferredType to deal with the problem the constant
 *     in in precondition and effect of in action.</li>
 *     <li> 30/03/19: Add a method to simplify methods.</li>
 * </ul>
 * </p>
 *
 * @author D. Pellier
 * @version 1.2 - 10.04.2010
 */
final class PreInstantiation implements Serializable {

    /**
     * The logger of the class.
     */
    private static final Logger LOGGER = LogManager.getLogger(PreInstantiation.class);

    /**
     * The default constructor with a private access to prevent instance creation.
     */
    private PreInstantiation() {
    }

    /**
     * This method proceeds over the actions of the domain and checks for all atom which kind of
     * inertia it is. For each atom it checks if it satisfies one of the following definitions:
     * <p>
     * <i>Definition:</i> A relation is a positive inertia iff it does not occur positively in an
     * unconditional effect or the consequent of a conditional effect of an action.
     * </p>
     * <p>
     * <i>Definition:</i> A relation is a negative inertia iff it does not occur negatively in an
     * unconditional effect or the consequent of a conditional effect of an action.
     * </p>
     * <p>
     * Relations, which are positive as well as negative inertia, are simply called inertia.
     * Relations, which are neither positive nor negative inertia, are called fluents. The detection
     * of inertia and fluents is easy because in ADL, effects are restricted to conjunctions of
     * literals. Furthermore, this information can be obtained with a single pass over the domain
     * description, which takes almost no time at all.
     * </p>
     * <p>
     * Note: before calling this method the domain must be encode into integer and the negation must
     * be move inward the expression.
     * </p>
     *
     * @param actions the list of actions to simplified.
     */
    static void extractInertia(final List<IntAction> actions) {
        final int nbPredicates = Encoder.tableOfPredicates.size();
        Encoder.tableOfInertia = new ArrayList<>(nbPredicates);
        for (int i = 0; i < nbPredicates; i++) {
            Encoder.tableOfInertia.add(Inertia.INERTIA);
        }
        for (final IntAction op : actions) {
            PreInstantiation.extract(op.getEffects());
        }

    }

    /**
     * Do a pass over the effects of an action and update the inertia table.
     *
     * @param exp the effect.
     */
    private static void extract(final IntExpression exp) {
        switch (exp.getConnective()) {
            case ATOM:
                int predicate = exp.getPredicate();
                switch (Encoder.tableOfInertia.get(predicate)) {
                    case INERTIA:
                        Encoder.tableOfInertia.set(predicate, Inertia.NEGATIVE);
                        break;
                    case POSITIVE:
                        Encoder.tableOfInertia.set(predicate, Inertia.FLUENT);
                        break;
                    default:
                        // do nothing
                }
                break;
            case AND:
            case OR:
                exp.getChildren().forEach(PreInstantiation::extract);
                break;
            case FORALL:
            case EXISTS:
            case AT_START:
            case AT_END:
                PreInstantiation.extract(exp.getChildren().get(0));
                break;
            case WHEN:
                PreInstantiation.extract(exp.getChildren().get(1));
                break;
            case NOT:
                final IntExpression neg = exp.getChildren().get(0);
                if (neg.getConnective().equals(PDDLConnective.ATOM)) {
                    predicate = neg.getPredicate();
                    switch (Encoder.tableOfInertia.get(predicate)) {
                        case INERTIA:
                            Encoder.tableOfInertia.set(predicate, Inertia.POSITIVE);
                            break;
                        case NEGATIVE:
                            Encoder.tableOfInertia.set(predicate, Inertia.FLUENT);
                            break;
                        default:
                            // do nothing
                    }
                }
                break;
            case F_EXP_T:
            case EQUAL_ATOM:
            case FN_HEAD:
            case FN_ATOM:
            case DURATION_ATOM:
            case LESS:
            case LESS_OR_EQUAL:
            case EQUAL:
            case GREATER:
            case GREATER_OR_EQUAL:
            case ASSIGN:
            case INCREASE:
            case DECREASE:
            case SCALE_UP:
            case SCALE_DOWN:
            case MUL:
            case DIV:
            case MINUS:
            case PLUS:
            case SOMETIME_AFTER:
            case SOMETIME_BEFORE:
            case WITHIN:
            case HOLD_AFTER:
            case ALWAYS_WITHIN:
            case HOLD_DURING:
            case TIME_VAR:
            case IS_VIOLATED:
            case NUMBER:
            case MINIMIZE:
            case MAXIMIZE:
            case UMINUS:
            case ALWAYS:
            case OVER_ALL:
            case SOMETIME:
            case AT_MOST_ONCE:
            case F_EXP:
                // do nothing
                break;
            default:
                // do nothing
        }
    }

    /**
     * Infer type from unary inertia.
     *
     * @param init the initial state.
     */
    static void inferTypesFromInertia(final Set<IntExpression> init) {
        Encoder.tableOfInferredDomains = new ArrayList<>(Encoder.tableOfPredicates.size());
        for (int i = 0; i < Encoder.tableOfPredicates.size(); i++) {
            if (Encoder.tableOfTypedPredicates.get(i).size() == 1
                && Encoder.tableOfInertia.get(i).equals(Inertia.INERTIA)) {
                final Set<Integer> newTypeDomain = new LinkedHashSet<>();
                for (IntExpression fact : init) {
                    if (fact.getConnective().equals(PDDLConnective.NOT)) {
                        fact = fact.getChildren().get(0);
                    }
                    if (fact.getPredicate() == i) {
                        newTypeDomain.add(fact.getArguments()[0]);
                    }
                }
                Encoder.tableOfInferredDomains.add(newTypeDomain);
            } else {
                Encoder.tableOfInferredDomains.add(null);
            }
        }
    }

    /**
     * This method creates the predicates predicatesTables used to simplify atomic expression.
     *
     * @param init the initial state.
     */
    static void createPredicatesTables(final Set<IntExpression> init) {
        final int tableSize = Encoder.tableOfConstants.size();
        final int nbPredicate = Encoder.tableOfPredicates.size();
        Encoder.predicatesTables = new ArrayList<>(nbPredicate);
        for (final List<Integer> arguments : Encoder.tableOfTypedPredicates) {
            final int arity = arguments.size();
            final int nbTables = (int) Math.pow(2, arity);
            final List<IntMatrix> pTables = new ArrayList<>(nbTables);
            for (int j = 0; j < nbTables; j++) {
                final int dimension = Integer.bitCount(j);
                pTables.add(new IntMatrix(tableSize, dimension));
            }
            Encoder.predicatesTables.add(pTables);
        }

        for (IntExpression fact : init) {
            if (fact.getConnective().equals(PDDLConnective.NOT)) {
                fact = fact.getChildren().get(0);
            }
            final int arity = Encoder.tableOfTypedPredicates.get(fact.getPredicate()).size();
            final List<IntMatrix> pTables = Encoder.predicatesTables.get(fact.getPredicate());
            final int[] set = new int[arity];
            final int[] args = fact.getArguments();
            for (final IntMatrix intMatrix : pTables) {
                int indexSize = 0;
                for (int aSet : set) {
                    if (aSet == 1) {
                        indexSize++;
                    }
                }
                final int[] index = new int[indexSize];
                int j = 0;
                for (int i = 0; i < set.length; i++) {
                    if (set[i] == 1) {
                        index[j] = args[i];
                        j++;
                    }
                }
                intMatrix.increment(index);
                PreInstantiation.incrementMask(set);
            }
        }
    }


    /**
     * Return an integer representation of the specified array of integer. For example, if
     * <code>mask = [0, 1, 1]</code> then the method will return 3.
     *
     * @param mask an array of integer that can only contain 0 or 1.
     * @return the integer representation of the specified array.
     */
    static int toInt(final int[] mask) {
        final int len = mask.length;
        if (len > 0) {
            int res = mask[0];
            for (int i = 1; i < len; i++) {
                res = res << 1 | mask[i];
            }
            return res;
        }
        return 0;
    }

    private static int[] incrementMask(final int[] set) {
        boolean overflow = false;
        for (int i = set.length - 1; i >= 0; i--) {
            if (set[i] == 0) {
                set[i] = 1;
                break;
            } else {
                set[i] = 0;
                overflow = i == 0;
            }
        }
        return overflow ? null : set;
    }

    /**
     * Print the predicates tables.
     *
     * @param tables predicates tables.
     */
    void printPredicatesTables(final List<List<IntMatrix>> tables) {
        LOGGER.trace("tables of predicates:");
        for (int predicate = 0; predicate < tables.size(); predicate++) {
            final List<IntMatrix> pTables = tables.get(predicate);
            final int arity = Encoder.tableOfTypedPredicates.get(predicate).size();
            final int[] mask = new int[arity];
            for (int i = 0; i < pTables.size(); i++) {
                this.print(predicate, arity, mask, new int[0], tables);
                PreInstantiation.incrementMask(mask);
            }
        }
    }

    /**
     * Print a specified predicate table.
     *
     * @param predicate the predicate.
     * @param arity     the arity of the predicate.
     * @param mask      the mask that indicate where the constants are.
     * @param index     the index in construction.
     * @param tables    the predicates tables.
     */
    private void print(final int predicate, final int arity, final int[] mask, final int[] index,
                       final List<List<IntMatrix>> tables) {
        if (index.length == arity) {
            final StringBuilder str = new StringBuilder();
            str.append("(");
            str.append(Encoder.tableOfPredicates.get(predicate));
            int var = 0;
            int realIndexSize = 0;
            for (int anIndex : index) {
                if (anIndex == -1) {
                    str.append(" X").append(var);
                    var++;
                } else {
                    realIndexSize++;
                    str.append(" ").append(Encoder.tableOfConstants.get(anIndex));
                }
            }
            str.append(")");
            final int[] realIndex = new int[realIndexSize];
            int j = 0;
            for (int anIndex : index) {
                if (anIndex != -1) {
                    realIndex[j] = anIndex;
                    j++;
                }
            }
            final int counter = tables.get(predicate).get(PreInstantiation.toInt(mask)).get(realIndex);
            if (counter != 0) {
                str.append(" : ").append(counter);
                LOGGER.trace(str);
            }
        } else if (mask[index.length] == 0) {
            final int[] newIndex = new int[index.length + 1];
            System.arraycopy(index, 0, newIndex, 0, index.length);
            newIndex[index.length] = -1;
            this.print(predicate, arity, mask, newIndex, tables);
        } else {
            for (int i = 0; i < Encoder.tableOfConstants.size(); i++) {
                final int[] newIndex = new int[index.length + 1];
                System.arraycopy(index, 0, newIndex, 0, index.length);
                newIndex[index.length] = i;
                this.print(predicate, arity, mask, newIndex, tables);
            }
        }
    }

    /**
     * Symplify the actions with the infered types.
     *
     * @return the list of simplified actions.
     */
    static List<IntAction> simplifyActionsWithInferredTypes(final List<IntAction> actions) {
        final List<IntAction> ops = new LinkedList<>();
        for (final IntAction op : actions) {
            ops.addAll(PreInstantiation.simplifyActionsWithInferredTypes(op));
        }
        return ops;
    }

    private static List<IntAction> simplifyActionsWithInferredTypes(final IntAction action) {
        final List<IntExpression> unaryInertia = new ArrayList<>();
        unaryInertia.addAll(PreInstantiation.collectUnaryInertia(action.getPreconditions()));
        unaryInertia.addAll(PreInstantiation.collectUnaryInertia(action.getEffects()));


        List<IntAction> actions = new LinkedList<>();
        actions.add(action);

        for (final IntExpression inertia : unaryInertia) {
            final List<IntAction> newActions = new ArrayList<>();
            for (final IntAction o : actions) {
                if (o.arity() > 0) {

                    int index = -inertia.getArguments()[0] - 1;
                    // Hack add for constant in predicate
                    if (index < 0) {
                        break;
                    }

                    final int dtIndex = action.getTypeOfParameters(index);

                    final String declaredType = Encoder.tableOfTypes.get(dtIndex);
                    final int itIndex = inertia.getPredicate();
                    final String inertiaType = Encoder.tableOfPredicates.get(itIndex);

                    final String sti = declaredType + "^" + inertiaType;
                    int ti = Encoder.tableOfTypes.indexOf(sti);
                    if (ti == -1) {
                        ti = Encoder.tableOfTypes.size();
                        Encoder.tableOfTypes.add(sti);
                        final Set<Integer> dt1 = new LinkedHashSet<>(Encoder.tableOfDomains.get(dtIndex));
                        dt1.retainAll(Encoder.tableOfInferredDomains.get(itIndex));
                        Encoder.tableOfDomains.add(dt1);
                    }

                    final String sts = declaredType + "\\" + inertiaType;
                    int ts = Encoder.tableOfTypes.indexOf(sts);
                    if (ts == -1) {
                        ts = Encoder.tableOfTypes.size();
                        Encoder.tableOfTypes.add(sts);
                        final Set<Integer> dt2 = new LinkedHashSet<>(Encoder.tableOfDomains.get(dtIndex));
                        dt2.removeAll(Encoder.tableOfInferredDomains.get(itIndex));
                        Encoder.tableOfDomains.add(dt2);
                    }
                    final IntAction op1 = new IntAction(o);
                    op1.setTypeOfParameter(index, ti);
                    PreInstantiation.replace(op1.getPreconditions(), inertia, PDDLConnective.TRUE, ti, ts);
                    PreInstantiation.replace(op1.getEffects(), inertia, PDDLConnective.TRUE, ti, ts);
                    if (!op1.getPreconditions().getConnective().equals(PDDLConnective.FALSE)
                        && !op1.getEffects().getConnective().equals(PDDLConnective.FALSE)) {
                        newActions.add(op1);
                    }

                    final IntAction op2 = new IntAction(o);
                    op2.setTypeOfParameter(index, ts);
                    PreInstantiation.replace(op2.getPreconditions(), inertia, PDDLConnective.FALSE, ti, ts);
                    PreInstantiation.replace(op2.getEffects(), inertia, PDDLConnective.FALSE, ti, ts);

                    if (!op2.getPreconditions().getConnective().equals(PDDLConnective.FALSE)
                        && !op2.getEffects().getConnective().equals(PDDLConnective.FALSE)) {
                        newActions.add(op2);
                    }
                }
            }
            actions.clear();
            actions.addAll(newActions);
        }
        return actions;
    }

    /**
     * Symplify the method with the infered types.
     *
     * @return the list of simplified methods.
     */
    static List<IntMethod> simplifyMethodsWithInferredTypes(final List<IntMethod> methods) {
        final List<IntMethod> meths = new LinkedList<>();
        for (final IntMethod meth : methods) {
            meths.addAll(PreInstantiation.simplifyMethodsWithInferredTypes(meth));
        }
        return meths;
    }

    private static List<IntMethod> simplifyMethodsWithInferredTypes(final IntMethod meth) {
        final List<IntExpression> unaryInertia = new ArrayList<>();
        unaryInertia.addAll(PreInstantiation.collectUnaryInertia(meth.getPreconditions()));

        List<IntMethod> methods = new LinkedList<>();
        methods.add(meth);

        for (final IntExpression inertia : unaryInertia) {
            final List<IntMethod> newMethods = new ArrayList<>();
            for (final IntMethod m : methods) {
                if (m.arity() > 0) {

                    int index = -inertia.getArguments()[0] - 1;
                    // Hack add for constant in predicate
                    if (index < 0) {
                        break;
                    }

                    final int dtIndex = meth.getTypeOfParameters(index);

                    final String declaredType = Encoder.tableOfTypes.get(dtIndex);
                    final int itIndex = inertia.getPredicate();
                    final String inertiaType = Encoder.tableOfPredicates.get(itIndex);

                    final String sti = declaredType + "^" + inertiaType;
                    int ti = Encoder.tableOfTypes.indexOf(sti);
                    if (ti == -1) {
                        ti = Encoder.tableOfTypes.size();
                        Encoder.tableOfTypes.add(sti);
                        final Set<Integer> dt1 = new LinkedHashSet<>(Encoder.tableOfDomains.get(dtIndex));
                        dt1.retainAll(Encoder.tableOfInferredDomains.get(itIndex));
                        Encoder.tableOfDomains.add(dt1);
                    }

                    final String sts = declaredType + "\\" + inertiaType;
                    int ts = Encoder.tableOfTypes.indexOf(sts);
                    if (ts == -1) {
                        ts = Encoder.tableOfTypes.size();
                        Encoder.tableOfTypes.add(sts);
                        final Set<Integer> dt2 = new LinkedHashSet<>(Encoder.tableOfDomains.get(dtIndex));
                        dt2.removeAll(Encoder.tableOfInferredDomains.get(itIndex));
                        Encoder.tableOfDomains.add(dt2);
                    }

                    final IntMethod meth1 = new IntMethod(m);
                    meth1.setTypeOfParameter(index, ti);
                    PreInstantiation.replace(meth1.getPreconditions(), inertia, PDDLConnective.TRUE, ti, ts);
                    if (!meth1.getPreconditions().getConnective().equals(PDDLConnective.FALSE)) {
                        newMethods.add(meth1);
                    }
                    final IntMethod meth2 = new IntMethod(m);
                    meth2.setTypeOfParameter(index, ts);
                    PreInstantiation.replace(meth2.getPreconditions(), inertia, PDDLConnective.FALSE, ti, ts);
                    if (!meth2.getPreconditions().getConnective().equals(PDDLConnective.FALSE)) {
                        newMethods.add(meth2);
                    }
                }
            }
            methods = newMethods;
        }
        return methods;
    }

    /**
     * Replace all the occurrences of a specified unary inertia contained in the a specified
     * expression by <code>TRUE</code> or <code>FALSE</code>.
     *
     * @param exp        the expression.
     * @param inertia    the unary inertia.
     * @param connective the connective.
     * @param ti         the type intersection.
     * @param ts         the type substract.
     */
    private static void replace(final IntExpression exp, final IntExpression inertia, final PDDLConnective connective,
                                final int ti, final int ts) {
        switch (exp.getConnective()) {
            case ATOM:
                if (exp.equals(inertia)) {
                    exp.setConnective(connective);
                }
                break;
            case AND:
                Iterator<IntExpression> i = exp.getChildren().iterator();
                while (i.hasNext() && exp.getConnective().equals(PDDLConnective.AND)) {
                    final IntExpression ei = i.next();
                    PreInstantiation.replace(ei, inertia, connective, ti, ts);
                    if (ei.getConnective().equals(PDDLConnective.FALSE)) {
                        exp.setConnective(PDDLConnective.FALSE);
                    } else if (ei.getConnective().equals(PDDLConnective.TRUE)) {
                        i.remove();
                    }
                }
                break;
            case OR:
                i = exp.getChildren().iterator();
                while (i.hasNext() && exp.getConnective().equals(PDDLConnective.OR)) {
                    final IntExpression ei = i.next();
                    PreInstantiation.replace(ei, inertia, connective, ti, ts);
                    if (ei.getConnective().equals(PDDLConnective.TRUE)) {
                        exp.setConnective(PDDLConnective.TRUE);
                    } else if (ei.getConnective().equals(PDDLConnective.FALSE)) {
                        i.remove();
                    }
                }
                break;
            case FORALL:
            case EXISTS:
                if (inertia.getArguments()[0] == exp.getVariable()) {
                    final IntExpression ei = new IntExpression(exp);
                    ei.setType(ti);
                    PreInstantiation.replace(ei, inertia, PDDLConnective.TRUE, ti, ts);
                    final IntExpression es = new IntExpression(exp);
                    es.setType(ts);
                    PreInstantiation.replace(es, inertia, PDDLConnective.FALSE, ti, ts);
                    exp.getChildren().clear();
                    if (exp.getConnective().equals(PDDLConnective.FORALL)) {
                        exp.setConnective(PDDLConnective.AND);
                    } else {
                        exp.setConnective(PDDLConnective.OR);
                    }
                    exp.getChildren().add(ei);
                    exp.getChildren().add(es);
                } else {
                    PreInstantiation.replace(exp.getChildren().get(0), inertia, connective, ti, ts);
                }
                break;
            case AT_START:
            case AT_END:
            case NOT:
            case ALWAYS:
            case OVER_ALL:
            case SOMETIME:
            case AT_MOST_ONCE:
            case SOMETIME_AFTER:
            case SOMETIME_BEFORE:
            case WITHIN:
            case HOLD_AFTER:
            case WHEN:
                PreInstantiation.replace(exp.getChildren().get(0), inertia, connective, ti, ts);
                break;
            case ALWAYS_WITHIN:
            case HOLD_DURING:
                PreInstantiation.replace(exp.getChildren().get(0), inertia, connective, ti, ts);
                PreInstantiation.replace(exp.getChildren().get(1), inertia, connective, ti, ts);
                PreInstantiation.replace(exp.getChildren().get(3), inertia, connective, ti, ts);
                break;
            case EQUAL_ATOM:
            case FN_HEAD:
            case FN_ATOM:
            case DURATION_ATOM:
            case LESS:
            case LESS_OR_EQUAL:
            case EQUAL:
            case GREATER:
            case GREATER_OR_EQUAL:
            case ASSIGN:
            case INCREASE:
            case DECREASE:
            case SCALE_UP:
            case SCALE_DOWN:
            case MUL:
            case DIV:
            case MINUS:
            case PLUS:
            case F_EXP_T:
            case NUMBER:
            case MINIMIZE:
            case MAXIMIZE:
            case UMINUS:
            case F_EXP:
            case TIME_VAR:
            case IS_VIOLATED:
                // do nothing
                break;
            default:
                // do nothing
        }
    }

    /**
     * Collect all unary inertia from a specified expression.
     *
     * @param exp the expression.
     * @return the list of unary inertia expression collected.
     */
    private static List<IntExpression> collectUnaryInertia(final IntExpression exp) {
        final List<IntExpression> unaryInertia = new ArrayList<>();
        switch (exp.getConnective()) {
            case ATOM:
                if (Encoder.tableOfInferredDomains.get(exp.getPredicate()) != null) {
                    unaryInertia.add(exp);
                }
                break;
            case AND:
            case OR:
                for (final IntExpression ei : exp.getChildren()) {
                    unaryInertia.addAll(PreInstantiation.collectUnaryInertia(ei));
                }
                break;
            case FORALL:
            case EXISTS:
                final IntExpression qExp = exp.getChildren().get(0);
                unaryInertia.addAll(PreInstantiation.collectUnaryInertia(qExp));
                break;
            case AT_START:
            case AT_END:
            case NOT:
            case ALWAYS:
            case OVER_ALL:
            case SOMETIME:
            case AT_MOST_ONCE:
            case SOMETIME_AFTER:
            case SOMETIME_BEFORE:
            case WITHIN:
            case HOLD_AFTER:
            case WHEN:
                unaryInertia.addAll(PreInstantiation.collectUnaryInertia(exp.getChildren().get(0)));
                break;
            case ALWAYS_WITHIN:
            case HOLD_DURING:
                unaryInertia.addAll(PreInstantiation.collectUnaryInertia(exp.getChildren().get(0)));
                unaryInertia.addAll(PreInstantiation.collectUnaryInertia(exp.getChildren().get(1)));
                unaryInertia.addAll(PreInstantiation.collectUnaryInertia(exp.getChildren().get(3)));
                break;
            case EQUAL_ATOM:
            case FN_HEAD:
            case FN_ATOM:
            case DURATION_ATOM:
            case LESS:
            case LESS_OR_EQUAL:
            case EQUAL:
            case GREATER:
            case GREATER_OR_EQUAL:
            case ASSIGN:
            case INCREASE:
            case DECREASE:
            case SCALE_UP:
            case SCALE_DOWN:
            case MUL:
            case DIV:
            case MINUS:
            case PLUS:
            case F_EXP_T:
            case NUMBER:
            case MINIMIZE:
            case MAXIMIZE:
            case UMINUS:
            case F_EXP:
            case TIME_VAR:
            case IS_VIOLATED:
                // do nothing
                break;
            default:
                // do nothing
        }
        return unaryInertia;
    }
}
