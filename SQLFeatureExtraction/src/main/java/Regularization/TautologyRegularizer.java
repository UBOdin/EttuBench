package Regularization;



import java.util.ArrayList;
import java.util.List;

import booleanFormulaEntities.ToolBox;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;

/**
 * It detects and removes all tautologies and contradictions
 * and shrink boolean expressions accordingly
 * @author tingxie
 *
 */
public class TautologyRegularizer{

	/**
	 * it checks if the expression is a tautology/contradiction/neither with value 1/-1/0
	 * @param exp
	 * @return
	 */
	private static int tautologyCheck(Expression exp){
		if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			Expression left=bexp.getLeftExpression();
			Expression right=bexp.getRightExpression();

			if (bexp instanceof EqualsTo){
				if (left.getClass().equals(right.getClass())){
					//more complex case for function,currently we just compare if two functions are exactly the same
					//if not, then we are not sure
					if (left instanceof Function){
						if (left.toString().equals(right.toString())&&!QueryToolBox.awarer.ifAcrossContext(left, right))
							return 1;
						else
							return 0;
					}
					else if (left instanceof LongValue){
						if (((LongValue) left).getValue()==((LongValue) right).getValue())
							return 1;
						else
							return -1;
					}
					else if (left instanceof TimeValue){
						if (((TimeValue) left).getValue().equals(((TimeValue) right).getValue()))
							return 1;
						else
							return -1;
					}
					else if (left instanceof TimestampValue){
						if (((TimestampValue) left).getValue().equals(((TimestampValue) right).getValue()))
							return 1;
						else
							return -1;
					}
					else if (left instanceof DateValue){
						if (((DateValue) left).getValue().equals(((DateValue) right).getValue()))
							return 1;
						else
							return -1;
					}
					else if (left instanceof StringValue){
						if (((StringValue) left).getValue().equals(((StringValue) right).getValue()))
							return 1;
						else
							return -1;
					}
					else if (left instanceof NullValue){
						return 0;
					}
					else{
						//if they are exactly the same and not across select
						if (left.toString().equals(right.toString())&&!QueryToolBox.awarer.ifAcrossContext(left, right))
							return 1;
						else return 0;					
					}
				}
				else return 0;

			}
			else if (bexp instanceof NotEqualsTo){
				if (left.getClass().equals(right.getClass())){
					//more complex case for function,currently we just compare if two functions are exactly the same
					//if not, then we are not sure
					if (left instanceof Function){
						if (left.toString().equals(right.toString())&&!QueryToolBox.awarer.ifAcrossContext(left, right))
							return -1;
						else
							return 0;
					}
					else if (left instanceof LongValue){
						if (((LongValue) left).getValue()==((LongValue) right).getValue())
							return -1;
						else
							return 1;
					}
					else if (left instanceof TimeValue){
						if (((TimeValue) left).getValue().equals(((TimeValue) right).getValue()))
							return -1;
						else
							return 1;
					}
					else if (left instanceof TimestampValue){
						if (((TimestampValue) left).getValue().equals(((TimestampValue) right).getValue()))
							return -1;
						else
							return 1;
					}
					else if (left instanceof DateValue){
						if (((DateValue) left).getValue().equals(((DateValue) right).getValue()))
							return -1;
						else
							return 1;
					}
					else if (left instanceof StringValue){
						if (((StringValue) left).getValue().equals(((StringValue) right).getValue()))
							return -1;
						else
							return 1;
					}
					else if (left instanceof NullValue){
						return 0;
					}
					else{
						//if exactly the same and not across select
						if (left.toString().equals(right.toString())&&!QueryToolBox.awarer.ifAcrossContext(left, right))
							return -1;
						else return 0;					
					}
				}
				else return 0;
			}
			else if (bexp instanceof MinorThan||bexp instanceof GreaterThan||bexp instanceof MinorThanEquals||bexp instanceof GreaterThanEquals){
				if (left.getClass().equals(right.getClass())){
					double value;
					double value2;
					if (left instanceof LongValue){
						value=(double)((LongValue) left).getValue();
						value2=(double)((LongValue) right).getValue();
					}
					else if (left instanceof DoubleValue){
						value=((DoubleValue) left).getValue();
						value2=((DoubleValue) right).getValue();
					}
					else if (left instanceof DateValue){
						value=(double)((DateValue) left).getValue().getTime();
						value2=(double)((DateValue) right).getValue().getTime();
					}
					else if (left instanceof TimeValue){
						value=(double)((TimeValue) left).getValue().getTime();
						value2=(double)((TimeValue) right).getValue().getTime();
					}					
					else if (left instanceof TimestampValue){
						value=(double)((TimestampValue) left).getValue().getTime();
						value2=(double)((TimestampValue) right).getValue().getTime();
					}
					//for string value, namely the value is encrypted, we just assume it to be unknown
					else if (left instanceof StringValue){
						return 0;
					}
					else if (left instanceof Column){
						//if exactly the same and not across select
						if (left.toString().equals(right.toString())&&!QueryToolBox.awarer.ifAcrossContext(left, right)){
							if (bexp instanceof MinorThanEquals||bexp instanceof GreaterThanEquals)
								return 1;
							else
								return -1;
						}							
						else return 0;
					}
					else if (left instanceof Function){
						//if exactly the same and not across select
						if (left.toString().equals(right.toString())&&!QueryToolBox.awarer.ifAcrossContext(left, right)){
							if (bexp instanceof MinorThanEquals||bexp instanceof GreaterThanEquals)
								return 1;
							else
								return -1;
						}							
						else return 0;
					}
					else return 0;

					if (bexp instanceof MinorThan){
						if (value<value2)
							return 1;
						else  return -1;						
					}
					else if (bexp instanceof MinorThanEquals){
						if (value<=value2)
							return 1;
						else  return -1;
					}
					else if (bexp instanceof GreaterThan){
						if (value>value2)
							return 1;
						else  return -1;	
					}
					else {
						if (value>=value2)
							return 1;
						else  return -1;	
					}
				}
				else return 0;

			}
			//for other cases, just return 0
			else return 0;
		}	
		else if (exp instanceof IsNullExpression){
			IsNullExpression isnull=(IsNullExpression) exp;
			Expression left=isnull.getLeftExpression();
			int ifnull=QueryToolBox.checkIfEvaluatedToNull(left);
			if (isnull.isNot()){
				return -ifnull;
			}
			else return ifnull;			
		}
		else if(exp instanceof InverseExpression){
			InverseExpression inv=(InverseExpression) exp;
			Expression e=inv.getExpression();
			return -tautologyCheck(e);
		}
		else if(exp instanceof Parenthesis){
			Parenthesis p=(Parenthesis) exp;
			Expression e=p.getExpression();
			int result=tautologyCheck(e);
			if (p.isNot()){
				return -result;
			}
			else
				return result;			
		}
		else if (exp instanceof Between){
			Between between=(Between) exp;
			Expression left=between.getLeftExpression();
			Expression innerleft=between.getBetweenExpressionStart();
			Expression innerright=between.getBetweenExpressionEnd();
			MinorThanEquals eq1=new MinorThanEquals(innerleft,left);
			MinorThanEquals eq2=new MinorThanEquals(left,innerright);
			int checkvalue1=tautologyCheck(eq1);
			int checkvalue2=tautologyCheck(eq2);
			int value;
			if(between.isNot())
				value=-1;
			else
				value=1;

			if(checkvalue1==-1||checkvalue2==-1)
				return -1*value;
			else if (checkvalue1==1||checkvalue2==1)
				return 1*value;
			else return 0;
		}
		else if (exp instanceof LikeExpression){
			//TODO fill in the mechanism for checking Like Expression
			return 0;			
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			ItemsList ilist=inexp.getItemsList();
			Expression left=inexp.getLeftExpression();

			if (ilist instanceof ExpressionList&&!(left instanceof SubSelect)){
				ExpressionList elist=(ExpressionList) ilist;
				List<Expression> list=elist.getExpressions();
				int value;
				if(inexp.isNot())
					value=-1;
				else
					value=1;

				for (Expression e: list){
					EqualsTo eq=new EqualsTo(left,e);
					int checkvalue=tautologyCheck(eq);
					if(checkvalue==1)
						return 1*value;
					else if (checkvalue==0)
						return 0;
				}

				return -1*value;
			}
			else return 0;
		}
		//other cases not sure, just return 0
		else return 0;
	}

	private static void detectAndRemoveTautologyInPlainSelect(PlainSelect input){
		//search through Having clause
		Expression having=input.getHaving();
		if (having!=null){
			Expression myhaving=detectAndRemoveTautologyInExpression(having);
			if (myhaving==ToolBox.contradiction)
				System.out.println("warning Having clause is contradiction! "+input);
			else if (myhaving==ToolBox.tautology)
				myhaving=null;
			input.setHaving(myhaving);
		}
		
		//search from items for sub-queries
		FromItem from=input.getFromItem();
		SubSelect sub;
		if(from instanceof SubSelect){
			sub=(SubSelect) from;
			detectAndRemoveTautologySelectBody(sub.getSelectBody());
		}
		//search through expressions in Join conditions
		List<Join> jlist=input.getJoins();
		if(jlist!=null){
			for(Join j: jlist){
				from=j.getRightItem();
				if(from instanceof SubSelect){
					sub=(SubSelect) from;
					detectAndRemoveTautologySelectBody(sub.getSelectBody());
				}
				//disregard using here
				Expression onExp=j.getOnExpression();
				
				if(onExp!=null){
					Expression myonExp=detectAndRemoveTautologyInExpression(onExp);	
					if (myonExp==ToolBox.contradiction)
						System.out.println("warning ON condition is contradiction!"+ input);
					else if (myonExp==ToolBox.tautology)
						myonExp=null;
					j.setOnExpression(myonExp);
				}


			}
		}
		//search through where clause
		Expression where=input.getWhere();
		if (where!=null){
			Expression mywhere=detectAndRemoveTautologyInExpression(where);
			if (mywhere==ToolBox.contradiction)
				System.out.println("warning Where clause is contradiction!"+input);
			else if (mywhere==ToolBox.tautology)
				mywhere=null;
			input.setWhere(mywhere);
		}
	}

	private static void detectAndRemoveTautologySelectBody(SelectBody body){
		if(body instanceof PlainSelect){
			detectAndRemoveTautologyInPlainSelect((PlainSelect) body);	
		}
		else{
			Union u=(Union) body;
			List<PlainSelect> plist=u.getPlainSelects();
			for (PlainSelect ps:plist)
				detectAndRemoveTautologyInPlainSelect(ps);	
		}
	}
	/**
	 * it does not change its input Expression and returns just a copy
	 * if sub-query is met, it will regularize tautology in the sub-query as well
	 * @param exp
	 * @return
	 */
	private static Expression detectAndRemoveTautologyInExpression(Expression exp){

		if (exp instanceof OrExpression){
			OrExpression or=(OrExpression) exp;
			Expression left=or.getLeftExpression();
			Expression right=or.getRightExpression();
			Expression myleft=detectAndRemoveTautologyInExpression(left);
			Expression myright=detectAndRemoveTautologyInExpression(right);
			int leftcheckvalue=0;
			int rightcheckvalue=0;
			if (myleft==ToolBox.tautology)
				leftcheckvalue=1;
			else if (myleft==ToolBox.contradiction)
				leftcheckvalue=-1;

			if (myright==ToolBox.tautology)
				rightcheckvalue=1;
			else if (myright==ToolBox.contradiction)
				rightcheckvalue=-1; 
			//begin tautology test
			if (leftcheckvalue==1||rightcheckvalue==1)
				return ToolBox.tautology;
			else if (leftcheckvalue==-1)
				return myright;
			else if (rightcheckvalue==-1)
				return myleft;
			else 
				return new OrExpression(myleft,myright);
		}
		else if (exp instanceof AndExpression){
			AndExpression and=(AndExpression) exp;
			Expression left=and.getLeftExpression();
			Expression right=and.getRightExpression();
			Expression myleft=detectAndRemoveTautologyInExpression(left);
			Expression myright=detectAndRemoveTautologyInExpression(right);
			int leftcheckvalue=0;
			int rightcheckvalue=0;
			if (myleft==ToolBox.tautology)
				leftcheckvalue=1;
			else if (myleft==ToolBox.contradiction)
				leftcheckvalue=-1;

			if (myright==ToolBox.tautology)
				rightcheckvalue=1;
			else if (myright==ToolBox.contradiction)
				rightcheckvalue=-1; 
			//begin tautology test
			if (leftcheckvalue==-1||rightcheckvalue==-1)
				return ToolBox.contradiction;
			else if (leftcheckvalue==1)
				return myright;
			else if (rightcheckvalue==1)
				return myleft;
			else 
				return new AndExpression(myleft,myright);   
		}
		else if (exp instanceof SubSelect){
			detectAndRemoveTautologySelectBody(((SubSelect) exp).getSelectBody());
			return exp;
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			ItemsList ilist=inexp.getItemsList();
			Expression left=inexp.getLeftExpression();
			if(ilist instanceof SubSelect||left instanceof SubSelect){
				if(ilist instanceof SubSelect){
					SubSelect sub=(SubSelect) ilist;
					SelectBody body=sub.getSelectBody();
					detectAndRemoveTautologySelectBody(body);
				}
				if(left instanceof SubSelect){
					SubSelect sub=(SubSelect) left;
					SelectBody body=sub.getSelectBody();
					detectAndRemoveTautologySelectBody(body);
					inexp.setLeftExpression(sub);
				}        		   
				return inexp;
			}
			else {
				int checkvalue=tautologyCheck(inexp);
				if (checkvalue==1){
					return ToolBox.tautology;
				}
				else if (checkvalue==-1){
					return ToolBox.contradiction;
				}
				else 
					return inexp; 
			}

		}
		else if (exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			SelectBody body=sub.getSelectBody();
			detectAndRemoveTautologySelectBody(body);
			return all;
		}
		else if (exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			SelectBody body=sub.getSelectBody();
			detectAndRemoveTautologySelectBody(body); 
			return any;
		}
		else if (exp instanceof CaseExpression){
			CaseExpression caseexp=(CaseExpression) exp;
			List<WhenClause> wclist=caseexp.getWhenClauses();
			Expression sw=caseexp.getSwitchExpression();

			if(sw==null){
				List<WhenClause> mywclist=new ArrayList<WhenClause>();
				for(WhenClause wc:wclist){
					Expression when=wc.getWhenExpression();
					Expression mywhen=detectAndRemoveTautologyInExpression(when);
					if(mywhen!=ToolBox.contradiction){
						wc.setWhenExpression(mywhen);
						mywclist.add(wc);
					}
					if (mywhen==ToolBox.tautology)
						break;      		          		  
				}
				caseexp.setWhenClauses(mywclist);				
				return caseexp;
			}
			else {				
				List<WhenClause> mywclist=new ArrayList<WhenClause>();
				for(WhenClause wc:wclist){
					Expression when=wc.getWhenExpression();
					int checkvalue=tautologyCheck(new EqualsTo(sw,when));
					if(checkvalue!=-1){
						wc.setWhenExpression(when);
						mywclist.add(wc);
					}

					if (checkvalue==1)
						break;      		          		  
				}
				caseexp.setWhenClauses(mywclist);
				return caseexp;
			}

		}
		else if (exp instanceof Parenthesis){
			Parenthesis p=(Parenthesis) exp;
			Expression myinnerp=detectAndRemoveTautologyInExpression(p.getExpression());
			if(p.isNot()){
				if(myinnerp==ToolBox.tautology)
					return ToolBox.contradiction;
				else if (myinnerp==ToolBox.contradiction)
					return ToolBox.tautology;
			}
			p.setExpression(myinnerp);
			return p;
		}
		else if (exp instanceof InverseExpression){
			InverseExpression p=(InverseExpression) exp;
			Expression myinnerp=detectAndRemoveTautologyInExpression(p.getExpression());
			if(myinnerp==ToolBox.tautology)
				return ToolBox.contradiction;
			else if (myinnerp==ToolBox.contradiction)
				return ToolBox.tautology;
			p.setExpression(myinnerp);
			return p;
		}
		else {
			int checkvalue=tautologyCheck(exp);
			if (checkvalue==1){
				return ToolBox.tautology;
			}
			else if (checkvalue==-1){
				return ToolBox.contradiction;
			}
			else 
				return exp;
		}
	}

}
