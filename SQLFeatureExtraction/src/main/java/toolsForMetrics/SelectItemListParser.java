package toolsForMetrics;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * This class parse SelectItems and gets Functions,binary Expressions and also the output Schema
 * fake_schema means columns in the schema may be aliased hence should only be used for displaying purpose while true_schema is good for Iterators
 * @author Ting Xie
 * 
 */
public class SelectItemListParser {


	public List<Expression> function_exp=new ArrayList<Expression>();
	public List<BinaryExpression> binary_exp=new ArrayList<BinaryExpression>();
//	public List<Operation> op=new ArrayList<Operation>();
	public Schema true_schema=new Schema();
	public Schema fake_schema=new Schema();
	public int flip=0;
	//	private List<Table> tables;
	public List<SubSelect> subselects = new ArrayList<SubSelect>();



	//find all columns with no table name or aliased table name,make them toUppercase
	public static void correct(Expression exp, List<Table> tables){

		if (exp instanceof Function){
			Function f=(Function)exp;
			f.setName(f.getName().toUpperCase());
			if (!f.isAllColumns())
			{
				ExpressionList explist= f.getParameters();
				if (explist != null) {
					@SuppressWarnings("unchecked")
					List<Expression> list=explist.getExpressions();
					for (int i=0;i<list.size();i++)
						correct(list.get(i), tables);
				}
			}
		}

		else if (exp instanceof Column){
			Column c=(Column) exp;
			if (c.getTable()==null||c.getTable().getName()==null){				  					
				Column v=Global.getColumnFullName(c,tables);
				c.setTable(v.getTable());
				c.setColumnName(v.getColumnName());
			}
			else{				  
				String tablename = c.getTable().getName();
				Table t = new Table();
				t.setName(tablename.toUpperCase());
				c.setTable(t);
			}
		}
		//for any other expression with more than one elements
		else if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			Expression l=bexp.getLeftExpression();
			Expression r=bexp.getRightExpression();
			correct(l,tables);
			correct(r,tables);
		}
		else if (exp instanceof Parenthesis) {
			Parenthesis p=(Parenthesis) exp;
			Expression exp1=p.getExpression();
			correct(exp1,tables);
		}
		else if (exp instanceof CaseExpression){
			CaseExpression c=(CaseExpression) exp;
			@SuppressWarnings("unchecked")
			List<WhenClause> e=c.getWhenClauses();
			for (int i=0;i<e.size();i++){
				WhenClause clause=e.get(i);
				correct(clause.getThenExpression(),tables);
				correct(clause.getWhenExpression(),tables);
			}
		}
		else if (exp instanceof SubSelect){
			//subselects.add((SubSelect)exp);
		}
		else {
			//do sth
		}
	}

}

