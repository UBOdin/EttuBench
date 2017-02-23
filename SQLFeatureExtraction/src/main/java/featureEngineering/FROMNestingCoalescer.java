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
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;

/**
 * a query with no nested sub-query in its FROM clause
 * exception is nested sub-query contains aggregation,in this case,
 * it will make sure the body of the sub-query is also FROMNestingCoalesced
 * input: any Select query with no SubJoin
 * @author tingxie
 *
 */
public class FROMNestingCoalescer {

	
	private static PlainSelect stickTopQueryToBottom(PlainSelect bottom,PlainSelect top){
		PlainSelect target=QueryToolBox.copyPlainSelect(bottom);
		//deal with select items
		if(top.getSelectItems().size()!=1||!(top.getSelectItems().get(0) instanceof AllColumns)){
			List<SelectItem> newslist=new ArrayList<SelectItem>();
			@SuppressWarnings("unchecked")
			List<SelectItem> toplist=top.getSelectItems();
			@SuppressWarnings("unchecked")
			List<SelectItem> targetlist=target.getSelectItems();
			
			for (SelectItem sitem:toplist){
				if(sitem instanceof AllTableColumns){
					AllTableColumns allt=(AllTableColumns) sitem;
					Table t=allt.getTable();
					//search through top query's select items
					for (SelectItem ssitem:targetlist){
						if(ssitem instanceof AllTableColumns&&ssitem.toString().equals(allt.toString())){
							newslist.add(ssitem);
							break;
						}
						else{
							SelectExpressionItem sexpitem=(SelectExpressionItem) ssitem;
							Expression exp=sexpitem.getExpression();
							if(exp instanceof Column){
								Column c=(Column) exp;
								Table ct=c.getTable();
								if(ct!=null&&ct.toString().equals(t.toString()))
									newslist.add(sexpitem);
							}
						}
					}
				}
				else {
				  newslist.add(sitem);	
				}
			}
			target.setSelectItems(newslist);
		}		
		
		//deal with where clause
		Expression topwhere=top.getWhere();
		Expression targetwhere=target.getWhere();
		if(topwhere!=null){
			if(targetwhere==null)
			target.setWhere(topwhere);
			else
				target.setWhere(new AndExpression(targetwhere,topwhere));
		}
		//deal with having clause
		Expression tophaving=top.getHaving();
		Expression targethaving=target.getHaving();
		if(tophaving!=null){
			if(targethaving==null)
			target.setHaving(tophaving);
			else
			target.setHaving(new AndExpression(targethaving,tophaving));
		}
		//deal with distinct
		if(top.getDistinct()!=null)		
				target.setDistinct(top.getDistinct());
		
		//deal with Limit
		if(top.getLimit()!=null){
			if(target.getLimit()==null)
				target.setLimit(top.getLimit());
			else{
				if(top.getLimit().getRowCount()<target.getLimit().getRowCount())
					target.setLimit(top.getLimit());
			}
		}
		//deal with TOP
		if(top.getTop()!=null){
			if(target.getTop()==null)
				target.setTop(top.getTop());
			else{
				if(top.getTop().getRowCount()<target.getTop().getRowCount())
					target.setTop(top.getTop());
			}
		}
		//deal with GROUPBY
		if(top.getGroupByColumnReferences()!=null)
				target.setGroupByColumnReferences(top.getGroupByColumnReferences());
		
		//deal with ORDER By
		if(top.getOrderByElements()!=null)
				target.setOrderByElements(top.getOrderByElements());
		
       return target;
	}
	
