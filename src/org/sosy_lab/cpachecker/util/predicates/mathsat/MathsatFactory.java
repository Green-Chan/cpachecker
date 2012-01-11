/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.mathsat;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

public class MathsatFactory {

  @Options(prefix="cpa.predicate.mathsat")
  private static class MathsatOptions {

    @Option(description="encode program variables as INTEGERs in MathSAT, instead of "
        + "using REALs. Since interpolation is not really supported by the laz solver, "
        + "when computing interpolants we still use the LA solver, "
        + "but encoding variables as ints might still be a good idea: "
        + "we can tighten strict inequalities, and split negated equalities")
    private boolean useIntegers = false;

    @Option(description="Encode program variables of bitvectors of a fixed size,"
        + "instead of using REALS. No interpolation and thus no refinement is"
        + "supported in this case.")
    private boolean useBitwise = false;

    @Option(description="With of the bitvectors if useBitwise is true.")
    @IntegerOption(min=1, max=128)
    private int bitWidth = 32;

  }

  public static MathsatFormulaManager createFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {

    MathsatOptions options = new MathsatOptions();
    config.inject(options);

    if (options.useBitwise) {
      if (options.useIntegers) {
        throw new InvalidConfigurationException("Can use either integers or bitvecors, not both!");

      } else {
        return new BitwiseMathsatFormulaManager(config, logger, options.bitWidth);
      }

    } else {
      return new ArithmeticMathsatFormulaManager(config, logger, options.useIntegers);
    }
  }

}
