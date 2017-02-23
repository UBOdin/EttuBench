package featureEngineering;

import java.util.ArrayList;
import java.util.List;

import booleanFormulaEntities.BooleanLiteral;
import booleanFormulaEntities.BooleanNormalClause;
import booleanFormulaEntities.DisjunctiveNormalForm;
import booleanFormulaEntities.FixedOrderExpression;
import booleanFormulaEntities.ToolBox;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;

/**
 * 
 * The list of special expressions that will be normalized:
 * 1. X Between A AND B -> A<=X AND X<=B
 * 
 * 2. X=CASE Expression
 * 
 * 3. any Function whose name contains isnull: A=isnull(X,Y) -> (X IS NULL AND A=X) OR (X IS NOT NULL AND A=Y)
 * 
 * 4. CONVERT(data_type(length),expression,style) Function: A=CONVERT(?,B,?) -> A=B
 * 
 * 5. X IN {a,b} -> X=a OR X=b
 * 
 * note this object need to return 
 *  
 * input: can be any Select query
 * @author tingxie
 *
 */

public class ExpressionRegularizer{

	public static SelectBody normalizeExpInSelectBody(SelectBody body){
		if(body instanceof PlainSelect){
			return normalizeExpInPlainSelect((PlainSelect) body);
		}
		else {
			Union u=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			List<PlainSelect> newplist=new ArrayList<PlainSelect>();
			for (PlainSelect ps:plist){
				SelectBody b=normalizeExpInPlainSelect(ps);
				if(b instanceof Union){
					Union union=(Union) b;
					@SuppressWarnings("unchecked")
					List<PlainSelect> pslist=union.getPlainSelects();
					newplist.addAll(pslist);
				}
				else
					newplist.add((PlainSelect) b);
			}
			u.setPlainSelects(newplist);
			return u;
		}
	}

//	private static void normalizeExpInFromItem(FromItem from){
//		if(from instanceof SubSelect){
//			SubSelect sub;
//			sub=(SubSelect) from;
//			sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
//		}
//		else if (from instanceof SubJoin){
//			SubJoin sub=(SubJoin) from;
//			FromItem left=sub.getLeft();
//			FromItem right=sub.getJoin().getRightItem();
//			normalizeExpInFromItem(left);
//			normalizeExpInFromItem(right);
//		}
//	}
	