	/**
	 * major function that coalesce all sub-queries
	 * note that FromItem can be null
	 * except for aggregation-contained sub-query
	 * @param ps
	 * @return
	 */
	private static SelectBody FromNestingCoalescePlainSelect(PlainSelect ps){
		//ignore having
//		System.out.println("~~~~~~");
//		System.out.println("I am getting "+ps);
		//search through From Items
		FromItem from=ps.getFromItem();

		if(from instanceof SubSelect){
			SubSelect sub=(SubSelect) from;	
			
			boolean pass=true;
			SelectBody body=sub.getSelectBody();
			if(body instanceof PlainSelect){
				PlainSelect pps=(PlainSelect) body;
				//check whether it is an aggregation
				if(QueryToolBox.ifContainAggregate(pps)){
					//if this query has only this sub-select as FromItem
					//we can safely merge
					if(ps.getJoins()==null){
						SelectBody bottombody=FromNestingCoalescePlainSelect(pps);
						if(bottombody instanceof PlainSelect)
						return stickTopQueryToBottom((PlainSelect) bottombody,ps);
						else{
							Union u=(Union) bottombody;
							List<PlainSelect> newlist=new ArrayList<PlainSelect>();
							@SuppressWarnings("unchecked")
							List<PlainSelect> plist=u.getPlainSelects();
							for(PlainSelect bottomps: plist){
								newlist.add(stickTopQueryToBottom(bottomps,ps));
							}
							u.setPlainSelects(newlist);
							return u;
						}
					}
				 pass=false;
				//regularize the sub-select but do not coalesce
				sub.setSelectBody(FromNestingCoalescePlainSelect(pps));
				}
			}
			else{
				//TODO
				System.out.println("UNION happens in FromNesting step, should be pulled up first! "+ps);
				pass=false;
				Union u=(Union) body;
				@SuppressWarnings("unchecked")
				List<PlainSelect> plist=u.getPlainSelects();
				for(PlainSelect pps: plist){
				sub.setSelectBody(FromNestingCoalescePlainSelect(pps));					
				}
			}
			
			if(pass){			
				PlainSelect host=QueryToolBox.copyPlainSelect(ps);
				host.setFromItem(null);				
				//coalesce host side with sub side
				Join j=new Join();
				j.setRightItem(sub);
				SelectBody hostbody=coalesceSubSelectWithHostQuery(host,j);
				if (hostbody instanceof PlainSelect){
					return FromNestingCoalescePlainSelect((PlainSelect) hostbody);
				}
				else {
					Union u=(Union) hostbody;
					@SuppressWarnings("unchecked")
					List<PlainSelect> plist=u.getPlainSelects();
					List<PlainSelect> newplist=new ArrayList<PlainSelect>();

					for (PlainSelect p: plist){
						SelectBody b=FromNestingCoalescePlainSelect(p);
						if(b instanceof PlainSelect)
							newplist.add((PlainSelect) b);
						else {
							Union uu=(Union) b;
							@SuppressWarnings("unchecked")
							List<PlainSelect> pslist=uu.getPlainSelects();
							newplist.addAll(pslist);
						}
					}
					u.setPlainSelects(newplist);
					return u;
				}
			}
		}

		//search through joins
		@SuppressWarnings("unchecked")
		List<Join> joins=ps.getJoins();
		if(joins!=null){
			int index=-1;
			for (int i=0;i<joins.size();i++){
				Join j=joins.get(i);
				//normalize sub-queries in expressions
				Expression onExp=j.getOnExpression();
				if(onExp!=null)
					traverseExpressionAndFromNestingCoalesceSubSelect(onExp);

				from=j.getRightItem();
				if(from instanceof SubSelect){
					//check whether it contains an aggregation
					boolean pass=true;
					SubSelect sub=(SubSelect) from;
					SelectBody body=sub.getSelectBody();
					if(body instanceof PlainSelect){
						PlainSelect pps=(PlainSelect) body;
						if(QueryToolBox.ifContainAggregate(pps)){
						pass=false;
						//regularize the sub-select but do not coalesce
						sub.setSelectBody(FromNestingCoalescePlainSelect(pps));
						}
					}
					else{
						//TODO
						System.out.println("UNION happens in FromNesting step, should be pulled up first! "+ps);
						pass=false;
						Union u=(Union) body;
						@SuppressWarnings("unchecked")
						List<PlainSelect> plist=u.getPlainSelects();
						for(PlainSelect pps: plist){
							//regularize the sub-select but do not coalesce
						sub.setSelectBody(FromNestingCoalescePlainSelect(pps));					
						}
					}					
					if(pass){
					index=i;
					break;
					}
				}
			}

			if(index!=-1){
				PlainSelect host=QueryToolBox.copyPlainSelect(ps);
				Join targetjoin=(Join) host.getJoins().get(index);
				
				//put the join's on condition into where clause
				Expression onExp=targetjoin.getOnExpression();
				if(onExp!=null){
					if(host.getWhere()==null)
						host.setWhere(onExp);
					else
						host.setWhere(new AndExpression(host.getWhere(),onExp));
				}

				//remove this join
				host.getJoins().remove(index);

				if(host.getJoins().isEmpty())
					host.setJoins(null);

				SelectBody hostbody=coalesceSubSelectWithHostQuery(host,targetjoin);

				if (hostbody instanceof PlainSelect){
					return FromNestingCoalescePlainSelect((PlainSelect) hostbody);
				}
				else {
					Union u=(Union) hostbody;
					@SuppressWarnings("unchecked")
					List<PlainSelect> plist=u.getPlainSelects();
					List<PlainSelect> newplist=new ArrayList<PlainSelect>();

					for (PlainSelect p: plist){
						SelectBody b=FromNestingCoalescePlainSelect(p);
						if(b instanceof PlainSelect)
							newplist.add((PlainSelect) b);
						else {
							Union uu=(Union) b;
							@SuppressWarnings("unchecked")
							List<PlainSelect> pslist=uu.getPlainSelects();
							newplist.addAll(pslist);
						}
					}
					u.setPlainSelects(newplist);
					return u;
				}
			}
		}

		//search through where clause and normalized potential sub-queries
		Expression where=ps.getWhere();
		if(where!=null)
			traverseExpressionAndFromNestingCoalesceSubSelect(where);

//		System.out.println("I am returning "+ps);
		return ps;
	}

