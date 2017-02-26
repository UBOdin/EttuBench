package querySimilarityMetrics;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;
import net.sf.jsqlparser.statement.select.WithItem;
import toolsForMetrics.ExtendedColumn;
import toolsForMetrics.Global;
import toolsForMetrics.Schema;
import toolsForMetrics.SelectItemListParser;
import toolsForMetrics.Util;

/**
 * Query similarity metric Aouiche
 * @author tingxie
 *
 */
public class Aouiche {
	
	private static TreeSet<ExtendedColumn> columnList1 = new TreeSet<ExtendedColumn>();
	private static TreeSet<ExtendedColumn> columnList2 = new TreeSet<ExtendedColumn>();
	
	public static TreeSet<ExtendedColumn> getQuerySet(Statement stmt1) {
		columnList1 = new TreeSet<ExtendedColumn>();

		if (stmt1 instanceof Select) {
			Select s1=(Select) stmt1;
			
			List<WithItem> with1 = s1.getWithItemsList();
			if (with1 != null) {
				for (int i = 0; i < with1.size(); i++) {
					executeSelect(with1.get(i).getSelectBody(), 1);
				}
			}
			
			//Collect where and group by and nothing else
			executeSelect(s1.getSelectBody(), 1);
			
			//System.out.println(columnList1);
		} else {
			System.err.println(stmt1 + " is not a Select query");
		}
		
		return columnList1;
	}
	
	public static double getDistanceAsRatio(TreeSet<ExtendedColumn> stmt1, TreeSet<ExtendedColumn> stmt2) {

		TreeSet<ExtendedColumn> intersection = new TreeSet<ExtendedColumn>(stmt1);
		intersection.retainAll(stmt2);
			
			
		TreeSet<ExtendedColumn> union = new TreeSet<ExtendedColumn>();
		union.addAll(stmt1);
		union.addAll(stmt2);
			
		if (union.size() > 0) {
			return (double)intersection.size() / (double) union.size();
		} else {
			return 0;
		}
	}

	public static double getDistanceAsRatio(Statement stmt1, Statement stmt2) {
		columnList1 = new TreeSet<ExtendedColumn>();
		columnList2 = new TreeSet<ExtendedColumn>();

		if (stmt1 instanceof Select && stmt2 instanceof Select) {
			Select s1=(Select) stmt1;				
			Select s2=(Select) stmt2;
			
			//Collect where and group by and nothing else
			executeSelect(s1.getSelectBody(), 1);
			executeSelect(s2.getSelectBody(), 2);
			
			//System.out.println(columnList1);
			//System.out.println(columnList2);
			
//			HashSet<String> tempList1 = new HashSet<String>();
//			HashSet<String> tempList2 = new HashSet<String>();
//			
//			for(String c : columnList1) {
//				tempList1.add(c.toString());
//			}
//			
//			for(String c : columnList2) {
//				tempList2.add(c.toString());
//			}
			
			TreeSet<ExtendedColumn> intersection = new TreeSet<ExtendedColumn>(columnList1);
			intersection.retainAll(columnList2);
			
			
			TreeSet<ExtendedColumn> union = new TreeSet<ExtendedColumn>();
			union.addAll(columnList1);
			union.addAll(columnList2);
			
			if (union.size() > 0) {
				return (double)intersection.size() / (double) union.size();
			} else {
				return 0;
			}
		} else if (stmt1 instanceof WithItem){
			
			
		} else {
			if (stmt1 instanceof Select)
				System.err.println(stmt2 + " is not a Select query");
			if (stmt2 instanceof Select)
				System.err.println(stmt1 + " is not a Select query");
		}
		
		return 0;
	}
	
