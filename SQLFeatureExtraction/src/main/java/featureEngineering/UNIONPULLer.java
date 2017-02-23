package featureEngineering;

import java.util.ArrayList;
import java.util.List;

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
 * This puller does not pull UNION out from aggregation
 * a query whose DNF UNION has been "pulled-up"
 * but this pull up stops by sub-query boundaries 
 * input: any Select
 * @author tingxie
 *
 */
public class UNIONPULLer{

	private static Expression UnionPullUpFromFromItem(FromItem from,boolean pullupAGG,PlainSelect ps,Expression mergedExp){
		if(from instanceof SubSelect){
			SubSelect sub=(SubSelect) from;
			sub.setSelectBody(UnionPullUpFromSelectBody(sub.getSelectBody(),pullupAGG));
		}
		else if (from instanceof SubJoin){
			SubJoin sub=(SubJoin) from;
			FromItem left=sub.getLeft();
			mergedExp=UnionPullUpFromFromItem(left,pullupAGG,ps,mergedExp);
			Join j=sub.getJoin();
			FromItem right=j.getRightItem();
			mergedExp=UnionPullUpFromFromItem(right,pullupAGG,ps,mergedExp);
			
			Expression onExp=j.getOnExpression();
			if(onExp!=null){
				if(mergedExp==null)
					mergedExp=onExp;
				else
					mergedExp=new AndExpression(mergedExp,onExp);
				j.setOnExpression(null);
			}
			//take using into consideration
			@SuppressWarnings("unchecked")
			List<Column> clist=j.getUsingColumns();
			if(clist!=null){
				Expression using=QueryToolBox.parseUsing(clist, j, ps);
				if (mergedExp==null)
					mergedExp=using;
				else
					mergedExp=new AndExpression(mergedExp,using);
				j.setUsingColumns(null);
			}
		}
		
		return mergedExp;
	}
	
