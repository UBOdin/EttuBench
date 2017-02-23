package querySimilarityMetrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;
import net.sf.jsqlparser.statement.select.WithItem;
import toolsForMetrics.ExtendedColumn;
import toolsForMetrics.Global;
import toolsForMetrics.Schema;
import toolsForMetrics.SelectItemListParser;
import toolsForMetrics.Util;

public class Aligon {

	private static TreeSet<ExtendedColumn> projectionList1 = new TreeSet<ExtendedColumn>();
	private static TreeSet<ExtendedColumn> projectionList2 = new TreeSet<ExtendedColumn>();
	
	private static TreeSet<ExtendedColumn> groupbyList1 = new TreeSet<ExtendedColumn>();
	private static TreeSet<ExtendedColumn> groupbyList2 = new TreeSet<ExtendedColumn>();
	
	private static TreeSet<ExtendedColumn> selectionList1 = new TreeSet<ExtendedColumn>();
	private static TreeSet<ExtendedColumn> selectionList2 = new TreeSet<ExtendedColumn>();
	
	public static void createQueryVector(Statement stmt1) {
		projectionList1 = new TreeSet<ExtendedColumn>();
		groupbyList1 = new TreeSet<ExtendedColumn>();
		selectionList1 = new TreeSet<ExtendedColumn>();

		if (stmt1 instanceof Select) {
			Select s1=(Select) stmt1;
			
			@SuppressWarnings("unchecked")
			List<WithItem> with1 = s1.getWithItemsList();
			if (with1 != null) {
				for (int i = 0; i < with1.size(); i++) {
					executeSelect(with1.get(i).getSelectBody(), 1);
				}
			}
			
			//Collect where and group by and nothing else
			executeSelect(s1.getSelectBody(), 1);
			
			//System.out.println(groupbyList1);
			//System.out.println(selectionList1);
			//System.out.println(projectionList1);
		} else {
			System.err.println(stmt1 + " is not a Select query");
		}
	}
	
	public static TreeSet<ExtendedColumn> getProjectionList() {
		return projectionList1;
	}
	
	public static TreeSet<ExtendedColumn> getSelectionList() {
		return selectionList1;
	}
	
