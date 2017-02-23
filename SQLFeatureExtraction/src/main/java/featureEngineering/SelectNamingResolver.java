package featureEngineering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;

import com.google.common.collect.HashMultiset;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;
import net.sf.jsqlparser.statement.select.WithItem;

/**
 * this class resolves all naming problems in the query including aliases removal
 * currently it does not ensure every column has its table name because this functionality
 * requires schema info, but we assume no schema info here for simplicity
 * @author tingxie
 *
 */
public class SelectNamingResolver{

	//records fromItem's realm,global
	public IdentityHashMap<SelectBody,SelectBodyNamingResolver> selectBodyBelongMap;
	//record alias mapping, global
	public  HashMap<String,Object> commonregister;
	private Select select;
	private boolean simplified;
	//canonical names for select body,global
	public HashMap<SelectBody, Table> CanonicalNames;
	public SelectBody transformedSelectBody;
	public static HashMap<String,HashMultiset<String>> schemaMap=new HashMap<String,HashMultiset<String>>();
	public static HashMap<String,HashMultiset<String>> antiSchemaMap=new HashMap<String,HashMultiset<String>>();
	public static HashSet<Integer> indices=new HashSet<Integer>();

	public SelectNamingResolver(Select input,boolean simplified){
		selectBodyBelongMap=new IdentityHashMap<SelectBody,SelectBodyNamingResolver>();
		commonregister =new HashMap<String,Object>();
		CanonicalNames=new HashMap<SelectBody, Table>();
		this.select=input;
		this.simplified=simplified;
	}

	public boolean isSimplified(){
		return this.simplified;
	}

	//	/**
	//	 * check if two expressions are across selects
	//	 * @return
	//	 */
	//	public boolean ifAcrossSelect(Expression left,Expression right){
	//		PlainSelectNamingResolver lmappedobj=searchThroughExpressionAndFindMapping(left);
	//		PlainSelectNamingResolver rmappedobj=searchThroughExpressionAndFindMapping(right);
	//		//if one mapped but one does not, then they are across select
	//		if (lmappedobj==null&&rmappedobj==null)
	//			return false;
	//		else if (lmappedobj==null)
	//			return true;
	//		else if (rmappedobj==null)
	//			return true;
	//		else if (lmappedobj!=rmappedobj)
	//			return true;
	//		else
	//			return false;
	//	}
	//	
	//	public PlainSelectNamingResolver searchThroughExpressionAndFindMapping(Expression exp){
	//		PlainSelectNamingResolver obj=objectBelongMap.get(exp);
	//		if(obj!=null)
	//			return obj;
	//
	//		if (exp instanceof BinaryExpression){
	//			BinaryExpression bexp=(BinaryExpression) exp;		  
	//			Expression left=bexp.getLeftExpression();
	//			Expression right=bexp.getRightExpression();
	//			obj=searchThroughExpressionAndFindMapping(left);
	//			if(obj!=null)
	//				return obj;
	//			
	//				obj=searchThroughExpressionAndFindMapping(right);
	//				if(obj!=null)
	//					return obj;
	//		}
	//		else if (exp instanceof Function){
	//			Function f=(Function) exp;
	//			ExpressionList explist=f.getParameters();
	//			if (explist!=null){
	//				List<Expression> elist= f.getParameters().getExpressions();	
	//				for (Expression e:elist){
	//					obj=searchThroughExpressionAndFindMapping(e);
	//					if(obj!=null)
	//						return obj;
	//				}
	//			}
	//		}
	//		else if (exp instanceof Parenthesis){
	//			Parenthesis p=(Parenthesis) exp;
	//			obj=searchThroughExpressionAndFindMapping(p.getExpression());
	//			if(obj!=null)
	//				return obj;
	//		}
	//		else if (exp instanceof InverseExpression){
	//			InverseExpression p=(InverseExpression) exp;
	//			obj=searchThroughExpressionAndFindMapping(p.getExpression());
	//			if(obj!=null)
	//				return obj;
	//		}
	//		
	//		//for all other cases, just return null		
	//		return null;
	//	}



