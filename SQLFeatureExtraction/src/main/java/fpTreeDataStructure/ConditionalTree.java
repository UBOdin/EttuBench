package fpTreeDataStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;


/**
 * this tree stores paths that:
 * conditioned on the existence of one item listed in 'white list' (with support greater than threshold)
 * based on its parent conditional tree, one can create a conditional tree furthered conditioned on some item:
 * e.g. if the parent is conditioned on item X, then a child can be created by further conditioned on Y, which means
 * the child is conditioned on X,Y both appears
 * @author Ting
 *
 */
public class ConditionalTree {
	private int supportThreshold;
	private HashMap<SortableItem,Integer> totalFrequencyMap=new HashMap<SortableItem,Integer>();//keep track of totalFrequency of all 1-item sets
	private ConditionalTree parent;
	private SortableItem conditionedItem;//the item that this conditional tree is conditioned on
	private FP_InferenceTree fptree;
	private int totalCount;
	private HashMap<Integer,HashMap<Integer,SortableItem>> itemMap;//its own sortable item map two keys itemID+occurrence
	private double hconfidence;
	private HashMap<SortableItem,Integer> rootTotalFrequencyMap;
	private int maxCount;
	
	/**
	 * create and initialize a conditional tree
	 * @param supportThreshold
	 * @param content
	 * @param conditionedItem
	 */
	public ConditionalTree(int supportThreshold,SortableItem conditionedItem,ConditionalTree parent,int totalCount){
		this.supportThreshold=supportThreshold;
		this.parent=parent;
		this.conditionedItem=conditionedItem;    	
		String extension="_"+conditionedItem.toString();
		String path=parent.getItsFPTree().getPath()+extension;
		String dumppath=parent.getItsFPTree().getDumpPath()+extension;
		this.fptree=new FP_InferenceTree(path,dumppath);
		this.totalCount=totalCount;
		this.itemMap=parent.itemMap;
	}
	
	/**
	 * create and initialize a conditional tree
	 * @param supportThreshold
	 * @param content
	 * @param conditionedItem
	 */
	public ConditionalTree(double hconfidence,SortableItem conditionedItem,ConditionalTree parent,int totalCount){
		this.hconfidence=hconfidence;
		this.parent=parent;
		this.conditionedItem=conditionedItem;    	
		String extension="_"+conditionedItem.toString();
		String path=parent.getItsFPTree().getPath()+extension;
		String dumppath=parent.getItsFPTree().getDumpPath()+extension;
		this.fptree=new FP_InferenceTree(path,dumppath);
		this.totalCount=totalCount;
		this.itemMap=parent.itemMap;
		this.rootTotalFrequencyMap=parent.rootTotalFrequencyMap;
		
		int mycount=this.rootTotalFrequencyMap.get(this.conditionedItem);
		
		if(mycount>parent.maxCount)
			this.maxCount=mycount;
		else
			this.maxCount=parent.maxCount;
	
	}

	public ConditionalTree(int supportThreshold,FP_InferenceTree fptree,HashMap<Integer,HashMap<Integer,SortableItem>> itemMap){
		this.supportThreshold=supportThreshold;
		this.fptree=fptree;
		//update its totalFrequency map
		HashMap<SortableItem, HashSet<FPNode>> trackMap=fptree.getTrackMap();
		for (Entry<SortableItem, HashSet<FPNode>> en: trackMap.entrySet()){
			SortableItem sitem=en.getKey();
			int count=0;
			for (FPNode node: en.getValue()){
				count+=node.getCount();
			}
			this.totalFrequencyMap.put(sitem, count);
		}
		//we do not need this info here
		this.totalCount=-1;
		this.itemMap=itemMap;
	}
	
	public ConditionalTree(double hconfidence,FP_InferenceTree fptree,HashMap<Integer,HashMap<Integer,SortableItem>> itemMap){
		this.hconfidence=hconfidence;
		this.fptree=fptree;
		//update its totalFrequency map
		HashMap<SortableItem, HashSet<FPNode>> trackMap=fptree.getTrackMap();
		for (Entry<SortableItem, HashSet<FPNode>> en: trackMap.entrySet()){
			SortableItem sitem=en.getKey();
			int count=0;
			for (FPNode node: en.getValue()){
				count+=node.getCount();
			}
			this.totalFrequencyMap.put(sitem, count);
		}
		//we do not need this info here
		this.totalCount=-1;
		this.itemMap=itemMap;
		this.rootTotalFrequencyMap=this.totalFrequencyMap;
		this.maxCount=-1;
	}

	public ConditionalTree getParent(){
		return this.parent;
	}


	public SortableItem getConditionedItem(){
		return this.conditionedItem;
	}

	public FP_InferenceTree getItsFPTree(){
		return this.fptree;
	}

	/**
	 * feed the FP tree inside the conditional tree with FPPaths
	 * needs to update the total frequency of all consumed sortable items
	 * totalFrequencyMap will be built after this step
	 * a must-have step before using function pruneTree()
	 * @param path
	 */
	private void feedInFPPath(FPPath path){
		//accumulate the total frequency of each sortable item met in the path
		for(FPNode node:path.getList()){
			SortableItem sitem=node.getSortableItem();
			Integer oldcount=this.totalFrequencyMap.get(sitem);
			if(oldcount==null)
				this.totalFrequencyMap.put(sitem,0+node.getCount());
			else
				this.totalFrequencyMap.put(sitem,oldcount+node.getCount());	
		}
		this.fptree.consume(path);
	}

