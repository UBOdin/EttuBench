package booleanFormulaEntities;

import featureEngineering.QueryToolBox;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;

/**
 * defines a boolean literal in any boolean formula.
 * input should be fixedOrderExpression
 * since A>B is the same as B<A, this class is responsible for keeping a uniform way of expressing it as B<A
 * it is also responsible for keeping a fixed order of left and right hand side for expression like A=B <=> B=A 
 * @author tingxie
 *
 */
public class BooleanLiteral {
	private FixedOrderExpression e;
	private int iftautology=0; // 1/-1/0 for tautology/contradiction/not sure
	/**
	 * returns whether this literal is tautology or contradiction
	 * @return
	 */
	public int ifTautology(){
		return this.iftautology;
	}
	/**
	 * this function returns a negated boolean literal
	 * 
	 * @param exp
	 * @return
	 */
	public static BooleanLiteral negateBooleanLiteral(BooleanLiteral bl) {
		if (bl.iftautology==1)
			return new BooleanLiteral(ToolBox.contradiction,-1);
		else if (bl.iftautology==-1)
			return new BooleanLiteral(ToolBox.tautology,1);
		//for not tautology or contradiction
		Expression exp = bl.getExpression().getExpression();
		if (exp instanceof Between) {
			Between between = (Between) exp;
			Between nbetween = new Between();
			nbetween.setNot(!between.isNot());
			nbetween.setLeftExpression(between.getLeftExpression());
			nbetween.setBetweenExpressionStart(between.getBetweenExpressionStart());
			nbetween.setBetweenExpressionEnd(between.getBetweenExpressionEnd());
			return new BooleanLiteral(nbetween,0);
		} else if (exp instanceof ExistsExpression) {
			ExistsExpression eexp = (ExistsExpression) exp;
			ExistsExpression neexp = new ExistsExpression();
			neexp.setNot(!eexp.isNot());
			neexp.setRightExpression(eexp.getRightExpression());
			return new BooleanLiteral(neexp,0);
		} else if (exp instanceof EqualsTo) {
			NotEqualsTo net = new NotEqualsTo();
			EqualsTo eq = (EqualsTo) exp;
			Expression left = eq.getLeftExpression();
			Expression right = eq.getRightExpression();
			// deal with any all comparisons
			if (left instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) left;
				left = new AllComparisonExpression(any.getSubSelect());
			} else if (left instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) left;
				left = new AnyComparisonExpression(all.getSubSelect());
			}

