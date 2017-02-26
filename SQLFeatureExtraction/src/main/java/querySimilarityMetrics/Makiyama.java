package querySimilarityMetrics;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
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

/**
 * Query similarity metric Makiyama
 * @author tingxie
 *
 */
public class Makiyama {
	
	private static TreeMap<String, Integer> columnList1 = new TreeMap<String, Integer>();
	private static TreeMap<String, Integer> columnList2 = new TreeMap<String, Integer>();
	
	public static TreeMap<String, Integer> getQueryVector(Statement stmt1) {
		columnList1 = new TreeMap<String, Integer>();

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
	
	public static double getDistanceAsRatio(TreeMap<String, Integer> stmt1, TreeMap<String, Integer> stmt2) {
			TreeSet<String> union = new TreeSet<String>();
			union.addAll(stmt1.keySet());
			union.addAll(stmt2.keySet());
			
			int[] vector1 = new int[union.size()];
			int[] vector2 = new int[union.size()];
			int tempIndex = 0;
			
			for (String columnName : union) {
				if (stmt1.containsKey(columnName)) {
					vector1[tempIndex] = stmt1.get(columnName).intValue();
				}
				if (stmt2.containsKey(columnName)) {
					vector2[tempIndex] = stmt2.get(columnName).intValue();
				}
				tempIndex++;
			}
			
			return cosineSimilarity(vector1, vector2);
	}

	public static double getDistanceAsRatio(Statement stmt1, Statement stmt2) {
		columnList1 = new TreeMap<String, Integer>();
		columnList2 = new TreeMap<String, Integer>();

		if (stmt1 instanceof Select && stmt2 instanceof Select) {
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
			
			TreeSet<String> union = new TreeSet<String>();
			union.addAll(columnList1.keySet());
			union.addAll(columnList2.keySet());
			
			int[] vector1 = new int[union.size()];
			int[] vector2 = new int[union.size()];
			int tempIndex = 0;
			
			for (String columnName : union) {
				if (columnList1.containsKey(columnName)) {
					vector1[tempIndex] = columnList1.get(columnName).intValue();
				}
				if (columnList2.containsKey(columnName)) {
					vector2[tempIndex] = columnList2.get(columnName).intValue();
				}
				tempIndex++;
			}
			
			return cosineSimilarity(vector1, vector2);
		} else {
			if (stmt1 instanceof Select)
				System.err.println(stmt2 + " is not a Select query");
			if (stmt2 instanceof Select)
				System.err.println(stmt1 + " is not a Select query");
		}
		
		return 0;
	}
	
	private static double cosineSimilarity(int[] vectorA, int[] vectorB) {
	    int dotProduct = 0;
	    int normA = 0;
	    int normB = 0;
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }
	    if ((double)(Math.sqrt(normA) * Math.sqrt(normB)) == 0)
	    	return 0;
	    return (double)dotProduct / (double)(Math.sqrt(normA) * Math.sqrt(normB));
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
		Column c;
		List<Table> tables = new ArrayList<Table>();
		TreeMap<String, Integer> collectiveColumns = new TreeMap<String, Integer>();

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
								Integer temp = collectiveColumns.get("Select_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString());
								if (temp == null)
									collectiveColumns.put("Select_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString(), 1);
								else {
									collectiveColumns.put("Select_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString(), temp + 1);
								}
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
							
							Integer temp = collectiveColumns.get("Where_" + new ExtendedColumn(column).toString());
							if (temp == null)
								collectiveColumns.put("Where_" + new ExtendedColumn(column).toString(), 1);
							else {
								collectiveColumns.put("Where_" + new ExtendedColumn(column).toString(), temp + 1);
							}
						}
					}
				}
				//Collections.sort(this.scanList);
			}

		} else
			System.err.println("no from item found,please check");
		
		List<SelectItem> selectItems = s.getSelectItems();
		
		SelectItemListParser parser = new SelectItemListParser(selectItems, tables);

		if (selectItems != null) {
			// check whether there is any function
			int flip = 0;
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
						Integer temp = collectiveColumns.get("Select_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString());
						if (temp == null)
							collectiveColumns.put("Select_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString(), 1);
						else {
							collectiveColumns.put("Select_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString(), temp + 1);
						}
					}
				}
				else if (ss instanceof SubSelect) {
					SubSelect temp = (SubSelect) ss;
					
					executeSelect(temp.getSelectBody(), queryOrder);
				}
				else if (ss instanceof AllColumns){
					Integer temp = collectiveColumns.get("Select_" + ss.toString());
					if (temp == null)
						collectiveColumns.put("Select_" + ss.toString(), 1);
					else {
						collectiveColumns.put("Select_" + ss.toString(), temp + 1);
					}
				}
				else if (ss instanceof AllTableColumns) {
					Integer temp = collectiveColumns.get("Select_" + ss.toString());
					if (temp == null)
						collectiveColumns.put("Select_" + ss.toString(), 1);
					else {
						collectiveColumns.put("Select_" + ss.toString(), temp + 1);
					}
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
					Integer temp = collectiveColumns.get("Where_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString());
					if (temp == null)
						collectiveColumns.put("Where_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString(), 1);
					else {
						collectiveColumns.put("Where_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString(), temp + 1);
					}
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
						Integer temp = collectiveColumns.get("GroupBy_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString());
						if (temp == null)
							collectiveColumns.put("GroupBy_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString(), 1);
						else {
							collectiveColumns.put("GroupBy_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString(), temp + 1);
						}
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
					Integer temp = collectiveColumns.get("Where_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString());
					if (temp == null)
						collectiveColumns.put("Where_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString(), 1);
					else {
						collectiveColumns.put("Where_" + new ExtendedColumn(selectSchema.getValues().get(j)).toString(), temp + 1);
					}
				}
			}
		}
		
		List<Expression> orderByRef = s.getOrderByElements();
		if (orderByRef != null) {
			// pop out the top iter
			for (int i = 0; i < orderByRef.size(); i++) {
				//breaking selection operators with AND
				OrderByElement ord = (OrderByElement) orderByRef.get(i);
				Expression exp = ord.getExpression();					
				SelectItemListParser.correct(exp, tables);
				
				Column cc = null;
				List<Expression> selects = null;
				try {
					cc = (Column) exp;
					selects = Util.processSelect(exp);
				} catch (Exception ex) {
					selects = Util.processSelect(exp);
				}
				
				
				for (int j = 0; j < selects.size(); j++) {
					Schema selectSchema = Util.processExpression(selects.get(j));
					for (int k = 0; k < selectSchema.getValues().size(); k++) {
						Integer temp = collectiveColumns.get("OrderBy_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString());
						if (temp == null)
							collectiveColumns.put("OrderBy_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString(), 1);
						else {
							collectiveColumns.put("OrderBy_" + new ExtendedColumn(selectSchema.getValues().get(k)).toString(), temp + 1);
						}
					}
				}
			}
		}

		System.gc();
		
		if (queryOrder == 1) {
			columnList1 = (TreeMap<String, Integer>)collectiveColumns.clone();
		} else if (queryOrder == 2) {
			columnList2 = (TreeMap<String, Integer>)collectiveColumns.clone();
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
