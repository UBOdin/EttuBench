package booleanFormulaEntities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;

/**
 * 
 * @author tingxie
 *
 */
public class ToolBox {
	public static final String[] placeholders=new String[]{"?",""," ","  ","''","' '","\"\"","\" \""};
	public static final HashSet<String> placeholderset=new HashSet<String>(Arrays.asList(placeholders));
	public static final EqualsTo tautology=new EqualsTo();
    static
    {
    	tautology.setLeftExpression(new LongValue("1"));
    	tautology.setRightExpression(new LongValue("1"));
    }
	public static final NotEqualsTo contradiction=new NotEqualsTo();
    static
    {
    	contradiction.setLeftExpression(new LongValue("1"));
    	contradiction.setRightExpression(new LongValue("1"));
    }
	public static boolean checkIfContainsPlaceHolder(Expression exp){
		if (exp!=null){
			if (exp instanceof BinaryExpression){
				BinaryExpression bexp=(BinaryExpression) exp;
				Expression left=bexp.getLeftExpression();
				Expression right=bexp.getRightExpression();
				if(checkIfContainsPlaceHolder(left))
					return true;
				if(checkIfContainsPlaceHolder(right))
					return true;
				return false;
			}
			else if (exp instanceof IsNullExpression){
				if(checkIfContainsPlaceHolder(((IsNullExpression) exp).getLeftExpression()))
					return true;
				else
					return false;
			}
			else if (exp instanceof StringValue){
				String s=((StringValue) exp).getValue();
				return placeholderset.contains(s);
			}
			else if (exp instanceof LikeExpression){
				LikeExpression like=(LikeExpression) exp;
				if(checkIfContainsPlaceHolder(like.getRightExpression()))
					return true;
				if(checkIfContainsPlaceHolder(like.getLeftExpression()))
					return true;
				else return false;
			}
			else if (exp instanceof Parenthesis){
				Parenthesis p=(Parenthesis) exp;
				return checkIfContainsPlaceHolder(p.getExpression());
			}
			else if (exp instanceof InverseExpression){
				InverseExpression p=(InverseExpression) exp;
				return checkIfContainsPlaceHolder(p.getExpression());
			}
			else if (exp instanceof Between){
				Between between=(Between) exp;
				if(checkIfContainsPlaceHolder(between.getBetweenExpressionStart()))
					return true;
				if(checkIfContainsPlaceHolder(between.getBetweenExpressionEnd()))
					return true;
				if(checkIfContainsPlaceHolder(between.getLeftExpression()))
					return true;
				return false;
			}
			else if (exp instanceof Function){
				Function f=(Function) exp;
				ExpressionList elist=f.getParameters();
				if(elist!=null){
					@SuppressWarnings("unchecked")
					List<Expression> explist=elist.getExpressions();
					for(Expression e:explist){
						if(checkIfContainsPlaceHolder(e))
							return true;
					}
					return false;
				}
				else return false;
			}
			else if (exp instanceof CaseExpression){
				CaseExpression caseexp=(CaseExpression) exp;
				Expression elseexp=caseexp.getElseExpression();
				if(checkIfContainsPlaceHolder(elseexp))
					return true;
				@SuppressWarnings("unchecked")
				List<WhenClause> wclist=caseexp.getWhenClauses();
				for(WhenClause wc:wclist){
					if(checkIfContainsPlaceHolder(wc.getWhenExpression()))
						return true;
					if(checkIfContainsPlaceHolder(wc.getThenExpression()))
						return true;
				}				
				return false;
				
			}
			else if (exp instanceof JdbcParameter){
			return true;	
			}
			else return false;
		}
		else return false;
	}
	
	/**
	 * recursively peel off parenthesis
	 * @param p
	 * @return
	 */
	public static Expression PeelParenthesis(Parenthesis p){		
		if (!p.isNot()){
			Expression e=p.getExpression();

			if(e instanceof Parenthesis){
				Parenthesis pp=(Parenthesis) e;
				e=PeelParenthesis(pp);			
			}

			return e;
		}
		else return p;
	}
}
