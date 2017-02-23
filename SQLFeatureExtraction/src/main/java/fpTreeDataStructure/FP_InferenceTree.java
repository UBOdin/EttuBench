package fpTreeDataStructure;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import querySimilarityMetrics.FeatureVector;




/**
 * this is the main part, FP_inference tree
 * @author Ting Xie
 *
 */
public class FP_InferenceTree {
	private List<SortableItem> curlist;// keep track of current order of sortableitems
	private HashMap<Integer,HashMap<Integer,SortableItem>> itemMap;//its own sortable item map two keys itemID+occurrence
	private HashMap<SortableItem,HashSet<FPNode>> trackMap;//tracking the position of sortable items
	private FPNode root;//root of this tree, defined as null
	private int count;//total number of queries parsed
	private int totalcount;
	private String path;//just a path to temporarily store consumed items on disk
	private String dumpPath;//just a path to visualize the content of the tree
	//private PrintWriter dump;
	private PrintWriter wr;
	private BufferedReader readFromFirstRun;
	public FP_InferenceTree (String path,String dumppath){
		this.itemMap=new HashMap<Integer,HashMap<Integer,SortableItem>>();
		this.trackMap=new HashMap<SortableItem,HashSet<FPNode>> ();
		SortableItem item = null;
		this.path=path;
		this.root=new FPNode(item);
		this.count=0;
		this.totalcount=0;
	}

	/**
	 * give a deep copy
	 * @param input
	 */
	public FP_InferenceTree(FP_InferenceTree input){
		if(input.curlist!=null)
			this.curlist=new ArrayList<SortableItem>(input.curlist);

		this.itemMap=new HashMap<Integer,HashMap<Integer,SortableItem>>(input.itemMap);

		this.trackMap=new HashMap<SortableItem,HashSet<FPNode>>(input.trackMap);

		this.root=input.root;
		this.count=input.count;
		this.totalcount=input.totalcount;
		this.path=input.path;
		this.dumpPath=input.dumpPath;
	}

	/**
	 * give a deep copy
	 * @param input
	 */
	public FP_InferenceTree(FP_InferenceTree input,String path,String dumppath){
		if(input.curlist!=null)
			this.curlist=new ArrayList<SortableItem>(input.curlist);

		this.itemMap=new HashMap<Integer,HashMap<Integer,SortableItem>>(input.itemMap);

		this.trackMap=new HashMap<SortableItem,HashSet<FPNode>>(input.trackMap);
		this.root=input.root;
		this.count=input.count;
		this.totalcount=input.totalcount;
		this.path=path;
		this.dumpPath=dumppath;
	}

	public String getPath(){
		return this.path;
	}

	public String getDumpPath(){
		return this.dumpPath;
	}

	public void setCount(int count){
		this.count=count;
	}

	public HashMap<SortableItem,HashSet<FPNode>> getTrackMap(){
		return this.trackMap;
	}

	public void clearTree(){
		this.root.getChildren().clear();
		this.curlist=null;
		this.itemMap.clear();
	}

