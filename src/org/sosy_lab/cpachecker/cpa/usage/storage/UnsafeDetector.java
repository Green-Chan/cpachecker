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
package org.sosy_lab.cpachecker.cpa.usage.storage;

import com.google.common.base.Preconditions;
import java.util.SortedSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.lock.DeadLockState.DeadLockTreeNode;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix="cpa.usage.unsafedetector")
public class UnsafeDetector {

  public static enum UnsafeMode {
    RACE,
    DEADLOCKCIRCULAR,
    DEADLOCKDISPATCH
  }

  @Option(description = "ignore unsafes only with empty callstacks", secure = true)
  private boolean ignoreEmptyLockset = true;

  @Option(description="defines what is unsafe",
      secure = true)
  private UnsafeMode unsafeMode = UnsafeMode.RACE;

  @Option(name = "intLock", description="A name of interrupt lock for checking deadlock free",
      secure = true)
  private String intLockName = null;

  public UnsafeDetector(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public boolean isUnsafe(AbstractUsagePointSet set) {
    if (set instanceof RefinedUsagePointSet) {
      return true;
    }
    return isUnsafe((UnrefinedUsagePointSet)set);
  }

  private boolean isUnsafe(UnrefinedUsagePointSet set) {
    return isUnsafe(set.getTopUsages());
  }

  public Pair<UsageInfo, UsageInfo> getUnsafePair(AbstractUsagePointSet set) {
    assert isUnsafe(set);

    if (set instanceof RefinedUsagePointSet) {
      return ((RefinedUsagePointSet)set).getUnsafePair();
    } else {
      UnrefinedUsagePointSet unrefinedSet = (UnrefinedUsagePointSet) set;
      Pair<UsagePoint, UsagePoint> result = getUnsafePair(unrefinedSet.getTopUsages());

      assert result != null;

      return Pair.of(unrefinedSet.getUsageInfo(result.getFirst()).getOneExample(),
          unrefinedSet.getUsageInfo(result.getSecond()).getOneExample());
    }
  }

  private boolean isUnsafe(SortedSet<UsagePoint> points) {
    for (UsagePoint point1 : points) {
      for (UsagePoint point2 : points) {
        if (isUnsafePair(point1, point2)) {
          return true;
        }
      }
    }
    return false;
  }

  private Pair<UsagePoint, UsagePoint> getUnsafePair(SortedSet<UsagePoint> set) {

    Pair<UsagePoint, UsagePoint> unsafePair = null;

    for (UsagePoint point1 : set) {
      // Operation is not commutative, not to optimize
      for (UsagePoint point2 : set) {
        if (isUnsafePair(point1, point2)) {
          Pair<UsagePoint, UsagePoint> newUnsafePair = Pair.of(point1, point2);
          if (unsafePair == null || compare(newUnsafePair, unsafePair) < 0) {
            unsafePair = newUnsafePair;
          }
        }
      }
    }
    // If we can not find an unsafe here, fail
    return unsafePair;
  }

  private int compare(Pair<UsagePoint, UsagePoint> pair1, Pair<UsagePoint, UsagePoint> pair2) {
    int result = 0;
    UsagePoint point1 = pair1.getFirst();
    UsagePoint point2 = pair1.getSecond();
    UsagePoint oPoint1 = pair2.getFirst();
    UsagePoint oPoint2 = pair2.getSecond();

    boolean isEmpty = point1.isEmpty() && point2.isEmpty();
    boolean otherIsEmpty = oPoint1.isEmpty() && oPoint2.isEmpty();
    if (isEmpty && !otherIsEmpty) {
      return 1;
    }
    if (!isEmpty && otherIsEmpty) {
      return -1;
    }
    result += point1.compareTo(oPoint1);
    result += point2.compareTo(oPoint2);
    return result;
  }

  public boolean isUnsafePair(UsagePoint point1, UsagePoint point2) {
    if (point1.isCompatible(point2)) {
      switch (unsafeMode) {
        case RACE:
          return isRace(point1, point2);

        case DEADLOCKDISPATCH:
          return isDeadlockDispatch(point1, point2);

        case DEADLOCKCIRCULAR:
          return isDeadlockCircular(point1, point2);

        default:
          throw new UnsupportedOperationException("Unknown mode: " + unsafeMode);
      }
    }
    return false;
  }


  private boolean isRace(UsagePoint point1, UsagePoint point2) {
    if (point1.getAccess() == Access.WRITE || point2.getAccess() == Access.WRITE) {
      if (ignoreEmptyLockset && point1.isEmpty() && point2.isEmpty()) {
        return false;
      }
      return true;
    }
    return false;
  }

  private boolean isDeadlockDispatch(UsagePoint point1, UsagePoint point2) {
    Preconditions.checkNotNull(intLockName);
    LockIdentifier intLock = LockIdentifier.of(intLockName);
    DeadLockTreeNode node1 = point1.get(DeadLockTreeNode.class);
    DeadLockTreeNode node2 = point2.get(DeadLockTreeNode.class);

    if (node2.contains(intLock) && !node1.contains(intLock)) {
      for (LockIdentifier lock1 : node1) {
        int index1 = node2.indexOf(lock1);
        int index2 = node2.indexOf(intLock);

        if (index1 > index2) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isDeadlockCircular(UsagePoint point1, UsagePoint point2) {
    //Deadlocks
    DeadLockTreeNode node1 = point1.get(DeadLockTreeNode.class);
    DeadLockTreeNode node2 = point2.get(DeadLockTreeNode.class);

    for (LockIdentifier lock1 : node1) {
      for (LockIdentifier lock2 : node2) {
        int index1 = node1.indexOf(lock1);
        int index2 = node1.indexOf(lock2);
        int otherIndex1 = node2.indexOf(lock1);
        int otherIndex2 = node2.indexOf(lock2);
        if (otherIndex1 >= 0 && index2 >= 0 &&
            ((index1 > index2 && otherIndex1 < otherIndex2) ||
             (index1 < index2 && otherIndex1 > otherIndex2))) {
          return true;
        }
      }
    }
    return false;
  }
 }