	private static SelectBody normalizeExpInPlainSelect(PlainSelect input){

		//search through Having clause
		Expression having=input.getHaving();
		if (having!=null){
			Expression myhaving=normalizeBooleanExpression(having);
			if (myhaving==ToolBox.contradiction)
				System.out.println("warning Having clause is contradiction! "+input);
			else if (myhaving==ToolBox.tautology)
				myhaving=null;
			input.setHaving(myhaving);
		}
		//search through from item for sub-queries
		FromItem from=input.getFromItem();
		SubSelect sub;
		if(from instanceof SubSelect){
			sub=(SubSelect) from;
			sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
		}
		else if (from instanceof SubJoin){
			SubJoin subj=(SubJoin) from;
			FromItem left=subj.getLeft();
			FromItem right=subj.getJoin().getRightItem();
			if(left instanceof SubSelect){
				sub=(SubSelect) left;
				sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
			}
			if(right instanceof SubSelect){
				sub=(SubSelect) right;
				sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
			}
		}

		//search through expressions in Join conditions
		@SuppressWarnings("unchecked")
		List<Join> jlist=input.getJoins();
		if(jlist!=null){
			for(Join j: jlist){
				from=j.getRightItem();
				if(from instanceof SubSelect){
					sub=(SubSelect) from;
					sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
				}
				else if (from instanceof SubJoin){
					SubJoin subj=(SubJoin) from;
					FromItem left=subj.getLeft();
					FromItem right=subj.getJoin().getRightItem();
					if(left instanceof SubSelect){
						sub=(SubSelect) left;
						sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
					}
					if(right instanceof SubSelect){
						sub=(SubSelect) right;
						sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
					}
				}

				//disregard using here
				Expression onExp=j.getOnExpression();
				if(onExp!=null){
					Expression myonExp=normalizeBooleanExpression(onExp);	
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
			Expression mywhere=normalizeBooleanExpression(where);
			if (mywhere==ToolBox.contradiction)
				System.out.println("warning Where clause is contradiction!"+input);
			else if (mywhere==ToolBox.tautology)
				mywhere=null;

			input.setWhere(mywhere);
		}

		//search case expressions through select items and generate Unions accordingly
		@SuppressWarnings("unchecked")
		List<SelectItem> slist=input.getSelectItems();
		List<List<Expression>> elist=new ArrayList<List<Expression>>();
		List<BooleanNormalClause> conditionlist=new ArrayList<BooleanNormalClause>();
		for (SelectItem sitem: slist){
			if(sitem instanceof SelectExpressionItem){
				SelectExpressionItem sexpitem=(SelectExpressionItem) sitem;	
				List<List<Expression>> elist1=new ArrayList<List<Expression>>();
				List<BooleanNormalClause> conditionlist1=new ArrayList<BooleanNormalClause>();
				List<ContingentValue> clist;	
					clist=translateExpressionForCombination(sexpitem.getExpression());
				if(elist.isEmpty()){
					for (ContingentValue cvalue:clist){
						List<Expression> newlist=new ArrayList<Expression>();
						newlist.add(cvalue.value.getExpression());
						elist1.add(newlist); 
						conditionlist1.add(cvalue.conjClause);
					}
				}
				else{
					for (ContingentValue cvalue:clist){
						for (int i=0;i<elist.size();i++){              		
							List<Expression> newlist=new ArrayList<Expression>(elist.get(i));
							newlist.add(cvalue.value.getExpression());
							elist1.add(newlist);
							BooleanNormalClause myclause=conditionlist.get(i);
							BooleanNormalClause yourclause=cvalue.conjClause;
							if(myclause!=null&&yourclause!=null)
								conditionlist1.add(BooleanNormalClause.mergeTwoBooleanNormalClauses(myclause, yourclause, false));
							else if (myclause==null)
								conditionlist1.add(yourclause);	
							else if (yourclause==null)
								conditionlist1.add(myclause);	
							else
								conditionlist1.add(null);	               			
						}
					}
				}
				//update
				elist=elist1;
				conditionlist=conditionlist1;
			}
			else if (sitem instanceof AllTableColumns){
				AllTableColumns alltc=(AllTableColumns) sitem;				
				Column c=new Column(alltc.getTable(),"*");

				List<List<Expression>> elist1=new ArrayList<List<Expression>>();
				List<BooleanNormalClause> conditionlist1=new ArrayList<BooleanNormalClause>();
				List<ContingentValue> clist;	
				clist=translateExpressionForCombination(c);
				if(elist.isEmpty()){
					for (ContingentValue cvalue:clist){
						List<Expression> newlist=new ArrayList<Expression>();
						newlist.add(cvalue.value.getExpression());
						elist1.add(newlist); 
						conditionlist1.add(cvalue.conjClause);
					}
				}
				else{
					for (ContingentValue cvalue:clist){
						for (int i=0;i<elist.size();i++){              		
							List<Expression> newlist=new ArrayList<Expression>(elist.get(i));
							newlist.add(cvalue.value.getExpression());
							elist1.add(newlist);
							BooleanNormalClause myclause=conditionlist.get(i);
							BooleanNormalClause yourclause=cvalue.conjClause;
							if(myclause!=null&&yourclause!=null)
								conditionlist1.add(BooleanNormalClause.mergeTwoBooleanNormalClauses(myclause, yourclause, false));
							else if (myclause==null)
								conditionlist1.add(yourclause);	
							else if (yourclause==null)
								conditionlist1.add(myclause);	
							else
								conditionlist1.add(null);	               			
						}
					}
				}
				//update
				elist=elist1;
				conditionlist=conditionlist1;
			}
		}

		if (!elist.isEmpty()){
			List<PlainSelect> pslist=new ArrayList<PlainSelect>();
			for (int i=0;i<elist.size();i++){
				PlainSelect newselect=QueryToolBox.copyPlainSelect(input);
				List<SelectItem> newitemlist=new ArrayList<SelectItem>();
				for (Expression exp:elist.get(i)){
					SelectExpressionItem sitem=new SelectExpressionItem();
					sitem.setExpression(exp);
					newitemlist.add(sitem);
				}
				newselect.setSelectItems(newitemlist);
				BooleanNormalClause condition=conditionlist.get(i);
				if(condition!=null&&newselect.getWhere()!=null)
					newselect.setWhere(new AndExpression(newselect.getWhere(),condition.reformExpression()));
				else if (condition!=null){
					newselect.setWhere(condition.reformExpression());	
				}					
				pslist.add(newselect);
			}
			if(pslist.size()>1){
				Union u=new Union();
				u.setPlainSelects(pslist);
				return u;
			}
			else
				return pslist.get(0);
		}

		else return input;
	}

	/**
	 * normalize boolean expression
	 * @param exp
	 * @return
	 */
	private static Expression normalizeBooleanExpression(Expression exp){

		if(exp instanceof OrExpression){
			OrExpression or=(OrExpression) exp;
			Expression left=normalizeBooleanExpression(or.getLeftExpression());
			Expression right=normalizeBooleanExpression(or.getRightExpression());
			or.setLeftExpression(left);
			or.setRightExpression(right);
			return or;
		}
		else if (exp instanceof AndExpression){
			AndExpression and=(AndExpression) exp;
			Expression left=normalizeBooleanExpression(and.getLeftExpression());
			Expression right=normalizeBooleanExpression(and.getRightExpression());
			and.setLeftExpression(left);
			and.setRightExpression(right);
			return and;	
		}
		//we only deal with boolean binary expression
		else if (exp instanceof MinorThan||exp instanceof MinorThanEquals||exp instanceof GreaterThan
				||exp instanceof GreaterThanEquals||exp instanceof EqualsTo||exp instanceof NotEqualsTo){
			BinaryExpression bexp=(BinaryExpression) exp;	    	
			Expression left=bexp.getLeftExpression();
			Expression right=bexp.getRightExpression();

			List<ContingentValue> leftlist,rightlist;
				leftlist=translateExpressionForCombination(left);
				rightlist=translateExpressionForCombination(right);

			try {
				Expression result;
				BinaryExpression nbexp=bexp.getClass().newInstance();
				if(bexp.isNot())
					nbexp.setNot();				
				nbexp.setLeftExpression(leftlist.get(0).value.getExpression());
				nbexp.setRightExpression(rightlist.get(0).value.getExpression());
				Expression start=nbexp;
				if(leftlist.get(0).conjClause!=null)
					start=new AndExpression(start,leftlist.get(0).conjClause.reformExpression());
				if(rightlist.get(0).conjClause!=null)
					start=new AndExpression(start,rightlist.get(0).conjClause.reformExpression());

				result=start;

				for (int i=0;i<leftlist.size();i++){
					for(int j=0;j<rightlist.size();j++){
						if(i+j==0)
							continue;
						else{
							nbexp=bexp.getClass().newInstance();
							if(bexp.isNot())
								nbexp.setNot();
							nbexp.setLeftExpression(leftlist.get(i).value.getExpression());
							nbexp.setRightExpression(rightlist.get(j).value.getExpression());
							start=nbexp;
							if(leftlist.get(i).conjClause!=null)
								start=new AndExpression(start,leftlist.get(i).conjClause.reformExpression());
							if(rightlist.get(j).conjClause!=null)
								start=new AndExpression(start,rightlist.get(j).conjClause.reformExpression());
							result=new OrExpression(result,start);
						}
					}
				}
				return result;
			} catch (InstantiationException e) {
				e.printStackTrace();
				return null;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}

		}
		else if (exp instanceof IsNullExpression){
			IsNullExpression isnull=(IsNullExpression) exp;
			if(isnull.getLeftExpression() instanceof SubSelect){
				SubSelect sub=(SubSelect) isnull.getLeftExpression();
				ExistsExpression exists=new ExistsExpression();
				exists.setRightExpression(sub);
				exists.setNot(!isnull.isNot());
						return exists;
			}
			
			List<ContingentValue> clist;
				clist=translateExpressionForCombination(isnull.getLeftExpression());
			IsNullExpression nisnull=new IsNullExpression();
			nisnull.setLeftExpression(clist.get(0).value.getExpression());
			nisnull.setNot(isnull.isNot());
			Expression result;
			Expression start;
			if(clist.get(0).conjClause!=null)
				start=new AndExpression(nisnull,clist.get(0).conjClause.reformExpression());
			else
				start=nisnull;

			result=start;

			for (int i=1;i<clist.size();i++){
				nisnull=new IsNullExpression();
				nisnull.setLeftExpression(clist.get(i).value.getExpression());
				nisnull.setNot(isnull.isNot());

				if(clist.get(i).conjClause!=null)
					result=new OrExpression(result,new AndExpression(nisnull,clist.get(i).conjClause.reformExpression()));
				else
					result=new OrExpression(result,nisnull);
			}
			return result;
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			if (inexp.getLeftExpression() instanceof SubSelect){
				SubSelect sub=(SubSelect) inexp.getLeftExpression();
				sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
			}
			List<ContingentValue> leftlist;
				leftlist=translateExpressionForCombination(inexp.getLeftExpression());  

			ItemsList ilist=inexp.getItemsList();
			if(ilist==null){
				return exp;
			}
			else if (ilist instanceof ExpressionList){
				ExpressionList elist=(ExpressionList) ilist;
				@SuppressWarnings("unchecked")
				List<Expression> list=elist.getExpressions();  	   
				if(!inexp.isNot()){
					Expression result=null;
					for (int i=0;i<leftlist.size();i++){
						EqualsTo eq=new EqualsTo(leftlist.get(i).value.getExpression(),list.get(0));
						Expression start;
						start=eq;		
						for (int j=1;j<list.size();j++){
							start=new OrExpression(start,new EqualsTo(leftlist.get(i).value.getExpression(),list.get(j)));	
						}

						if (leftlist.get(i).conjClause!=null)
							start=new AndExpression(start,leftlist.get(i).conjClause.reformExpression());	

						if(i==0)
							result=start;
						else
							result=new OrExpression(result,start);
					}
					return result;
				}
				//if NOT IN
				else{
					Expression result=null;
					for (int i=0;i<leftlist.size();i++){
						NotEqualsTo neq=new NotEqualsTo(leftlist.get(i).value.getExpression(),list.get(0));
						Expression start=neq;		
						for (int j=1;j<list.size();j++)
							start=new AndExpression(start,new NotEqualsTo(leftlist.get(i).value.getExpression(),list.get(j)));								

						if (leftlist.get(i).conjClause!=null)
							start=new AndExpression(start,leftlist.get(i).conjClause.reformExpression());	

						if(i==0)
							result=start;
						else
							result=new OrExpression(result,start);
					}
					return result;
				}
			}
			//if it contains sub-select on right side
			else{
				//normalize the body
				SubSelect sub=(SubSelect) ilist;
				sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));

				Expression result;
				InExpression ninexp=new InExpression();
				ninexp.setLeftExpression(leftlist.get(0).value.getExpression());
				ninexp.setItemsList(ilist);
				ninexp.setNot(inexp.isNot());
				if(leftlist.get(0).conjClause!=null)
					result=new AndExpression(ninexp,leftlist.get(0).conjClause.reformExpression());
				else
					result=ninexp;

				for (int i=1;i<leftlist.size();i++){
					ninexp=new InExpression();
					ninexp.setLeftExpression(leftlist.get(i).value.getExpression());
					ninexp.setItemsList(ilist);
					ninexp.setNot(inexp.isNot());
					if(leftlist.get(i).conjClause!=null)
						result=new OrExpression(result,new AndExpression(ninexp,leftlist.get(i).conjClause.reformExpression()));
					else
						result=new OrExpression(result,ninexp);	
				}					
				return result;
			}
		}
		else if (exp instanceof Between){
			Between between=(Between) exp;	
			List<ContingentValue> leftlist;
				leftlist=translateExpressionForCombination(between.getLeftExpression());	
			Expression innerleft=between.getBetweenExpressionStart();
			Expression innerright=between.getBetweenExpressionEnd();
			if (!between.isNot()){
				Expression left=leftlist.get(0).value.getExpression();
				MinorThanEquals mte1=new MinorThanEquals();
				mte1.setLeftExpression(innerleft);
				mte1.setRightExpression(left);

				MinorThanEquals mte2=new MinorThanEquals();
				mte2.setLeftExpression(left);
				mte2.setRightExpression(innerright);

				AndExpression and=new AndExpression();
				and.setLeftExpression(mte1);
				and.setRightExpression(mte2);
				Expression result;
				if(leftlist.get(0).conjClause!=null)
					result=new AndExpression(and,leftlist.get(0).conjClause.reformExpression());
				else
					result=and;
				for (int i=1;i<leftlist.size();i++){
					left=leftlist.get(i).value.getExpression();
					mte1=new MinorThanEquals();
					mte1.setLeftExpression(innerleft);
					mte1.setRightExpression(left);

					mte2=new MinorThanEquals();
					mte2.setLeftExpression(left);
					mte2.setRightExpression(innerright);

					and=new AndExpression();
					and.setLeftExpression(mte1);
					and.setRightExpression(mte2);
					if(leftlist.get(i).conjClause!=null)
						result=new OrExpression(result,new AndExpression(and,leftlist.get(i).conjClause.reformExpression()));
					else
						result=new OrExpression(result,and);

				}
				return result;
			}
			else {
				Expression left=leftlist.get(0).value.getExpression();
				MinorThan mt1=new MinorThan();
				mt1.setLeftExpression(left);
				mt1.setRightExpression(innerleft);

				MinorThan mt2=new MinorThan();
				mt2.setLeftExpression(innerright);
				mt2.setRightExpression(left);

				OrExpression or=new OrExpression();
				or.setLeftExpression(mt1);
				or.setRightExpression(mt2);
				Expression result;
				if(leftlist.get(0).conjClause!=null)
					result=new AndExpression(or,leftlist.get(0).conjClause.reformExpression());
				else
					result=or;
				for (int i=1;i<leftlist.size();i++){
					left=leftlist.get(i).value.getExpression();
					mt1=new MinorThan();
					mt1.setLeftExpression(left);
					mt1.setRightExpression(innerleft);

					mt2=new MinorThan();
					mt2.setLeftExpression(innerright);
					mt2.setRightExpression(left);

					or=new OrExpression();
					or.setLeftExpression(mt1);
					or.setRightExpression(mt2);

					if(leftlist.get(i).conjClause!=null)
						result=new OrExpression(result,new AndExpression(or,leftlist.get(i).conjClause.reformExpression()));
					else
						result=new OrExpression(result,or);
				}
				return result;						
			}

		}
		else if (exp instanceof Parenthesis){
			Parenthesis p=(Parenthesis) exp;
			p.setExpression(normalizeBooleanExpression(p.getExpression()));
			return p;
		}
		else if (exp instanceof InverseExpression){
			InverseExpression p=(InverseExpression) exp;
			p.setExpression(normalizeBooleanExpression(p.getExpression()));
			return p;
		}
		else if (exp instanceof ExistsExpression){
			ExistsExpression exists=(ExistsExpression) exp;
			SubSelect sub=(SubSelect) exists.getRightExpression();
			sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
			return exp;
		}
		else return exp;
	}