	/**
	 * replace all aliases with its referred Structure in the query
	 * (1) alias in FromItem
	 * (1) alias in SelectItem
	 * (1) alias in Boolean Formula
	 */		
	public SelectBody aliasReplaceSelect(){
		if(this.transformedSelectBody==null){
			//deal with With statement
			@SuppressWarnings("unchecked")
			List<WithItem> withlist= select.getWithItemsList();
			List<WithItem> newwithlist= new ArrayList<WithItem>();
			HashMap<String,Object> addon=new HashMap<String,Object>();
			if (withlist!=null){
				for (WithItem witem: withlist){
					WithItem newwitem=new WithItem();
					@SuppressWarnings("unchecked")
					List<SelectExpressionItem> wlist=witem.getWithItemList();
					if (wlist!=null){
						for (SelectExpressionItem sitem: wlist){
							//register alias of select items
							String alias=sitem.getAlias().toUpperCase();
							sitem.setAlias(alias);
							sitem.setExpression(this.replaceExpressionWithUpperCase(sitem.getExpression()));
							if (alias!=null)
								commonregister.put(alias, sitem.getExpression());
						}
					}

					SelectBody b=witem.getSelectBody();
					this.replaceSelectBodyWithUppercase(b);
					SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(b,this,addon);
					addon.putAll(resolver.aliasRegister);

					//alias replace with statement first
					SelectBody newbody=resolver.aliasReplaceSelectBody();
					//register alias mapping and selectbody mapping
					String wname=witem.getName().toUpperCase();
					witem.setName(wname);
					this.commonregister.put(wname, newbody);				
					newwitem.setName(witem.getName());
					newwitem.setWithItemList(witem.getWithItemList());
					newwitem.setSelectBody(newbody);
					newwithlist.add(newwitem);
				}
			}

			//deal with its own select body
			SelectBody body=select.getSelectBody();
			this.replaceSelectBodyWithUppercase(body);
			SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(body,this,addon);
			SelectBody newbody=resolver.aliasReplaceSelectBody();			
			this.transformedSelectBody=newbody;		
		}		
		return this.transformedSelectBody;
	}

