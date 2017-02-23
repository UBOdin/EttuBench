package querySimilarityMetrics;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * this class hides details about the actual representation of a feature vector
 * you can use given methods to modify the content of this vector 
 * @author tingxie
 *
 */
public class FeatureVector{
    private HashMap<Integer,Integer> labelMap=new HashMap<Integer,Integer>();
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
   
    public int length(){
    	return this.labelMap.size();
    }
    
    /**
     * add the same feature with multiple occurrrences in
     * @param featureID
     * @param occurrence
     */
    public void addFeatureWithOccurrence(int featureID, int occurrence){
    	if(occurrence>0){
    	Integer occur=this.labelMap.get(featureID);
    	if(occur==null)
    		this.labelMap.put(featureID, occurrence);
    	else
    		this.labelMap.put(featureID, occur+occurrence);
    	}
    	else {
    		System.out.println("occurrence must be positive when adding feature to feature vector!");
    		return;
    	}
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
    public Set<Integer> getDistinctFeatures(){ 
    	return this.labelMap.keySet();
    }
    
    public String toString(){
    	return this.labelMap.toString();
    }
    
    public static FeatureVector readFeatureVectorFromFormattedString(String line){
    	//parse into feature vector
		String tokens[]=line.split("\\s+|,");
		FeatureVector featurevector=new FeatureVector();
		for (String token: tokens){
			String innertokens[]=token.split(":");
			int label=Integer.parseInt(innertokens[0]);
			int occur=Integer.parseInt(innertokens[1]);
			featurevector.addFeatureWithOccurrence(label, occur);
		}
		return featurevector;    	
    }
    
    public String toFormattedString(){
    	String line="";
    	for(Entry<Integer,Integer> en:this.labelMap.entrySet()){
    		line+=","+en.getKey()+":"+en.getValue();
    	}
    	return line.substring(1,line.length());
    }
    
    public boolean isEmpty(){
    	return this.labelMap.isEmpty();
    }
    
    @Override
    public boolean equals(Object o){
    	FeatureVector vector=(FeatureVector) o;
    	return this.labelMap.equals(vector.labelMap);
    }
    
    @Override
    public int hashCode(){
    	return this.labelMap.hashCode();
    }
    
    public static FeatureVector intersection(FeatureVector left, FeatureVector right){
    	FeatureVector intersec=new FeatureVector();
    	int leftlength=left.length();
    	int rightlength=right.length();
    	if(leftlength>rightlength){
    		Set<Integer> rightfeatures= right.getDistinctFeatures();
    		for(Integer ID: rightfeatures){
    			int rightoccur=right.getFeatureOccurrence(ID);
    			int leftoccur=left.getFeatureOccurrence(ID);
    			int min=Math.min(rightoccur, leftoccur);
    			if(min>0)
    				intersec.addFeatureWithOccurrence(ID, min);
    		}
    	}
    	else {
    		Set<Integer> leftfeatures= left.getDistinctFeatures();
    		for(Integer ID: leftfeatures){
    			int rightoccur=right.getFeatureOccurrence(ID);
    			int leftoccur=left.getFeatureOccurrence(ID);
    			int min=Math.min(rightoccur, leftoccur);
    			if(min>0)
    				intersec.addFeatureWithOccurrence(ID, min);
    		}
    	}
    	return intersec;
    }
    
    public static FeatureVector setMinus(FeatureVector left, FeatureVector right){
    	FeatureVector result=new FeatureVector();
    	result.addWholeFeatureVectorIn(left);
    	Set<Integer> rightfeatures= right.getDistinctFeatures();
    	for(Integer ID:rightfeatures){
    		int rightoccur=right.getFeatureOccurrence(ID);
    		int leftoccur=left.getFeatureOccurrence(ID);
    		int value=leftoccur-rightoccur;
    		if(value>0)
    		result.labelMap.put(ID, value);
    		else if(leftoccur>0)
    		result.labelMap.remove(ID);    	    	
    	}
    	return result;	
    }
   
}