	public static TreeSet<ExtendedColumn> getGroupByList() {
		return groupbyList1;
	}
	
public static double getDistanceAsRatio(TreeSet<ExtendedColumn> stmt1projection,
										TreeSet<ExtendedColumn> stmt1groupBy,
										TreeSet<ExtendedColumn> stmt1selection,
										TreeSet<ExtendedColumn> stmt2projection,
										TreeSet<ExtendedColumn> stmt2groupBy,
										TreeSet<ExtendedColumn> stmt2selection) {
		

			TreeSet<ExtendedColumn> intersectionGroupBy = new TreeSet<ExtendedColumn>(stmt1groupBy);
			intersectionGroupBy.retainAll(stmt2groupBy);
			
			TreeSet<ExtendedColumn> unionGroupBy = new TreeSet<ExtendedColumn>();
			unionGroupBy.addAll(stmt1groupBy);
			unionGroupBy.addAll(stmt2groupBy);
			
			TreeSet<ExtendedColumn> intersectionSelection = new TreeSet<ExtendedColumn>(stmt1selection);
			intersectionSelection.retainAll(stmt2selection);
			
			TreeSet<ExtendedColumn> unionSelection = new TreeSet<ExtendedColumn>();
			unionSelection.addAll(stmt1selection);
			unionSelection.addAll(stmt2selection);
			
			TreeSet<ExtendedColumn> intersectionProjection = new TreeSet<ExtendedColumn>(stmt1projection);
			intersectionProjection.retainAll(stmt2projection);
			
			//TreeSet<ExtendedColumn> intersectionProjection = ExtendedColumn.intersect(projectionList1, projectionList2);
			
			TreeSet<ExtendedColumn> unionProjection = new TreeSet<ExtendedColumn>();
			unionProjection.addAll(stmt1projection);
			unionProjection.addAll(stmt2projection);
			
			double variableSize = 0.0;
			double groupByScore = 0;
			double selectionScore = 0;
			double projectionScore = 0;
			
			if (unionGroupBy.size() > 0) {
				groupByScore = 1 - ((double)(unionGroupBy.size() - intersectionGroupBy.size()) / (double)unionGroupBy.size());
				variableSize++;
			}
			
			if (unionSelection.size() > 0) {
				selectionScore = 1 - ((double)(unionSelection.size() - intersectionSelection.size()) / (double)unionSelection.size());
				variableSize++;
			}
			
			if (unionProjection.size() > 0) {
				projectionScore = (double)intersectionProjection.size() / (double)unionProjection.size();
				variableSize++;
			}
			
			if (variableSize == 0.0) {
				return 0;
			}
			
			return (groupByScore + selectionScore + projectionScore) / variableSize;
	}

	
	
	
	public static double getDistanceAsRatio(Statement stmt1, Statement stmt2) {
		
		projectionList1 = new TreeSet<ExtendedColumn>();
		projectionList2 = new TreeSet<ExtendedColumn>();
		
		groupbyList1 = new TreeSet<ExtendedColumn>();
		groupbyList2 = new TreeSet<ExtendedColumn>();
		
		selectionList1 = new TreeSet<ExtendedColumn>();
		selectionList2 = new TreeSet<ExtendedColumn>();

		if (stmt1 instanceof Select && stmt2 instanceof Select) {
			Select s1=(Select) stmt1;
			
			@SuppressWarnings("unchecked")
			List<WithItem> with1 = s1.getWithItemsList();
			if (with1 != null) {
				for (int i = 0; i < with1.size(); i++) {
					executeSelect(with1.get(i).getSelectBody(), 1);
				}
			}
			
			Select s2=(Select) stmt2;
			
			@SuppressWarnings("unchecked")
			List<WithItem> with2 = s2.getWithItemsList();
			if (with2 != null) {
				for (int i = 0; i < with2.size(); i++) {
					executeSelect(with2.get(i).getSelectBody(), 2);
				}
			}
			
			//Collect where and group by and nothing else
			executeSelect(s1.getSelectBody(), 1);
			executeSelect(s2.getSelectBody(), 2);
			
			//System.out.println(groupbyList1);
			//System.out.println(groupbyList2);
			//System.out.println(selectionList1);
			//System.out.println(selectionList2);
			//System.out.println(projectionList1);
			//System.out.println(projectionList2);
			
			TreeSet<ExtendedColumn> intersectionGroupBy = new TreeSet<ExtendedColumn>(groupbyList1);
			intersectionGroupBy.retainAll(groupbyList2);
			
			TreeSet<ExtendedColumn> unionGroupBy = new TreeSet<ExtendedColumn>();
			unionGroupBy.addAll(groupbyList1);
			unionGroupBy.addAll(groupbyList2);
			
			TreeSet<ExtendedColumn> intersectionSelection = new TreeSet<ExtendedColumn>(selectionList1);
			intersectionSelection.retainAll(selectionList2);
			
			TreeSet<ExtendedColumn> unionSelection = new TreeSet<ExtendedColumn>();
			unionSelection.addAll(selectionList1);
			unionSelection.addAll(selectionList2);
			
			TreeSet<ExtendedColumn> intersectionProjection = new TreeSet<ExtendedColumn>(projectionList1);
			intersectionProjection.retainAll(projectionList2);
			
			//TreeSet<ExtendedColumn> intersectionProjection = ExtendedColumn.intersect(projectionList1, projectionList2);
			
			TreeSet<ExtendedColumn> unionProjection = new TreeSet<ExtendedColumn>();
			unionProjection.addAll(projectionList1);
			unionProjection.addAll(projectionList2);
			
			double variableSize = 0.0;
			double groupByScore = 0;
			double selectionScore = 0;
			double projectionScore = 0;
			
			if (unionGroupBy.size() > 0) {
				groupByScore = 1 - ((double)(unionGroupBy.size() - intersectionGroupBy.size()) / (double)unionGroupBy.size());
				variableSize++;
			}
			
			if (unionSelection.size() > 0) {
				selectionScore = 1 - ((double)(unionSelection.size() - intersectionSelection.size()) / (double)unionSelection.size());
				variableSize++;
			}
			
			if (unionProjection.size() > 0) {
				projectionScore = (double)intersectionProjection.size() / (double)unionProjection.size();
				variableSize++;
			}
			
			if (variableSize == 0.0) {
				return 0;
			}
			
			return (groupByScore + selectionScore + projectionScore) / variableSize;
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
			@SuppressWarnings("unchecked")
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
		List<Table> tables = new ArrayList<Table>();
		HashSet<ExtendedColumn> groupByColumns = new HashSet<ExtendedColumn>();
		HashSet<ExtendedColumn> selectionColumns = new HashSet<ExtendedColumn>();
		HashSet<ExtendedColumn> projectionColumns = new HashSet<ExtendedColumn>();
			
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
								selectionColumns.add(new ExtendedColumn(selectSchema.getValues().get(k)));
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
							selectionColumns.add(new ExtendedColumn(column));
						}
					}
				}
				//Collections.sort(this.scanList);
			}

		} else
			System.err.println("no from item found,please check");
		
		List<SelectItem> selectItems = s.getSelectItems();
		
		//SelectItemListParser parser = new SelectItemListParser(selectItems, tables);

		if (selectItems != null) {
			// check whether there is any function
			//int flip = 0;
			for (int i = 0; i < selectItems.size(); i++) {
				SelectItem ss = selectItems.get(i);
				if (ss instanceof SelectExpressionItem) {
					Expression sss = ((SelectExpressionItem) ss)
							.getExpression();
					if (sss instanceof SubSelect) {
						SubSelect temp = (SubSelect) sss;
						
						executeSelect(temp.getSelectBody(), queryOrder);
					}
					Schema selectSchema = Util.processExpression(sss);
					for (int j = 0; j < selectSchema.getValues().size(); j++) {
						projectionColumns.add(new ExtendedColumn(selectSchema.getValues().get(j)));
					}
				}
				else if (ss instanceof SubSelect) {
					SubSelect temp = (SubSelect) ss;
					
					executeSelect(temp.getSelectBody(), queryOrder);
				}
				else if (ss instanceof AllColumns){
					projectionColumns.add(new ExtendedColumn("*"));
				}
				else if (ss instanceof AllTableColumns) {
					projectionColumns.add(new ExtendedColumn(ss.toString()));
				}
				else {
					//System.out.println(ss);
				}
			}
		}

		// 2.check where condition and do selection
		Expression where = s.getWhere();
		if (where != null) {
			// pop out the top iter
			SelectItemListParser.correct(where, tables);
			//breaking selection operators with AND
			List<Expression> selects = Util.processSelect(where);

			for (int i = 0; i < selects.size(); i++) {
				Schema selectSchema = Util.processExpression(selects.get(i));
				for (int j = 0; j < selectSchema.getValues().size(); j++) {
					selectionColumns.add(new ExtendedColumn(selectSchema.getValues().get(j)));
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
					for (int k = 0; k < selectSchema.getValues().size(); k++) {
						groupByColumns.add(new ExtendedColumn(selectSchema.getValues().get(k)));
						
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
				for (int j = 0; j < selectSchema.getValues().size(); j++) {
					selectionColumns.add(new ExtendedColumn(selectSchema.getValues().get(j)));
				}
			}
		}

		System.gc();
		
		if (queryOrder == 1) {
			projectionList1.addAll((HashSet<ExtendedColumn>)projectionColumns.clone());
			selectionList1.addAll((HashSet<ExtendedColumn>)selectionColumns.clone());
			groupbyList1.addAll((HashSet<ExtendedColumn>)groupByColumns.clone());
		} else if (queryOrder == 2) {
			projectionList2.addAll((HashSet<ExtendedColumn>)projectionColumns.clone());
			selectionList2.addAll((HashSet<ExtendedColumn>)selectionColumns.clone());
			groupbyList2.addAll((HashSet<ExtendedColumn>)groupByColumns.clone());
		}
	}
	
	private static void consumeFromItem(FromItem fromitem, List<Table> tables, int queryOrder) {
		
		if (fromitem instanceof Table) {
			Table t = (Table) fromitem;
			// uppercase it
			t.setName(t.getName().toUpperCase());
			
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
