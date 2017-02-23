package toolsForMetrics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;



import net.sf.jsqlparser.expression.LeafValue;
import net.sf.jsqlparser.expression.StringValue;

/**
 * this defines the data type of a tuple,here it is defined as an ArrayList of
 * LeafValues
 * 
 * @author Ting Xie
 *
 */
public class Tuple implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2331158075588259089L;
	
	private List<LeafValue> tuple;

	public Tuple() {
		tuple = new ArrayList<LeafValue>();
	}

	public List<LeafValue> getValues() {
		return this.tuple;
	}
	
	public LeafValue getValue(int index) {
		return tuple.get(index);
	}

	public void setValues(List<LeafValue> tuple) {
		this.tuple = tuple;
	}

	public void addValue(LeafValue value) {
		this.tuple.add(value);
	}

	public void addAll(List<LeafValue> value) {
		this.tuple.addAll(value);
	}

	public String toString() {
		String result = "";
		if (tuple == null)
			return "NULL";
		else {
			for (int i = 0; i < this.tuple.size() - 1; i++) {
				if (this.tuple.get(i) instanceof StringValue)
					result +=((StringValue) this.tuple.get(i)).getNotExcapedValue() + "|";
				else
					result += this.tuple.get(i).toString() + "|";
			}
			if (this.tuple.get(this.tuple.size() - 1) instanceof StringValue)
				result +=((StringValue) this.tuple.get(this.tuple.size() - 1)).getNotExcapedValue();
			else
				result +=( this.tuple.get(this.tuple.size() - 1)).toString();

		}
		return result;

	}
	
	public boolean equals(Tuple other) {
		//starting optimistic
		boolean retVal = true;
		
		if (other.getValues().size() != this.getValues().size()) {
			retVal = false;
		}
		else {
			for (int i = 0; i < other.getValues().size(); i++) {
				if (this.getValue(i).equals(other.getValue(i))) {
					retVal = true;
				}
				else {
					return false;
				}
			}
		}
		
		return retVal;
	}

}