			if (right instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) right;
				right = new AllComparisonExpression(any.getSubSelect());
			} else if (right instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) right;
				right = new AnyComparisonExpression(all.getSubSelect());
			}
			net.setLeftExpression(left);
			net.setRightExpression(right);
			return new BooleanLiteral(net,0);
		} else if (exp instanceof NotEqualsTo) {
			EqualsTo et = new EqualsTo();
			NotEqualsTo neq = (NotEqualsTo) exp;
			Expression left = neq.getLeftExpression();
			Expression right = neq.getRightExpression();
			// deal with any all comparisons
			if (left instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) left;
				left = new AllComparisonExpression(any.getSubSelect());
			} else if (left instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) left;
				left = new AnyComparisonExpression(all.getSubSelect());
			}

			if (right instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) right;
				right = new AllComparisonExpression(any.getSubSelect());
			} else if (right instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) right;
				right = new AnyComparisonExpression(all.getSubSelect());
			}
			et.setLeftExpression(left);
			et.setRightExpression(right);
			return new BooleanLiteral(et,0);
		} else if (exp instanceof GreaterThan) {
			MinorThanEquals mte = new MinorThanEquals();
			GreaterThan gt = (GreaterThan) exp;
			Expression left = gt.getLeftExpression();
			Expression right = gt.getRightExpression();
			// deal with any all comparisons
			if (left instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) left;
				left = new AllComparisonExpression(any.getSubSelect());
			} else if (left instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) left;
				left = new AnyComparisonExpression(all.getSubSelect());
			}

			if (right instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) right;
				right = new AllComparisonExpression(any.getSubSelect());
			} else if (right instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) right;
				right = new AnyComparisonExpression(all.getSubSelect());
			}
			mte.setLeftExpression(left);
			mte.setRightExpression(right);
			return new BooleanLiteral(mte,0);
		} else if (exp instanceof GreaterThanEquals) {
			MinorThan mt = new MinorThan();
			GreaterThanEquals gte = (GreaterThanEquals) exp;
			Expression left = gte.getLeftExpression();
			Expression right = gte.getRightExpression();
			// deal with any all comparisons
			if (left instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) left;
				left = new AllComparisonExpression(any.getSubSelect());
			} else if (left instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) left;
				left = new AnyComparisonExpression(all.getSubSelect());
			}

			if (right instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) right;
				right = new AllComparisonExpression(any.getSubSelect());
			} else if (right instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) right;
				right = new AnyComparisonExpression(all.getSubSelect());
			}
			mt.setLeftExpression(left);
			mt.setRightExpression(right);
			return new BooleanLiteral(mt,0);
		} else if (exp instanceof MinorThan) {
			GreaterThanEquals gte = new GreaterThanEquals();
			MinorThan mt = (MinorThan) exp;
			Expression left = mt.getLeftExpression();
			Expression right = mt.getRightExpression();
			// deal with any all comparisons
			if (left instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) left;
				left = new AllComparisonExpression(any.getSubSelect());
			} else if (left instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) left;
				left = new AnyComparisonExpression(all.getSubSelect());
			}

			if (right instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) right;
				right = new AllComparisonExpression(any.getSubSelect());
			} else if (right instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) right;
				right = new AnyComparisonExpression(all.getSubSelect());
			}
			gte.setLeftExpression(left);
			gte.setRightExpression(right);
			return new BooleanLiteral(gte,0);
		} else if (exp instanceof MinorThanEquals) {
			GreaterThan gt = new GreaterThan();
			MinorThanEquals mte = (MinorThanEquals) exp;
			Expression left = mte.getLeftExpression();
			Expression right = mte.getRightExpression();
			// deal with any all comparisons
			if (left instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) left;
				left = new AllComparisonExpression(any.getSubSelect());
			} else if (left instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) left;
				left = new AnyComparisonExpression(all.getSubSelect());
			}

			if (right instanceof AnyComparisonExpression) {
				AnyComparisonExpression any = (AnyComparisonExpression) right;
				right = new AllComparisonExpression(any.getSubSelect());
			} else if (right instanceof AllComparisonExpression) {
				AllComparisonExpression all = (AllComparisonExpression) right;
				right = new AnyComparisonExpression(all.getSubSelect());
			}
			gt.setLeftExpression(left);
			gt.setRightExpression(right);
			return new BooleanLiteral(gt,0);
		} else if (exp instanceof InExpression) {
			InExpression inExp = (InExpression) exp;
			InExpression ninExp = new InExpression();
			ninExp.setNot(!inExp.isNot());
			ninExp.setItemsList(inExp.getItemsList());
			ninExp.setLeftExpression(inExp.getLeftExpression());
			return new BooleanLiteral(ninExp,0);
		} else if (exp instanceof IsNullExpression) {
			IsNullExpression nullExp = (IsNullExpression) exp;
			IsNullExpression nnullExp = new IsNullExpression();
			nnullExp.setNot(!nullExp.isNot());
			nnullExp.setLeftExpression(nullExp.getLeftExpression());
			return new BooleanLiteral(nnullExp,0);
		} else if (exp instanceof LikeExpression) {
			LikeExpression likeExp = (LikeExpression) exp;
			LikeExpression nlikeExp = new LikeExpression();
			nlikeExp.setLeftExpression(likeExp.getLeftExpression());
			nlikeExp.setRightExpression(likeExp.getRightExpression());
			nlikeExp.setEscape(likeExp.getEscape());
			nlikeExp.setNot(!likeExp.isNot());
			return new BooleanLiteral(nlikeExp,0);
		} else {
			System.out.println("when negate literals,please take care of boolean literal : " + exp);
			return null;
		}
	}

	/**
	 * public it only accepts expression with its left and right hand side order fixed 
	 * constructor with context
	 * @param e
	 */
	public BooleanLiteral(FixedOrderExpression fe,ExpressionContextAware contextAwarer) {		
		this.iftautology=fe.ifTautology(); 
		this.e = fe;	
	}

	/**
	 * public it only accepts expression with its left and right hand side order fixed 
	 * constructor without context
	 * @param e
	 */
	public BooleanLiteral(FixedOrderExpression fe) {		
		this.iftautology=fe.ifTautology(); 
		this.e = fe;
	}

	/**
	 * private it also accepts expression that assumed to be fixed order
	 * constructor without context
	 * @param e
	 */
	private BooleanLiteral(Expression e,int ifTautology){
		this.e = FixedOrderExpression.createAnyWay(e,ifTautology,ToolBox.checkIfContainsPlaceHolder(e));
		this.iftautology=ifTautology;
	}


	public String toString() {
		return this.e.toString();
	}

	public boolean equals(Object o) {
		BooleanLiteral target = (BooleanLiteral) o;
		return this.e.equals(target.e);
	}

	public int hashCode() {
		return this.e.hashCode();
	}

	public FixedOrderExpression getExpression() {
		return this.e;
	}

	/**
	 * return RegionRelationship between left input and right input with context
	 * input literals must contain a non-value object with a valued object and type must match
	 * input literals must NOT across different sub-query
	 * all regions are inclusive
	 * @param left
	 * @param right
	 * @return
	 */
	public static RegionRelationship regionContainmentCheck(BooleanLiteral left,BooleanLiteral right){
		
		if(QueryToolBox.awarer!=null){
			Expression l=left.getExpression().getExpression();
			Expression r=right.getExpression().getExpression();
			if (l instanceof BinaryExpression&&r instanceof BinaryExpression){
				AttributeWithRegion lregion=AttributeWithRegion.generateRegion(l);
				if (lregion!=null){	
					AttributeWithRegion rregion=AttributeWithRegion.generateRegion(r);
					if (rregion!=null){		
						if (lregion.exp.toString().equals(rregion.exp.toString())&&!QueryToolBox.awarer.ifAcrossContext(rregion.exp, lregion.exp)){					
							double llbound=lregion.lbound;
							double lrbound=lregion.rbound;
							double rlbound=rregion.lbound;
							double rrbound=rregion.rbound;                                 	
							if(llbound>=rlbound&&lrbound<=rrbound)
								return RegionRelationship.Contained;                   
							else if (llbound<=rlbound&&lrbound>=rrbound)
								return RegionRelationship.Contains;
							else if (llbound>rrbound&&llbound-rrbound<=AttributeWithRegion.epsilon)
								return RegionRelationship.NonOverLapButMeet;
							else if (llbound>rrbound){
								return RegionRelationship.NonOverLapNoMeet;
							}						
							else if (lrbound<rlbound&&rlbound-lrbound<=AttributeWithRegion.epsilon)
								return RegionRelationship.NonOverLapButMeet;
							else if(lrbound<rlbound)
								return RegionRelationship.NonOverLapNoMeet;
							else return RegionRelationship.Overlap;
						}
					}
				}
			}
			else if (l instanceof IsNullExpression&&r instanceof IsNullExpression){
				IsNullExpression lisnull=(IsNullExpression) l;
				IsNullExpression risnull=(IsNullExpression) r;
				Expression ltarget=lisnull.getLeftExpression();
				Expression rtarget=risnull.getLeftExpression();
				if (ltarget.toString().equals(rtarget.toString())&&!QueryToolBox.awarer.ifAcrossContext(ltarget, rtarget)){					
					if (lisnull.isNot()^risnull.isNot())
						return RegionRelationship.NonOverLapButMeet;           
					else 
						return RegionRelationship.Contains;             
				}
			}
		}
		else {
			Expression l=left.getExpression().getExpression();
			Expression r=right.getExpression().getExpression();
			if (l instanceof BinaryExpression&&r instanceof BinaryExpression){
				AttributeWithRegion lregion=AttributeWithRegion.generateRegion(l);
				if (lregion!=null){	
					AttributeWithRegion rregion=AttributeWithRegion.generateRegion(r);
					if (rregion!=null){		
						if (lregion.exp.toString().equals(rregion.exp.toString())){					
							double llbound=lregion.lbound;
							double lrbound=lregion.rbound;
							double rlbound=rregion.lbound;
							double rrbound=rregion.rbound;                                 	
							if(llbound>=rlbound&&lrbound<=rrbound)
								return RegionRelationship.Contained;                   
							else if (llbound<=rlbound&&lrbound>=rrbound)
								return RegionRelationship.Contains;
							else if (llbound>rrbound&&llbound-rrbound<=AttributeWithRegion.epsilon)
								return RegionRelationship.NonOverLapButMeet;
							else if (llbound>rrbound){
								return RegionRelationship.NonOverLapNoMeet;
							}						
							else if (lrbound<rlbound&&rlbound-lrbound<=AttributeWithRegion.epsilon)
								return RegionRelationship.NonOverLapButMeet;
							else if(lrbound<rlbound)
								return RegionRelationship.NonOverLapNoMeet;
							else return RegionRelationship.Overlap;
						}
					}
				}
			}
			else if (l instanceof IsNullExpression&&r instanceof IsNullExpression){
				IsNullExpression lisnull=(IsNullExpression) l;
				IsNullExpression risnull=(IsNullExpression) r;
				Expression ltarget=lisnull.getLeftExpression();
				Expression rtarget=risnull.getLeftExpression();
				if (ltarget.toString().equals(rtarget.toString())){					
					if (lisnull.isNot()^risnull.isNot())
						return RegionRelationship.NonOverLapButMeet;           
					else 
						return RegionRelationship.Contains;             
				}
			}	
		}

		return RegionRelationship.NotComparable;
	}

}