	/**
	 * this translates a CaseExpression into a set of contingent values 
	 * with context
	 * @param f
	 * @return
	 */
	public static List<ContingentValue> translateCaseExpressionForCombination(CaseExpression caseexp){
		@SuppressWarnings("unchecked")
		List<WhenClause> wclist=caseexp.getWhenClauses();
		Expression swexp=caseexp.getSwitchExpression();
		Expression elseexp=caseexp.getElseExpression();		
		List<ContingentValue> result=new ArrayList<ContingentValue>();

		//for switch, we assume that all values in switch are simple 'values', no complex formulae
		if (swexp!=null){
			List<ContingentValue> swvalues=translateExpressionForCombination(swexp);
			for (ContingentValue sw: swvalues){
				//start by a tautology
				BooleanNormalClause negateconditions=new BooleanNormalClause(1);

				for (WhenClause wc: wclist){
					Expression when=wc.getWhenExpression();
					Expression then=wc.getThenExpression();
					EqualsTo eq=new EqualsTo(sw.value.getExpression(),when);
					BooleanLiteral bl;
					if(QueryToolBox.awarer!=null)
					 bl=new BooleanLiteral(new FixedOrderExpression(eq),QueryToolBox.awarer);
					else
						bl=new BooleanLiteral(new FixedOrderExpression(eq));
					BooleanNormalClause eqclause=BooleanNormalClause.createClause(bl, false);
					BooleanNormalClause combinedclause=BooleanNormalClause.mergeTwoBooleanNormalClauses(negateconditions, eqclause, false);
					//if it is a tautology,then no need to check rest when clauses
					if (combinedclause.ifTautology()==1){
						BooleanNormalClause finalclause=sw.conjClause;
						if(finalclause==null||finalclause.ifTautology()!=-1){
							ContingentValue newcv=ContingentValue.createValue(then,finalclause);
							result.add(newcv);

							//negateconditions is now updated as a contradiction clause
							negateconditions=new BooleanNormalClause(-1);
						}
						//since the condition is a tautology, no need to check the rest conditions
						break;
					}
					//if it is not a contradiction
					else if(combinedclause.ifTautology()!=-1){
						BooleanNormalClause finalclause;
						if(sw.conjClause!=null)
							finalclause=BooleanNormalClause.mergeTwoBooleanNormalClauses(combinedclause,sw.conjClause, false);
						else
							finalclause=combinedclause;
						if(finalclause.ifTautology()!=-1){
							ContingentValue newcv=ContingentValue.createValue(then,finalclause);
							result.add(newcv);					
							//update negate condition list
							NotEqualsTo neq=new NotEqualsTo(eq.getLeftExpression(),eq.getRightExpression());						
							BooleanLiteral nbl;
							if(QueryToolBox.awarer!=null)
							 nbl=new BooleanLiteral(new FixedOrderExpression(neq),QueryToolBox.awarer);
							else
								nbl=new BooleanLiteral(new FixedOrderExpression(neq));
							
							BooleanNormalClause neqclause=BooleanNormalClause.createClause(nbl, false);
							negateconditions=BooleanNormalClause.mergeTwoBooleanNormalClauses(negateconditions, neqclause, false);
						}
					}		    	
				}
				//for else expression
				if (elseexp!=null&&negateconditions.ifTautology()!=-1){
					BooleanNormalClause finalclause;
					if(sw.conjClause!=null)
						finalclause=BooleanNormalClause.mergeTwoBooleanNormalClauses(negateconditions,sw.conjClause, false);
					else
						finalclause=negateconditions;
					if(finalclause.ifTautology()!=-1){
						ContingentValue newcv=ContingentValue.createValue(elseexp,finalclause);
						result.add(newcv);
					}
				}
			}
		}
		//for simple case expression, we need to consider complex boolean expression hidden in when clause
		else {				
			//start by tautology
			DisjunctiveNormalForm negateconditions=new DisjunctiveNormalForm(1);

			for (WhenClause wc: wclist){
				//normalize it
				Expression when=wc.getWhenExpression();
				//we assume there is no complex expression in then expression
				Expression then=wc.getThenExpression();
				//turn it into normal form because it may be of very complex structure
				DisjunctiveNormalForm whennormalform;
				if (QueryToolBox.awarer!=null)
				whennormalform=DisjunctiveNormalForm.DNFDecomposition(when,QueryToolBox.awarer);
				else
					whennormalform=DisjunctiveNormalForm.DNFDecomposition(when);
				DisjunctiveNormalForm combinedform=DisjunctiveNormalForm.AndTogetherTwoNormalForms(negateconditions, whennormalform);
				//if when clause is a tautology, there is no need to continue checking rest conditions
				if(combinedform.ifTautology()==1){					
					ContingentValue cv=ContingentValue.createValue(then, null);
					result.add(cv);
					return result;
				}
				//if the combined expression is not a contradiction
				else if (combinedform.ifTautology()!=-1){
					for (BooleanNormalClause conjunctive: combinedform.getContent()){
						result.add(ContingentValue.createValue(then,conjunctive));				
					}
					//update negationconditions
					negateconditions=DisjunctiveNormalForm.AndTogetherTwoNormalForms(negateconditions,whennormalform.getNegatedForm());
				}			    	
			}
			//for else expression
			if (elseexp!=null&&negateconditions.ifTautology()!=-1){
				for (BooleanNormalClause conjunctive: negateconditions.getContent())
					result.add(ContingentValue.createValue(elseexp, conjunctive));
			}
		}
		return result;

	}

