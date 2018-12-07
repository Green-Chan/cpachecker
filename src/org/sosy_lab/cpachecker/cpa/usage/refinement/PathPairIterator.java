/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphIterator;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class PathPairIterator extends
    GenericIterator<Pair<UsageInfo, UsageInfo>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final Set<List<Integer>> refinedStates = new HashSet<>();
  //private final BAMCPA bamCpa;
  private BAMMultipleCEXSubgraphComputer subgraphComputer;
  private final Map<UsageInfo, BAMSubgraphIterator> targetToPathIterator;
  private final Set<UsageInfo> skippedUsages;

  //Statistics
  private StatTimer computingPath = new StatTimer("Time for path computing");
  private StatTimer additionTimerCheck = new StatTimer("Time for addition checks");
  private StatCounter numberOfPathCalculated = new StatCounter("Number of path calculated");
  private StatCounter numberOfPathFinished = new StatCounter("Number of new path calculated");
  private StatCounter numberOfRepeatedConstructedPaths = new StatCounter("Number of repeated path computed");
  //private int numberOfrepeatedPaths = 0;

  private Map<UsageInfo, List<ExtendedARGPath>> computedPathsForUsage = new IdentityHashMap<>();
  private Map<UsageInfo, Iterator<ExtendedARGPath>> currentIterators = new IdentityHashMap<>();
  // Not set, hash is changed
  private List<ExtendedARGPath> missedPaths = new ArrayList<>();

  private final Function<ARGState, Integer> idExtractor;

  //internal state
  private ExtendedARGPath firstPath = null;

  public PathPairIterator(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      BAMMultipleCEXSubgraphComputer pComputer,
      Function<ARGState, Integer> pExtractor) {
    super(pWrapper);
    subgraphComputer = pComputer;
    targetToPathIterator = new IdentityHashMap<>();
    skippedUsages = new HashSet<>();
    idExtractor = pExtractor;
  }

  @Override
  protected void init(Pair<UsageInfo, UsageInfo> pInput) {
    firstPath = null;
  }

  @Override
  protected Pair<ExtendedARGPath, ExtendedARGPath> getNext(Pair<UsageInfo, UsageInfo> pInput) {
    UsageInfo firstUsage, secondUsage;
    firstUsage = pInput.getFirst();
    secondUsage = pInput.getSecond();

    if (skippedUsages.contains(firstUsage) || skippedUsages.contains(secondUsage)) {
      // We know, that it have no valuable paths
      // Note, there are some paths, but they are declined by 'refinedStates'
      return null;
    }

    if (firstPath == null) {
      //First time or it was unreachable last time
      firstPath = getNextPath(firstUsage);
      if (firstPath == null) {
        return null;
      }
    }

    ExtendedARGPath secondPath = getNextPath(secondUsage);
    if (secondPath == null) {
      //Reset the iterator
      currentIterators.remove(secondUsage);
      //And move shift the first one
      firstPath = getNextPath(firstUsage);
      if (firstPath == null) {
        return null;
      }
      secondPath = getNextPath(secondUsage);
      if (secondPath == null) {
        return null;
      }
    }
    return Pair.of(firstPath, secondPath);
  }

  private boolean checkIsUsageUnreachable(UsageInfo pInput) {
    return !computedPathsForUsage.containsKey(pInput)
        || computedPathsForUsage.get(pInput).size() == 0;
  }

  @Override
  protected void finishIteration(Pair<ExtendedARGPath, ExtendedARGPath> pathPair, RefinementResult wrapperResult) {
    ExtendedARGPath firstExtendedPath, secondExtendedPath;

    firstExtendedPath = pathPair.getFirst();
    secondExtendedPath = pathPair.getSecond();

    Object predicateInfo = wrapperResult.getInfo(PredicateRefinerAdapter.class);
    if (predicateInfo != null && predicateInfo instanceof List) {
      @SuppressWarnings("unchecked")
      List<ARGState> affectedStates = (List<ARGState>)predicateInfo;
      //affectedStates may be null, if the path was refined somewhen before

      //A feature of GenericSinglePathRefiner: if one path is false, the second one is not refined
      if (firstExtendedPath.isUnreachable()) {
        //This one is false
        handleAffectedStates(affectedStates);
        //Need to clean first path
        firstPath = null;
      } else {
        //The second one must be
        Preconditions.checkArgument(secondExtendedPath.isUnreachable(), "Either the first path, or the second one must be unreachable here");
        handleAffectedStates(affectedStates);
      }
    } else {
      if (firstPath.isUnreachable()){
        firstPath = null;
      }
    }
    updateTheComputedSet(firstExtendedPath);
    updateTheComputedSet(secondExtendedPath);
  }

  @Override
  protected void finish(Pair<UsageInfo, UsageInfo> pInput, RefinementResult pResult) {
    UsageInfo firstUsage = pInput.getFirst();
    UsageInfo secondUsage = pInput.getSecond();
    List<UsageInfo> unreacheableUsages = new ArrayList<>(2);

    if (!missedPaths.isEmpty()) {
      for (ExtendedARGPath path : new ArrayList<>(missedPaths)) {
        updateTheComputedSet(path);
      }
    }

    if (checkIsUsageUnreachable(firstUsage)) {
      unreacheableUsages.add(firstUsage);
    }
    if (checkIsUsageUnreachable(secondUsage)) {
      unreacheableUsages.add(secondUsage);
    }
    pResult.addInfo(this.getClass(), unreacheableUsages);
  }

  @Override
  protected void printDetailedStatistics(StatisticsWriter pOut) {
    pOut.spacer()
      .put(computingPath)
      .put(additionTimerCheck)
      .put(numberOfPathCalculated)
      .put(numberOfPathFinished)
      .put(numberOfRepeatedConstructedPaths);
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> callerClass) {
    if (callerClass.equals(IdentifierIterator.class)) {
      //Refinement iteration finishes
      refinedStates.clear();
      targetToPathIterator.clear();
      firstPath = null;
    } else if (callerClass.equals(PointIterator.class)) {
      currentIterators.clear();
      computedPathsForUsage.clear();
      skippedUsages.clear();
      assert missedPaths.isEmpty();
    }
  }

  private void updateTheComputedSet(ExtendedARGPath path) {
    UsageInfo usage = path.getUsageInfo();

    boolean alreadyComputed = computedPathsForUsage.containsKey(usage);
    missedPaths.remove(path);

    if (!path.isUnreachable()) {
      List<ExtendedARGPath> alreadyComputedPaths;
      if (!alreadyComputed) {
        alreadyComputedPaths = new ArrayList<>();
        computedPathsForUsage.put(usage, alreadyComputedPaths);
      } else {
        alreadyComputedPaths = computedPathsForUsage.get(usage);
      }
      if (!alreadyComputedPaths.contains(path)) {
        alreadyComputedPaths.add(path);
      }
    } else if (path.isUnreachable() && alreadyComputed) {
      List<ExtendedARGPath> alreadyComputedPaths = computedPathsForUsage.get(usage);
      if (alreadyComputedPaths.contains(path)) {
        //We should reset iterator to avoid ConcurrentModificationException
        alreadyComputedPaths.remove(path);
      }
    }
  }

  private ExtendedARGPath getNextPath(UsageInfo info) {
    ARGPath currentPath;
    // Start from already computed set (it is partially refined)
    numberOfPathCalculated.inc();
    Iterator<ExtendedARGPath> iterator = currentIterators.get(info);
    if (iterator == null && computedPathsForUsage.containsKey(info)) {
      // first call
      // Clone the set to avoid concurrent modification
      iterator = Lists.newArrayList(computedPathsForUsage.get(info)).iterator();
      currentIterators.put(info, iterator);
    }

    if (iterator != null && iterator.hasNext()) {
      return iterator.next();
    }

    computingPath.start();
    //try to compute more paths
    BAMSubgraphIterator pathIterator;
    if (targetToPathIterator.containsKey(info)) {
      pathIterator = targetToPathIterator.get(info);
    } else {
      ARGState target = (ARGState)info.getKeyState();
      pathIterator = subgraphComputer.iterator(target);
      targetToPathIterator.put(info, pathIterator);
    }
    currentPath = pathIterator.nextPath(refinedStates);
    computingPath.stop();

    if (currentPath == null) {
      // no path to iterate, finishing
      if (!computedPathsForUsage.containsKey(info) || computedPathsForUsage.get(info).size() == 0) {
        skippedUsages.add(info);
      }
      return null;
    }
    numberOfPathFinished.inc();
    ExtendedARGPath result = new ExtendedARGPath(currentPath, info);
    missedPaths.add(result);
    //Not add result now, only after refinement
    return result;
  }

  private void handleAffectedStates(List<ARGState> affectedStates) {
    List<Integer> changedStateNumbers = from(affectedStates).transform(idExtractor).toList();
    refinedStates.add(changedStateNumbers);
  }
}