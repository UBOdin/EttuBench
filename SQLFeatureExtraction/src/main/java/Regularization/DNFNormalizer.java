package Regularization;



import java.util.List;

import booleanFormulaEntities.DisjunctiveNormalForm;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;

/**
 * This module collects all possible boolean formula and merge them into one and then normalize it into its DNF form
 * @author tingxie
 *
 */
public class DNFNormalizer{
	

	private static void DNFNormalizePlainSelect(PlainSelect ps){
		Expression mergedExp=null;
		//search through having
		Expression having=ps.getHaving();

		if(having!=null){
			mergedExp=having;
			ps.setHaving(null);
		}
		
		//search through from items and joins for sub-select
		FromItem from=ps.getFromItem();
		if(from instanceof SubSelect){
			SubSelect sub=(SubSelect) from;
			DNFNormalizeSelectBody(sub.getSelectBody());
		}
		else if (from instanceof SubJoin){
			SubJoin subj=(SubJoin) from;
			FromItem left=subj.getLeft();
			Join j=subj.getJoin();
			if(left instanceof SubSelect){
				SubSelect sub=(SubSelect) left;
				DNFNormalizeSelectBody(sub.getSelectBody());
			}
			
			FromItem right=j.getRightItem();
			if(right instanceof SubSelect){
				SubSelect sub=(SubSelect) right;
				DNFNormalizeSelectBody(sub.getSelectBody());
			}
			Expression onExp=j.getOnExpression();
			if(onExp!=null){
					mergedExp=onExp;
				    j.setOnExpression(null);
			}
			//take using into consideration
			List<Column> clist=j.getUsingColumns();
			if(clist!=null){
				Expression using=QueryToolBox.parseUsing(clist, j, left);
				if (mergedExp==null)
					mergedExp=using;
				else
					mergedExp=new AndExpression(mergedExp,using);
				j.setUsingColumns(null);
			}
			
		}
		List<Join> jlist=ps.getJoins();
		if(jlist!=null){
			for (Join j:jlist){
				from=j.getRightItem();
				if(from instanceof SubSelect){
					SubSelect sub=(SubSelect) from;
					DNFNormalizeSelectBody(sub.getSelectBody());
				}
				Expression onExp=j.getOnExpression();
				if(onExp!=null){
					if (mergedExp==null)
						mergedExp=onExp;
					else
						mergedExp=new AndExpression(mergedExp,onExp);
					j.setOnExpression(null);
				}
				//take using into consideration
				List<Column> clist=j.getUsingColumns();
				if(clist!=null){
					Expression using=QueryToolBox.parseUsing(clist, j, ps.getFromItem());
					if (mergedExp==null)
						mergedExp=using;
					else
						mergedExp=new AndExpression(mergedExp,using);
					j.setUsingColumns(null);
				}
			}
		}
		Expression where=ps.getWhere();
		if(where!=null){
			if (mergedExp==null)
				mergedExp=where;
			else
				mergedExp=new AndExpression(mergedExp,where);   
		}

		if(mergedExp!=null){
			Expression newwhere;
			DisjunctiveNormalForm form;
			if(QueryToolBox.awarer==null){				 
				form=DisjunctiveNormalForm.DNFDecomposition(mergedExp);				
				newwhere=form.getReformedExpression();
			}
			else{
				form=DisjunctiveNormalForm.DNFDecomposition(mergedExp,QueryToolBox.awarer);
				newwhere=form.getReformedExpression();
			}
			//normalize any potential sub-query
			traverseExpressionAndDNFSubSelect(newwhere);
			ps.setWhere(newwhere);
		}
	}

	/**
	 * traverse an expression and DNF normalize any potential sub-select
	 * @param exp
	 */
	private static void traverseExpressionAndDNFSubSelect(Expression exp){
		if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			traverseExpressionAndDNFSubSelect(bexp.getLeftExpression());
			traverseExpressionAndDNFSubSelect(bexp.getRightExpression());
		}
		else if (exp instanceof IsNullExpression){
			Expression left=((IsNullExpression) exp).getLeftExpression();
			traverseExpressionAndDNFSubSelect(left);
		}
		else if (exp instanceof ExistsExpression){
			SubSelect sub=(SubSelect) ((ExistsExpression) exp).getRightExpression();
			DNFNormalizeSelectBody(sub.getSelectBody());
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			traverseExpressionAndDNFSubSelect(inexp.getLeftExpression());
			ItemsList ilist=inexp.getItemsList();
			if(ilist instanceof SubSelect){
				SubSelect sub=(SubSelect) ilist;
				DNFNormalizeSelectBody(sub.getSelectBody());
			} 				
		}
		else if (exp instanceof SubSelect){			
			SubSelect sub=(SubSelect) exp;
			DNFNormalizeSelectBody(sub.getSelectBody());			
		}
		else if (exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			DNFNormalizeSelectBody(sub.getSelectBody());
		}
		else if (exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			DNFNormalizeSelectBody(sub.getSelectBody());
		}
		else if (exp instanceof Parenthesis)	{
			traverseExpressionAndDNFSubSelect(((Parenthesis) exp).getExpression());
		}
		else if (exp instanceof InverseExpression){
			traverseExpressionAndDNFSubSelect(((InverseExpression) exp).getExpression());
		}

	}

	public static void DNFNormalizeSelectBody(SelectBody body){
		if(body instanceof PlainSelect){
			DNFNormalizePlainSelect((PlainSelect) body);
		}
		else {
			Union u=(Union) body;
			List<PlainSelect> plist=u.getPlainSelects();
			for (PlainSelect ps:plist)
				DNFNormalizePlainSelect(ps);
		}
	}

}
