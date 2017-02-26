package Regularization;


import booleanFormulaEntities.BooleanNormalClause;
import booleanFormulaEntities.FixedOrderExpression;
import net.sf.jsqlparser.expression.Expression;

/**
 * First attribute is a 'Value' Expression that
 * can be used to combine with that of other ConditionedValue;
 * Second attribute, if any,will be a conjunctive clause that describes the condition of this 'value'.
 * publicly it only accepts fixed order expression
 * @author tingxie
 *
 */
public class ContingentValue {
	public FixedOrderExpression value;
	public BooleanNormalClause conjClause;
	int hashcode=Integer.MIN_VALUE;

	private  ContingentValue(FixedOrderExpression value, BooleanNormalClause conditions){
		this.value=value;
		this.conjClause=conditions;
	}


	/**
	 * this controls the creation of conditioned value
	 * it returns null if the condition is a contradiction
	 * @param value
	 * @param conditions
	 * @return
	 */
	public static ContingentValue createValue(Expression value, BooleanNormalClause conditions){
		
		if (conditions==null||(conditions!=null&&conditions.ifTautology()!=-1)){
			return new ContingentValue(new FixedOrderExpression(value),conditions);
		}
		else{
			System.out.println("condition is contradiction, should not call this method to create conditioned value");
			return null;  
		}
	}

	/**
	 * this controls the creation of conditioned value
	 * it returns null if the condition is a contradiction
	 * @param value
	 * @param conditions
	 * @return
	 */
	public static ContingentValue createValue(FixedOrderExpression value, BooleanNormalClause conditions){
		int checkvalue=conditions.ifTautology();
		if (checkvalue!=-1&&value!=null){
			return new ContingentValue(value,conditions);
		}
		else{
			System.out.println("value is null or condition is contradiction, should not call this method to create conditioned value");
			return null;  
		}
	}

	@Override
	public boolean equals(Object obj){
		ContingentValue v=(ContingentValue) obj; 
		if(this.conjClause==null^v.conjClause==null)
			return false;
		else if (this.conjClause==null)
			return this.value.equals(v.value);
		else
			return this.value.equals(v.value)&&this.conjClause.equals(v.conjClause);
	}

	@Override
	public int hashCode(){
		if(this.hashcode==Integer.MIN_VALUE){
			if(this.conjClause!=null)
				this.hashcode=this.value.hashCode()+this.conjClause.hashCode();
			else
				this.hashcode=this.value.hashCode()+(new BooleanNormalClause(1)).hashCode();
		}
		return this.hashcode;
	}

	public String toString(){
		String line=value.toString();
		if(conjClause!=null){
			line+=" WHEN ";
			line+=conjClause.toString();
		}
		return line;
	}


}
