package fpTreeDataStructure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * an FPQuery is a set of SortableItems
 * it can be sorted later
 * @author Ting Xie
 *
 */
public class SortableItemSet{

	private HashMap<Integer,Integer> itemset=new HashMap<Integer,Integer>();
	private HashSet<SortableItem> materializedSet=new HashSet<SortableItem>();;
	int lineID;

	public HashMap<Integer,Integer> getSet(){
		return this.itemset;
	}
	
	public HashSet<SortableItem> getMaterializedSet(){
		return this.materializedSet;
	}

	public int size(){
		return this.materializedSet.size();
	}
	
	public SortableItemSet(int lineID){
		this.lineID=lineID;
	}

	public SortableItemSet(){
		this.lineID=-1;
	}

	public SortableItemSet(SortableItemSet q){
		this.itemset=new  HashMap<Integer,Integer>(q.getSet());
		this.lineID=q.lineID;
		this.materializedSet=new HashSet<SortableItem>(q.getMaterializedSet());
	}


	/**
	 * reconstruct the items from reading the file line by line
	 * @param line
	 */


	public SortableItemSet(String line,HashMap<Integer,HashMap<Integer,SortableItem>> itemMap){
		SortableItem item;
		String[] tokens=line.split(GlobalVariables.specialSeparator);
		this.lineID=Integer.parseInt(tokens[0]);

		String[] Itemtokens=tokens[1].split(GlobalVariables.ItemSeparator);
		String itemlabel;
		String[] t;
		try{
			int size=Integer.parseInt(Itemtokens[0]);
			if (size!=Itemtokens.length-1){
				System.out.println("read error, #items parsed does not match recorded# :  "+line+"I am expecting:"+size+" number of labels");
				return;
			}
			else {	
				this.itemset= new HashMap<Integer,Integer>();
				for (int i=1;i<Itemtokens.length;i++){
					itemlabel=Itemtokens[i];
					t=itemlabel.split(GlobalVariables.OccurSeparator);
					item=SortableItem.createNewSortableItem(Integer.parseInt(t[0]),Integer.parseInt(t[1]),itemMap);
					this.addToSet(item,itemMap);               
				}
			}
		}
		catch(NumberFormatException e){
			System.out.println("read error, the first string should be the size of this query item list");
			return;
		}
	}

	public void addToSet(SortableItem item,HashMap<Integer,HashMap<Integer,SortableItem>> itemMap){
		//add to materialized set	
		int targetoccur=item.getOccurrence();
		int targetID=item.getItemID();	
		for (int i=1;i<=targetoccur;i++){
			SortableItem sitem=SortableItem.createNewSortableItem(targetID,i,itemMap);
			this.materializedSet.add(sitem);
		}
		//update hashmap
        Integer occur=this.itemset.get(targetID);
        if(occur==null||occur<targetoccur)
		this.itemset.put(targetID,targetoccur);
	}

	public void addToSetAnyWay(SortableItem item){
		//add to materialized set
		this.materializedSet.add(item);
		//update hashmap
        Integer occur=this.itemset.get(item.getItemID());
        if(occur==null||occur<item.getOccurrence())
		this.itemset.put(item.getItemID(),item.getOccurrence());
	}

	public Boolean contains(SortableItem item){
		return this.materializedSet.contains(item);
	}
	
	public Boolean containsAll(SortableItemSet set){
		boolean pass=true;
		for (Entry<Integer, Integer> en:set.itemset.entrySet()){
			int ID=en.getKey();
			int maxOccur=en.getValue();
			Integer matchedOccur=this.itemset.get(ID);
			if(matchedOccur==null||matchedOccur<maxOccur){
				pass=false;
				break;
			}
		}
		//System.out.println("--------");
		//System.out.println("comparing "+set+ " with "+this);
		//System.out.println(pass);
		return pass;
	}

	@Override 
	public String toString(){ 
		String inputline="";
		for(Entry<Integer,Integer> en: this.itemset.entrySet()){
			int ID=en.getKey();
			int occur=en.getValue();
			for (int i=0;i<occur;i++){
				inputline+=","+ID;
			}
		}		
		inputline=inputline.substring(1, inputline.length());
		return inputline;
	}

	/**
	 * this function is critical, it determines the format before the read of second round
	 */  
	public String toLabelString(){
		String line =null;
		if (this.materializedSet!=null&&!this.materializedSet.isEmpty()){
			line =Integer.toString(this.lineID);
			line+=GlobalVariables.specialSeparator+String.valueOf(this.materializedSet.size());
			for (SortableItem sitem: this.materializedSet){				
				line+=GlobalVariables.ItemSeparator+sitem.toString();
			}
			
		}
		return line;
	}


	@Override
	public int hashCode(){
		return this.itemset.hashCode();
	}

	@Override
	public boolean equals(Object o){
		SortableItemSet yourset=(SortableItemSet) o;
		if(this.itemset.equals(yourset.itemset))
			return true;
		else
			return false;
	}
}

