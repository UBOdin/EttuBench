
package fpTreeDataStructure;

import java.util.HashMap;

/**
 * a node in the FP-tree
 * @author Ting Xie
 *
 */
public class FPNode implements Comparable<FPNode>{
      private SortableItem item;
      private int depth;//depth of this node
      private int count;//count of # of occurrences of the pattern represented by this node
	  private HashMap<Integer, HashMap<Integer,FPNode>> chMap;// hashmap of its children branch
	  //the first Integer is the label, second Integer is the occurrence
	  private FPNode parent;//its parent node
	  private int offset;//used to offset the occurrences of its sortableItem
	  
    public FPNode(SortableItem item){
    	this.item=item;
    	this.count=1;
    	this.chMap=new HashMap<Integer, HashMap<Integer,FPNode>>();
    	this.depth=0;
    	this.offset=0;
    }
    
    public FPNode(FPNode node){
    	this.item=node.item;
    	this.count=0;
    	this.chMap=new HashMap<Integer, HashMap<Integer,FPNode>>();
    	this.depth=0;
    	this.offset=0;
    }
    
    public int getOffSet(){
    	return this.offset;
    }
    
    public void setOffSet(int count){
    	if (count>this.offset)
    	this.offset=count;
    }
    
    public void addOffSet(int count){
    	this.offset+=count;
    }
   
    /**
     * count of # of occurrences of the pattern represented by this node
     * @return
     */
    public int getCount(){
    	return this.count;
    }
    
    public FPNode getParent(){return this.parent;}
    
    public void setParent(FPNode n){this.parent=n;}
    
    public SortableItem getSortableItem(){
    	return this.item;
    }

    
    /**
     * add a single node as its child
     * @param node
     */
    public void addChild(FPNode node){
    	int label=node.getSortableItem().getItemID();
    	int occurrence=node.getSortableItem().getOccurrence();
    	
    	HashMap<Integer,FPNode> map=this.chMap.get(label);
    	if (map==null){
    		map=new HashMap<Integer,FPNode> ();
    		map.put(occurrence, node);
    		this.chMap.put(label, map);
    	}
    	else{
    	map.put(occurrence,node);   	
    	}
    	
    	//let it know I am your parent
    	node.setParent(this);
    	//set its depth
    	node.depth=this.depth+1;
    }
    
    /**
     * assume child is already in its children map
     * @param child
     */
    public void removeChild(FPNode child){
    	SortableItem sitem=child.getSortableItem();
    	int occurrence=sitem.getOccurrence();   	
    	int label=sitem.getItemID();
    	
    	HashMap<Integer, FPNode> map=this.chMap.get(label);
    	if(map!=null){
    	map.remove(occurrence,child);
    	//if map is empty
    	if(map.isEmpty())
    		this.chMap.remove(label,map);
    	}
    	child.setParent(null);
    	
    }
    
//   /**
//    * add a subtree as its child 
//    */
//    public void addChildTree(MyNode root){
//    	int key=root.getSortableItem().getKey();
//    	if (!this.chMap.isEmpty()){
//    	MyNode child=this.chMap.get(key);
//    	if (child!=null){
//    		FP_InferenceTree.mergeTwoTrees(root, child);
//    	}
//    	else{
//    		root.setParent(this);
//    		this.chMap.put(key, root);
//    	}
//    	}
//    	else
//    		this.chMap.put(key, root);
//    		
//    }
    
    

    public int getDepth(){
    	return this.depth;
    }
	public void addCount(int count) {
		if (count<0){
			System.out.println("negative input found, try to use deductCount method: "+count);
		}
		else
			this.count+=count;	
	}
	
	public void deductCount(int count) {
		if (count<0){
			System.out.println("negative input found, try to use addCount method: "+count);
		}
		else
			this.count-=count;	
	}
	
//    /**
//     * check this node is frequent or not
//     * @return
//     */
//    public boolean isFrequent(int totalcount){
//  	  if (((double)this.count/(double)totalcount)>Util.Count_Cap||((double)this.count/(double)this.getParent().getCount())>Util.Inference_threshold){
//  	  return true;
//  	  }
//  	  else
//  	  return false;
//    	
//    }
    
	@Override
	public String toString(){
		return this.item.toString();
	}
	
	
	public HashMap<Integer, HashMap<Integer,FPNode>>  getChildren(){
		return this.chMap;
	}


	@Override
	public int compareTo(FPNode o) {		
		return this.getSortableItem().compareTo(o.getSortableItem());
	}
}

