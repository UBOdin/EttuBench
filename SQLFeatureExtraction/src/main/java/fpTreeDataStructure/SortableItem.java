
package fpTreeDataStructure;

import java.util.HashMap;

/**
 * Sortable item=item+occurrence 
 * it stores the total contribution of this item
 * contribution: total number of other items that coexist with this item
 * contribution determines the priority of this item in a Query
 * @author Ting Xie
 *
 */

public class SortableItem implements Comparable<SortableItem>{
	private int itemID;
    private int totalContribution;
    public int startcount=-1;
    private int occurrence;
    private int hashcode;
    
	private SortableItem(int itemID,int occurrence){
		this.itemID=itemID;
		this.totalContribution=0;
		this.occurrence=occurrence;
		//create hashcode
		int hashCode = 31 + Integer.hashCode(this.itemID);
		hashCode = 31*hashCode + Integer.hashCode(this.occurrence);
		this.hashcode=hashCode;
	}
	
	public int getItemID(){
		return this.itemID;
	}
	
	public int getOccurrence(){return this.occurrence;}
	
    public Integer getContribution(){
   	 return this.totalContribution;
    }

    
    public static SortableItem createNewSortableItem(int itemID,int occurrence,HashMap<Integer,HashMap<Integer,SortableItem>> itemMap){
 
    	
    	HashMap<Integer,SortableItem> map=itemMap.get(itemID);
    	SortableItem it;   	
    	if (map==null){
    		it=new SortableItem(itemID,occurrence);
    		//register in itemmap
               map=new HashMap<Integer,SortableItem>();
               map.put(occurrence,it);
    		   itemMap.put(itemID, map);
    	}
    	else {
    		it=map.get(occurrence);
    		if (it ==null){
    			it=new SortableItem(itemID,occurrence);
    			map.put(occurrence,it);
    		}
    	}
		return it;  	
    }
 
    /**
     * updates total contribution of this sortableitem
     */
    public void resetContribution(){
   	    this.totalContribution=0;   	   
    }


	@Override
	public boolean equals(Object o){
		SortableItem item=(SortableItem) o;
		if (this.itemID==item.itemID&&this.occurrence==item.occurrence) return true;
		else return false;	
	}
	
	@Override
	public int hashCode(){		
		return this.hashcode;
	}
	
	
	@Override
	//need to consider question mark and longer items
	public int compareTo(SortableItem arg0) {
		if (this.totalContribution>arg0.getContribution())
		return -1;
		else if (this.totalContribution<arg0.getContribution())
			return 1;
		else {
			int occur1=this.occurrence;
			int occur2=arg0.getOccurrence();
			if(occur1>occur2)
				return 1;
			else if (occur1<occur2)
				return -1;
			else if(this.itemID>arg0.itemID)
				return -1;
			else if (this.itemID<arg0.itemID)
				return 1;
			else
				return 0;
		}			
	}
	
	
	@Override
	public String toString(){
		return this.itemID+GlobalVariables.OccurSeparator+this.occurrence;		
	}
	
	public static String turnIDToString(int itemID,int occur){
		return itemID+GlobalVariables.OccurSeparator+occur;
	}
	
	public void addContribution(int a) {
		this.totalContribution+=a;		
	}
	
	
}