	public static double getDistanceInteger(Statement stmt1, Statement stmt2) {
		
		columnList1 = new TreeSet<ExtendedColumn>();
		columnList2 = new TreeSet<ExtendedColumn>();

		if ((stmt1 instanceof Select || stmt1 instanceof WithItem) && (stmt2 instanceof Select || stmt1 instanceof WithItem)) {
			
			Select s1=(Select) stmt1;
			
			List<WithItem> with1 = s1.getWithItemsList();
			if (with1 != null) {
				for (int i = 0; i < with1.size(); i++) {
					executeSelect(with1.get(i).getSelectBody(), 1);
				}
			}
			
			Select s2=(Select) stmt2;
			
			List<WithItem> with2 = s2.getWithItemsList();
			if (with2 != null) {
				for (int i = 0; i < with2.size(); i++) {
					executeSelect(with2.get(i).getSelectBody(), 2);
				}
			}
			
			
			//Collect where and group by and nothing else
			executeSelect(s1.getSelectBody(), 1);
			executeSelect(s2.getSelectBody(), 2);
			
			//System.out.println(columnList1);
			//System.out.println(columnList2);
			
			//Produces wrong result, override column to implement equal on table and column name
			
//			HashSet<String> tempList1 = new HashSet<String>();
//			HashSet<String> tempList2 = new HashSet<String>();
//			
//			for(String c : columnList1) {
//				tempList1.add(c.toString());
//			}
//			
//			for(String c : columnList2) {
//				tempList2.add(c.toString());
//			}
			
			TreeSet<ExtendedColumn> intersection = new TreeSet<ExtendedColumn>(columnList1);
			intersection.retainAll(columnList2);
			
			TreeSet<ExtendedColumn> union = new TreeSet<ExtendedColumn>();
			union.addAll(columnList1);
			union.addAll(columnList2);
			
			return (double) union.size() - intersection.size();
		} else {
			if (stmt1 instanceof Select)
				System.err.println(stmt2 + " is not a Select query");
			if (stmt2 instanceof Select)
				System.err.println(stmt1 + " is not a Select query");
		}
		
		return 0;
	}

	
	//it returns the top iterator after executing the selectbody
	private static void executeSelect(SelectBody body, int queryOrder){
		if (body instanceof PlainSelect){
			executePlainSelect((PlainSelect)body, queryOrder);
		}
		else if(body instanceof Union){
			//System.out.println("currently Union does not handle Distinct!");
			Union u=(Union)body;
			List<PlainSelect> list= u.getPlainSelects();
			//executePlainSelect(list.get(0));
			for (int i = 0; i < list.size(); i++) {
				executePlainSelect(list.get(i), queryOrder);
			}
		}
	}
	
