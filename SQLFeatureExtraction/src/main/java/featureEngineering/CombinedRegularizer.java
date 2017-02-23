package featureEngineering;

import net.sf.jsqlparser.statement.select.SelectBody;

/**
 * Combine regularization/feature engineering rules together and 
 * form a final regularizer
 * @author tingxie
 *
 */
public class CombinedRegularizer {
	
	/**
	 * regularize a query with a combined rule
	 * @param input
	 */
    public static SelectBody regularize(SelectBody body){    	
   	   body=PredicateNestingCoalescer.predicateNestingCoalesceSelectBody(body, true); 	   
       body=FROMNestingCoalescer.FromNestingCoalesceSelectBody(body);       
       body=UNIONPULLer.UnionPullUpFromSelectBody(body, true); 	     
       return body;
    }
   
    
}
