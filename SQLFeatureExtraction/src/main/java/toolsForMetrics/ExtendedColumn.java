package toolsForMetrics;

import java.util.Collection;
import java.util.TreeSet;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.schema.Table;

/**
 * A extended definition of a column. It can have the table name it belongs to.
 */
public final class ExtendedColumn extends net.sf.jsqlparser.schema.Column implements Comparable {

    private Table table;
    private String columnName;

    public ExtendedColumn() {
    }
    
    public ExtendedColumn(net.sf.jsqlparser.schema.Column column) {
    	setTable(column.getTable());
    	setColumnName(column.getColumnName());
    }

    public ExtendedColumn(Table table, String columnName) {
        setTable(table);
        setColumnName(columnName);
    }

    public ExtendedColumn(String columnName) {
        this(null, columnName);
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public String getColumnName() {
        return columnName.toUpperCase();
    }

    public void setColumnName(String string) {
        columnName = string;
    }

    public String getFullyQualifiedName() {
        StringBuilder fqn = new StringBuilder();

        if (table != null && table.getWholeTableName() != null) {
            fqn.append(table.getWholeTableName().toUpperCase());
        }
        if (fqn.length()>0) {
            fqn.append('.');
        }
        if (columnName != null) {
            fqn.append(columnName.toUpperCase());
        }
        return fqn.toString();
    }

    @Override
    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    @Override
    public String toString() {
        return getFullyQualifiedName();
    }
    
    @Override
    public boolean equals(Object o) {
    	ExtendedColumn parsedObject = (ExtendedColumn) o;
    	
    	if (parsedObject.toString().contains(".")){
    		if (parsedObject.toString().substring(parsedObject.toString().indexOf('.')).contains(this.toString())) {
    			return true;
    		}
    	}
    	
    	if (this.toString().contains(".")) {
    		if (this.toString().substring(this.toString().indexOf('.')).contains(parsedObject.toString())) {
    			return true;
    		}
    	}
    	
    	if (parsedObject.toString().equals(this.toString())) {
    		return true;
    	}
		return false;
    	
    }
    
    public static <ExtendedColumn> TreeSet<ExtendedColumn> intersect (Collection <? extends ExtendedColumn> set1, Collection <? extends ExtendedColumn> set2)
    {
    	TreeSet<ExtendedColumn> result = new TreeSet<ExtendedColumn> ();
    	TreeSet<ExtendedColumn> s1 = new TreeSet<ExtendedColumn>(set1);
    	TreeSet<ExtendedColumn> s2 = new TreeSet<ExtendedColumn>(set2);

        for (ExtendedColumn t: s1) {
            if (s2.remove (t))
            	result.add (t);
        }

        return result;
    }

	@Override
	public int compareTo(Object o) {
		if (this.equals(o)) {
			return 0;
		}
		else {
			return this.toString().compareTo(((ExtendedColumn)o).toString());
		}
	}
}
