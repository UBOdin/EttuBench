package toolsForMetrics;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.schema.Column;

/**
 * this defines the data type of a tuple,here it is defined as an ArrayList of LeafValues
 * @author Ting Xie
 *
 */
public class Schema {

	private List<Column> schema;

	public Schema() {
		schema = new ArrayList<Column>();
	}

	public List<Column> getValues() {
		return this.schema;
	}

	public void setValues(List<Column> schema) {
		this.schema = schema;
	}

	public void setValue(Column col, int i) {
		this.schema.set(i, col);
	}

	public void addValue(Column value) {
		this.schema.add(value);
	}

	public void addAll(List<Column> value) {
		this.schema.addAll(value);
	}

	public String toString() {
		String result = "";
		if (schema == null)
			return "NULL";
		else {
			for (Column value : this.schema)
				result += value.toString() + "|";
		}
		return result;

	}

}