	/**
	 * this translates a Function 
	 * without context
	 * @param f
	 * @return
	 */
	private static List<ContingentValue> translateFunctionForCombination(Function f){
		String fname=f.getName().toLowerCase();
		if (fname.contains("isnull")){
			List<ContingentValue> result=new ArrayList<ContingentValue>();
			List<ContingentValue> innerleftlist=translateExpressionForCombination((Expression) f.getParameters().getExpressions().get(0));
			List<ContingentValue> innerrightlist=translateExpressionForCombination((Expression) f.getParameters().getExpressions().get(1));
			//for all Xs
			for (ContingentValue innerleft:innerleftlist){
				//X IS NULL
				IsNullExpression isnull=new IsNullExpression();
				isnull.setLeftExpression(innerleft.value.getExpression());
				//X IS NOT NULL
				IsNullExpression isnotnull=new IsNullExpression();
				isnotnull.setLeftExpression(innerleft.value.getExpression());
				isnotnull.setNot(true);

				BooleanLiteral bisnull=new BooleanLiteral(new FixedOrderExpression(isnull));
				BooleanLiteral bisnotnull=new BooleanLiteral(new FixedOrderExpression(isnotnull));
				BooleanNormalClause isnullclause=BooleanNormalClause.createClause(bisnull, false);
				BooleanNormalClause isnotnullclause=BooleanNormalClause.createClause(bisnotnull, false);
				//if X IS NULL is a tautology
				if (isnullclause.ifTautology()==1){
					for (ContingentValue innerright:innerrightlist){
						BooleanNormalClause combinedclause;
						if(innerleft.conjClause!=null&&innerright.conjClause!=null)
							combinedclause=BooleanNormalClause.mergeTwoBooleanNormalClauses(innerleft.conjClause, innerright.conjClause, false);
						else if (innerleft.conjClause==null)
							combinedclause=innerright.conjClause;
						else if (innerright.conjClause==null)
							combinedclause=innerleft.conjClause;
						else 
							combinedclause=null;

						ContingentValue newcv=ContingentValue.createValue(innerright.value, combinedclause);
						result.add(newcv);
					}
				}
				//if X IS NOT NULL, then there is nothing to do with innerright
				else if (isnullclause.ifTautology()==-1){
					result.add(innerleft);
				}
				else{						
					for (ContingentValue innerright:innerrightlist){
						if (innerleft.conjClause!=null){
							isnullclause= BooleanNormalClause.mergeTwoBooleanNormalClauses(innerleft.conjClause, isnullclause, false);
							isnotnullclause= BooleanNormalClause.mergeTwoBooleanNormalClauses(innerleft.conjClause, isnotnullclause, false);
						}
						if (innerright.conjClause!=null){
							isnullclause= BooleanNormalClause.mergeTwoBooleanNormalClauses(innerright.conjClause,isnullclause,false);
							isnotnullclause= BooleanNormalClause.mergeTwoBooleanNormalClauses(innerright.conjClause,isnotnullclause,false);

						}
						ContingentValue isnullvalue=ContingentValue.createValue(innerright.value, isnullclause);			
						ContingentValue isnotnullvalue=ContingentValue.createValue(innerleft.value, isnotnullclause);
						result.add(isnullvalue);
						result.add(isnotnullvalue);
					}
				}

			}
			return result;
		}
		else if (fname.equals("convert")){
			return translateExpressionForCombination((Expression) f.getParameters().getExpressions().get(1));
		}
		else if (fname.equals("lower")||fname.equals("upper")){
			return translateExpressionForCombination((Expression) f.getParameters().getExpressions().get(0));
		}
		//other cases just treat it as a 'value'
		else {
			List<ContingentValue> result=new ArrayList<ContingentValue>();
			//  check if it is a null value
			int checknull=QueryToolBox.checkIfEvaluatedToNull(f);
			if (checknull==1){
				ContingentValue myvalue=ContingentValue.createValue(new NullValue(), null);
				result.add(myvalue);	
			}
			else {
				ContingentValue myvalue=ContingentValue.createValue(f,null);
				result.add(myvalue);
			}
			return result;
		}
	}