	public void prepareToReceiveItemList(){
		try {
			this.wr=new PrintWriter(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public void finishReceivingItemList(){
		this.wr.close();
	}

	/**
	 * this method consumes an FPPath and build the tree step by step
	 * it assumes the input FPPath is correctly ordered
	 * @param list
	 */
	public void consume(FPPath P){
		//traverse this tree
		FPNode start=null;//start is the node that last match happens
		if (root.getChildren().isEmpty()){			
			branch(root,P);
		}
		else {			
			start=this.locate(root, P);
			if (!P.getList().isEmpty())
				branch(start,P);
		}

	}

	/**
	 * branch on unmatched parts and add nodes one by one
	 * @param start
	 * @param P
	 */
	private void branch(FPNode start,FPPath P){

		FPNode currentnode=start;
		FPNode node;
		while (!P.getList().isEmpty()){
			node=P.pullFirst();
			currentnode.addChild(node);			
			//register this node in track map whenever a new node is added
			SortableItem sitem=node.getSortableItem();
			if(sitem!=null){
				HashSet<FPNode> list=this.trackMap.get(sitem);
				if(list==null){
					list=new HashSet<FPNode>();
					//register it into trackmap
					this.trackMap.put(sitem, list);
				}
				list.add(node);	 
			}
			//then start from the newly added node as the current node	
			currentnode=node;
		}
	}


	public FPNode getRoot(){
		return this.root;
	}

	public void setTrackMap(HashMap<SortableItem,HashSet<FPNode>> input){
		this.trackMap=input;
	}

	/**
	 * assumes root is matched, try to locate the position of the last match of P from root onwards
	 * and add the count of current pattern in the common path
	 * @param item
	 * @return
	 */
	private FPNode locate(FPNode root,FPPath P){

		if (!P.getList().isEmpty()){
			FPNode targetNode=P.getFirst();
			HashMap<Integer, HashMap<Integer,FPNode>> chMap=root.getChildren();
			HashMap<Integer,FPNode> map=chMap.get(targetNode.getSortableItem().getItemID());
			FPNode nextnode=null;
			if (map!=null)
				nextnode=map.get(targetNode.getSortableItem().getOccurrence());

			if (nextnode==null){
				return root;
			}
			else {	
				nextnode.addCount(targetNode.getCount());
				P.removeFirst();
				return locate(nextnode,P);
			}

		}
		else return root;
	}

	public boolean validateNodes(FPNode root){

		HashMap<Integer, HashMap<Integer, FPNode>> map=root.getChildren();
		//if it has children
		if(!map.isEmpty()){
			int count=0;
			for(Entry<Integer, HashMap<Integer, FPNode>> en: map.entrySet()){
				HashMap<Integer, FPNode> itemmap=en.getValue();
				for(Entry<Integer,FPNode> enn: itemmap.entrySet()){
					if(enn.getValue().getCount()<=0)
						return false;
					else
						count+=enn.getValue().getCount();	

					boolean result=validateNodes(enn.getValue());
					if(result==false)
						return false;				
				}
			}

			if(count>root.getCount()&&root.getSortableItem()!=null)
				return false;
		}

		return true;
	}

	public boolean validateTrackMap(FPNode root){
		HashMap<Integer, HashMap<Integer, FPNode>> map=root.getChildren();
		//if it has children
		if(!map.isEmpty()){
			for(Entry<Integer, HashMap<Integer, FPNode>> en: map.entrySet()){
				HashMap<Integer, FPNode> itemmap=en.getValue();
				for(Entry<Integer,FPNode> enn: itemmap.entrySet()){
					FPNode node=enn.getValue();
					SortableItem sitem=node.getSortableItem();
					if(!this.trackMap.get(sitem).contains(node))
						return false;
					else {
						if(validateTrackMap(node)==false)
							return false;
					}						
				}
			}
		}
		return true;
	}


	/**
	 * validate if this tree is correct
	 */
	public void validateTree(){
		System.out.println("this tree's nodes are correctly built?: "+this.validateNodes(this.root));
		System.out.println("this tree's trackMap is correctly built?: "+this.validateTrackMap(this.root));
		System.out.println("this tree has "+this.count+" number of item sets");
		System.out.println("this tree has "+this.getNodeNumber(this.root)+" number of nodes in total");
		System.out.println("total number of consumed items are: "+this.totalcount);
	}

	public int getNodeNumber(FPNode root){
		int count=1;
		HashMap<Integer, HashMap<Integer,FPNode>> map = root.getChildren();
		if (!map.isEmpty()){
			for (Entry<Integer, HashMap<Integer,FPNode>> en: map.entrySet()){
				for (Entry<Integer, FPNode> entry: en.getValue().entrySet()){
					count+=getNodeNumber(entry.getValue());
				}
			}
		}
		return count;
	}

	public int getNodeNumber(){
		return getNodeNumber(this.root);
	}

	public boolean isEmpty(){
		return this.root.getChildren().isEmpty();
	}

	/**
	 * get all patterns
	 * @param root
	 * @return
	 */
	public List<FPPath> getAllPatterns(FPNode root){
		List<FPPath> list=new ArrayList<FPPath>();

		if (!root.getChildren().isEmpty()){

			HashMap<Integer,HashMap<Integer,FPNode>> map=root.getChildren();
			int validsum=0;
			for(Entry<Integer,HashMap<Integer,FPNode>> en:map.entrySet()){
				for (Entry<Integer, FPNode> entry: en.getValue().entrySet()){
					FPNode child=entry.getValue();
					List<FPPath> plist=this.getAllPatterns(child);
					for (FPPath pattern:plist){
						FPNode n=new FPNode(root);
						n.addCount(pattern.getFirst().getCount());
						validsum+=pattern.getFirst().getCount();
						pattern.addToFirst(n);
						list.add(pattern);
					}	
				}
			}

			int diff=root.getCount()-validsum;
			FPPath pp=new FPPath();
			FPNode node=new FPNode(root);
			node.addCount(diff);
			node.setParent(root.getParent());

			if (diff>0){
				pp.addToTail(node);
				list.add(pp);
			}

		}
		else {
			FPPath p=new FPPath();
			FPNode node=new FPNode(root);
			node.addCount(root.getCount());
			p.addToTail(node);
			list.add(p);					
		}



		return list;
	}

	/**
	 * get the frequent patterns of this tree
	 * @param root
	 * @return
	 */
	public HashMap<SortableItemSet,Integer> getFrequentPatterns(int supportThreshold){
		//create a conditional tree out of this FP tree which conditioned on nothing at first
		ConditionalTree ctree=new ConditionalTree(supportThreshold,this,this.itemMap);
		return ctree.getFrequentPatterns();
	}
	
	/**
	 * get the hyperclique patterns of this tree
	 * @param root
	 * @return
	 */
	public HashMap<SortableItemSet,Integer> getHyperCliquePatterns(double hconfidence){
		//create a conditional tree out of this FP tree which conditioned on nothing at first
		ConditionalTree ctree=new ConditionalTree(hconfidence,this,this.itemMap);
		return ctree.getHyperCliquePatterns();
	}

	public FPPath stripPathEndOnNodeExcluding(FPNode node){		
		FPPath result=new FPPath();
		//trace the full path that involves this node
		FPNode parent=node.getParent();
		while(parent!=null&&parent.getSortableItem()!=null){
			//copy this node
			FPNode parentnode=new FPNode(parent);
			parentnode.addCount(node.getCount());
			result.addToFirst(parentnode);
			parent=parent.getParent();
		}

		return result;
	}


	//	/**
	//	 * reorganize itself by reordering the items and thus the tree structure
	//	 */
	//	public void selfReOrganizing(){		
	//		//update contributions of all sortable items
	//		this.updateContributions();
	//
	//		List<MySortableItem> oldlist=new ArrayList<MySortableItem>(this.curlist);
	//		HashMap<MySortableItem,Integer> indexmap=new HashMap<MySortableItem,Integer>();
	//		//update the order of sortable items
	//		this.updateItemOrder();
	//
	//		for (int i=0;i<this.curlist.size();i++){
	//			indexmap.put(this.curlist.get(i), i);
	//		}
	//
	//		if (Util.checkDisorderness(oldlist, this.curlist,indexmap)){
	//			System.out.println("begin self reorganizing ");
	//			long start=System.nanoTime();
	//			this.reorganize(indexmap);
	//			long end=System.nanoTime();
	//			System.out.println("self reorganizing finished, time used is: "+(end-start)/1000000+" milliseconds");
	//		}
	//		else
	//			System.out.println(" no need to reorganize,skipped");
	//	}
	//
	//	/**
	//	 * 
	//	 * reorganize the tree according to the new order
	//	 * use existing patterns in the tree and rebuild the tree
	//	 */
	//	private void reorganize(HashMap<MySortableItem,Integer> indexmap){
	//		for (Entry<Integer,MyNode> en:this.root.getChildren().entrySet()){
	//			pruneFrom(en.getValue(),this.root,indexmap);          
	//		}
	//
	//	}
	//
	//	/**
	//	 * this merges trees to a single tree, these two trees need to have the same root node
	//	 * @param from
	//	 * @param to
	//	 */
	//	public static void mergeTwoTrees(MyNode from,MyNode to){
	//		to.addCount(from.getCount());
	//		for (Entry<Integer,MyNode> en:from.getChildren().entrySet()){
	//			MyNode child=en.getValue();
	//				MyNode target=to.getChildren().get(child.getSortableItem().getKey());
	//				if (target!=null){
	//					mergeTwoTrees(child,target);
	//				}
	//				else {
	//                  to.addChildTree(child);
	//				}			
	//		}
	//	}
	//
	//	/**
	//	 * this method prunes unordered branch out
	//	 * not finished
	//	 * @param node
	//	 * @param root
	//	 * @param indexmap
	//	 */
	//	private void pruneFrom(MyNode node,MyNode root,HashMap<MySortableItem,Integer> indexmap){
	//		
	//	}

	/**
	 * reset this tree by clear the root's children
	 */
	public void resetTree(){
		this.root.getChildren().clear();
		this.count=0;
		this.totalcount=0;
	}

	/**
	 * get an ordered list of all items
	 */
	public void getItemOrder(){
		if (this.curlist==null){
			this.curlist=new ArrayList<SortableItem>();
			for (Entry<Integer,HashMap<Integer,SortableItem>> en:this.itemMap.entrySet()){
				for (Entry<Integer,SortableItem> item:en.getValue().entrySet())
					this.curlist.add(item.getValue());
			}    		
		}
		Collections.sort(curlist);
	}
	/**
	 * check the disorderness of two sorted arrays
	 * @param oldlist
	 * @param newlist
	 * @return
	 */


	//	/**
	//	 * update the contributions values for all Sortable items in one tree traverse
	//	 */
	//	public void updateContributions(){
	//		//first reset all contributions of all sortable items  			
	//		for (Entry<Integer,HashMap<Integer,SortableItem>> en:this.itemMap.entrySet()){
	//			for (Entry<Integer,SortableItem> item:en.getValue().entrySet())
	//			item.getValue().resetContribution();
	//		} 		
	//		updateFrom(root);
	//	}
	//
	//	/**
	//	 * update from its Node onwards and provide its parent with its accumulative count
	//	 * @param node
	//	 * @param childrencount
	//	 */
	//	private Integer updateFrom(FPNode node){
	//		if (node.getSortableItem()==null){
	//			for (Entry<Integer, HashMap<Integer, FPNode>> en: node.getChildren().entrySet()){
	//				for (Entry<Integer,FPNode> entry:en.getValue().entrySet()){
	//				updateFrom(entry.getValue());
	//				}
	//			}
	//			return 0;
	//		}
	//		else {
	//			HashMap<Integer, HashMap<Integer, FPNode>> children=node.getChildren();
	//			Integer totalcount=0;
	//			Integer upstream_count=(node.getDepth()-1)*node.getCount();		
	//			for (Entry<Integer, HashMap<Integer, FPNode>> en: children.entrySet()){
	//				for (Entry<Integer,FPNode> entry:en.getValue().entrySet()){
	//				totalcount+=updateFrom(entry.getValue());
	//				}
	//			}
	//
	//			//update its node by upstream count and totalcount from all of its children
	//			node.getSortableItem().addContribution(totalcount+upstream_count);
	//			//return the total count of its children added by its own count
	//			return totalcount+node.getCount();
	//		}
	//	}

	/**
	 * read from what's saved and build its tree
	 */
	public void buildTree(){
		try {
			SortableItemSet q;
			FPPath p;
			System.out.println("begin reading stored item sets.");
			String line;
			readFromFirstRun = new BufferedReader(new FileReader(this.path));
			while ((line = readFromFirstRun.readLine()) != null) {
				q=new SortableItemSet(line,this.itemMap);
				if (!q.getSet().isEmpty()){	
					ArrayList<SortableItem> qlist=new ArrayList<SortableItem>();
					for(SortableItem item: q.getMaterializedSet())
						qlist.add(item);
					//sort the items first
					Collections.sort(qlist);

					p=new FPPath();
					for (SortableItem item:qlist){						
						p.addToTail(new FPNode(item));
					}
					this.consume(p);
				}
				else {
					System.out.println("error parsing line: "+line+" empty items sequence found");
				}				
			}				
			readFromFirstRun.close();
			this.validateTree();
			this.clearTempFilesOnDisk();
		}  catch (IOException e) {
			e.printStackTrace();
		}	
	}

	//	public LinkedList<Integer> findQueriesByPattern(FPPath p,List<Integer> index){
	//		index.clear();
	//		LinkedList<Integer> wrapperlabellist=new LinkedList<Integer>();
	//		SortableItemSet q;
	//		System.out.println("begin searching queries that has the pattern shrinked as: "+p.getMyShrinkedLabels());
	//		String line;
	//
	//		try {
	//			int count=0;
	//			readFromFirstRun = new BufferedReader(new FileReader(this.path));
	//			while ((line = readFromFirstRun.readLine()) != null) {
	//				q=new SortableItemSet(line,this.itemMap);
	//				if (!q.getSet().isEmpty()){
	//					List<SortableItem> list =new ArrayList<SortableItem>();
	//					for(SortableItem item: q.getSet())
	//						list.add(item);					
	//					Collections.sort(list);
	//
	//					Iterator<SortableItem> myit=list.iterator();
	//					Iterator<FPNode> it=p.getList().iterator();
	//
	//					boolean pass=true;
	//					while(it.hasNext()){
	//						if(it.next().getSortableItem()!=myit.next()){
	//							pass=false;
	//							break;
	//						}
	//					}
	//					if(pass){
	//						//find the wrapper label
	//						int max=-1;
	//						for (SortableItem sitem:list){
	//							if (sitem.getItem().getOwnLabel()>max){
	//								max=sitem.getItem().getOwnLabel();
	//							}
	//						}
	//						wrapperlabellist.add(max);
	//						index.add(count);
	//					}
	//
	//				}
	//				else {
	//					System.out.println("error parsing line: "+line+" empty items sequence found");
	//				}
	//				count++;
	//			}
	//			readFromFirstRun.close();
	//			System.out.println("finished searching");;
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		return wrapperlabellist;
	//
	//	}

	/**
	 * turns a list of MyItem with occurrences into a HashSet of MySortableItem
	 * and register it in this tree
	 */
	public void consumeItemList(FeatureVector vector,int lineID){
		SortableItemSet itemset=new SortableItemSet(lineID);
		Set<Integer> distinctFeatures=vector.getDistinctFeatures();
		//create an array of sortable items
		for (Integer feature: distinctFeatures){
			int itemID=feature;
			int occur=vector.getFeatureOccurrence(itemID);
			//critical! if an item happens occur times, then it implies from 1 to occur it all happens
			SortableItem sitem = null;
			for (int i=1;i<=occur;i++){
				sitem=SortableItem.createNewSortableItem(itemID,i, this.itemMap);
				itemset.addToSetAnyWay(sitem);
			}
		}

		if (!itemset.getSet().isEmpty()){										
			this.count++;
			this.totalcount+=itemset.getMaterializedSet().size();
			this.wr.println(itemset.toLabelString());
			//add the contribution to all sortable items
			int size=itemset.getSet().size()-1;
			for (SortableItem item: itemset.getMaterializedSet()){
				item.addContribution(size);
				//item.addContribution(1);
			}
		}
		else{
			System.out.println("empty query found at line : " +lineID);
		}		
	}

	private void clearTempFilesOnDisk(){
		try {
			Path temppath = Paths.get(path);
			Files.deleteIfExists(temppath);
		} catch (NoSuchFileException x) {
			System.err.format("%s: no such" + " file or directory%n", path);
		} catch (DirectoryNotEmptyException x) {
			System.err.format("%s not empty%n", path);
		} catch (IOException x) {
			// File permission problems are caught here.
			System.err.println(x);
		}
	}

	/**
	 * returns the number of occurrences of the set of items represented by the feature vector
	 * @param vector
	 * @return
	 */
	public int getCountOfFeatureVector(FeatureVector vector){
		Set<Integer> distinctfeatures=vector.getDistinctFeatures();
		ArrayList<SortableItem> itemlist=new ArrayList<SortableItem>();
		for (Integer label: distinctfeatures){
			SortableItem sitem=this.itemMap.get(label).get(vector.getFeatureOccurrence(label));
			if(sitem==null)
				System.out.println("FeatureID non-match found, input feature is "+label+" with occurrence "+vector.getFeatureOccurrence(label));
			else
			itemlist.add(sitem);
		}

		int sum=0;
		if(!itemlist.isEmpty()){
			//sort 
			Collections.sort(itemlist);
			HashSet<FPNode> candidatepaths=this.trackMap.get(itemlist.get(itemlist.size()-1));
			
			if(itemlist.size()>1){
				for (FPNode node: candidatepaths){
					boolean pass=false;
					FPNode startNode=node.getParent();
					int index=itemlist.size()-2;
					while(startNode!=null&&startNode.getSortableItem()!=null){
						SortableItem targetItem=itemlist.get(index);
						if(targetItem.equals(startNode.getSortableItem())){
							index--;
							if(index==-1){
								pass=true;
								break;
							}
						}						
						startNode=startNode.getParent();	
					}
					
                     if(pass)
                    	 sum+=node.getCount();
				}
			}
			else {
				for (FPNode node: candidatepaths){
					sum+=node.getCount();
				}
			}
		}
		else
			System.out.println("warning, input feature vector is empty");
		return sum;	
	}

	public int getTotalCount(){
		return this.count;
	}



}