	private Expression replaceExpressionWithUpperCase(Expression exp){
		if(exp instanceof Function){
			Function f=(Function) exp;
			f.setName(f.getName().toUpperCase());
			if(f.getParameters()!=null){
				ExpressionList elist=f.getParameters();
				@SuppressWarnings("unchecked")
				List<Expression> explist=elist.getExpressions();
				List<Expression> newlist=new ArrayList<Expression>();
				for (Expression e: explist){
					newlist.add(replaceExpressionWithUpperCase(e));
				}
				elist.setExpressions(newlist);
			}
			return f;
		}
		else if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;			
			Expression left=replaceExpressionWithUpperCase(bexp.getLeftExpression());
			Expression right=replaceExpressionWithUpperCase(bexp.getRightExpression());
			bexp.setLeftExpression(left);
			bexp.setRightExpression(right);
			return bexp;
		}
		else if (exp instanceof Column){
			Column c=(Column) exp;
			Table t=c.getTable();
			if(t!=null&&t.getName()!=null){
				t.setName(t.getName().toUpperCase());
			}			
			c.setColumnName(c.getColumnName().toUpperCase());
			return c;
		}
		else if (exp instanceof IsNullExpression){
			IsNullExpression isnull=(IsNullExpression) exp;
			isnull.setLeftExpression(replaceExpressionWithUpperCase(isnull.getLeftExpression()));
			return isnull;
		}
		else if(exp instanceof Parenthesis){
			Parenthesis p=(Parenthesis) exp;
			p.setExpression(replaceExpressionWithUpperCase(p.getExpression()));
			return p;
		}
		else if (exp instanceof InverseExpression){
			InverseExpression p=(InverseExpression) exp;
			p.setExpression(replaceExpressionWithUpperCase(p.getExpression()));
			return p;
		}
		else if (exp instanceof Between){
			Between between=(Between) exp;
			Expression left=replaceExpressionWithUpperCase(between.getLeftExpression());
			Expression start=replaceExpressionWithUpperCase(between.getBetweenExpressionStart());
			Expression end=replaceExpressionWithUpperCase(between.getBetweenExpressionEnd());
			between.setBetweenExpressionStart(start);
			between.setBetweenExpressionEnd(end);
			between.setLeftExpression(left);
			return between;
		}
		else if (exp instanceof CaseExpression){
			CaseExpression caseexp=(CaseExpression) exp;
			Expression elseexpression=caseexp.getElseExpression();
			if(elseexpression!=null)
				caseexp.setElseExpression(replaceExpressionWithUpperCase(elseexpression));

			Expression switchexp=caseexp.getSwitchExpression();
			if(switchexp!=null)
				caseexp.setSwitchExpression(replaceExpressionWithUpperCase(switchexp));

			@SuppressWarnings("unchecked")
			List<WhenClause> wlist=caseexp.getWhenClauses();
			for (WhenClause wc:wlist){
				wc.setWhenExpression(replaceExpressionWithUpperCase(wc.getWhenExpression()));
				wc.setThenExpression(replaceExpressionWithUpperCase(wc.getThenExpression()));
			}
			return caseexp;
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			inexp.setLeftExpression(replaceExpressionWithUpperCase(inexp.getLeftExpression()));
			ItemsList ilist=inexp.getItemsList();
			if(ilist instanceof SubSelect){
				SelectBody body=((SubSelect) ilist).getSelectBody();
				this.replaceSelectBodyWithUppercase(body);
			}				
			return inexp;
		}
		else if (exp instanceof LikeExpression){
			LikeExpression like=(LikeExpression) exp;
			like.setLeftExpression(replaceExpressionWithUpperCase(like.getLeftExpression()));
			like.setRightExpression(replaceExpressionWithUpperCase(like.getRightExpression()));
			return like;
		}
		else if (exp instanceof ExistsExpression){
			ExistsExpression exists=(ExistsExpression) exp;
			exists.setRightExpression(this.replaceExpressionWithUpperCase(exists.getRightExpression()));
			return exists;
		}
		else if (exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			SelectBody body=sub.getSelectBody();
			this.replaceSelectBodyWithUppercase(body);
			return any;
		}
		else if (exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			SelectBody body=sub.getSelectBody();
			this.replaceSelectBodyWithUppercase(body);
			return all;
		}
		else if (exp instanceof SubSelect){
			SubSelect sub=(SubSelect) exp;
			this.replaceSelectBodyWithUppercase(sub.getSelectBody());
			return sub;
		}
		else return exp;
	}

	private FromItem replaceFromItemWithUpperCase(FromItem from){
		if(from.getAlias()!=null)
			from.setAlias(from.getAlias().toUpperCase());

		if(from instanceof Table){
			Table t=(Table) from;			
			t.setName(t.getName().toUpperCase());
			return t;
		}
		else if(from instanceof SubSelect){
			SubSelect sub=(SubSelect) from;
			SelectBody body=sub.getSelectBody();
			this.replaceSelectBodyWithUppercase(body);
			return sub;
		}
		else if(from instanceof SubJoin){
			SubJoin subj=(SubJoin) from;
			subj.setLeft(replaceFromItemWithUpperCase(subj.getLeft()));
			subj.setJoin(replaceJoinWithUpperCase(subj.getJoin()));
			return subj;
		}
		else return from;
	}

	private Join replaceJoinWithUpperCase(Join j){
		if(j.getOnExpression()!=null)
			j.setOnExpression(replaceExpressionWithUpperCase(j.getOnExpression()));
		j.setRightItem(replaceFromItemWithUpperCase(j.getRightItem()));
		if(j.getUsingColumns()!=null){
			@SuppressWarnings("unchecked")
			List<Expression> cols=j.getUsingColumns();
			ArrayList<Expression> newlist=new ArrayList<Expression>();
			for(Expression exp:cols){
				newlist.add(replaceExpressionWithUpperCase(exp));
			}
			j.setUsingColumns(newlist);
		}
		return j;
	}

	private void replacePlainSelectWithUppercase(PlainSelect ps){

		//scan through SelectItems
		@SuppressWarnings("unchecked")
		List<SelectItem> slist=ps.getSelectItems();
		for (SelectItem sitem:slist){
			if (sitem instanceof SelectExpressionItem){
				SelectExpressionItem seitem=(SelectExpressionItem) sitem;
				String alias=seitem.getAlias();
				if(alias!=null)
					seitem.setAlias(seitem.getAlias().toUpperCase());
				Expression e=replaceExpressionWithUpperCase(seitem.getExpression());
				seitem.setExpression(e);
			}
		}

		//go through where clause
		if(ps.getWhere()!=null)
			ps.setWhere(replaceExpressionWithUpperCase(ps.getWhere()));

		//scan through FromItem
		FromItem from=replaceFromItemWithUpperCase(ps.getFromItem());
		ps.setFromItem(from);
		//scan through joins
		@SuppressWarnings("unchecked")
		List<Join> joins=ps.getJoins();
		if (joins!=null){
			List<Join> newjoins=new ArrayList<Join>();
			for (Join j: joins){
				Join newj=replaceJoinWithUpperCase(j);
				newjoins.add(newj);
				from=newj.getRightItem();			
			}
			ps.setJoins(newjoins);
		}

		//go through having
		if(ps.getHaving()!=null)
			ps.setHaving(replaceExpressionWithUpperCase(ps.getHaving()));

		//go though group by
		if(ps.getGroupByColumnReferences()!=null){
			@SuppressWarnings("unchecked")
			List<Expression> gblist=ps.getGroupByColumnReferences();
			List<Expression> newlist=new ArrayList<Expression>();
			for(Expression exp:gblist){
				newlist.add(replaceExpressionWithUpperCase(exp));
			}
			ps.setGroupByColumnReferences(newlist);
		}

		//go through order by
		if(ps.getOrderByElements()!=null){
			@SuppressWarnings("unchecked")
			List<OrderByElement> oblist=ps.getOrderByElements();
			for(OrderByElement ele:oblist){
				ele.setExpression(replaceExpressionWithUpperCase(ele.getExpression()));
			}
		}
		//go through DISTINCT
		if(ps.getDistinct()!=null){
			Distinct distinct=ps.getDistinct();
			@SuppressWarnings("unchecked")
			List<Expression> list=distinct.getOnSelectItems();
			if(list!=null){
				List<Expression> newlist=new ArrayList<Expression>();
				for(Expression exp:list)
					newlist.add(replaceExpressionWithUpperCase(exp));
				distinct.setOnSelectItems(newlist);
			}
		}
	}

	private void replaceSelectBodyWithUppercase(SelectBody body){
		if(body instanceof PlainSelect){
			replacePlainSelectWithUppercase((PlainSelect) body);
		}
		else{
			Union u=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			for(PlainSelect ps:plist){
				replacePlainSelectWithUppercase(ps);
			}
		}
	}

	public static List<String> giveTableNamesInSelectBody(SelectBody body,Integer index){
		if(body instanceof PlainSelect){
			return giveTableNamesInPlainSelect((PlainSelect) body,index);
		}
		else{
			List<String> tablelist=new ArrayList<String>();
			Union u=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			for(PlainSelect ps: plist){
				tablelist.addAll(giveTableNamesInPlainSelect(ps,index));
			}
			return tablelist;
		}
	}

	private static List<String> giveTableNamesInPlainSelect(PlainSelect ps,Integer index){
		List<String> tablelist=new ArrayList<String>();		
		//for from items
		FromItem from=ps.getFromItem();
		tablelist.addAll(giveTableNamesInFromItem(from,tablelist,index));

		//for joins
		if(ps.getJoins()!=null){
			@SuppressWarnings("unchecked")
			List<Join> joins=ps.getJoins();
			for(Join j:joins)
				tablelist.addAll(giveTableNamesInJoin(j,tablelist,index));
		}

		//for select items
		@SuppressWarnings("unchecked")
		List<SelectItem> slist=ps.getSelectItems();		
		for (SelectItem sitem:slist){
			if(sitem instanceof SelectExpressionItem){
				SelectExpressionItem sexpitem=(SelectExpressionItem) sitem;
				sexpitem.setExpression(giveTableNamesInExpression(sexpitem.getExpression(), tablelist,index));
			}		
		}
		//for where clause
		if(ps.getWhere()!=null)
			ps.setWhere(giveTableNamesInExpression(ps.getWhere(), tablelist,index));
		//for having
		if(ps.getHaving()!=null)
			ps.setHaving(giveTableNamesInExpression(ps.getHaving(), tablelist,index));
		//for orderby 
		if(ps.getOrderByElements()!=null){
			List<OrderByElement> orderbylist=new ArrayList<OrderByElement>();
			for(OrderByElement ele: orderbylist){
				ele.setExpression(giveTableNamesInExpression(ele.getExpression(), tablelist,index));
			}
		}
		//for groupby
		if(ps.getGroupByColumnReferences()!=null){
			@SuppressWarnings("unchecked")
			List<Expression> explist=ps.getGroupByColumnReferences();
			List<Expression> newexplist=new ArrayList<Expression>();
			for(Expression exp:explist)
				newexplist.add(giveTableNamesInExpression(exp, tablelist,index));
			ps.setGroupByColumnReferences(newexplist);
		}
		return tablelist;

	}

	private static Expression giveTableNamesInExpression(Expression exp, List<String> tableset,Integer index){
		if(exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			Expression left=bexp.getLeftExpression();
			Expression right=bexp.getRightExpression();
			bexp.setLeftExpression(giveTableNamesInExpression(left, tableset,index));
			bexp.setRightExpression(giveTableNamesInExpression(right, tableset,index));
			return bexp;
		}
		else if(exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			giveTableNamesInSelectBody(sub.getSelectBody(),index);
			return all;
		}
		else if(exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			giveTableNamesInSelectBody(sub.getSelectBody(),index);
			return any;
		}
		else if(exp instanceof Between){
			Between between=(Between) exp;
			Expression left=between.getLeftExpression();
			Expression start=between.getBetweenExpressionStart();
			Expression end=between.getBetweenExpressionEnd();
			between.setLeftExpression(giveTableNamesInExpression(left, tableset,index));
			between.setBetweenExpressionStart(giveTableNamesInExpression(start, tableset,index));
			between.setBetweenExpressionEnd(giveTableNamesInExpression(end, tableset,index));
			return between;
		}
		else if (exp instanceof CaseExpression){
			CaseExpression caseexp=(CaseExpression) exp;
			Expression elseexp=caseexp.getElseExpression();
			if(elseexp!=null)
				caseexp.setElseExpression(giveTableNamesInExpression(caseexp.getElseExpression(), tableset,index));
			Expression sw=caseexp.getSwitchExpression();
			if(sw!=null)
				caseexp.setSwitchExpression(caseexp.getSwitchExpression());
			@SuppressWarnings("unchecked")
			List<WhenClause> wclist=caseexp.getWhenClauses();
			for(WhenClause wc:wclist){
				wc.setThenExpression(giveTableNamesInExpression(wc.getThenExpression(), tableset,index)); 
				wc.setWhenExpression(giveTableNamesInExpression(wc.getWhenExpression(), tableset,index));
			}       		 
			return caseexp;
		}
		else if (exp instanceof Column){
			Column c=(Column) exp;
			Table t=c.getTable();
			if(t==null||t.getName()==null){
				Table matchedTable=findBestMatchedTable(c,tableset);
				if(matchedTable!=null){
					c.setTable(matchedTable);
					SelectNamingResolver.indices.add(index);
				}
			}
			 
			return c;
		}
		else if(exp instanceof ExistsExpression){
			ExistsExpression exists=(ExistsExpression) exp;
			exists.setRightExpression(giveTableNamesInExpression(exists.getRightExpression(), tableset,index));
			return exists;
		}
		else if (exp instanceof Function){
			Function f=(Function) exp;
			ExpressionList explist= f.getParameters();
			if(explist!=null){
				@SuppressWarnings("unchecked")
				List<Expression> elist=explist.getExpressions();
				List<Expression> newelist=new ArrayList<Expression>();
				for(Expression e:elist)
					newelist.add(giveTableNamesInExpression(e, tableset,index));
				explist.setExpressions(newelist);
			}
			return f;
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			inexp.setLeftExpression(giveTableNamesInExpression(inexp.getLeftExpression(), tableset,index));
			ItemsList ilist=inexp.getItemsList();
			if(ilist instanceof SubSelect){
				SubSelect sub=(SubSelect) ilist;
				giveTableNamesInSelectBody(sub.getSelectBody(),index);
			}
			return inexp;
		}
		else if (exp instanceof InverseExpression){
			InverseExpression inv=(InverseExpression) exp;
			inv.setExpression(giveTableNamesInExpression(inv.getExpression(), tableset,index));
			return inv;
		}
		else if (exp instanceof Parenthesis){
			Parenthesis p=(Parenthesis) exp;
			p.setExpression(giveTableNamesInExpression(p.getExpression(), tableset,index));
			return p;
		}
		else if (exp instanceof SubSelect){
			SubSelect sub=(SubSelect) exp;
			giveTableNamesInSelectBody(sub.getSelectBody(),index);
			return sub;
		}
		else if (exp instanceof IsNullExpression){
			IsNullExpression isnull=(IsNullExpression) exp;
			isnull.setLeftExpression(giveTableNamesInExpression(isnull.getLeftExpression(), tableset,index));
			return isnull;
		}
		else if (exp instanceof LikeExpression){
			LikeExpression like=(LikeExpression) exp;
			like.setLeftExpression(giveTableNamesInExpression(like.getLeftExpression(), tableset,index));
			return like;
		}         
		else
			return exp;

	}

	private static List<String> giveTableNamesInJoin(Join j,List<String> tset,Integer index){
		List<String> tableset=new ArrayList<String>();
		List<String> tableset1=new ArrayList<String>();
		tableset1.addAll(tset);
		FromItem from=j.getRightItem();
		tableset.addAll(giveTableNamesInFromItem(from,tableset,index));
		tableset1.addAll(tableset);
        	
		if(j.getOnExpression()!=null){
			j.setOnExpression(giveTableNamesInExpression(j.getOnExpression(), tableset1,index));
		}
		if(j.getUsingColumns()!=null){
			@SuppressWarnings("unchecked")
			List<Expression> clist=j.getUsingColumns();
			List<Expression> newclist=new ArrayList<Expression>();
			for(Expression exp:clist){
				newclist.add(giveTableNamesInExpression(exp, tableset1,index));
			}
			j.setUsingColumns(newclist);
		}	
		
		return tableset;
	}

	private static List<String> giveTableNamesInFromItem(FromItem from,List<String> tset,Integer index){

		if(from instanceof SubSelect){
			SubSelect sub=(SubSelect) from;
			return giveTableNamesInSelectBody(sub.getSelectBody(),index);
		}
		else if (from instanceof SubJoin){
			List<String> tableset=new ArrayList<String>();
			List<String> tableset1=new ArrayList<String>();
			tableset1.addAll(tset);			
			SubJoin subj=(SubJoin) from;
			Join j=subj.getJoin();
			tableset.addAll(giveTableNamesInFromItem(subj.getLeft(),tableset1,index));
			tableset1.addAll(tableset);			
			tableset.addAll(giveTableNamesInJoin(j,tableset1,index));			
			return tableset;
		}
		else {
			Table t=(Table) from;
			List<String> tableset=new ArrayList<String>();
			tableset.add(t.getName());
			return tableset;		
		}
	}

	private static Table findBestMatchedTable(Column c,List<String> involvedTables){
		HashMultiset<String>  candidateTables=antiSchemaMap.get(c.getColumnName());
		if(candidateTables!=null){
			String bestmatch=null;
			int occurrence=Integer.MIN_VALUE;
			for (String tname: involvedTables){
				int occur= candidateTables.count(tname);
				   if(occur>0&&occur>occurrence){
					   occurrence=occur;
					   bestmatch=tname;
				   }					   													
			}
			Table t=new Table();
			t.setName(bestmatch);
			return t;
		}
		else return null;
	}
	//
	//	@Override
	//	public boolean ifAcrossContext(Expression left, Expression right) {
	//		return this.ifAcrossSelect(left, right);		
	//	}



}