	/**
	 * 
	 * @param host
	 * @param sub
	 */
	private static SelectBody coalesceSubSelectWithHostQuery(PlainSelect host,Join j){

		SelectBody sub=((SubSelect) j.getRightItem()).getSelectBody();

		List<Join> sublist=new ArrayList<Join>();		
		List<PlainSelect> result=new ArrayList<PlainSelect>();


		if(sub instanceof PlainSelect){
			sublist.add(j);
		}
		else{
			Union u=(Union) sub;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			
			for (PlainSelect ps: plist){
				Join jj=QueryToolBox.copyJoin(j);
				SubSelect newsub=new SubSelect();
				newsub.setSelectBody(ps);
				jj.setRightItem(newsub);
				sublist.add(jj);
			}
		}

		//do coalesce
		for (Join myjoin: sublist){
			PlainSelect mergedhost=QueryToolBox.copyPlainSelect(host);
			PlainSelect mysub=(PlainSelect) ((SubSelect) myjoin.getRightItem()).getSelectBody();

			//add in where clauses
			if(mysub.getWhere()!=null){
				if(mergedhost.getWhere()!=null)
					mergedhost.setWhere(new AndExpression(mergedhost.getWhere(),mysub.getWhere()));
				else
					mergedhost.setWhere(mysub.getWhere());
			}
			//add in from items
			List<Join> subJoinList=new ArrayList<Join>();
			FromItem from=mysub.getFromItem();
			Join newj=QueryToolBox.copyJoin(myjoin);

			newj.setRightItem(from);
			subJoinList.add(newj);

			if(mysub.getJoins()!=null){
				@SuppressWarnings("unchecked")
				List<Join> joinlist=mysub.getJoins();			
				subJoinList.addAll(joinlist);
			}
			//merge join list from sub to host

			if (mergedhost.getFromItem()==null)	{
				if(subJoinList.get(0).getRightItem()==null){
					System.out.println("be aware, the sub-query ready for merge to host, its own from item is null."+j);
					return null;
				}
				mergedhost.setFromItem(subJoinList.get(0).getRightItem());
				subJoinList.remove(0);
			}
			//merge the rest
			if(!subJoinList.isEmpty()){
				if(mergedhost.getJoins()==null)
					mergedhost.setJoins(new ArrayList<Join>(subJoinList));
				else{
					@SuppressWarnings("unchecked")
					List<Join> mergedJoins=mergedhost.getJoins();
					mergedJoins.addAll(subJoinList);
					mergedhost.setJoins(mergedJoins);;
				}
			}

			//add in select items
			if(mergedhost.getSelectItems().get(0) instanceof AllColumns)
				mergedhost.setSelectItems(mysub.getSelectItems());

			//add in having
			if(mysub.getHaving()!=null){
				if(mergedhost.getWhere()==null)
					mergedhost.setWhere(mysub.getHaving());
				else
					mergedhost.setWhere(new AndExpression(mysub.getHaving(),mergedhost.getWhere()));
			}

			if(host.getFromItem()==null&&host.getJoins()==null){
				//addin top
				if(mergedhost.getTop()==null&&mysub.getTop()!=null)
					mergedhost.setTop(mysub.getTop());
				//add in limit
				if(mergedhost.getLimit()==null&&mysub.getLimit()!=null)
					mergedhost.setLimit(mysub.getLimit());
				//add in DISTINCT
				if(mergedhost.getDistinct()==null&&mysub.getDistinct()!=null)
					mergedhost.setDistinct(mysub.getDistinct());
				//add in groupby
				if(mergedhost.getGroupByColumnReferences()==null&&mysub.getGroupByColumnReferences()!=null)
					mergedhost.setGroupByColumnReferences(mysub.getGroupByColumnReferences());
				//add in order by
				if(mergedhost.getOrderByElements()==null&&mysub.getOrderByElements()!=null)
					mergedhost.setOrderByElements(mysub.getOrderByElements());
			}
			//add in mergdedhost
			result.add(mergedhost);
		}

		if(result.size()>1){	
			Union u=new Union();
			u.setPlainSelects(result);
			return u;
		}
		else{
			return result.get(0);
		}
	}

