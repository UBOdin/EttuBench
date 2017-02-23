package booleanFormulaEntities;

import net.sf.jsqlparser.expression.Expression;

/**
 * if BooleanNormalClause is under certain context, e.g. A.a=A.a is not
 * a tautology if A.a are from different query bodies
 * @author tingxie
 *
 */
public interface ExpressionContextAware{
   public boolean ifAcrossContext(Expression left,Expression right);
}