	/**
	 * get frequent patterns that pass the support threshold from this conditional tree
	 * @return
	 */
	public HashMap<SortableItemSet,Integer> getFrequentPatterns(){
		HashMap<SortableItemSet,Integer>  result=new HashMap<SortableItemSet,Integer>();
		
		//first we get a  white list of items
		HashMap<SortableItem,Integer> whitelist=new HashMap<SortableItem,Integer>();
		List<SortableItem> sortedList=new ArrayList<SortableItem>();
		
		for(Entry<SortableItem, HashSet<FPNode>> en: this.fptree.getTrackMap().entrySet()){
			SortableItem sitem=en.getKey();
			int count=this.totalFrequencyMap.get(sitem);			
			if(count>supportThreshold){
			whitelist.put(sitem,count); 			
			sortedList.add(sitem);
			}
		}
		//next we sort these items by their order in the tree
		Collections.sort(sortedList);
		
		//starting from the tail item
		while(!sortedList.isEmpty()){
			SortableItem tailItem=sortedList.get(sortedList.size()-1);
		   //conditioned on the tailItem and build a conditional tree
		    ConditionalTree childTree=new ConditionalTree(this.supportThreshold,tailItem,this,whitelist.get(tailItem)); 
		    //track the nodes of this tail item
		   HashSet<FPNode> nodeset=this.fptree.getTrackMap().get(tailItem);
		    for (FPNode node: nodeset){
		    	FPPath path=this.fptree.stripPathEndOnNodeExcluding(node);
		    	//feed the child with the path
		    	childTree.feedInFPPath(path);
		    }
		    //get frequent patterns from child 
		    HashMap<SortableItemSet,Integer> childresult=childTree.getFrequentPatterns();
		    result.putAll(childresult);
		    //remove this tail item
		    sortedList.remove(sortedList.size()-1);
		}

		//add its condition in
		if(this.conditionedItem!=null){
			HashMap<SortableItemSet,Integer> newresult=new HashMap<SortableItemSet,Integer>();
			//its condition itself is a frequent pattern, add it in first
			SortableItemSet mypattern=new SortableItemSet();
			mypattern.addToSet(this.conditionedItem,this.itemMap);
			newresult.put(mypattern, this.totalCount);
			//then add its condition as header to the frequent patterns got from children	
			for (Entry<SortableItemSet,Integer> en: result.entrySet()){
				SortableItemSet pattern=en.getKey();
				SortableItemSet newpattern=new SortableItemSet(pattern);
				newpattern.addToSet(this.conditionedItem,this.itemMap);
				newresult.put(newpattern, en.getValue());
			}				
			result=newresult;
		}
		
		return result;
	}
	
	/**
	 * get frequent patterns that pass the support threshold from this conditional tree
	 * @return
	 */
	
	public HashMap<SortableItemSet,Integer> getHyperCliquePatterns(){
		HashMap<SortableItemSet,Integer>  result=new HashMap<SortableItemSet,Integer>();
		
		//first we get a  white list of items
		HashMap<SortableItem,Integer> whitelist=new HashMap<SortableItem,Integer>();
		List<SortableItem> sortedList=new ArrayList<SortableItem>();
		
		for(Entry<SortableItem, HashSet<FPNode>> en: this.fptree.getTrackMap().entrySet()){
			SortableItem sitem=en.getKey();
			int count=this.totalFrequencyMap.get(sitem);
			int maxC=Math.max(this.maxCount, this.rootTotalFrequencyMap.get(sitem));
			double maxCC=(double)maxC;
			double hconfidenceratio=((double)count)/maxCC;
//			    System.out.println("-------");
//			    if(this.parent!=null&&this.parent.conditionedItem!=null)
//			    System.out.println(this.parent.conditionedItem);
//			    System.out.println(sitem);
//				System.out.println(count);
//				System.out.println(maxCC);
			if(hconfidenceratio>=this.hconfidence){
			whitelist.put(sitem,count); 			
			sortedList.add(sitem);
			}
		}
		
		//next we sort these items by their order in the tree
		Collections.sort(sortedList);
		
		//starting from the tail item
		while(!sortedList.isEmpty()){
			SortableItem tailItem=sortedList.get(sortedList.size()-1);
		   //conditioned on the tailItem and build a conditional tree
		    ConditionalTree childTree=new ConditionalTree(this.hconfidence,tailItem,this,whitelist.get(tailItem)); 
		    //track the nodes of this tail item
		   HashSet<FPNode> nodeset=this.fptree.getTrackMap().get(tailItem);
		    for (FPNode node: nodeset){
		    	FPPath path=this.fptree.stripPathEndOnNodeExcluding(node);
		    	//feed the child with the path
		    	childTree.feedInFPPath(path);
		    }
		    //get frequent patterns from child 
		    HashMap<SortableItemSet,Integer> childresult=childTree.getHyperCliquePatterns();
		    result.putAll(childresult);
		    //remove this tail item
		    sortedList.remove(sortedList.size()-1);
		}

		//add its condition in
		if(this.conditionedItem!=null){
			HashMap<SortableItemSet,Integer> newresult=new HashMap<SortableItemSet,Integer>();
			//its condition itself is a frequent pattern, add it in first
			SortableItemSet mypattern=new SortableItemSet();
			mypattern.addToSet(this.conditionedItem,this.itemMap);
			newresult.put(mypattern, this.totalCount);
			//then add its condition as header to the frequent patterns got from children	
			for (Entry<SortableItemSet,Integer> en: result.entrySet()){
				SortableItemSet pattern=en.getKey();
				SortableItemSet newpattern=new SortableItemSet(pattern);
				newpattern.addToSet(this.conditionedItem,this.itemMap);
				newresult.put(newpattern, en.getValue());
			}				
			result=newresult;
		}		
		return result;
	}


}
