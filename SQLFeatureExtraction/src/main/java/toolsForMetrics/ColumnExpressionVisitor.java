package toolsForMetrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;

public class ColumnExpressionVisitor implements ExpressionVisitor, ItemsListVisitor{

	private void processBinaryOperation(BinaryExpression be){ 
		be.getLeftExpression().accept(this); 
		be.getRightExpression().accept(this); 
	}     

	private List<Column> columns = new ArrayList<Column>(); 

	public List<Column> getColumns(){ 
		return new ArrayList<Column>(columns); 
	} 

	@Override 
	public void visit(AndExpression ae) { 
		processBinaryOperation(ae); 
	} 

	@Override 
	public void visit(OrExpression oe) { 
		processBinaryOperation(oe); 
	} 

	@Override 
	public void visit(Addition adtn) { 
		processBinaryOperation(adtn); 
	} 

	@Override 
	public void visit(Multiplication m) { 
		processBinaryOperation(m); 
	} 

	@Override 
	public void visit(Division dvsn) { 
		processBinaryOperation(dvsn); 
	} 

	@Override 
	public void visit(Subtraction s) { 
		processBinaryOperation(s); 
	} 

	@Override 
	public void visit(EqualsTo et) { 
		processBinaryOperation(et); 
	} 

	@Override 
	public void visit(LikeExpression le) { 
		processBinaryOperation(le); 
	} 

	@Override 
	public void visit(GreaterThan gt) { 
		processBinaryOperation(gt); 
	} 

	@Override 
	public void visit(GreaterThanEquals gte) { 
		processBinaryOperation(gte); 
	} 


	@Override 
	public void visit(MinorThan mt) { 
		processBinaryOperation(mt); 
	} 

	@Override 
	public void visit(MinorThanEquals mte) { 
		processBinaryOperation(mte); 
	} 

	@Override 
	public void visit(NotEqualsTo net) { 
		processBinaryOperation(net); 
	} 

	@Override 
	public void visit(ExpressionList el) { 
		for (@SuppressWarnings("unchecked")
		Iterator<Expression> iter = el.getExpressions().iterator(); iter.hasNext();) { 
			Expression expression = (Expression) iter.next(); 
			expression.accept(this); 
		} 
	} 

	@Override 
	public void visit(Column column) { 
		if (!columns.contains(column))
			columns.add(column); 
	}

	@Override
	public void visit(NullValue arg0) {

	}


	@Override
	public void visit(Function arg0) {
		ExpressionList list = arg0.getParameters(); 
		if(list != null){ 
			visit(list); 
		}
	}

	@Override
	public void visit(InverseExpression arg0) {

	}

	@Override
	public void visit(JdbcParameter arg0) {

	}

	@Override
	public void visit(DoubleValue arg0) {

	}

	@Override
	public void visit(LongValue arg0) {

	}

	@Override
	public void visit(DateValue arg0) {

	}

	@Override
	public void visit(TimeValue arg0) {

	}

	@Override
	public void visit(TimestampValue arg0) {

	}

	@Override
	public void visit(Parenthesis arg0) {
		Util.deleteParanthesis(arg0).accept(this);

	}

	@Override
	public void visit(StringValue arg0) {

	}


	@Override
	public void visit(InExpression arg0) {
		Expression exp1 = arg0.getLeftExpression();
		if (exp1 instanceof SubSelect) {
			System.out.println(arg0);
			System.out.println(exp1);
		}
		if (exp1 != null) {
			exp1.accept(this);
		}
		ItemsList list = arg0.getItemsList();
		list.accept(this);
	}

	@Override
	public void visit(IsNullExpression arg0) {
		Expression exp1 = arg0.getLeftExpression();
		if (exp1 != null) {
			exp1.accept(this);
		}
	}

	@Override
	public void visit(SubSelect arg0) {
		SubSelectVisitor visitor = new SubSelectVisitor(this, null);
		if (arg0.getSelectBody() instanceof PlainSelect) {
			((PlainSelect)arg0.getSelectBody()).accept(visitor);
			List<Column> columnList = visitor.getColumns();
			if (columnList != null) {
				for (int i = 0; i < columnList.size(); i++) {
					if (!columns.contains(columnList.get(i)))
						columns.add(columnList.get(i)); 
				}
			}
		} else {
			Union union = (Union)arg0.getSelectBody();
			@SuppressWarnings("unchecked")
			List<PlainSelect> select_list = union.getPlainSelects();
			for (PlainSelect plainSelect : select_list) {
				plainSelect.accept(visitor);
				List<Column> columnList = visitor.getColumns();
				if (columnList != null) {
					for (int i = 0; i < columnList.size(); i++) {
						if (!columns.contains(columnList.get(i)))
							columns.add(columnList.get(i)); 
					}
				}
			}
		}
	}

	@Override
	public void visit(CaseExpression arg0) {
		Expression exp1=arg0.getSwitchExpression();
		if (exp1!=null)
			exp1.accept(this);
		Expression exp2=arg0.getElseExpression();
		if (exp2!=null)
			exp2.accept(this);

		@SuppressWarnings("unchecked")
		List<WhenClause> exp3=arg0.getWhenClauses();
		WhenClause  when;
		if (exp3!=null){
			for (int i=0;i<exp3.size();i++){
				when =exp3.get(i);
				if (when.getThenExpression()!=null)
					when.getThenExpression().accept(this);
				if (when.getWhenExpression()!=null)
					when.getWhenExpression().accept(this);
			}
		}
	}

	@Override
	public void visit(WhenClause arg0) {

	}

	@Override
	public void visit(ExistsExpression arg0) {

	}

	@Override
	public void visit(AllComparisonExpression arg0) {

	}

	@Override
	public void visit(AnyComparisonExpression arg0) {

	}

	@Override
	public void visit(Concat arg0) {

	}

	@Override
	public void visit(Matches arg0) {

	}

	@Override
	public void visit(BitwiseAnd arg0) {

	}

	@Override
	public void visit(BitwiseOr arg0) {

	}

	@Override
	public void visit(BitwiseXor arg0) {

	}

	@Override
	public void visit(Between arg0) {

	}

}