	/**
	 * traverse an expression and coalesce any potential sub-select
	 * @param exp
	 */
	private static void traverseExpressionAndFromNestingCoalesceSubSelect(Expression exp){
		if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			traverseExpressionAndFromNestingCoalesceSubSelect(bexp.getLeftExpression());
			traverseExpressionAndFromNestingCoalesceSubSelect(bexp.getRightExpression());
		}
		else if (exp instanceof IsNullExpression){
			Expression left=((IsNullExpression) exp).getLeftExpression();
			traverseExpressionAndFromNestingCoalesceSubSelect(left);
		}
		else if (exp instanceof ExistsExpression){
			SubSelect sub=(SubSelect) ((ExistsExpression) exp).getRightExpression();
			sub.setSelectBody(FromNestingCoalesceSelectBody(sub.getSelectBody()));
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			traverseExpressionAndFromNestingCoalesceSubSelect(inexp.getLeftExpression());
			ItemsList ilist=inexp.getItemsList();
			if(ilist instanceof SubSelect){
				SubSelect sub=(SubSelect) ilist;
				sub.setSelectBody(FromNestingCoalesceSelectBody(sub.getSelectBody()));
			} 				
		}
		else if (exp instanceof SubSelect){			
			SubSelect sub=(SubSelect) exp;
			sub.setSelectBody(FromNestingCoalesceSelectBody(sub.getSelectBody()));			
		}
		else if (exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			sub.setSelectBody(FromNestingCoalesceSelectBody(sub.getSelectBody()));
		}
		else if (exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			sub.setSelectBody(FromNestingCoalesceSelectBody(sub.getSelectBody()));
		}
		else if (exp instanceof Parenthesis)	{
			traverseExpressionAndFromNestingCoalesceSubSelect(((Parenthesis) exp).getExpression());
		}
		else if (exp instanceof InverseExpression){
			traverseExpressionAndFromNestingCoalesceSubSelect(((InverseExpression) exp).getExpression());
		}

	}

	public static SelectBody FromNestingCoalesceSelectBody(SelectBody body){
        //need to do Union pull up first
	   	 body=UNIONPULLer.UnionPullUpFromSelectBody(body, false);
	   	 //then start 
		if(body instanceof PlainSelect){
			return FromNestingCoalescePlainSelect((PlainSelect) body);
		}
		else {
			Union u=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			List<PlainSelect> newplist=new ArrayList<PlainSelect>();
			for (PlainSelect ps:plist){
				SelectBody b=FromNestingCoalescePlainSelect(ps);
				if(b instanceof PlainSelect)
					newplist.add((PlainSelect) b);	
				else{
					Union uu=(Union) b;
					@SuppressWarnings("unchecked")
					List<PlainSelect> pslist=uu.getPlainSelects();
					newplist.addAll(pslist);
				};	
			}
			u.setPlainSelects(newplist);
			return u;
		}		
	}

}
