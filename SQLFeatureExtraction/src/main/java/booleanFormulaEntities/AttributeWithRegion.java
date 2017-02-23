package booleanFormulaEntities;

import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;

/**
 * a data structure that describes a column/attribute with its filtering condition
 * that specifies a range/region
 * @author tingxie
 *
 */
public class AttributeWithRegion {
      Expression exp;
      double lbound;
      double rbound;
      public static final double epsilon=1.0E-7;
      
      private AttributeWithRegion(Expression exp,double lbound,double rbound){
    	  this.exp=exp;
    	  this.lbound=lbound;
    	  this.rbound=rbound;
      }
      
      /**
       * all regions all inclusive, we will epsilon to distinguish inclusive/non-inclusive
       * @param exp
       * @return
       */
	public static AttributeWithRegion generateRegion(Expression exp){
		
		if (exp instanceof MinorThan){
		   MinorThan mt=(MinorThan) exp;
		   Expression left=mt.getLeftExpression();
		   Expression right=mt.getRightExpression();
		   
		   if((left instanceof Column||left instanceof Function)&&(right instanceof LongValue||right instanceof DoubleValue)){
			  if (right instanceof LongValue) 
				  return new AttributeWithRegion(left,Double.NEGATIVE_INFINITY,(double)((LongValue) right).getValue()-epsilon);
			  else 
				  return new AttributeWithRegion(left,Double.NEGATIVE_INFINITY,((DoubleValue)right).getValue()-epsilon);							
		   }
		   else if((right instanceof Column||right instanceof Function)&&(left instanceof LongValue||left instanceof DoubleValue)){
				  if (left instanceof LongValue) 
					  return new AttributeWithRegion(right,(double)((LongValue) left).getValue()+epsilon,Double.POSITIVE_INFINITY);
				  else 
					  return new AttributeWithRegion(right,((DoubleValue)right).getValue()+epsilon,Double.POSITIVE_INFINITY);							 
		   }		   
		}
		else if (exp instanceof EqualsTo){
			EqualsTo eq=(EqualsTo) exp;
			   Expression left=eq.getLeftExpression();
			   Expression right=eq.getRightExpression();
			   Expression target = null;
			   double regionvalue = Double.NEGATIVE_INFINITY;
			   
			   if(left instanceof Column||left instanceof Function)
				   target=left;
			   else if (left instanceof LongValue){
				   LongValue lv=(LongValue) left;
				   regionvalue=(double)lv.getValue(); 
			   }
			   else if (left instanceof DoubleValue){
				   DoubleValue lv=(DoubleValue) left;
				   regionvalue=lv.getValue(); 
			   }			      
			   
			   if(right instanceof Column||right instanceof Function)
				   target=right;
			   else if (right instanceof LongValue){
				   LongValue lv=(LongValue) right;
				   regionvalue=(double)lv.getValue(); 
			   }
			   else if (right instanceof DoubleValue){
				   DoubleValue lv=(DoubleValue) right;
				   regionvalue=lv.getValue(); 
			   }
			   
			   if(target!=null&&regionvalue>Double.NEGATIVE_INFINITY)
				   return new AttributeWithRegion(target,regionvalue,regionvalue);
		}
		else if (exp instanceof MinorThanEquals){
			   MinorThanEquals mte=(MinorThanEquals) exp;
			   Expression left=mte.getLeftExpression();
			   Expression right=mte.getRightExpression();			   
			   if((left instanceof Column||left instanceof Function)&&(right instanceof LongValue||right instanceof DoubleValue)){
				  if (right instanceof LongValue) 
					  return new AttributeWithRegion(left,Double.NEGATIVE_INFINITY,(double)((LongValue) right).getValue());
				  else 
					  return new AttributeWithRegion(left,Double.NEGATIVE_INFINITY,((DoubleValue)right).getValue());							
			   }
			   else if((right instanceof Column||right instanceof Function)&&(left instanceof LongValue||left instanceof DoubleValue)){
					  if (left instanceof LongValue) 
						  return new AttributeWithRegion(right,(double)((LongValue) left).getValue(),Double.POSITIVE_INFINITY);
					  else 
						  return new AttributeWithRegion(right,((DoubleValue)left).getValue(),Double.POSITIVE_INFINITY);							 
			   }
		}
		else if (exp instanceof GreaterThan){
			GreaterThan gt=(GreaterThan) exp;
			   Expression left=gt.getLeftExpression();
			   Expression right=gt.getRightExpression();
			   if((right instanceof Column||right instanceof Function)&&(left instanceof LongValue||left instanceof DoubleValue)){
				  if (left instanceof LongValue) 
					  return new AttributeWithRegion(right,Double.NEGATIVE_INFINITY,(double)((LongValue) left).getValue()-epsilon);
				  else 
					  return new AttributeWithRegion(right,Double.NEGATIVE_INFINITY,((DoubleValue)left).getValue()-epsilon);							
			   }
			   else if((left instanceof Column||left instanceof Function)&&(right instanceof LongValue||right instanceof DoubleValue)){
					  if (right instanceof LongValue) 
						  return new AttributeWithRegion(left,(double)((LongValue) right).getValue()+epsilon,Double.POSITIVE_INFINITY);
					  else 
						  return new AttributeWithRegion(left,((DoubleValue)right).getValue()+epsilon,Double.POSITIVE_INFINITY);							 
			   }
		}
		else if (exp instanceof GreaterThanEquals){
			GreaterThanEquals gte=(GreaterThanEquals) exp;
			   Expression left=gte.getLeftExpression();
			   Expression right=gte.getRightExpression();
			   if((right instanceof Column||right instanceof Function)&&(left instanceof LongValue||left instanceof DoubleValue)){
				  if (left instanceof LongValue) 
					  return new AttributeWithRegion(right,Double.NEGATIVE_INFINITY,(double)((LongValue) left).getValue());
				  else 
					  return new AttributeWithRegion(right,Double.NEGATIVE_INFINITY,((DoubleValue)left).getValue());							
			   }
			   else if((left instanceof Column||left instanceof Function)&&(right instanceof LongValue||right instanceof DoubleValue)){
					  if (right instanceof LongValue) 
						  return new AttributeWithRegion(left,(double)((LongValue) right).getValue(),Double.POSITIVE_INFINITY);
					  else 
						  return new AttributeWithRegion(left,((DoubleValue)right).getValue(),Double.POSITIVE_INFINITY);							 
			   }
		}
		else if (exp instanceof Between){
			Between bt=(Between) exp;
			   Expression left=bt.getBetweenExpressionStart();
			   Expression right=bt.getBetweenExpressionEnd();
			   Expression target = bt.getLeftExpression();
			   double regionleftvalue = Double.NEGATIVE_INFINITY;
			   double regionrightvalue = Double.POSITIVE_INFINITY;
			   
			   if(target instanceof Column||target instanceof Function){
				   if(left instanceof LongValue){
					   LongValue lv=(LongValue) left;
					   regionleftvalue=(double)lv.getValue();					   
				   }
				   else if (left instanceof DoubleValue){
						   DoubleValue lv=(DoubleValue) left;
						   regionleftvalue=lv.getValue(); 
				   }
				   else return null;
				   
				   if(right instanceof LongValue){
					   LongValue lv=(LongValue) right;
					   regionrightvalue=(double)lv.getValue();					   
				   }
				   else if (right instanceof DoubleValue){
						   DoubleValue lv=(DoubleValue) right;
						   regionrightvalue=lv.getValue(); 
				   }
				   else return null;  
				   return new AttributeWithRegion(target,regionleftvalue,regionrightvalue);  
			   }	
		}
		
		return null;
	}
}
