package featureEngineering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import booleanFormulaEntities.BooleanNormalClause;
import booleanFormulaEntities.DisjunctiveNormalForm;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;

/**
 * a query that transforms any OR expression it meets in the DNFed formula of the selection predicates
 * and turn it into corresponding UNION representation
 * note it will first automatically do DNFNormalization
 * input: any Select query
 * @author tingxie
 *
 */
public class DNFUNIONTransformer{
	
   
	private static SelectBody UnionTransformPlainSelect(PlainSelect ps){
        //search through having
		Expression having=ps.getHaving();
		if(having!=null)
			traverseExpressionAndUnionTransformSubSelect(having);
		
		//search through from items and joins for sub-select
		FromItem from=ps.getFromItem();

		if(from instanceof SubSelect){
			SubSelect sub=(SubSelect) from;
			sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
		}
		else if (from instanceof SubJoin){
			SubJoin subj=(SubJoin) from;
			FromItem left=subj.getLeft();
			if(left instanceof SubSelect){
				SubSelect sub=(SubSelect) left;
				sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
			}
			FromItem right=subj.getJoin().getRightItem();
			if(right instanceof SubSelect){
				SubSelect sub=(SubSelect) right;
				sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
			}
		}

		
		@SuppressWarnings("unchecked")
		List<Join> jlist=ps.getJoins();
		if(jlist!=null){
			for (Join j:jlist){
				from=j.getRightItem();
				if(from instanceof SubSelect){
					SubSelect sub=(SubSelect) from;
					sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
				}
				else if (from instanceof SubJoin){
					SubJoin subj=(SubJoin) from;
					FromItem left=subj.getLeft();
					if(left instanceof SubSelect){
						SubSelect sub=(SubSelect) left;
						sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
					}
					FromItem right=subj.getJoin().getRightItem();
					if(right instanceof SubSelect){
						SubSelect sub=(SubSelect) right;
						sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
					}
				}

         //after DNF normalization there should not be any onExpression
         //so we just ignore it
			}
		}
		
		Expression where=ps.getWhere();
		if(where!=null){
			//first do this process for all possible sub-queries
			traverseExpressionAndUnionTransformSubSelect(where); 
			//OR into UNION
			return decomposeWhereInDNF(ps);
		}
		else
		return ps;		
	}
	
//	private static void UnionTransformFromItem(FromItem from){
//		if(from instanceof SubSelect){
//			SubSelect sub=(SubSelect) from;
//			sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
//		}
//		else if (from instanceof SubJoin){
//			SubJoin sub=(SubJoin) from;
//			FromItem left=sub.getLeft();
//			UnionTransformFromItem(left);
//			FromItem right=sub.getJoin().getRightItem();
//			UnionTransformFromItem(right);
//		}
//	}

	/**
	 * traverse an expression and DNF normalize any potential sub-select
	 * @param exp
	 */
	private static void traverseExpressionAndUnionTransformSubSelect(Expression exp){
		if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			traverseExpressionAndUnionTransformSubSelect(bexp.getLeftExpression());
			traverseExpressionAndUnionTransformSubSelect(bexp.getRightExpression());
		}
		else if (exp instanceof IsNullExpression){
			Expression left=((IsNullExpression) exp).getLeftExpression();
			traverseExpressionAndUnionTransformSubSelect(left);
		}
		else if (exp instanceof ExistsExpression){
			SubSelect sub=(SubSelect) ((ExistsExpression) exp).getRightExpression();
			sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			traverseExpressionAndUnionTransformSubSelect(inexp.getLeftExpression());
			ItemsList ilist=inexp.getItemsList();
			if(ilist instanceof SubSelect){
				SubSelect sub=(SubSelect) ilist;
				sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
			} 				
		}
		else if (exp instanceof SubSelect){			
			SubSelect sub=(SubSelect) exp;
			sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));		
		}
		else if (exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
		}
		else if (exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			sub.setSelectBody(UnionTransformSelectBody(sub.getSelectBody()));
		}
		else if (exp instanceof Parenthesis)	{
			traverseExpressionAndUnionTransformSubSelect(((Parenthesis) exp).getExpression());
		}
		else if (exp instanceof InverseExpression){
			traverseExpressionAndUnionTransformSubSelect(((InverseExpression) exp).getExpression());
		}

	}

	private static SelectBody decomposeWhereInDNF(PlainSelect input){
		Expression where=input.getWhere();
		DisjunctiveNormalForm dnf;
		if(QueryToolBox.awarer!=null)
		dnf=DisjunctiveNormalForm.DNFDecomposition(where,QueryToolBox.awarer);
		else
		dnf=DisjunctiveNormalForm.DNFDecomposition(where);

		int size=dnf.getDNFSize();
		if(size>1){
			List<PlainSelect> pslist=new ArrayList<PlainSelect>();
			HashSet<BooleanNormalClause> content=dnf.getContent();
			for (BooleanNormalClause clause: content){
				PlainSelect copy=QueryToolBox.copyPlainSelect(input);
				copy.setWhere(clause.reformExpression());
				pslist.add(copy);
			}
			Union u=new Union();
			u.setPlainSelects(pslist);
			return u;
		}
		else
			return input;		
	}
	
	public static SelectBody UnionTransformSelectBody(SelectBody body){
		//do DNF first
		DNFNormalizer.DNFNormalizeSelectBody(body);
        //do expression regularize secondly
        body=ExpressionRegularizer.normalizeExpInSelectBody(body);
        //then do DNF again
        DNFNormalizer.DNFNormalizeSelectBody(body);
        
		if(body instanceof PlainSelect){			
			return UnionTransformPlainSelect((PlainSelect) body);
		}
		else {
			Union u=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			List<PlainSelect> newplist=new ArrayList<PlainSelect>();
			for (PlainSelect ps:plist){
				SelectBody b=UnionTransformPlainSelect(ps);
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

}
