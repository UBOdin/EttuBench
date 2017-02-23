package toolsForMetrics;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.statement.select.SubSelect;
/**
 * This is a service module with many tool methods
 * @author Gokhan
 *
 */
public class Util {
	
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
		
	
	public static Expression deleteParanthesis(Expression input) {
		if (input instanceof Parenthesis) {
			input = ((Parenthesis) input).getExpression();
			deleteParanthesis(input);
			return input;
		}
		return input;
	}


	
}
