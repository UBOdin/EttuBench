
package fpTreeDataStructure;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * an FP path is an ordered list of FPNodes
 * each element in the list should be the same count of occurrences
 * @author Ting Xie
 *
 */

public class FPPath implements Comparable<FPPath>{	  
	  private LinkedList<FPNode> list;
	 // private LinkedList<FPNode> slist;
	  
      public FPPath(LinkedList<FPNode> list){
    	  this.list=list;    
      }
      
      public FPPath(FPNode n){
    	  this.list=new LinkedList<FPNode>();
    	  this.list.add(n);
      }
      public FPPath(){
    	  this.list=new LinkedList<FPNode>();
      }
      
      public void addToTail(FPNode node){
    	  if (this.list.isEmpty()||node.getCount()==this.list.get(0).getCount())
    	  this.list.add(node);    	  
    	  else
    	  System.out.println("add to pattern failed, need to have the same count");
    	  
      }
      
      public void addAllToTail(FPPath path){
    	  this.list.addAll(path.list);
      }
      
      public void removeLast(){
    	  this.list.removeLast();
      }
      
      public void removeFirst(){
    	  this.list.removeFirst();
      }
      
      public void addToFirst(FPNode node){
    	  if (this.list.isEmpty()||node.getCount()==this.list.get(0).getCount())
    	  this.list.addFirst(node);    	  
    	  else
    	  System.out.println("add to pattern failed, need to have the same count");

      }
      
      public FPNode  getFirst(){
    	  return this.list.getFirst();
      }
      
      /**
       * retrive and remove the first element
       * @return
       */
      public FPNode  pullFirst(){
    	  return this.list.pollFirst();
      }
      
      public FPNode  getLast(){
    	  return this.list.getLast();
      }          
      
           
      public String getMyLabels(){
    	  String line="";
    	  for (FPNode n: this.list)
    		  line+=" "+n.getSortableItem().getItemID()+":"+n.getSortableItem().getOccurrence()+":"+n.getCount();
		return line;
      }
      
//      public String getMyShrinkedLabels(){
//    	  String line="";
//    	  for (FPNode n: this.slist)
//    		  line+=" "+n.getSortableItem().getItem().getOwnLabel()+":"+n.getSortableItem().getOccurrence()+":"+n.getCount();
//		return line;
//      }
      
      /**
       * the string format of this pattern such that it is more human readable
       */
      @Override
      public String toString(){
    	  Iterator<FPNode> it=this.list.iterator();
    	  String line="";
    	  FPNode node;
    	  
    	 while(it.hasNext()){
    		 node=it.next();
    		  line+=GlobalVariables.leadsTo+node.getCount()+":"+node.toString()+"\n";
    	  }
    	 
		return line;   	  
      }
      
      /**
       * count the total number of queries contained in this pattern
       * @return
       */
      public int countTotal(){
    	  return this.list.getFirst().getCount();
    	  
      }
      
      public int getlength(){
    	  return this.list.size();
      }
      public LinkedList<FPNode> getList(){
    	  return this.list;
      }

	@Override
	public int compareTo(FPPath arg0) {		
		return Integer.compare(this.list.get(0).getCount(), arg0.getFirst().getCount());
	}
}