	/**
	 *  specifies how to execute Select,major part
	 * @param s
	 */
	@SuppressWarnings("unchecked")
	private static void executePlainSelect(PlainSelect s, int queryOrder) {
		//Column c;
		List<Table> tables = new ArrayList<Table>();
		TreeSet<ExtendedColumn> collectiveColumns = new TreeSet<ExtendedColumn>();

		//	List<Schema> schemas = collectQueryItems(s);

		// do check based on their priority to nest iterators
		// 1.first check things from the FROM CLAUSE

		FromItem fromitem = s.getFromItem();
		
		if (fromitem != null) {
			List<Join> joinlist = s.getJoins();

			if (joinlist == null || joinlist.isEmpty()) {
				//consumeFromItem(fromitem, tables, schemas);
				consumeFromItem(fromitem, tables, queryOrder);
			}
			// if multiple tables
			else {
				//consumeFromItem(fromitem, tables, schemas);
				consumeFromItem(fromitem, tables, queryOrder);
				for (int i = 0; i < joinlist.size(); i++) {
					consumeFromItem(joinlist.get(i).getRightItem(), tables, queryOrder);
					
					Expression sss = joinlist.get(i).getOnExpression();
					
					//System.out.println(sss);
					if (sss != null) {
						// pop out the top iter
						SelectItemListParser.correct(sss, tables);
						//breaking selection operators with AND
						List<Expression> selects = Util.processSelect(sss);

						for (int j = 0; j < selects.size(); j++) {
							Schema selectSchema = Util.processExpression(selects.get(j));
							for (int k = 0; k < selectSchema.getValues().size(); k++) {
								collectiveColumns.add(new ExtendedColumn(selectSchema.getValues().get(k)));
							}
						}
					}
					
					List<Column> columns = joinlist.get(i).getUsingColumns();
					if (columns != null) {
						for (int j = 0; j < columns.size(); j++) {
							//System.out.println(columns.get(j).toString());
							Column column=(Column) columns.get(j);
							if (column.getTable()==null||column.getTable().getName()==null){				  					
								Column v=Global.getColumnFullName(column,tables);
								column.setTable(v.getTable());
								column.setColumnName(v.getColumnName());
							}
							else{				  
								String tablename = column.getTable().getName();
								Table t = new Table();
								t.setName(tablename.toUpperCase());
								column.setTable(t);
							}
							
							//System.out.println(column.toString());
							collectiveColumns.add(new ExtendedColumn(column));
						}
					}
				}
				//Collections.sort(this.scanList);
			}

		} else
			System.err.println("no from item found,please check");

		// 2.check where condition and do selection
		Expression where = s.getWhere();
		if (where != null) {
			// pop out the top iter
			SelectItemListParser.correct(where, tables);
			//breaking selection operators with AND
			List<Expression> selects = Util.processSelect(where);

			for (int i = 0; i < selects.size(); i++) {
				Schema selectSchema = Util.processExpression(selects.get(i));
				for(int j = 0; j < selectSchema.getValues().size(); j++) {
					collectiveColumns.add(new ExtendedColumn(selectSchema.getValues().get(j)));
				}
			}
		}

		// 3. check group by and corresponding aggregation
		List<Expression> groupbyRef = s.getGroupByColumnReferences();
		if (groupbyRef != null) {
			// pop out the top iter
			for (int i = 0; i < groupbyRef.size(); i++) {
				SelectItemListParser.correct(groupbyRef.get(i), tables);
				//breaking selection operators with AND
				List<Expression> columns = Util.processSelect(groupbyRef.get(i));
				for (int j = 0; j < columns.size(); j++) {
					Schema selectSchema = Util.processExpression(columns.get(j));
					for(int k = 0; k < selectSchema.getValues().size(); k++) {
						collectiveColumns.add(new ExtendedColumn(selectSchema.getValues().get(k)));
					}
				}
			}
		}
		
		// 4. check Having clause
		Expression having = s.getHaving();
		if (having != null) {
			// pop out the top iter
			SelectItemListParser.correct(having, tables);
			//breaking selection operators with AND
			List<Expression> selects = Util.processSelect(having);

			for (int i = 0; i < selects.size(); i++) {
				Schema selectSchema = Util.processExpression(selects.get(i));
				for(int j = 0; j < selectSchema.getValues().size(); j++) {
					collectiveColumns.add(new ExtendedColumn(selectSchema.getValues().get(j)));
				}
			}
		}

		System.gc();
		
		if (queryOrder == 1) {
			columnList1.addAll((TreeSet<ExtendedColumn>)collectiveColumns.clone());
		} else if (queryOrder == 2) {
			columnList2.addAll((TreeSet<ExtendedColumn>)collectiveColumns.clone());
		}
	}
	
	private static void consumeFromItem(FromItem fromitem, List<Table> tables, int queryOrder) {
		
		if (fromitem instanceof Table) {
			Table t = (Table) fromitem;
			// uppercase it
			t.setName(t.getName().toUpperCase());
			//System.out.println(t.getName());
			
			// if there is any alias of this table,create a key to map to original table
			if (t.getAlias() != null) {
				//t.setAlias(t.getName());
				Global.tableAlias.put(t.getAlias(), t.getName());
			}
			if (t.getAlias() == null) {
				Iterator<Entry<String, String>> it = Global.tableAlias.entrySet().iterator();
				while(it.hasNext()) {
					Entry<String, String> temp = it.next();
					if (temp.getValue().equals(t.getName())) {
						t.setAlias(temp.getKey());
					}
				}
			}
			
			tables.add(t);
		} else if (fromitem instanceof SubSelect){
			SubSelect temp = (SubSelect) fromitem;
			
			executeSelect(temp.getSelectBody(), queryOrder);
		}
	}
}
