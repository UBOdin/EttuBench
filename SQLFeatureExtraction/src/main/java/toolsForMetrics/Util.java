package toolsForMetrics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * This is a service module for query similarity metrics
 * @author Gokhan
 *
 */
public class Util {
	
//	/**
//	 * Switches two given operators in the given list
//	 * @param operators
//	 * @param iter1
//	 * @param iter2
//	 * @return new operator list
//	 */
//	public static List<SqlIterator> switchIterators(List<SqlIterator> iterators, SqlIterator iter1, SqlIterator iter2) {
//		int tempIndex1 = 0;
//		int tempIndex2 = 0;
//		
//		for(int i = 0; i < iterators.size(); i++) {
//			if (iterators.get(i).equals(iter1)) {
//				tempIndex1 = i;
//			}
//			else if (iterators.get(i).equals(iter2)) {
//				tempIndex2 = i;
//			}
//		}
//		
//		iterators.set(tempIndex1, iter2);
//		iterators.set(tempIndex2, iter1);
//		
//		return iterators;
//	}
	
	/**
	 * This class process Expressions and get all the columns involved in the expression
	 * @param ex
	 * @return
	 */
	public static Schema processExpression(Expression ex) {
		Schema expressionSchema = new Schema();
		
		ex = Util.deleteParanthesis(ex);
		
		ColumnExpressionVisitor visitor = new ColumnExpressionVisitor(); 
        ex.accept(visitor); 
        
        expressionSchema.addAll(visitor.getColumns());
        return expressionSchema;
	}
	
	/**
	 * This gets select expressions contained in a more complex expression
	 * @param e
	 * @return
	 */
	public static List<Expression> processSelect(Expression e) 
	{
		List<Expression> retVal = new ArrayList<Expression>();
		
		if (e instanceof Parenthesis) {
			retVal.addAll(processSelect(deleteParanthesis(e)));
		} else if (e instanceof ExistsExpression) {
			retVal.addAll(processSelect(((ExistsExpression) e).getRightExpression()));
		} else if (e instanceof BinaryExpression){
			BinaryExpression a = (BinaryExpression)e;
			retVal.addAll(processSelect(a.getLeftExpression()));
			retVal.addAll(processSelect(a.getRightExpression()));
		} else if (e instanceof SubSelect) {
			ColumnExpressionVisitor visitor = new ColumnExpressionVisitor(); 
	        e.accept(visitor); 
	        
	        retVal.addAll(visitor.getColumns());
		} else if (e instanceof InExpression) {
			InExpression a = (InExpression)e;
			retVal.addAll(processSelect(a.getLeftExpression()));
			
			ItemsList it = a.getItemsList();
			if (it != null) {
				ColumnExpressionVisitor visitor = new ColumnExpressionVisitor();
				it.accept(visitor);
				retVal.addAll(visitor.getColumns());
			}
		} else {
			retVal.add(e);
		}
		
		return retVal;
	}
		
	/**
	 * This method consumes a select schema and finds all the tables contained in the schema without duplicate
	 * @param selectSchema
	 * @return
	 */
	public static List<Table> processSchema(Schema selectSchema) {
		
		List<Column> columns = selectSchema.getValues();
		
		HashSet<Table> set = new HashSet<Table>();
		
		for(int i = 0; i < columns.size(); i++) {
			set.add(columns.get(i).getTable());			
		}
		
		return new ArrayList<Table>(set);
	}
	
	/**
	 * Checks if first element has all the tables of the second element
	 * @param child
	 * @param parent
	 * @return
	 */
	public static boolean containsAll(List<Table> child, List<Table> parent) {
				
		if (child.containsAll(parent)) 
			return true;
		else
			return false;
	} 
	
//	/**
//	 * This method returns the best match of iterator from the list matches according to their schema
//	 * @param iterator
//	 * @param matches
//	 * @return
//	 */
//	public static SqlIterator returnBestMatch(SqlIterator iterator, List<SqlIterator> matches) {
//		SqlIterator retVal = null;
//		int iteratorSize = processSchema(iterator.getSchema()).size();
//		int[] distances = new int[matches.size()];
// 		
//		for (int i = 0; i < matches.size(); i++) {
//			distances[i] = processSchema(matches.get(i).getSchema()).size() - iteratorSize;
//		}
//		
//		int min = Integer.MAX_VALUE;
//		int retIndex = Integer.MAX_VALUE;
//		
//		for(int i = 0; i < distances.length; i++) {
//			if (distances[i] < min) {
//				min = distances[i];
//				retIndex = i;
//			}
//		}
//		
//		retVal = matches.get(retIndex);
//		
//		return retVal;
//	}
	
	public static Expression deleteParanthesis(Expression input) {
		if (input instanceof Parenthesis) {
			input = ((Parenthesis) input).getExpression();
			deleteParanthesis(input);
			return input;
		}
		return input;
	}

	/**
	 * This method is written by Gokhan~ask him about its functionality
	 * @param selectItem
	 * @return
	 */
	public static List<Column> findColumns(SelectItem selectItem) {
		
		Set<String> tableNames = Global.tables.keySet();
		
		Iterator<String> iter = tableNames.iterator();
		String foundTable = "";
		boolean isFound = false;
		while (iter.hasNext()) {
			foundTable = iter.next().toString();
		    if (selectItem.toString().contains(foundTable.toUpperCase())) {
		    	isFound = true;
		    	break;
		    }
		}
		
		if (isFound) {
			CreateTable table = Global.tables.get(foundTable);
			
			List<ColumnDefinition> colDefs = table.getColumnDefinitions();
			List<Column> retVal = new ArrayList<Column>();
			Column tempColumn = null;
			for (ColumnDefinition colDef : colDefs) {
				String tempString = foundTable.toUpperCase() + "." + colDef.getColumnName();
				
				if(selectItem.toString().contains(tempString)) {
					tempColumn = new Column();
					tempColumn.setColumnName(colDef.getColumnName());
					tempColumn.setTable(table.getTable());
					retVal.add(tempColumn);
				}
			}
			
			return retVal;
			
		} else {
			return null;
		}
	}

	
}
