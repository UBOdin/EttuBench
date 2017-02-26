package dataset;



import java.util.List;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
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
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Top;
import net.sf.jsqlparser.statement.select.Union;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;


public class QuerySkeletonCheck {
	
	// done
	public static boolean compareStatement(Statement s1, Statement s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if (s1.getClass().equals(s2.getClass())) {
			if (s1 instanceof CreateTable) {
				return compareCreateTable((CreateTable)s1, (CreateTable)s2);
			} else if (s1 instanceof Delete) {
				return compareDelete((Delete)s1, (Delete)s2);
			} else if (s1 instanceof Drop) {
				return compareDrop((Drop)s1, (Drop)s2);
			} else if (s1 instanceof Insert) {
				return compareInsert((Insert)s1, (Insert)s2);
			} else if (s1 instanceof Replace) {
				return compareReplace((Replace)s1, (Replace)s2);
			} else if (s1 instanceof Select) {
				return compareSelect((Select)s1, (Select)s2);
			} else if (s1 instanceof Truncate) {
				return compareTruncate((Truncate)s1, (Truncate)s2);
			} else if (s1 instanceof Update) {
				return compareUpdate((Update)s1, (Update)s2);
			}       
		}
		return false;
	}
	
	// done
	public static boolean compareCreateTable(CreateTable c1, CreateTable c2) {
		if ((c1 == null) && (c2 == null))
			return true;
		if ((c1 == null) && (c2 != null))
			return false;
		if ((c1 != null) && (c2 == null))
			return false;
		if (compareTable(c1.getTable(), c2.getTable()) == false)
			return false;
		List<ColumnDefinition> cd1 = c1.getColumnDefinitions();
		List<ColumnDefinition> cd2 = c2.getColumnDefinitions();
		if ((cd1 == null) && (cd2 != null))
			return false;
		if ((cd1 != null) && (cd2 == null))
			return false;
		if ((cd1 != null) && (cd2 != null)) {
			if (cd1.size() != cd2.size())
				return false;
			for (int i = 0; i < cd1.size(); ++i) {
				if (compareColumnDefinition(cd1.get(i), cd2.get(i)) == false)
					return false;
			}
 		}
		List<Index> i1 = c1.getIndexes();
		List<Index> i2 = c2.getIndexes();
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		if ((i1 != null) && (i2 != null)) {
			if (i1.size() != i2.size())
				return false;
			for (int i = 0; i < i1.size(); ++i) {
				if (compareIndex(i1.get(i), i2.get(i)) == false)
					return false;
			}
 		}
		List<String> s1 = c1.getTableOptionsStrings();
		List<String> s2 = c2.getTableOptionsStrings();
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if ((s1 != null) && (s2 != null)) {
			if (s1.size() != s2.size())
				return false;
			for (int i = 0; i < s1.size(); ++i) {
				if (s1.get(i).equals(s2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	// done
	public static boolean compareDelete(Delete d1, Delete d2) {
		if ((d1 == null) && (d2 == null))
			return true;
		if ((d1 == null) && (d2 != null))
			return false;
		if ((d1 != null) && (d2 == null))
			return false;
		if (compareTable(d1.getTable(), d2.getTable()) == false)
			return false;
		if (compareExpression(d1.getWhere(), d2.getWhere()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareDrop(Drop d1, Drop d2) {
		if ((d1 == null) && (d2 == null))
			return true;
		if ((d1 == null) && (d2 != null))
			return false;
		if ((d1 != null) && (d2 == null))
			return false;
		if (d1.getName().equals(d2.getName()) == false)
			return false;
		if ((d1.getType() != null) && (d2.getType() != null))
			if (d1.getType().equals(d2.getType()) == false)
				return false;
		
		return true;
	}
	
	// done
	public static boolean compareInsert(Insert i1, Insert i2) {
		if ((i1 == null) && (i2 == null))
			return true;
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		if (i1.isUseValues() != i2.isUseValues()) 
			return false;
		if (compareItemsList(i1.getItemsList(), i2.getItemsList()) == false)
			return false;
		if (compareTable(i1.getTable(), i2.getTable()) == false)
			return false;
		List<Column> l1 = i1.getColumns();
		List<Column> l2 = i2.getColumns();
		if ((l1 == null) && (l2 != null))
			return false;
		if ((l1 != null) && (l2 == null))
			return false;
		if ((l1 != null) && (l2 != null)) {
			if (l1.size() != l2.size())
				return false;
			for (int i = 0; i < l1.size(); ++i) {
				if (compareColumn(l1.get(i), l2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	// done
	public static boolean compareReplace(Replace i1, Replace i2) {
		if ((i1 == null) && (i2 == null))
			return true;
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		if (i1.isUseValues() != i2.isUseValues())
			return false;
		if (compareItemsList(i1.getItemsList(), i2.getItemsList()) == false)
			return false;
		List<Column> l1 = i1.getColumns();
		List<Column> l2 = i2.getColumns();
		if ((l1 == null) && (l2 != null))
			return false;
		if ((l1 != null) && (l2 == null))
			return false;
		if ((l1 != null) && (l2 != null)) {
			if (l1.size() != l2.size())
				return false;
			for (int i = 0; i < l1.size(); ++i) {
				if (compareColumn(l1.get(i), l2.get(i)) == false)
					return false;
			}
 		}
		List<Expression> e1 = i1.getExpressions();
		List<Expression> e2 = i2.getExpressions();
		if ((e1 == null) && (e2 != null))
			return false;
		if ((e1 != null) && (e2 == null))
			return false;
		if ((e1 != null) && (e2 != null)) {
			if (e1.size() != e2.size())
				return false;
			for (int i = 0; i < e1.size(); ++i) {
				if (compareExpression(e1.get(i), e2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	// done
	public static boolean compareSelect(Select s1, Select s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		SelectBody selectBody1 = s1.getSelectBody();
		SelectBody selectBody2 = s2.getSelectBody();
		return compareSelectBody(selectBody1, selectBody2);
	}
	
	// done
	public static boolean compareTruncate(Truncate t1, Truncate t2) {
		if ((t1 == null) && (t2 == null))
			return true;
		if ((t1 == null) && (t2 != null))
			return false;
		if ((t1 != null) && (t2 == null))
			return false;
		if (compareTable(t1.getTable(), t2.getTable()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareUpdate(Update u1, Update u2) {
		if ((u1 == null) && (u2 == null))
			return true;
		if ((u1 == null) && (u2 != null))
			return false;
		if ((u1 != null) && (u2 == null))
			return false;
		List<Column> c1 = u1.getColumns();
		List<Column> c2 = u2.getColumns();
		if ((c1 == null) && (c2 != null))
			return false;
		if ((c1 != null) && (c2 == null))
			return false;
		if ((c1 != null) && (c2 != null)) {
			if (c1.size() != c2.size())
				return false;
			for (int i = 0; i < c1.size(); ++i) {
				if (compareColumn(c1.get(i), c2.get(i)) == false)
					return false;
			}
 		}
		List<Expression> e1 = u1.getExpressions();
		List<Expression> e2 = u2.getExpressions();
		if ((e1 == null) && (e2 != null))
			return false;
		if ((e1 != null) && (e2 == null))
			return false;
		if ((e1 != null) && (e2 != null)) {
			if (e1.size() != e2.size())
				return false;
			for (int i = 0; i < e1.size(); ++i) {
				if (compareExpression(e1.get(i), e2.get(i)) == false)
					return false;
			}
 		}
		if (compareTable(u1.getTable(), u2.getTable()) == false)
			return false;
		if (compareExpression(u1.getWhere(), u2.getWhere()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareExpression(Expression e1, Expression e2) {
		if ((e1 == null) && (e2 == null))
			return true;
		if ((e1 == null) && (e2 != null))
			return false;
		if ((e1 != null) && (e2 == null))
			return false;
		if (e1.getClass().equals(e2.getClass())) {
			if (e1 instanceof AllComparisonExpression) {
				return compareAllComparisonExpression((AllComparisonExpression) e1, (AllComparisonExpression)e2);
			} else if (e1 instanceof AnyComparisonExpression) {
				return compareAnyComparisonExpression((AnyComparisonExpression)e1, (AnyComparisonExpression)e2);
			} else if (e1 instanceof Between) {
				return compareBetween((Between)e1, (Between)e2);
			} else if (e1 instanceof BinaryExpression) {
				return compareBinaryExpression((BinaryExpression)e1, (BinaryExpression)e2);
			} else if (e1 instanceof CaseExpression) {
				return compareCaseExpression((CaseExpression)e1, (CaseExpression)e2);
			} else if (e1 instanceof Column) {
				return compareColumn((Column)e1, (Column)e2);
			} else if (e1 instanceof DateValue) {
				return compareDateValue((DateValue)e1, (DateValue)e2);
			} else if (e1 instanceof DoubleValue) {
				return compareDoubleValue((DoubleValue)e1, (DoubleValue)e2);
			} else if (e1 instanceof ExistsExpression) {
				return compareExistsExpression((ExistsExpression)e1, (ExistsExpression)e2);
			} else if (e1 instanceof Function) {
				return compareFunction((Function)e1, (Function)e2);
			} else if (e1 instanceof InExpression) {
				return compareInExpression((InExpression)e1, (InExpression)e2);
			} else if (e1 instanceof InverseExpression) {
				return compareInverseExpression((InverseExpression)e1, (InverseExpression)e2);
			} else if (e1 instanceof IsNullExpression) {
				return compareIsNullExpression((IsNullExpression)e1, (IsNullExpression)e2);
			} else if (e1 instanceof JdbcParameter) {
				return compareJdbcParameter((JdbcParameter)e1, (JdbcParameter)e2);
			} else if (e1 instanceof LongValue) {
				return compareLongValue((LongValue)e1, (LongValue)e2);
			} else if (e1 instanceof NullValue) {
				return compareNullValue((NullValue)e1, (NullValue)e2);
			} else if (e1 instanceof Parenthesis) {
				return compareParenthesis((Parenthesis)e1, (Parenthesis)e2);
			} else if (e1 instanceof StringValue) {
				return compareStringValue((StringValue)e1, (StringValue)e2);
			} else if (e1 instanceof SubSelect) {
				return compareSubSelect((SubSelect)e1, (SubSelect)e2);
			} else if (e1 instanceof TimestampValue) {
				return compareTimestampValue((TimestampValue)e1, (TimestampValue)e2);
			} else if (e1 instanceof TimeValue) {
				return compareTimeValue((TimeValue)e1, (TimeValue)e2);
			} else if (e1 instanceof WhenClause) {
				return compareWhenClause((WhenClause)e1, (WhenClause)e2);
			} 
		}
		return false;
	}
	
	// done
	public static boolean compareAllComparisonExpression(AllComparisonExpression a1, AllComparisonExpression a2) {
		if ((a1 == null) && (a2 == null))
			return true;
		if ((a1 == null) && (a2 != null))
			return false;
		if ((a1 != null) && (a2 == null))
			return false;
		return compareSubSelect(a1.getSubSelect(), a2.getSubSelect());
	}
	
	// done
	public static boolean compareAnyComparisonExpression(AnyComparisonExpression a1, AnyComparisonExpression a2) {
		if ((a1 == null) && (a2 == null))
			return true;
		if ((a1 == null) && (a2 != null))
			return false;
		if ((a1 != null) && (a2 == null))
			return false;
		return compareSubSelect(a1.getSubSelect(), a2.getSubSelect());
	}
	
	// done
	public static boolean compareBetween(Between b1, Between b2) {
		if ((b1 == null) && (b2 == null))
			return true;
		if ((b1 == null) && (b2 != null))
			return false;
		if ((b1 != null) && (b2 == null))
			return false;
		if (b1.isNot() != b2.isNot())
			return false;
		if (compareExpression(b1.getBetweenExpressionEnd(), b2.getBetweenExpressionEnd()) == false)
			return false;
		if (compareExpression(b1.getBetweenExpressionStart(), b2.getBetweenExpressionStart()) == false) 
			return false;
		if (compareExpression(b1.getLeftExpression(), b2.getLeftExpression()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareBinaryExpression(BinaryExpression b1, BinaryExpression b2) {
		if ((b1 == null) && (b2 == null))
			return true;
		if ((b1 == null) && (b2 != null))
			return false;
		if ((b1 != null) && (b2 == null))
			return false;
		if (compareExpression(b1.getLeftExpression(), b2.getLeftExpression()) == false)
			return false;
		if (compareExpression(b1.getRightExpression(), b2.getRightExpression()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareCaseExpression(CaseExpression c1, CaseExpression c2) {
		if ((c1 == null) && (c2 == null))
			return true;
		if ((c1 == null) && (c2 != null))
			return false;
		if ((c1 != null) && (c2 == null))
			return false;
		if (compareExpression(c1.getElseExpression(), c2.getElseExpression()) == false)
			return false;
		if (compareExpression(c1.getSwitchExpression(), c2.getSwitchExpression()) == false)
			return false;
		List<WhenClause> l1 = c1.getWhenClauses();
		List<WhenClause> l2 = c2.getWhenClauses();
		if ((l1 == null) && (l2 != null))
			return false;
		if ((l1 != null) && (l2 == null))
			return false;
		if ((l1 != null) && (l2 != null)) {
			if (l1.size() != l2.size())
				return false;
			for (int i = 0; i < l1.size(); ++i) {
				if (compareWhenClause(l1.get(i), l2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	// done
	public static boolean compareColumn(Column c1, Column c2) {
		if ((c1 == null) && (c2 == null))
			return true;
		if ((c1 == null) && (c2 != null))
			return false;
		if ((c1 != null) && (c2 == null))
			return false;
		if (compareTable(c1.getTable(), c2.getTable()) == false)
			return false;
		if ((c1.getColumnName() != null) && (c2.getColumnName() != null))
			if (c1.getColumnName().equals(c2.getColumnName()) == false)
				return false;
		return true;
	}
	
	// done
	public static boolean compareDateValue(DateValue d1, DateValue d2) {
		if ((d1 == null) && (d2 == null))
			return true;
		if ((d1 == null) && (d2 != null))
			return false;
		if ((d1 != null) && (d2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareDoubleValue(DoubleValue d1, DoubleValue d2) {
		if ((d1 == null) && (d2 == null))
			return true;
		if ((d1 == null) && (d2 != null))
			return false;
		if ((d1 != null) && (d2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareExistsExpression(ExistsExpression e1, ExistsExpression e2) {
		if ((e1 == null) && (e2 == null))
			return true;
		if ((e1 == null) && (e2 != null))
			return false;
		if ((e1 != null) && (e2 == null))
			return false;
		if (e1.isNot() != e2.isNot())
			return false;
		if (compareExpression(e1.getRightExpression(), e2.getRightExpression()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareFunction(Function f1, Function f2) {
		if ((f1 == null) && (f2 == null))
			return true;
		if ((f1 == null) && (f2 != null))
			return false;
		if ((f1 != null) && (f2 == null))
			return false;
		if (f1.getName().equals(f2.getName()) == false)
			return false;
		if (f1.isAllColumns() != f2.isAllColumns())
			return false;
		if (f1.isDistinct() != f2.isDistinct())
			return false;
		if (f1.isEscaped() != f2.isEscaped())
			return false;
		if (compareExpressionList(f1.getParameters(), f2.getParameters()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareInExpression(InExpression i1, InExpression i2) {
		if ((i1 == null) && (i2 == null))
			return true;
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		if (i1.isNot() != i2.isNot())
			return false;
		if (compareExpression(i1.getLeftExpression(), i2.getLeftExpression()) == false)
			return false;
		if (compareItemsList(i1.getItemsList(), i2.getItemsList()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareInverseExpression(InverseExpression i1, InverseExpression i2) {
		if ((i1 == null) && (i2 == null))
			return true;
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		return compareExpression(i1.getExpression(), i2.getExpression());
	}
	
	// done
	public static boolean compareIsNullExpression(IsNullExpression i1, IsNullExpression i2) {
		if ((i1 == null) && (i2 == null))
			return true;
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		if (compareExpression(i1.getLeftExpression(), i2.getLeftExpression()) == false)
			return false;
		if (i1.isNot() != i2.isNot())
			return false;
		return true;
	}
	
	// done
	public static boolean compareJdbcParameter(JdbcParameter j1, JdbcParameter j2) {
		if ((j1 == null) && (j2 == null))
			return true;
		if ((j1 == null) && (j2 != null))
			return false;
		if ((j1 != null) && (j2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareLongValue(LongValue l1, LongValue l2) {
		if ((l1 == null) && (l2 == null))
			return true;
		if ((l1 == null) && (l2 != null))
			return false;
		if ((l1 != null) && (l2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareNullValue(NullValue n1, NullValue n2) {
		if ((n1 == null) && (n2 == null))
			return true;
		if ((n1 == null) && (n2 != null))
			return false;
		if ((n1 != null) && (n2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareParenthesis(Parenthesis p1, Parenthesis p2) {
		if ((p1 == null) && (p2 == null))
			return true;
		if ((p1 == null) && (p2 != null))
			return false;
		if ((p1 != null) && (p2 == null))
			return false;
		if (p1.isNot() != p2.isNot())
			return false;
		if (compareExpression(p1.getExpression(), p2.getExpression()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareStringValue(StringValue s1, StringValue s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareSubSelect(SubSelect s1, SubSelect s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if (compareSelectBody(s1.getSelectBody(), s2.getSelectBody()) == false)
			return false;
		if ((s1.getAlias() != null) && (s2.getAlias() != null)) {
			if (s1.getAlias().equals(s2.getAlias()) == false)
				return false;
		}
		return true;
	}
	
	// done
	public static boolean compareTimestampValue(TimestampValue t1, TimestampValue t2) {
		if ((t1 == null) && (t2 == null))
			return true;
		if ((t1 == null) && (t2 != null))
			return false;
		if ((t1 != null) && (t2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareTimeValue(TimeValue t1, TimeValue t2) {
		if ((t1 == null) && (t2 == null))
			return true;
		if ((t1 == null) && (t2 != null))
			return false;
		if ((t1 != null) && (t2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareWhenClause(WhenClause w1, WhenClause w2) {
		if ((w1 == null) && (w2 == null))
			return true;
		if ((w1 == null) && (w2 != null))
			return false;
		if ((w1 != null) && (w2 == null))
			return false;
		if (compareExpression(w1.getWhenExpression(), w2.getWhenExpression()) == false)
			return false;
		if (compareExpression(w1.getThenExpression(), w2.getThenExpression()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareSelectBody(SelectBody s1, SelectBody s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		
		if (s1.getClass().equals(s2.getClass())) {
			if (s1 instanceof PlainSelect) {
				return comparePlainSelect((PlainSelect)s1, (PlainSelect)s2);
			} else if (s1 instanceof Union) {
				return compareUnion((Union)s1, (Union)s2);
			}
		}
		return false;
	}
	
	// done
	public static boolean comparePlainSelect(PlainSelect p1, PlainSelect p2) {
		if ((p1 == null) && (p2 == null))
			return true;
		if ((p1 == null) && (p2 != null))
			return false;
		if ((p1 != null) && (p2 == null))
			return false;
		// compare Top
		Top top1 = p1.getTop();
		Top top2 = p2.getTop();
		if (compareTop(top1, top2) == false)
			return false;
		// compare Distinct
		Distinct d1 = p1.getDistinct();
		Distinct d2 = p2.getDistinct();
		if (compareDistinct(d1, d2) == false) 
			return false;
		// compare from item
		FromItem f1 = p1.getFromItem();
		FromItem f2 = p2.getFromItem();
		if (compareFromItem(f1, f2) == false)
			return false;
		// compare group by column list
		List<Column> g1 = p1.getGroupByColumnReferences();
		List<Column> g2 = p2.getGroupByColumnReferences();
		if ((g1 == null) && (g2 != null))
			return false;
		if ((g1 != null) && (g2 == null))
			return false;
		if ((g1 != null) && (g2 != null)) {
			if (g1.size() != g2.size())
				return false;
			for (int i = 0; i < g1.size(); ++i) {
				if (compareExpression(g1.get(i), g2.get(i)) == false)
					return false;
			}
 		}
		// compare having expression
		Expression h1 = p1.getHaving();
		Expression h2 = p2.getHaving();
		if (compareExpression(h1, h2) == false)
			return false;
		// compare Join list
		List<Join> j1 = p1.getJoins();
		List<Join> j2 = p2.getJoins();
		if ((j1 == null) && (j2 != null))
			return false;
		if ((j1 != null) && (j2 == null))
			return false;
		if ((j1 != null) && (j2 != null)) {
			if (j1.size() != j2.size())
				return false;
			for (int i = 0; i < j1.size(); ++i) {
				if (compareJoin(j1.get(i), j2.get(i)) == false)
					return false;
			}
 		}
		// compare limit
		Limit l1 = p1.getLimit();
		Limit l2 = p2.getLimit();
		if (compareLimit(l1, l2) == false)
			return false;
		// compare order by list
		List<OrderByElement> o1 = p1.getOrderByElements();
		List<OrderByElement> o2 = p2.getOrderByElements();
		if ((o1 == null) && (o2 != null))
			return false;
		if ((o1 != null) && (o2 == null))
			return false;
		if ((o1 != null) && (o2 != null)) {
			if (o1.size() != o2.size())
				return false;
			for (int i = 0; i < o1.size(); ++i) {
				if (compareOrderByElement(o1.get(i), o2.get(i)) == false)
					return false;
			}
 		}
		// compare Select Item list
		List<SelectItem> s1 = p1.getSelectItems();
		List<SelectItem> s2 = p2.getSelectItems();
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if ((s1 != null) && (s2 != null)) {
			if (s1.size() != s2.size())
				return false;
			for (int i = 0; i < s1.size(); ++i) {
				if (compareSelectItem(s1.get(i), s2.get(i)) == false)
					return false;
			}
 		}
		// compare where expression
		Expression w1 = p1.getWhere();
		Expression w2 = p2.getWhere();
		if (compareExpression(w1, w2) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareUnion(Union u1, Union u2) {
		if ((u1 == null) && (u2 == null))
			return true;
		if ((u1 == null) && (u2 != null))
			return false;
		if ((u1 != null) && (u2 == null))
			return false;
		// compare isAll
		if (u1.isAll() != u2.isAll())
			return false;
		// compare isDistinct
		if (u1.isDistinct() != u2.isDistinct())
			return false;
		// compare limit
		Limit l1 = u1.getLimit();
		Limit l2 = u2.getLimit();
		if (compareLimit(l1, l2) == false)
			return false;
		// compare order by list
		List<OrderByElement> o1 = u1.getOrderByElements();
		List<OrderByElement> o2 = u2.getOrderByElements();
		if ((o1 == null) && (o2 != null))
			return false;
		if ((o1 != null) && (o2 == null))
			return false;
		if ((o1 != null) && (o2 != null)) {
			if (o1.size() != o2.size())
				return false;
			for (int i = 0; i < o1.size(); ++i) {
				if (compareOrderByElement(o1.get(i), o2.get(i)) == false)
					return false;
			}
 		}
		// compare PlainSelect
		List<PlainSelect> p1 = u1.getPlainSelects();
		List<PlainSelect> p2 = u2.getPlainSelects();
		if ((p1 == null) && (p2 != null))
			return false;
		if ((p1 != null) && (p2 == null))
			return false;
		if ((p1 != null) && (p2 != null)) {
			if (p1.size() != p2.size())
				return false;
			for (int i = 0; i < p1.size(); ++i) {
				if (comparePlainSelect(p1.get(i), p2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	// done
	public static boolean compareDistinct(Distinct d1, Distinct d2) {
		if ((d1 == null) && (d2 == null))
			return true;
		if ((d1 == null) && (d2 != null))
			return false;
		if ((d1 != null) && (d2 == null))
			return false;
		List<SelectExpressionItem> l1 = d1.getOnSelectItems();
		List<SelectExpressionItem> l2 = d2.getOnSelectItems();
		if ((l1 == null) && (l2 != null)) {
			return false;
		} 
		if ((l1 != null) && (l2 == null)) {
			return false;
		}
 		if ((l1 != null) && (l2 != null)) {
 			if (l1.size() != l2.size())
 				return false;
 			for (int i = 0; i < l1.size(); ++i) {
 				if (compareSelectExpressionItem(l1.get(i), l2.get(i)) == false)
 					return false;
 			}
 		}
		return true;
	}
	
	// done
	public static boolean compareFromItem(FromItem f1, FromItem f2) {
		if ((f1 == null) && (f2 == null))
			return true;
		if ((f1 == null) && (f2 != null))
			return false;
		if ((f1 != null) && (f2 == null))
			return false;
		if (f1.getClass().equals(f2.getClass())) {
			if (f1 instanceof SubJoin)
				return compareSubJoin((SubJoin)f1, (SubJoin)f2);
			else if (f1 instanceof SubSelect) 
				return compareSubSelect((SubSelect)f1, (SubSelect)f2);
			else if (f1 instanceof Table) 
				return compareTable((Table)f1, (Table)f2);
		}
		return false;
	}
	
	// done
	public static boolean compareJoin(Join j1, Join j2) {
		if ((j1 == null) && (j2 == null))
			return true;
		if ((j1 == null) && (j2 != null))
			return false;
		if ((j1 != null) && (j2 == null))
			return false;
		if (j1.isFull() != j2.isFull())
			return false;
		if (j1.isLeft() != j2.isLeft())
			return false;
		if (j1.isInner() != j2.isInner())
			return false;
		if (j1.isNatural() != j2.isNatural())
			return false;
		if (j1.isOuter() != j2.isOuter())
			return false;
		if (j1.isRight() != j2.isRight())
			return false;
		if (j1.isSimple() != j2.isSimple())
			return false;
		if (compareFromItem(j1.getRightItem(), j2.getRightItem()) == false)
			return false;
		if (compareExpression(j1.getOnExpression(), j2.getOnExpression()) == false)
			return false;
		List<Column> u1 = j1.getUsingColumns();
		List<Column> u2 = j2.getUsingColumns();
		if ((u1 == null) && (u2 != null))
			return false;
		if ((u1 != null) && (u2 == null))
			return false;
		if ((u1 != null) && (u2 != null)) {
			if (u1.size() != u2.size())
				return false;
			for (int i = 0; i < u1.size(); ++i) {
				if (compareColumn(u1.get(i), u2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	// done
	public static boolean compareLimit(Limit l1, Limit l2) {
		if ((l1 == null) && (l2 == null))
			return true;
		if ((l1 == null) && (l2 != null))
			return false;
		if ((l1 != null) && (l2 == null))
			return false;
		if (l1.getOffset() != l2.getOffset())
			return false;
		if (l1.getRowCount() != l2.getRowCount())
			return false;
		if (l1.isLimitAll() != l2.isLimitAll())
			return false;
		if (l1.isOffsetJdbcParameter() != l2.isOffsetJdbcParameter())
			return false;
		if (l1.isRowCountJdbcParameter() != l2.isRowCountJdbcParameter())
			return false;
		return true;
	}
	
	// done
	public static boolean compareOrderByElement(OrderByElement o1, OrderByElement o2) {
		if ((o1 == null) && (o2 == null))
			return true;
		if ((o1 == null) && (o2 != null))
			return false;
		if ((o1 != null) && (o2 == null))
			return false;
		if (o1.isAsc() != o2.isAsc())
			return false;
		if (compareExpression(o1.getExpression(), o2.getExpression()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareSelectItem(SelectItem s1, SelectItem s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if (s1.getClass().equals(s2.getClass())) {
			if (s1 instanceof AllColumns)
				return compareAllColumns((AllColumns)s1, (AllColumns)s2);
			else if (s1 instanceof AllTableColumns) 
				return compareAllTableColumns((AllTableColumns)s1, (AllTableColumns)s2);
			else if (s1 instanceof SelectExpressionItem)
				return compareSelectExpressionItem((SelectExpressionItem)s1, (SelectExpressionItem)s2);
		}
		return false;
	}
	
	//done
	public static boolean compareAllColumns(AllColumns a1, AllColumns a2) {
		if ((a1 == null) && (a2 == null))
			return true;
		if ((a1 == null) && (a2 != null))
			return false;
		if ((a1 != null) && (a2 == null))
			return false;
		return true;
	}
	
	// done
	public static boolean compareAllTableColumns(AllTableColumns a1, AllTableColumns a2) {
		if ((a1 == null) && (a2 == null))
			return true;
		if ((a1 == null) && (a2 != null))
			return false;
		if ((a1 != null) && (a2 == null))
			return false;
		return compareTable(a1.getTable(), a2.getTable());
	}
	
	// done
	public static boolean compareSelectExpressionItem(SelectExpressionItem s1, SelectExpressionItem s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if ((s1.getAlias() != null) && (s2.getAlias() != null)) {
			if (s1.getAlias().equals(s2.getAlias()) == false)
				return false;
		}
		return compareExpression(s1.getExpression(), s2.getExpression());
	}
	
	// done
	public static boolean compareTable(Table t1, Table t2) {
		if ((t1 == null) && (t2 == null))
			return true;
		if ((t1 == null) && (t2 != null))
			return false;
		if ((t1 != null) && (t2 == null))
			return false;
		if ((t1.getAlias() != null) && (t2.getAlias() != null))
			if (t1.getAlias().equals(t2.getAlias()) == false)
				return false;
		if ((t1.getName() != null) && (t2.getName() != null))
			if (t1.getName().equals(t2.getName()) == false)
				return false;
		if ((t1.getSchemaName() != null) && (t2.getSchemaName() != null))
			if (t1.getSchemaName().equals(t2.getSchemaName()) == false)
				return false;
		return true;
	} 
	
	// done
	public static boolean compareSubJoin(SubJoin s1, SubJoin s2) {
		if ((s1 == null) && (s2 == null))
			return true;
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if (compareJoin(s1.getJoin(), s2.getJoin()) == false)
			return false;
		if (compareFromItem(s1.getLeft(), s2.getLeft()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareItemsList(ItemsList i1, ItemsList i2) {
		if ((i1 == null) && (i2 == null))
			return true;
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		if (i1.getClass().equals(i2.getClass())) {
			if (i1 instanceof ExpressionList)
				return compareExpressionList((ExpressionList)i1, (ExpressionList)i2);
			if (i1 instanceof SubSelect)
				return compareSubSelect((SubSelect)i1, (SubSelect)i2);
		}
		return false;
	}
	
	// done
	public static boolean compareExpressionList(ExpressionList e1, ExpressionList e2) {
		if ((e1 == null) && (e2 == null))
			return true;
		if ((e1 == null) && (e2 != null))
			return false;
		if ((e1 != null) && (e2 == null))
			return false;
		List<Expression> l1 = e1.getExpressions();
		List<Expression> l2 = e2.getExpressions();
		if ((l1 == null) && (l2 != null))
			return false;
		if ((l1 != null) && (l2 == null))
			return false;
		if ((l1 != null) && (l2 != null)) {
			if (l1.size() != l2.size())
				return false;
			for (int i = 0; i < l1.size(); ++i) {
				if (compareExpression(l1.get(i), l2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	// done
	public static boolean compareColumnDefinition(ColumnDefinition c1, ColumnDefinition c2) {
		if ((c1 == null) && (c2 == null))
			return true;
		if ((c1 == null) && (c2 != null))
			return false;
		if ((c1 != null) && (c2 == null))
			return false;
		if ((c1.getColumnName() != null) && (c2.getColumnName() != null))
			if (c1.getColumnName().equals(c2.getColumnName()) == false)
				return false;
		List<String> s1 = c1.getColumnSpecStrings();
		List<String> s2 = c2.getColumnSpecStrings();
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if ((s1 != null) && (s2 != null)) {
			if (s1.size() != s2.size())
				return false;
			for (int i = 0; i < s1.size(); ++i) {
				if (s1.get(i).equals(s2.get(i)) == false)
					return false;
			}
 		}
		if (compareColDataType(c1.getColDataType(), c2.getColDataType()) == false)
			return false;
		return true;
	}
	
	// done
	public static boolean compareColDataType(ColDataType c1, ColDataType c2) {
		if ((c1 == null) && (c2 == null))
			return true;
		if ((c1 == null) && (c2 != null))
			return false;
		if ((c1 != null) && (c2 == null))
			return false;
		List<String> s1 = c1.getArgumentsStringList();
		List<String> s2 = c2.getArgumentsStringList();
		if ((s1 == null) && (s2 != null))
			return false;
		if ((s1 != null) && (s2 == null))
			return false;
		if ((s1 != null) && (s2 != null)) {
			if (s1.size() != s2.size())
				return false;
			for (int i = 0; i < s1.size(); ++i) {
				if (s1.get(i).equals(s2.get(i)) == false)
					return false;
			}
 		}
		if ((c1.getDataType() != null) && (c2.getDataType() != null))
			if (c1.getDataType().equals(c2.getDataType()) == false)
				return false;
		return true;
	}
	
	// done
	public static boolean compareIndex(Index i1, Index i2) {
		if ((i1 == null) && (i2 == null))
			return true;
		if ((i1 == null) && (i2 != null))
			return false;
		if ((i1 != null) && (i2 == null))
			return false;
		if ((i1.getName() != null) && (i2.getName() != null))
			if (i1.getName().equals(i2.getName()) == false)
				return false;
		if ((i1.getType() != null) && (i2.getType() != null)) 
			if (i1.getType().equals(i2.getType()) == false)
				return false;
		List<String> c1 = i1.getColumnsNames();
		List<String> c2 = i2.getColumnsNames();
		if ((c1 == null) && (c2 != null))
			return false;
		if ((c1 != null) && (c2 == null))
			return false;
		if ((c1 != null) && (c2 != null)) {
			if (c1.size() != c2.size())
				return false;
			for (int i = 0; i < c1.size(); ++i) {
				if (c1.get(i).equals(c2.get(i)) == false)
					return false;
			}
 		}
		return true;
	}
	
	public static boolean compareTop(Top top1, Top top2) {
		if ((top1 == null) && (top2 == null))
			return true;
		if ((top1 == null) && (top2 != null))
			return false;
		if ((top1 != null) && (top2 == null))
			return false;
		if (top1.getRowCount() == top2.getRowCount())
			return true;
		return false;
	}
}

