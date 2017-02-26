package booleanFormulaEntities;

import java.util.List;

import Regularization.QueryToolBox;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;

/**
 * fix the order of expression when the operator does not care about the order
 * for >=,> always turn them into corresponding <,<=
 * make sure this expression representation is unique and hashcode can be computed
 * @author tingxie
 *
 */
public class FixedOrderExpression {
	private Expression e;
	private String content;
	private int hashCode;
	private int ifTautology=0;
	private boolean containsPlaceHolder=false;

	public FixedOrderExpression(Expression e){
		if (e==null)
			System.out.println("input of fixed order expression cannot be null");
		else {
			if (e==ToolBox.tautology)
				this.ifTautology=1;
			else if (e==ToolBox.contradiction)
				this.ifTautology=-1;
			else{
				if(e instanceof GreaterThan){
					GreaterThan gt=(GreaterThan) e;
					if(!gt.isNot()){
						MinorThan mt=new MinorThan();
						mt.setLeftExpression(gt.getRightExpression());
						mt.setRightExpression(gt.getLeftExpression());
					    e=mt;					
					}
					else{
						MinorThanEquals mte=new MinorThanEquals();
						mte.setLeftExpression(gt.getLeftExpression());
						mte.setRightExpression(gt.getRightExpression());
					    e=mte;
					}
					
					
				}
				else if (e instanceof GreaterThanEquals){
					GreaterThanEquals gte=(GreaterThanEquals) e;
				    if(!gte.isNot()){
						MinorThanEquals mte=new MinorThanEquals();
						mte.setLeftExpression(gte.getRightExpression());
						mte.setRightExpression(gte.getLeftExpression());
					    e=mte;
				    }
				    else{
						MinorThan mt=new MinorThan();
						mt.setLeftExpression(gte.getLeftExpression());
						mt.setRightExpression(gte.getRightExpression());
					    e=mt;	
				    }
				}
				else if (e instanceof EqualsTo){
					EqualsTo eq=(EqualsTo) e;
					if(eq.isNot()){
						NotEqualsTo neq=new NotEqualsTo();
					    neq.setLeftExpression(eq.getLeftExpression());
					    neq.setRightExpression(eq.getRightExpression());
					    e=neq;
					}					
				}
				else if (e instanceof NotEqualsTo){
					NotEqualsTo neq=(NotEqualsTo) e;
					if(neq.isNot()){
						EqualsTo eq=new EqualsTo();
					    eq.setLeftExpression(neq.getLeftExpression());
					    eq.setRightExpression(neq.getRightExpression());
					    e=eq;
					}					
				}
				this.ifTautology=0;
				fixedOrderExpression(e);
			}
			this.e=e;
			this.content=this.e.toString();
			this.hashCode=this.content.hashCode();
		}
	}

	public int ifTautology(){
		return this.ifTautology;
	}
	/**
	 * note it changes the input expression
	 * @param e
	 * @return
	 */
	public void fixedOrderExpression(Expression e){
		if (e instanceof EqualsTo||e instanceof NotEqualsTo||e instanceof AndExpression||e instanceof OrExpression||e instanceof Addition||e instanceof Multiplication){
			BinaryExpression bexp=(BinaryExpression) e;
			Expression left=bexp.getLeftExpression();
			Expression right=bexp.getRightExpression();
			fixedOrderExpression(left);
			fixedOrderExpression(right);

			boolean l_ifParameter=false;
			boolean r_ifParameter=false;
			for (Class<Expression> c:QueryToolBox.ConstantTypes){
				if(c.isInstance(left)){
					l_ifParameter=true;
					break;
				}            	
			}
			for (Class<Expression> c:QueryToolBox.ConstantTypes){
				if(c.isInstance(right)){
					r_ifParameter=true;
					break;
				}            	
			}

			if(l_ifParameter&&!r_ifParameter){
				bexp.setLeftExpression(right);
				bexp.setRightExpression(left);
			}
			else if (!l_ifParameter&&r_ifParameter){
				return;
			}
			else{	
				int l=left.toString().hashCode();
				int r=right.toString().hashCode();			
				if (l<r){
					bexp.setLeftExpression(right);
					bexp.setRightExpression(left);
				}
			}
		}
		else if (e instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) e;
			fixedOrderExpression(bexp.getLeftExpression());
			fixedOrderExpression(bexp.getRightExpression());
		}
		else if (e instanceof IsNullExpression){
			IsNullExpression isnull=(IsNullExpression) e;			
			fixedOrderExpression(isnull.getLeftExpression());
		}
		else if (e instanceof InExpression){
			InExpression inexp=(InExpression) e;			
			fixedOrderExpression(inexp.getLeftExpression());
		}
		else if (e instanceof Parenthesis){
			Parenthesis p=(Parenthesis) e;
			fixedOrderExpression(p.getExpression());
		}
		else if (e instanceof InverseExpression){
			InverseExpression p=(InverseExpression) e;
			fixedOrderExpression(p.getExpression());
		}
		else if (e instanceof Between){
			Between bt=(Between) e;
			fixedOrderExpression(bt.getLeftExpression());
			fixedOrderExpression(bt.getBetweenExpressionStart());
			fixedOrderExpression(bt.getBetweenExpressionEnd());
		}
		else if (e instanceof CaseExpression){
			CaseExpression caseexp=(CaseExpression) e;
			fixedOrderExpression(caseexp.getElseExpression());
			fixedOrderExpression(caseexp.getSwitchExpression());
			@SuppressWarnings("unchecked")
			List<WhenClause> wclist=caseexp.getWhenClauses();
			for (WhenClause wc:wclist){
				fixedOrderExpression(wc.getWhenExpression());
				fixedOrderExpression(wc.getThenExpression()); 
			}
		}
		else if (e instanceof Function){
			Function f=(Function) e;
			ExpressionList elist=f.getParameters();
			if (elist!=null){
				@SuppressWarnings("unchecked")
				List<Expression> list=elist.getExpressions();
				for (Expression exp: list){
					fixedOrderExpression(exp);
				}			
			}
		}
		else if (e instanceof StringValue){
			this.containsPlaceHolder=ToolBox.checkIfContainsPlaceHolder(e);
		}

	}

	private FixedOrderExpression(Expression e,int ifTautology,boolean containsPlaceHolder){
		this.e=e;
		this.content=e.toString();	
		this.containsPlaceHolder=containsPlaceHolder;
		int random=0;
		if(this.containsPlaceHolder)
			random=(int)(Math.random()*100);			
		this.hashCode=this.content.hashCode()+random;

		this.ifTautology=ifTautology;
	}

	public static FixedOrderExpression createAnyWay(Expression e,int ifTautology,boolean containsPlaceHolder){
		if (e==null)
			System.out.println("input for fixed order expression private constructor cannot be null");
		return new FixedOrderExpression(e,ifTautology,containsPlaceHolder);
	}

	public Expression getExpression(){
		return this.e;
	}

	public String toString(){
		return this.content;
	}

	@Override
	public boolean equals(Object o){
		FixedOrderExpression target=(FixedOrderExpression) o;
		return this.content.equals(target.content);
	}
	public int hashCode(){
		return this.hashCode;
	}
}
