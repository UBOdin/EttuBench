package querySimilarityMetrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * this class hides details about the actual representation of a feature vector
 * you can use given methods to modify the content of this vector 
 * @author tingxie
 *
 */
public class FeatureVector {
    private HashMap<Integer,Integer> labelMap=new HashMap<Integer,Integer>();
    private HashSet<Integer> distinctFeatures;
    
    /**
     * add one feature into this vector
     * @param featureID
     */
    public void addOneFeatureIn(int featureID){
    	Integer occur=this.labelMap.get(featureID);
    	if(occur==null)
    		this.labelMap.put(featureID, 1);
    	else
    		this.labelMap.put(featureID, occur+1);
    }
    
    /**
     * add the same feature with multiple occurrrences in
     * @param featureID
     * @param occurrence
     */
    public void addFeatureWithOccurrence(int featureID, int occurrence){
    	Integer occur=this.labelMap.get(featureID);
    	if(occur==null)
    		this.labelMap.put(featureID, occurrence);
    	else
    		this.labelMap.put(featureID, occur+occurrence);
    }
    
    /**
     * add the content of whole input feature vector into this vector
     */
    public void addWholeFeatureVectorIn(FeatureVector input){
    	for (Entry<Integer,Integer> en: input.labelMap.entrySet()){
             int featureID=en.getKey();
             int occurrence=en.getValue();
             Integer occur=this.labelMap.get(featureID);
         	if(occur==null)
         		this.labelMap.put(featureID, occurrence);
         	else
         		this.labelMap.put(featureID, occur+occurrence);
    	}
    }
    
    
    
    /**
     * get the occurrence number of input feature in this vector
     * @param featureID
     * @return
     */
    public int getFeatureOccurrence(int featureID){
    	Integer result=this.labelMap.get(featureID);
    	if(result==null)
    	return 0;
    	else 
    	return result;
    }
    
    /**
     * get the set of distinct features out from this vector
     * @return
     */
    public HashSet<Integer> getDistinctFeatures(){
    	if(this.distinctFeatures==null){
    	HashSet<Integer> result=new HashSet<Integer>();
    	for (Entry<Integer,Integer> en: this.labelMap.entrySet()){
    		result.add(en.getKey());
    	}
    	this.distinctFeatures=result;
    	}
    	
    	return this.distinctFeatures;
    }
    
    public String toString(){
    	return this.labelMap.toString();
    }
    
    
    
}
