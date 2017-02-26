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
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
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
	public List<Operation> op=new ArrayList<Operation>();
	public Schema true_schema=new Schema();
	public Schema fake_schema=new Schema();
	public int flip=0;
	private List<Table> tables;
	public List<SubSelect> subselects = new ArrayList<SubSelect>();


	public  SelectItemListParser(List<SelectItem> sitems,List<Table> tables){		 
		// create its own schema using source schema and sitems
		//by keeping the groupby column and add expression as fake columns
		this.tables=tables;
		//be careful about ALLCOLUMN and ALLTABLECOLUMN in selection item expression
		for (int i=0;i<sitems.size();i++){
			SelectItem s=sitems.get(i);
			parseSelectItem(s);
		}
	}

	private void parseSelectItem(SelectItem s){
		getColumn(s,0);
		if (s instanceof SelectExpressionItem){
			//remove its alias
			((SelectExpressionItem) s).setAlias(null);
			Expression ss=((SelectExpressionItem) s).getExpression();
			correct(ss,tables);
			//get the true column schema
			getColumn(s,1);
			parseSimpleExpression(ss); 
		}
	}

	private void parseSimpleExpression(Expression ss){
		if (ss instanceof Function){
			Function f=(Function)ss;  		
			String fName=f.getName();
			if (fName.equals("MAX")){
				op.add(Operation.MAX);
			}
			else if(fName.equals("MIN")){
				op.add(Operation.MIN);
			}
			else if (fName.equals("SUM")){
				op.add(Operation.SUM);
			}
			else if (fName.equals("AVG")){
				op.add(Operation.AVG);
			}
			else if (fName.equals("COUNT")){
				op.add(Operation.COUNT);
			}
			else {
				//do sth
			}

			if (!f.isAllColumns()){
				if (f.getParameters() != null) {
					function_exp.add((Expression) f.getParameters().getExpressions().get(0));
				} else {
					function_exp.add(f);
				}
			}
			else{
				function_exp.add(f);
			}

		}
		else if (ss instanceof Parenthesis){
			Parenthesis p=(Parenthesis) ss;
			Expression exp=p.getExpression();
			parseSimpleExpression(exp);   			
		}

		else if (ss instanceof BinaryExpression){
			this.binary_exp.add((BinaryExpression) ss);
		}
		else {
			//do sth
		}
	}

	private void getColumn(SelectItem item,int value){

		if (item instanceof AllTableColumns){
			AllTableColumns all=(AllTableColumns) item;
			String tname=all.getTable().getName();
			CreateTable ct=Global.tables.get(tname);
			if (ct != null) {
				List<ColumnDefinition> colDef=ct.getColumnDefinitions();
				for (int i=0;i<colDef.size();i++){
					Column cc=new Column();
					cc.setTable(ct.getTable());
					cc.setColumnName(colDef.get(i).getColumnName());
					if(value==0)
						this.fake_schema.addValue(cc);
					else
						this.true_schema.addValue(cc);	
				}
			} else {
				//all.getTable().
			}
		}


		else if (item instanceof AllColumns)
			flip=1;
		//if it is a selectExpressionItem
		else {
			SelectExpressionItem sitem=(SelectExpressionItem)item;
			if (sitem.getAlias()!=null){
				Column cc=new Column();
				cc.setTable(new Table());
				cc.setColumnName(sitem.getAlias());
				this.fake_schema.addValue(cc);
			}
			else{
				Expression exp=sitem.getExpression();

				if (exp instanceof Column){				 
					Column cc=(Column)exp;


					if(value==0){
						Column c=new Column();
						c.setColumnName(cc.getColumnName());
						Table t=new Table();
						t.setName(cc.getTable().getName());
						c.setTable(t);
						this.fake_schema.addValue(c);
					}
					else{
						Column c=Global.getColumnFullName(cc, tables);
						this.true_schema.addValue(c);
					}

				}
				else if (exp instanceof Function){
					Column cc=new Column();
					cc.setTable(new Table());
					cc.setColumnName(exp.toString());
					if(value==0)
						this.fake_schema.addValue(cc);
					else
						this.true_schema.addValue(cc);

				}
				else if (exp instanceof Parenthesis){
					Parenthesis p=(Parenthesis) exp;
					Expression exp1=p.getExpression();
					Column cc=new Column();
					cc.setTable(new Table());
					cc.setColumnName(exp1.toString());
					if(value==0)
						this.fake_schema.addValue(cc);
					else
						this.true_schema.addValue(cc);
				}
				else{
					Column cc=new Column();
					cc.setTable(new Table());
					cc.setColumnName(exp.toString());
					if(value==0)
						this.fake_schema.addValue(cc);
					else
						this.true_schema.addValue(cc);
				}


			}

		}

	}


	//find all columns with no table name or aliased table name,make them toUppercase
	public static void correct(Expression exp, List<Table> tables){

		if (exp instanceof Function){
			Function f=(Function)exp;
			f.setName(f.getName().toUpperCase());
			if (!f.isAllColumns())
			{
				ExpressionList explist= f.getParameters();
				if (explist != null) {
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