	/**
	 * this translates any value expression
	 * with context
	 * @param e
	 * @return
	 */
	private static List<ContingentValue> translateExpressionForCombination(Expression e){		
		if (e instanceof Addition||e instanceof Multiplication||e instanceof Division||e instanceof Subtraction){
			BinaryExpression bexp=(BinaryExpression) e;
			List<ContingentValue> leftlist=translateExpressionForCombination(bexp.getLeftExpression());
			List<ContingentValue> rightlist=translateExpressionForCombination(bexp.getRightExpression());
			List<ContingentValue> newlist=new ArrayList<ContingentValue>();
			for (ContingentValue leftvalue: leftlist){
				for (ContingentValue rightvalue: rightlist){
					BooleanNormalClause newclause;
					if(leftvalue.conjClause!=null&&rightvalue.conjClause!=null)
						newclause=BooleanNormalClause.mergeTwoBooleanNormalClauses(leftvalue.conjClause, rightvalue.conjClause, false);
					else if (leftvalue.conjClause==null)
						newclause=rightvalue.conjClause;
					else if (rightvalue.conjClause==null)
						newclause=leftvalue.conjClause;
					else newclause=null;
					try {            		  
						BinaryExpression bbexp;
						bbexp = bexp.getClass().newInstance();
						bbexp.setLeftExpression(leftvalue.value.getExpression());
						bbexp.setRightExpression(rightvalue.value.getExpression());
						newlist.add(ContingentValue.createValue(bbexp, newclause));	
					} catch (InstantiationException e1) {
						e1.printStackTrace();
					} catch (IllegalAccessException e1) {
						e1.printStackTrace();
					}            	

				}
			}
			return newlist;         
		}
		else  if (e instanceof Function)
			return translateFunctionForCombination((Function) e);
		else if (e instanceof CaseExpression)
			return translateCaseExpressionForCombination((CaseExpression) e);
		//just regard it as a 'value'
		else if (e!=null){
			//search through sub-query
			if(e instanceof AllComparisonExpression){
				AllComparisonExpression all=(AllComparisonExpression) e;
				SubSelect sub=all.getSubSelect();
				sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
			}
			else if (e instanceof AnyComparisonExpression){
				AnyComparisonExpression any=(AnyComparisonExpression) e;
				SubSelect sub=any.getSubSelect();
				sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));
			}
			else if (e instanceof SubSelect){
				SubSelect sub=(SubSelect) e;
				sub.setSelectBody(normalizeExpInSelectBody(sub.getSelectBody()));	
			}

			List<ContingentValue> result=new ArrayList<ContingentValue>();
			result.add(ContingentValue.createValue(e,null));
			return result; 
		}
		else{
			//System.out.println("input of translateExpressionforBinaryOperation cannot be null.");
			List<ContingentValue> result=new ArrayList<ContingentValue>();
			result.add(ContingentValue.createValue(new StringValue(ToolBox.placeholders[0]),null));
			return result; 
		}
	}

}
