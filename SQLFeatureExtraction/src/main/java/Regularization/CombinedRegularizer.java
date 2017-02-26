package Regularization;
import java.util.HashSet;
import net.sf.jsqlparser.statement.select.Select;

/**
 * A combination of modules chosen by the user that forms a final query regularization strategy
 * @author tingxie
 *
 */
public class CombinedRegularizer {
	/**
	 * regulariza a query with selected modules
	 * @param input
	 * @param modules
	 * @return
	 */
    public static Select regularize(Select input,HashSet<Integer> modules){ 
    	Select result=input;
    	//first solve naming problem first
    	//currently do not consider any context
    	if(modules.contains(1)){
    	SelectNamingResolver resolver=new SelectNamingResolver(result,true);
    	result=resolver.aliasReplaceSelect();
    	}
    	//first try to remove sub-queries in predicate-nested queries
    	if(modules.contains(2))
   	    result.setSelectBody(PredicateNestingCoalescer.predicateNestingCoalesceSelectBody(result.getSelectBody()));
   	   // from nesting coalescer
    	if(modules.contains(3))
   	    result.setSelectBody(FROMNestingCoalescer.FromNestingCoalesceSelectBody(result.getSelectBody()));
   	   //pull up UNIONs    
    	if(modules.contains(4))
    	result.setSelectBody(UNIONPULLer.UnionPullUpFromSelectBody(result.getSelectBody(), true)); 	    
    	//then coalesce all sub-queries  
    	return result;
    }
    
    
}
