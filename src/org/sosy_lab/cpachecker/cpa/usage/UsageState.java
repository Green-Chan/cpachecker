// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Represents one abstract state of the Usage CPA. */
public class UsageState extends AbstractSingleWrapperState
    implements LatticeAbstractState<UsageState>, AbstractStateWithEdge, AliasInfoProvider {
  /* Boilerplate code to avoid serializing this class */

  private static final long serialVersionUID = -898577877284268426L;
  private final transient StateStatistics stats;

  private transient ImmutableMap<AbstractIdentifier, AbstractIdentifier>
      variableBindingRelation;

  private UsageState(
      final AbstractState pWrappedElement,
      final ImmutableMap<AbstractIdentifier, AbstractIdentifier> pVarBind,
      final StateStatistics pStats) {
    super(pWrappedElement);
    variableBindingRelation = pVarBind;
    stats = pStats;
    pStats.statCounter.setNextValue(variableBindingRelation.size());
  }

  public static UsageState createInitialState(final AbstractState pWrappedElement) {
    return new UsageState(
        pWrappedElement,
        ImmutableMap.of(),
        new StateStatistics());
  }

  private UsageState(final AbstractState pWrappedElement, final UsageState state) {
    this(
        pWrappedElement,
        state.variableBindingRelation,
        state.stats);
  }

  public UsageState removeInternalLinks(final String functionName) {
    boolean noRemove = true;
    ImmutableMap.Builder<AbstractIdentifier, AbstractIdentifier> builder = ImmutableMap.builder();
    for (Entry<AbstractIdentifier, AbstractIdentifier> entry : variableBindingRelation.entrySet()) {
      AbstractIdentifier key = entry.getKey();
      if (key instanceof LocalVariableIdentifier
          && ((LocalVariableIdentifier) key).getFunction().equals(functionName)) {
        noRemove = false;
      } else {
        builder.put(entry);
      }
    }
    if (noRemove) {
      return this;
    }
    return new UsageState(this.getWrappedState(), builder.build(), stats);
  }

  public UsageState put(Collection<Pair<AbstractIdentifier, AbstractIdentifier>> newLinks) {
    boolean sameMap = true;
    ImmutableMap<AbstractIdentifier, AbstractIdentifier> newMap = variableBindingRelation;

    for (Pair<AbstractIdentifier, AbstractIdentifier> pair : newLinks) {
      AbstractIdentifier id1 = pair.getFirst();
      AbstractIdentifier id2 = getLinksIfNecessary(pair.getSecond());
      if (!id1.equals(id2)) {
        AbstractIdentifier newId1 = id1.cloneWithDereference(0);
        AbstractIdentifier newId2 =
            id2.cloneWithDereference(id2.getDereference() - id1.getDereference());
        ImmutableMap.Builder<AbstractIdentifier, AbstractIdentifier> builder =
            ImmutableMap.builder();
        boolean new_entry = true;

        // If there was already an entry with same first AbstractIdentifier in
        // variableBindingRelation,
        // change it.
        for (Entry<AbstractIdentifier, AbstractIdentifier> entry : newMap.entrySet()) {
          AbstractIdentifier key = entry.getKey();
          if (key.equals(newId1)) {
            // Can not remove from builder, so have to go through a map manually
            builder.put(newId1, newId2);
            new_entry = false;
            if (!newId2.equals(entry.getValue())) {
              sameMap = false;
            }
          } else {
            builder.put(entry);
          }
        }
        // If this is an entry with new first AbstractIdentifier, add it.
        if (new_entry) {
          builder.put(newId1, newId2);
          sameMap = false;
        }
        newMap = builder.build();
      }
    }

    if (sameMap) {
      return this;
    } else {
      return new UsageState(this.getWrappedState(), newMap, stats);
    }
  }

  private AbstractIdentifier getLinksIfNecessary(final AbstractIdentifier id) {
    /* Special get!
     * If we get **b, having (*b, c), we give *c
     */
    AbstractIdentifier newId = id.cloneWithDereference(0);
    if (variableBindingRelation.containsKey(newId)) {
      AbstractIdentifier initialId = variableBindingRelation.get(newId);
      AbstractIdentifier pointsTo =
          initialId.cloneWithDereference(initialId.getDereference() + id.getDereference());
      if (newId.compareTo(initialId.cloneWithDereference(0)) != 0) {
        return getLinksIfNecessary(pointsTo);
      } else {
        return pointsTo;
      }
    }

    return id;
  }

  public UsageState copy(final AbstractState pWrappedState) {
    return new UsageState(pWrappedState, this);
  }

  public UsageState reduced(final AbstractState pWrappedState, final String func) {
    UsageState result = new UsageState(pWrappedState, this);

    ImmutableMap.Builder<AbstractIdentifier, AbstractIdentifier> builder = ImmutableMap.builder();
    for (Entry<AbstractIdentifier, AbstractIdentifier> entry : variableBindingRelation.entrySet()) {
      AbstractIdentifier key = entry.getKey();
      if (key.isGlobal()) {
        builder.put(entry);
      } else if (key instanceof LocalVariableIdentifier
          && ((LocalVariableIdentifier) key).getFunction().equals(func)) {
        builder.put(entry);
      }
    }
    result.variableBindingRelation = builder.build();
    return result;
  }

   public UsageState expanded(final AbstractState pWrappedState, final UsageState state, final String func) {

     ImmutableMap.Builder<AbstractIdentifier, AbstractIdentifier> builder = ImmutableMap.builder();
     builder.putAll(state.variableBindingRelation);
     for (Entry<AbstractIdentifier, AbstractIdentifier> entry : variableBindingRelation.entrySet()) {
       AbstractIdentifier key = entry.getKey();
       if (key instanceof LocalVariableIdentifier
           && !((LocalVariableIdentifier) key).getFunction().equals(func)) {
         builder.put(entry);
       }
     }
     UsageState result = new UsageState(pWrappedState, builder.build(), state.stats);

     return result;
   }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(variableBindingRelation);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof UsageState)) {
      return false;
    }
    UsageState other = (UsageState) obj;
    boolean b =
        Objects.equals(variableBindingRelation, other.variableBindingRelation)
        && getWrappedState().equals(other.getWrappedState());

    return b;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append("[");
    Joiner.on(", ").withKeyValueSeparator("->").appendTo(str, variableBindingRelation);
    str.append("]\n");
    str.append(getWrappedState());
    return str.toString();
  }

  @Override
  public boolean isLessOrEqual(final UsageState other) {
    // If we are here, the wrapped domain return true and the stop depends only on this value
    stats.lessTimer.start();
    // this element is not less or equal than the other element, if that one contains less elements
    if (this.variableBindingRelation.size() > other.variableBindingRelation.size()) {
      stats.lessTimer.stop();
      return false;
    }

    // also, this element is not less or equal than the other element,
    // if any one constant's value of the other element differs from the constant's value in this
    // element
    if (from(variableBindingRelation.keySet())
        .anyMatch(Predicates.not(other.variableBindingRelation::containsKey))) {
      stats.lessTimer.stop();
      return false;
    }

    stats.lessTimer.stop();
    return true;
  }

  public StateStatistics getStatistics() {
    return stats;
  }

  public static class StateStatistics {
    private StatTimer joinTimer = new StatTimer("Time for joining");
    private StatTimer lessTimer = new StatTimer("Time for cover check");
    private StatInt statCounter =
        new StatInt(StatKind.SUM, "Sum of variableBindingRelation's sizes");

    public StateStatistics() {}

    public void printStatistics(StatisticsWriter out) {
      out.spacer().put(joinTimer).put(lessTimer).put(statCounter);
    }
  }

  public static UsageState get(AbstractState state) {
    return AbstractStates.extractStateByType(state, UsageState.class);
  }


  @Override
  public Set<AbstractIdentifier> getAllPossibleAliases(AbstractIdentifier id) {
    AbstractIdentifier newId = getLinksIfNecessary(id);
    if (newId != id) {
      return ImmutableSet.of(newId);
    } else {
      return ImmutableSet.of();
    }
  }

  @Override
  public void filterAliases(AbstractIdentifier pIdentifier, Collection<AbstractIdentifier> pSet) {
    AbstractIdentifier newId = getLinksIfNecessary(pIdentifier);
    if (newId != pIdentifier) {
      pSet.remove(pIdentifier);
    }
  }

  @Override
  public UsageState join(UsageState pOther) {
    stats.joinTimer.start();

    ImmutableMap.Builder<AbstractIdentifier, AbstractIdentifier> newRelation =
        ImmutableMap.builder();
    newRelation.putAll(variableBindingRelation);

    for (Entry<AbstractIdentifier, AbstractIdentifier> entry : pOther.variableBindingRelation
        .entrySet()) {

      if (variableBindingRelation.containsKey(entry.getKey())) {
        if (!variableBindingRelation.get(entry.getKey()).equals(entry.getValue())) {
          throw new Error("Cannot handle it yet");
        }
      } else {
        newRelation.put(entry.getKey(), entry.getValue());
      }
    }
    stats.joinTimer.stop();
    return new UsageState(this.getWrappedState(), newRelation.build(), stats);
  }

  protected Object readResolve() {
    return new UsageState(
        getWrappedState(),
        ImmutableMap.of(),
        new StateStatistics());
  }

  @Override
  public AbstractEdge getAbstractEdge() {
    return ((AbstractStateWithEdge) getWrappedState()).getAbstractEdge();
  }

  @Override
  public boolean hasEmptyEffect() {
    return ((AbstractStateWithEdge) getWrappedState()).hasEmptyEffect();
  }

  @Override
  public boolean isProjection() {
    AbstractState wrapped = getWrappedState();
    if (wrapped instanceof AbstractStateWithEdge) {
      return ((AbstractStateWithEdge) wrapped).isProjection();
    } else {
      return false;
    }
  }
}
