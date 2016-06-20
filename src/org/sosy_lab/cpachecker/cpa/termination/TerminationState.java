/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.termination;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithDummyLocation;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.concurrent.Immutable;

@Immutable
public class TerminationState extends AbstractSingleWrapperState
    implements AbstractStateWithDummyLocation, Graphable {

  private static final long serialVersionUID = 3L;

  private final boolean loop;

  private final boolean dummyLocation;

  private final Collection<CFAEdge> enteringEdges;

  /**
   * Creates a new {@link TerminationState} that is part of the
   * lasso's stem and has no dummy location.
   *
   * @param pWrappedState
   *          the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public static TerminationState createStemState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, false, false, Collections.emptyList());
  }

  private TerminationState(
      AbstractState pWrappedState,
      boolean pLoop,
      boolean pDummyLocation,
      Collection<CFAEdge> pEnteringEdges) {
    super(checkNotNull(pWrappedState));
    Preconditions.checkArgument(pDummyLocation || pEnteringEdges.isEmpty());
    loop = pLoop;
    dummyLocation = pDummyLocation;
    enteringEdges = checkNotNull(pEnteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} from this {@link TerminationState}
   * but with the given <code>pWrappedState</code>.
   *
   * @param pWrappedState
   *            the {@link AbstractState} to wrap
   * @return the created {@link TerminationState}
   */
  public TerminationState withWrappedState(AbstractState pWrappedState) {
    return new TerminationState(pWrappedState, loop, dummyLocation, enteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} that is the first state of the lasso's loop.
   *
   * @return the created {@link TerminationState}
   */
  public TerminationState enterLoop() {
    Preconditions.checkArgument(!loop, "% is already part of the lasso's loop", this);
    return new TerminationState(getWrappedState(), true, dummyLocation, enteringEdges);
  }

  /**
   * Creates a new {@link TerminationState} with a dummy location and  the given entering edges.
   *
   * @param pEnteringEdges
   *         the edges entering the location represented by the created state
   * @return the created {@link TerminationState}
   */
  public TerminationState withDummyLocation(Collection<CFAEdge> pEnteringEdges) {
    return new TerminationState(getWrappedState(), loop, true, pEnteringEdges);
  }

  @Override
  public boolean isDummyLocation() {
    return dummyLocation;
  }

  @Override
  public Collection<CFAEdge> getEnteringEdges() {
    return enteringEdges;
  }

  /**
   * @return <code>true</code> iff this {@link TerminationState} is part of the lasso's loop.
   */
  public boolean isPartOfLoop() {
    return loop;
  }

  /**
   * @return <code>true</code> iff this {@link TerminationState} is part of the lasso's stem.
   */
  public boolean isPartOfStem() {
    return !loop;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    if (loop) {
      sb.append("loop");
    } else {
      sb.append("stem");
    }

    if (getWrappedState() instanceof Graphable) {
      sb.append("\n");
      sb.append(((Graphable) getWrappedState()).toDOTLabel());
    }

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    if (getWrappedState() instanceof Graphable) {
      return ((Graphable) getWrappedState()).shouldBeHighlighted();
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(TerminationState.class.getSimpleName());
    if (loop) {
      sb.append("(loop)");
    } else {
      sb.append("(stem)");
    }

    sb.append(" ");
    sb.append(getWrappedState());

    return sb.toString();
  }
}
