package Regularization;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import booleanFormulaEntities.ExpressionContextAware;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.BooleanValue;
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
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllTableColumns;
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

/**
 * tool box for regularization
 * @author tingxie
 *
 */
public class QueryToolBox {
	public static final String[] SET_VALUES = new String[] { "max","maximum", "min","minimum","avg","average","count","sum"};
	public static final HashSet<String> AggName_SET = new HashSet<String>(Arrays.asList(SET_VALUES));
    public static ExpressionContextAware awarer;
    public static HashMap<HashSet<String>,Table> CanonicalNames=new HashMap<HashSet<String>,Table>();
	public static final Class<Expression>[] ColTypes=new Class[]{Function.class,Column.class,LongValue.class,DoubleValue.class,NullValue.class,StringValue.class,DateValue.class,TimeValue.class,TimestampValue.class,BooleanValue.class};
	public static final Class<Expression>[] TableTypes=new Class[]{Table.class,SelectBody.class};
	public static final Class<Expression>[] ConstantTypes=new Class[]{StringValue.class,LongValue.class,DoubleValue.class,NullValue.class,BooleanValue.class,DateValue.class,TimeValue.class,TimestampValue.class};
	
	public static Join copyJoin(Join j){
		Join join=new Join();
		join.setFull(j.isFull());
		join.setInner(j.isInner());
		join.setLeft(j.isLeft());
		join.setNatural(j.isNatural());
		join.setOnExpression(j.getOnExpression());
		join.setOuter(j.isOuter());
		join.setRight(j.isRight());
		join.setRightItem(j.getRightItem());
		join.setSimple(j.isSimple());
		if(j.getUsingColumns()!=null)
		join.setUsingColumns(new ArrayList<Column>(j.getUsingColumns()));
           return join;
	}
	
