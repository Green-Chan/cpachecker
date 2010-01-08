/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction.trace;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import symbpredabstraction.interfaces.Predicate;
import cpa.common.interfaces.AbstractElement;


/**
 * A class that stores information about a counterexample trace. For
 * real counterexamples, this stores the actual execution trace leading to
 * the error. For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class CounterexampleTraceInfo {
    private final boolean spurious;
    private final Map<AbstractElement, Set<Predicate>> pmap;
    private ConcreteTrace ctrace;

    public CounterexampleTraceInfo(boolean spurious) {
        this.spurious = spurious;
        pmap = new HashMap<AbstractElement, Set<Predicate>>();
        ctrace = null;
    }
    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious() { return spurious; }

    /**
     * returns the list of Predicates that were discovered during
     * counterexample analysis for the given AbstractElement. The invariant is
     * that the union of all the predicates for all the AbstractElements in
     * the spurious counterexample is sufficient for refining the abstract
     * model such that this trace is no longer feasible in it
     *
     * @return a list of predicates
     */
    public Collection<Predicate> getPredicatesForRefinement(AbstractElement e) {
        if (pmap.containsKey(e)) {
            return pmap.get(e);
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Adds some predicates to the list of those corresponding to the given
     * AbstractElement
     */
    public void addPredicatesForRefinement(AbstractElement e,
                                           Set<Predicate> preds) {
      if (!preds.isEmpty()) {
        if (pmap.containsKey(e)) {
            Set<Predicate> old = pmap.get(e);
            old.addAll(preds);
            pmap.put(e, old);
        } else {
            pmap.put(e, preds);
        }
      }
    }

    /**
     * for real counterexamples, returns the concrete execution trace leading
     * to the error
     * @return a ConcreteTrace from the entry point of the program to an error
     *         location
     */
    public ConcreteTrace getConcreteTrace() { return ctrace; }

    public void setConcreteTrace(ConcreteTrace ctrace) {
        this.ctrace = ctrace;
    }
    
    @Override
    public String toString() {
      return "Spurious: " + isSpurious() +
        (isSpurious() ? ", new predicates: " + pmap : ", trace: " + getConcreteTrace());
    }
}