	private static SelectBody UnionPullUpFromPlainSelect(PlainSelect ps,boolean pullupAGG){

		Expression mergedExp=null;
		List<Join> myjlist=new ArrayList<Join>();
		//search through from items and joins for sub-select
		FromItem from=ps.getFromItem();
		mergedExp=UnionPullUpFromFromItem(from,pullupAGG,ps,mergedExp);
		Join join=new Join();
		join.setRightItem(from);
		myjlist.add(join);

		@SuppressWarnings("unchecked")
		List<Join> jlist=ps.getJoins();
		if(jlist!=null){
			for (Join j:jlist){	
				from=j.getRightItem();
				mergedExp=UnionPullUpFromFromItem(from,pullupAGG,ps,mergedExp);
				myjlist.add(j);
				Expression onExp=j.getOnExpression();
				if(onExp!=null){
					if(mergedExp==null)
						mergedExp=onExp;
					else
						mergedExp=new AndExpression(mergedExp,onExp);
					j.setOnExpression(null);
				}
				//take using into consideration
				@SuppressWarnings("unchecked")
				List<Column> clist=j.getUsingColumns();
				if(clist!=null){
					Expression using=QueryToolBox.parseUsing(clist, j, ps);
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
			if(mergedExp==null)
				mergedExp=where;
			else
				mergedExp=new AndExpression(mergedExp,where);
		}
		
		//regularize any potential sub-query in expression
		traverseExpressionAndUnionPullUpSubSelect(mergedExp,pullupAGG);
		//update where clause
		ps.setWhere(mergedExp);

		//if pullup AGG or this query does not contain AGG
		if(pullupAGG||(!pullupAGG&&!QueryToolBox.ifContainAggregate(ps))){
			//analyze tables and sub-queries
			List<List<Join>> jjlist=new ArrayList<List<Join>>();
			jjlist.add(new ArrayList<Join>());
			for (Join j: myjlist){
				FromItem f=j.getRightItem();
				if(f instanceof SubSelect){
					SubSelect sub=(SubSelect) f;
					SelectBody body=sub.getSelectBody();
					if(body instanceof PlainSelect){
						for (List<Join> list:jjlist){
							list.add(j);
						}
					}
					else {
						//apply distributive law
						Union u=(Union) body;
						@SuppressWarnings("unchecked")
						List<PlainSelect> pslist=u.getPlainSelects();
						List<List<Join>> newjjlist=new ArrayList<List<Join>>();
						for (List<Join> list:jjlist){
							for(PlainSelect select:pslist){
								List<Join> newjlist=new ArrayList<Join>();
								SubSelect newsub=new SubSelect();
								newsub.setSelectBody(select);
								Join newjoin=QueryToolBox.copyJoin(j);
								newjoin.setRightItem(newsub);

								newjlist.addAll(list);
								newjlist.add(newjoin);
								newjjlist.add(newjlist);
							}
						}				
						//update fflist
						jjlist=newjjlist;
					}
				}
				//if it is just a table
				else {
					for (List<Join> list:jjlist) {
						list.add(j); 
					}
				}
			}

			//if UNION happens
			if(jjlist.size()>1){
				Union u=new Union();
				List<PlainSelect> pslist=new ArrayList<PlainSelect>();
				for (List<Join> list: jjlist){	
					PlainSelect newps=QueryToolBox.copyPlainSelect(ps);
					//reset the from items		
					newps.setJoins(null);
					newps.setFromItem(list.get(0).getRightItem());					
					if(list.size()>1)
						newps.setJoins(list.subList(1, list.size()));
					pslist.add(newps);
				}
				u.setPlainSelects(pslist);
				return u;
			}
			else{
				return ps;
			}
		}
		else {
			return ps;
		}

	}

	/**
	 * traverse an expression and DNF normalize any potential sub-select
	 * @param exp
	 */
	private static void traverseExpressionAndUnionPullUpSubSelect(Expression exp,boolean pullUpAGG){
		if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			traverseExpressionAndUnionPullUpSubSelect(bexp.getLeftExpression(),pullUpAGG);
			traverseExpressionAndUnionPullUpSubSelect(bexp.getRightExpression(),pullUpAGG);
		}
		else if (exp instanceof IsNullExpression){
			Expression left=((IsNullExpression) exp).getLeftExpression();
			traverseExpressionAndUnionPullUpSubSelect(left,pullUpAGG);
		}
		else if (exp instanceof ExistsExpression){
			SubSelect sub=(SubSelect) ((ExistsExpression) exp).getRightExpression();
			sub.setSelectBody(UnionPullUpFromSelectBody(sub.getSelectBody(),pullUpAGG));			
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			traverseExpressionAndUnionPullUpSubSelect(inexp.getLeftExpression(),pullUpAGG);
			ItemsList ilist=inexp.getItemsList();
			if(ilist instanceof SubSelect){
				SubSelect sub=(SubSelect) ilist;
				sub.setSelectBody(UnionPullUpFromSelectBody(sub.getSelectBody(),pullUpAGG));
			} 				
		}
		else if (exp instanceof SubSelect){			
			SubSelect sub=(SubSelect) exp;
			sub.setSelectBody(UnionPullUpFromSelectBody(sub.getSelectBody(),pullUpAGG));			
		}
		else if (exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			sub.setSelectBody(UnionPullUpFromSelectBody(sub.getSelectBody(),pullUpAGG));
		}
		else if (exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			sub.setSelectBody(UnionPullUpFromSelectBody(sub.getSelectBody(),pullUpAGG));
		}
		else if (exp instanceof Parenthesis)	{
			traverseExpressionAndUnionPullUpSubSelect(((Parenthesis) exp).getExpression(),pullUpAGG);
		}
		else if (exp instanceof InverseExpression){
			traverseExpressionAndUnionPullUpSubSelect(((InverseExpression) exp).getExpression(),pullUpAGG);
		}

	}
	public static SelectBody UnionPullUpFromSelectBody(SelectBody body,boolean pullupAGG){
		if(body instanceof PlainSelect){
			return UnionPullUpFromPlainSelect((PlainSelect) body,pullupAGG);
		}
		else {
			Union u=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			List<PlainSelect> newplist=new ArrayList<PlainSelect>();
			for (PlainSelect ps:plist){
				SelectBody b=UnionPullUpFromPlainSelect(ps,pullupAGG);
				if(b instanceof Union){
					Union uu=(Union) b;
					@SuppressWarnings("unchecked")
					List<PlainSelect> pslist=uu.getPlainSelects();
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