	/**
	 * it checks if an expression on the left side of Is NULL expression is evaluated to Null
	 * returns 1/-1/0 that represents Null/Not Null/Not sure
	 * @param exp
	 * @return
	 */
	public static int checkIfEvaluatedToNull(Expression exp){
		if (exp instanceof NullValue)
			return 1;
		else if (exp instanceof ExistsExpression||exp instanceof LongValue||exp instanceof DoubleValue||exp instanceof StringValue||exp instanceof TimeValue||exp instanceof TimestampValue||exp instanceof DateValue)
			return -1;
		else if (exp instanceof Column)
			return 0;
		else if (exp instanceof Function){
			Function f=(Function) exp;
			String fname=f.getName().toLowerCase();
			if (fname.equals("lower")||fname.equals("upper")){
				Expression inner=(Expression) f.getParameters().getExpressions().get(0);
				return checkIfEvaluatedToNull(inner);
			}
			else if (fname.equals("convert")){
				Expression inner=(Expression) f.getParameters().getExpressions().get(1);
				return checkIfEvaluatedToNull(inner);  
			}
			//for aggregation functions
			else if (QueryToolBox.AggName_SET.contains(fname)){
				ExpressionList elist=f.getParameters();
				if (elist!=null){
				Expression inner=(Expression) elist.getExpressions().get(0);
				return checkIfEvaluatedToNull(inner);
				}
				else 
				return 0;
			}
			//for any other cases just return 0
			else return 0;
		}
		else if (exp instanceof Parenthesis){
			Parenthesis p=(Parenthesis) exp;
			Expression e=p.getExpression();
			return checkIfEvaluatedToNull(e);
		}
		else if (exp instanceof CaseExpression){
			CaseExpression caseexp=(CaseExpression) exp;
			List<ContingentValue> cvlist=ExpressionRegularizer.translateCaseExpressionForCombination(caseexp);
			for (ContingentValue cv: cvlist){
				if (cv.conjClause.ifTautology()==1){
					return checkIfEvaluatedToNull(cv.value.getExpression());
				}
			}
			return 0;
		}
		else if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			Expression left=bexp.getLeftExpression();
			Expression right=bexp.getRightExpression();
			int lv=checkIfEvaluatedToNull(left);
			int rv=checkIfEvaluatedToNull(right);
			if (lv==1||rv==1) return 1;
			else if (lv==-1&&rv==-1) return -1;
			else return 0;
		}
		else if (exp instanceof InverseExpression){
			InverseExpression inv=(InverseExpression) exp;
			Expression e=inv.getExpression();
			return checkIfEvaluatedToNull(e);
		}
		//for other cases
		else return 0;
	}
	
	/**
	 * parse using expression
	 * @param clist
	 * @param j
	 * @param ps
	 * @return
	 */
	public static Expression parseUsing(List<Column> clist,Join j,FromItem leftSide){
		Expression using = null;
		Table leftT=null;
		
        if (leftSide!=null){
        	
		if (leftSide instanceof Table){
			leftT=new Table();
			leftT.setName(((Table) leftSide).getName());
	       
		}
		else if (leftSide instanceof SubSelect){
			leftT=new Table();			
		}
		else {
			leftT=new Table();	
		}

		Table rightT=null;
		FromItem f=j.getRightItem();
		
		if (f instanceof Table){
			rightT=new Table();
			rightT.setName(((Table) f).getName());
		}
		else if (f instanceof SubSelect){
			rightT=new Table();
		}
		else {
			rightT=new Table();
		}

		Column newLeft=new Column(leftT,clist.get(0).getColumnName());
		Column newRight=new Column(rightT,clist.get(0).getColumnName());
		
		using=new EqualsTo(newLeft,newRight);
		
		for (int i=1;i<clist.size();i++){
			newLeft=new Column(leftT,clist.get(i).getColumnName());
			newRight=new Column(rightT,clist.get(i).getColumnName());
			using=new AndExpression(using,new EqualsTo(newLeft,newRight));           	
		}
        }
		return using;
	}
	
	/**
	 * remove subjoin in plainselect
	 * @param ps
	 */
	public static void removeSubJoinForPlainSelect(PlainSelect ps){
		List<Join> newjlist=new ArrayList<Join>();
		FromItem from=ps.getFromItem();		
		newjlist.addAll(analyzeSubJoinInFromItem(from));
		List<Join> jlist=ps.getJoins();
		if(jlist!=null){
			for (Join j:jlist){
				newjlist.addAll(analyzeSubJoinInJoin(j));
			}
		}				
		ps.setFromItem(newjlist.get(0).getRightItem());	
		if(newjlist.size()>1){
		ps.setJoins(newjlist.subList(1, newjlist.size()));
		}
		else
		ps.setJoins(null);
	}
	
	public static ArrayList<Join> analyzeSubJoinInFromItem(FromItem from){
		ArrayList<Join> result=new ArrayList<Join>();
		if (from instanceof SubJoin){
			SubJoin subj=(SubJoin) from;
			FromItem left=subj.getLeft();
			Join right=subj.getJoin();
			Join j=new Join();
			 j.setSimple(true);
			 j.setRightItem(left);
			 result.add(j);
			 result.addAll(analyzeSubJoinInJoin(right));
		}
		else {
		 Join j=new Join();
		 j.setSimple(true);
		 j.setRightItem(from);
		 result.add(j);
		}
		return result;
	}
	
	public static ArrayList<Join> analyzeSubJoinInJoin(Join join){
		ArrayList<Join> result=new ArrayList<Join>();
		FromItem right=join.getRightItem();
		ArrayList<Join> rightresult =analyzeSubJoinInFromItem(right);
		join.setRightItem(rightresult.get(0).getRightItem());
		result.add(join);
		result.addAll(rightresult.subList(1, rightresult.size()));					
		return result;
	}
	/**
	 * removes subjoins from select body
	 * @param body
	 */
	public static void removeSubJoinForSelectBody(SelectBody body){
		if(body instanceof PlainSelect){
			removeSubJoinForPlainSelect((PlainSelect) body);
		}
		else
		{
			Union u=(Union) body;
			List<PlainSelect> plist=u.getPlainSelects();
			for (PlainSelect ps:plist)
				removeSubJoinForPlainSelect(ps);
		}
	}
	
	/**
	 * check if sub select contains aggregation or not
	 * @param sub
	 * @return
	 */
	public static boolean ifContainAggregate(PlainSelect ps){
		if (ps.getGroupByColumnReferences()!=null||ps.getLimit()!=null||ps.getTop()!=null||ps.getDistinct()!=null)
			return true;
		else {
			List<SelectItem> slist=ps.getSelectItems();
			for (SelectItem item: slist){
				if (item instanceof SelectExpressionItem){
					Expression e=((SelectExpressionItem) item).getExpression();
					if (e instanceof Function){
						if(ifAggregationFunction(e))
							return true;							
					}
				}
			}
			return false;
		}
	}
	

	public static Table traverseAndGetCanonicalTableName(SelectBody body,boolean skipNamingResolve){
		//regularize names in the select body first
		if(!skipNamingResolve){
		Select select=new Select();
		select.setSelectBody(body);
    	SelectNamingResolver resolver=new SelectNamingResolver(select,false);
    	select= resolver.aliasReplaceSelect();
    	body=select.getSelectBody();
		}
    	//after naming regularizing
		HashSet<String> tnames=traverseAndGetAllTables(body);
		Table ctable=CanonicalNames.get(tnames);
		if(ctable!=null)
			return ctable;
		else {
			//if only has one table
			if(tnames.size()==1){
				String s=tnames.iterator().next();
				return new Table(null,s);
			}
			else{
				//order these names
			List<String> slist=new ArrayList<String>();
			for(String s: tnames)
				slist.add(s);
			Collections.sort(slist);
			Table t=new Table(null,slist.toString());			
			CanonicalNames.put(tnames,t);
			return t;
			}
		}
	}

	public static HashSet<String> traverseAndGetAllTables(SelectBody body){
		HashSet<String> result=new HashSet<String>();
		if(body instanceof PlainSelect)
			return traversePlainSelectAndGetAllTables((PlainSelect) body);
		else {
			Union u=(Union) body;
			List<PlainSelect> plist=u.getPlainSelects();
			for(PlainSelect ps:plist)
				result.addAll(traversePlainSelectAndGetAllTables(ps));
			return result;
		}
	}
	
	public static HashSet<String> traversePlainSelectAndGetAllTables(PlainSelect ps){
		HashSet<String> result=new HashSet<String>();
		FromItem from=ps.getFromItem();
		if(from instanceof Table){
			result.add(from.toString());
		}
		else {
			SubSelect sub=(SubSelect) from;
			result.addAll(traverseAndGetAllTables(sub.getSelectBody()));
		}
		//check joins
		List<Join> jlist=ps.getJoins();
		if(jlist!=null){
			for(Join j: jlist){
				from=j.getRightItem();
				if(from instanceof Table){
					result.add(from.toString());
				}
				else {
					SubSelect sub=(SubSelect) from;
					result.addAll(traverseAndGetAllTables(sub.getSelectBody()));
				}
			}
		}
			return result;
	}
	
	/**
	 * check if the function is an aggregation funciton
	 * @param e
	 * @return
	 */
	public static boolean ifAggregationFunction(Expression e){
		if(e instanceof Function){
			Function f=(Function) e;
			ExpressionList elist=f.getParameters();
			String fname=f.getName().toLowerCase();
			if (AggName_SET.contains(fname)) return true;
			else if(elist!=null){
				List<Expression> explist=elist.getExpressions();
				for (Expression exp: explist){
					if (ifAggregationFunction(exp))
						return true;
				}
				return false;
			}
			else return false;
		}
		else 
			return false;
	}


	/**
	 * extract the first select item from plain select
	 * @param ps
	 * @return
	 */
	public static Expression extractFirstSelectItemFromPlainSelectForBinaryOperation(PlainSelect ps){
		List<SelectItem> slist=ps.getSelectItems();
		SelectItem sitem=slist.get(0);
		if(sitem instanceof SelectExpressionItem)
			return((SelectExpressionItem) sitem).getExpression();
		else if (sitem instanceof AllTableColumns){
			return new Column(((AllTableColumns) sitem).getTable(),"*");
		}
		else {
			FromItem from=ps.getFromItem();
			if(from instanceof SubSelect){
				PlainSelect select=(PlainSelect) ((SubSelect) from).getSelectBody();
				return extractFirstSelectItemFromPlainSelectForBinaryOperation(select);
			}
			else
			return new Column((Table)ps.getFromItem(),"*");
		}
	}
	
	/**
	 * need to make a deep copy of a plainselect
	 * @param input
	 * @return
	 */
	public static PlainSelect copyPlainSelect(PlainSelect input){
		PlainSelect result=new PlainSelect();
		result.setDistinct(input.getDistinct());
		result.setFromItem(input.getFromItem());
		if(input.getGroupByColumnReferences()!=null)
		result.setGroupByColumnReferences(new ArrayList<Column>(input.getGroupByColumnReferences()));
		result.setHaving(input.getHaving());
		result.setInto(input.getInto());
		if(input.getJoins()!=null)
		result.setJoins(new ArrayList<Join>(input.getJoins()));
		result.setLimit(input.getLimit());
		if(input.getOrderByElements()!=null)
		result.setOrderByElements(new ArrayList<OrderByElement>(input.getOrderByElements()));
		if(input.getSelectItems()!=null)
		result.setSelectItems(new ArrayList<SelectItem>(input.getSelectItems()));
		result.setTop(input.getTop());
		result.setWhere(input.getWhere());
		return result;
	}
	
	public static SelectBody copySelectBody(SelectBody body){
		if(body instanceof PlainSelect)
			return copyPlainSelect((PlainSelect) body);
		else{
			Union newu=new Union();
			Union u=(Union) body;
			List<PlainSelect> plist=u.getPlainSelects();
			List<PlainSelect> newplist=new ArrayList<PlainSelect>();
			for (PlainSelect ps:plist)
				newplist.add(copyPlainSelect(ps));
			
			newu.setPlainSelects(newplist);
			return newu;
		}
	}
	
	/**
	 * remove GROUPBY without Aggregate/ORDERBY/TOP/LIMIT/DISTINCT in Exists
	 */
	public static ExistsExpression cleanExists(ExistsExpression exists){
		SubSelect sub=(SubSelect) exists.getRightExpression();
		SelectBody body=sub.getSelectBody();
		if(body instanceof PlainSelect){
			PlainSelect ps=cleanPlainSelectInExists((PlainSelect) body);
			sub.setSelectBody(ps);
		}
		else {
			Union u=(Union) body;
			List<PlainSelect> plist=u.getPlainSelects();
			List<PlainSelect> newplist=new ArrayList<PlainSelect> ();
			for(PlainSelect ps: plist)
				newplist.add(cleanPlainSelectInExists(ps));
			u.setPlainSelects(newplist);
			u.setDistinct(false);
			u.setLimit(null);
			u.setOrderByElements(null);			
		}
		
		return exists;
	}
	
	/**
	 * remove GROUPBY without Aggregate/ORDERBY/TOP/LIMIT/DISTINCT in PlainSelect in Exists
	 */
	private static PlainSelect cleanPlainSelectInExists(PlainSelect ps){
		//make a copy of the input PlainSelect
		PlainSelect newps=copyPlainSelect(ps);

		if(newps.getOrderByElements()!=null){
			List<OrderByElement> orderbylist=newps.getOrderByElements();
			if(newps.getLimit()!=null&&newps.getLimit().getRowCount()==1&&orderbylist.size()==1){
				Expression orderby=orderbylist.get(0).getExpression();
				List<SelectItem> sitemlist=newps.getSelectItems();
				boolean found=false;
					for (SelectItem item: sitemlist){
						if(item instanceof SelectExpressionItem){
							SelectExpressionItem sexpitem=(SelectExpressionItem) item;
							Expression sitem=sexpitem.getExpression();
							if(sitem.toString().equals(orderby.toString())||(sexpitem.getAlias()!=null&&sexpitem.getAlias().equals(orderby.toString()))){
								Function f=new Function();
								f.setName("max");
								ExpressionList elist=new ExpressionList();
								List<Expression> explist=new ArrayList<Expression>();
								explist.add(sitem);
								elist.setExpressions(explist);
								f.setParameters(elist);
								sexpitem.setExpression(f);
								found=true;
							}
						}
					}
					if(found){
						newps.setOrderByElements(null);
						newps.setLimit(null);
					}
			}
			else if (newps.getTop()!=null&&newps.getTop().getRowCount()==1&&orderbylist.size()==1){
				Expression orderby=orderbylist.get(0).getExpression();
				List<SelectItem> sitemlist=newps.getSelectItems();
				boolean found=false;
					for (SelectItem item: sitemlist){
						if(item instanceof SelectExpressionItem){
							SelectExpressionItem sexpitem=(SelectExpressionItem) item;
							Expression sitem=sexpitem.getExpression();
							if(sitem.toString().equals(orderby.toString())||(sexpitem.getAlias()!=null&&sexpitem.getAlias().equals(orderby.toString()))){
								Function f=new Function();
								f.setName("max");
								ExpressionList elist=new ExpressionList();
								List<Expression> explist=new ArrayList<Expression>();
								explist.add(sitem);
								elist.setExpressions(explist);
								f.setParameters(elist);
								sexpitem.setExpression(f);
								found=true;
							}
						}
					}
					if(found){
						newps.setOrderByElements(null);
						newps.setLimit(null);
					}
			}
		}
		
		if(!ifContainAggregate(newps)){
			newps.setGroupByColumnReferences(null);
			newps.setDistinct(null);
			newps.setOrderByElements(null);
		}
		
		return newps;
	}
	
}
