package fql.frontend.ast.predicate;

import java.util.Iterator;
import java.util.LinkedList;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.FQLNode;

public class Predicates implements FQLNode, Iterable<Predicate> {
  private LinkedList<Predicate> mPredicates = new LinkedList<Predicate>(); 
  
  public void add(Predicate pPredicate) {
    assert(pPredicate != null);
    
    mPredicates.add(pPredicate);
  }
  
  public void add(Predicates pPredicates) {
    assert(pPredicates != null);
    
    mPredicates.addAll(pPredicates.mPredicates);
  }
  
  public boolean isEmpty() {
    return mPredicates.isEmpty();
  }
  
  @Override
  public String toString() {
    return mPredicates.toString();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      Predicates lOther = (Predicates)pOther;
      
      return mPredicates.equals(lOther.mPredicates);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 24383 + mPredicates.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

  @Override
  public Iterator<Predicate> iterator() {
    return mPredicates.iterator();
  }

}
