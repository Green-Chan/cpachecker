/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;


public interface BAMDataManager {

  /**
   * Associate the value previously associated with {@code oldState} with
   * {@code newState}.
   *
   * @param oldStateMustExist If set, assumes that {@code oldState} is in the
   *                          cache, otherwise, fails silently if it isn't.
   */
  void replaceStateInCaches(
      AbstractState oldState, AbstractState newState, boolean oldStateMustExist);

  /**
   * Create a new reached-set with the given state as root and register it in the cache.
   **/
  ReachedSet createAndRegisterNewReachedSet(
      AbstractState initialState, Precision initialPrecision, Block context);

  /**
   * Register an expanded state in our data-manager,
   * such that we know later, which state in which block was expanded to the state.
   * */
  void registerExpandedState(AbstractState expandedState, Precision expandedPrecision,
      AbstractState reducedState, Block innerBlock);

  /**
   * @param state Has to be a block-end state.
   * It can be expanded or reduced (or even reduced expanded),
   * because this depends on the nesting of blocks,
   * i.e. if there are several overlapping block-end-nodes
   * (e.g. nested loops or program calls 'exit()' inside a function).
   *
   * @return Whether the current state is at a node,
   * where several block-exits are available and one of them was already left.
   **/
  boolean alreadyReturnedFromSameBlock(AbstractState state, Block block);

  AbstractState getInnermostState(AbstractState state);

  /**
   * Get a list of states {@code [s1,s2,s3...]},
   * such that {@code expand(s1)=s2}, {@code expand(s2)=s3},...
   * The state {@code s1} is the most inner state.
   */
  List<AbstractState> getExpandedStatesList(AbstractState state);

  void registerInitialState(AbstractState state, ReachedSet reachedSet);

  ReachedSet getReachedSetForInitialState(AbstractState state);

  boolean hasInitialState(AbstractState state);

  AbstractState getReducedStateForExpandedState(AbstractState state);

  boolean hasExpandedState(AbstractState state);

  BAMCache getCache();

  void clearExpandedStateToExpandedPrecision();

  /** return a matching precision for the given state, or Null if state is not found. */
  @Nullable
  Precision getExpandedPrecisionForState(AbstractState pState);
}
