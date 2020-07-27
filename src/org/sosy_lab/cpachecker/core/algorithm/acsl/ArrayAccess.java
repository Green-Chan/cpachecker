package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ArrayAccess implements ACSLTerm {

  private final ACSLTerm array;
  private final ACSLTerm index;

  public ArrayAccess(ACSLTerm pArray, ACSLTerm pIndex) {
    array = pArray;
    index = pIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ArrayAccess) {
      ArrayAccess other = (ArrayAccess) o;
      return array.equals(other.array) && index.equals(other.index);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 3 * array.hashCode() + index.hashCode();
  }

  @Override
  public String toString() {
    return array.toString() + "[" + index.toString() + "]";
  }

  public ACSLTerm getArray() {
    return array;
  }

  public ACSLTerm getIndex() {
    return index;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    return new ArrayAccess(array.useOldValues(), index.useOldValues());
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return array.isAllowedIn(clauseType) && index.isAllowedIn(clauseType);
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    return builder.addAll(array.getUsedBuiltins()).addAll(index.getUsedBuiltins()).build();
  }
}
