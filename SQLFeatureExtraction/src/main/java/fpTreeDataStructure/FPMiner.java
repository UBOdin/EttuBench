package fpTreeDataStructure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import fpTreeDataStructure.FP_InferenceTree;
import fpTreeDataStructure.SortableItemSet;
import querySimilarityMetrics.FeatureVector;

import java.util.TreeMap;

/**
 * This class returns the frequent item sets defined by the support threshold.    
 * User should give input file name, output destination name plus a support threshold.  
 * A typical command line example is: java FPFrequentMiner -input input -output output -support 100.
 * Above command line assumes default directory, user can also specify a complete input path like C:/user/input.txt.
 * Each line in input file represents an item set and should contain item IDs(Integer) with its occurrences.
 * A typical format is 1:3,2:4 meaning feature 1 occurred 3 times and feature 2 occurred 4 times
 * IDs should be separated by either whitespace or comma(,)
 * In Output file each row is comma separated recording item IDs in the corresponding item set.
 * 
 * if the user want it to also output Weighted Frequent Itemsets(WFIs) then 
 * the user should add command option -WFIoutput
 * a typical command line example is: java FPFrequentMiner -input input -output output -WFIoutput WFIout -support 100
 * 
 * if the user want it to also return the projected feature vectors in the 
 * input using WFIs, then the user need to specify another type of output path
 * by using option -WFIProjected WFIProjectedout
 * a typical command line example is: java FPFrequentMiner -input input -output output -WFIoutput WFIout -WFIProjected WFIProjectedout  -support 100
 * @author Ting
 *
 */
public class FPMiner {

	public static void main(String[] args){
		String inputPath=null;
		int supportThreshold=-1;
		String outputPath=null;
        double hconfidence=-1;
        
		//parsing input arguments
		for (int i=0;i<args.length;i++){
			String s=args[i];
			//get input path
			if(s.equalsIgnoreCase("-input")){
				if(i+1==args.length)
					System.out.println("please give argument -input an path.");
				else
					inputPath=args[i+1];								
			}

			//get output path
			if(s.equalsIgnoreCase("-output")){
				if(i+1==args.length)
					System.out.println("please give argument -output an path.");
				else
					outputPath=args[i+1];								
			}			

			//get support thredhold
			if(s.equalsIgnoreCase("-support")){
				if(i+1==args.length)
					System.out.println("please give argument -support an argument.");
				else{
					String support=args[i+1];					
					supportThreshold=Integer.parseInt(support);	
					if(supportThreshold<0){
						System.out.println("please give a valid support threshold.");
					}				
				}
			}
			//get hconfidence
			if(s.equalsIgnoreCase("-hconfidence")){
				if(i+1==args.length)
					System.out.println("please give argument -hconfidence an argument.");
				else{
					String hconf=args[i+1];					
					hconfidence=Double.parseDouble(hconf);	
					if(hconfidence<0){
						System.out.println("please give a valid hconfidence threshold.");
					}				
				}
			}
		}

		if(inputPath==null){
			System.out.println("please use -input to give the path for input file.");
		}
		else if (supportThreshold*hconfidence>0){
			System.out.println("please give either a valid support Threshold or hconfidence.");
		}
		else if (outputPath==null){
			System.out.println("please use -output to give at least one file path for output destination file.");
		}
		else {
			FP_InferenceTree mytree=new FP_InferenceTree("itemsets","summarization");
			mytree.prepareToReceiveItemList();
			int lineID=0;
			System.out.println("start reading from path "+inputPath+" and receiving itemsets.");
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(new File(inputPath)));
				long start=System.nanoTime();
				FeatureVector featurevector;
				int linecount=0;
				String line;
				try {
					while((line=br.readLine())!=null){
						lineID++;
						//parse into feature vector
						featurevector=FeatureVector.readFeatureVectorFromFormattedString(line);
						if(!featurevector.isEmpty()){
							mytree.consumeItemList(featurevector, lineID);
							linecount++;
						}
					}
					long end=System.nanoTime();
					mytree.finishReceivingItemList();
					br.close();
					System.out.println("itemset reception finished,Time used: "+(end-start)/1000000+" milliseconds");
					System.out.println("total number of itemsets feeded : "+linecount);
					System.out.println();

					System.out.println("start building FP Tree.");
					start=System.nanoTime();
					mytree.buildTree();
					
					//FeatureVector vec=new FeatureVector();
					//vec.addFeatureWithOccurrence(6, 1);
					//vec.addFeatureWithOccurrence(6, 2);
					//vec.addFeatureWithOccurrence(4, 1);
					//vec.addFeatureWithOccurrence(3, 1);
					//System.out.println("count of vector "+vec+" is "+mytree.getCountOfFeatureVector(vec));
					
					end=System.nanoTime();
					System.out.println("FP Tree building finished,Time used: "+(end-start)/1000000+" milliseconds");
					System.out.println();
					
					HashMap<SortableItemSet,Integer> patterns;
					if(supportThreshold>0){
					System.out.println("start getting frequent patterns with support threshold: "+supportThreshold);
					start=System.nanoTime();
					
					patterns= mytree.getFrequentPatterns(supportThreshold);

					end=System.nanoTime();
					System.out.println(patterns.size()+" frequent patterns got,Time used: "+(end-start)/1000000+" milliseconds");
					System.out.println();
					}
					else{
						System.out.println("start getting hyperclique patterns with hconfidence: "+hconfidence);
						start=System.nanoTime();			
						patterns= mytree.getHyperCliquePatterns(hconfidence);
						end=System.nanoTime();
						System.out.println(patterns.size()+" hyperclique patterns got,Time used: "+(end-start)/1000000+" milliseconds");
						System.out.println();	
					}

					//write frequent patterns to file if -output parameter given
						File f=new File(outputPath);
						System.out.println("starting writing result to output destination: "+f.getAbsolutePath()+" given by -output command.");
						start=System.nanoTime();
						PrintWriter pw=new PrintWriter(f);
						for(Entry<SortableItemSet,Integer> en:patterns.entrySet()){
							SortableItemSet set=en.getKey();
							String inputline=set.toString();							
							if(!inputline.isEmpty())
								pw.println(inputline+";"+en.getValue());								
						} 
						pw.close();
						end=System.nanoTime();
						System.out.println("frequent patterns saved,Time used: "+(end-start)/1000000+" milliseconds");
						System.out.println();						
				} catch (IOException e) {						
					e.printStackTrace();
				}

			} catch (FileNotFoundException e1) {
				System.out.println("cannot find file at path: "+inputPath);
				e1.printStackTrace();
			}
		}
	}

	public static void getProjectedFeatureVectors(String inputPath,String WFIProjectedPath,String WFIoutputPath,int supportThreshold){
        String outputPath=null;
		if(inputPath==null){
			System.out.println("please use -input to give the path for input file.");
		}
		else if (supportThreshold<0){
			System.out.println("please give a non-negative integer based -support Threshold.");
		}
		else if (outputPath==null&&WFIoutputPath==null&&WFIProjectedPath==null){
			System.out.println("please use -output or -WFIoutput or -WFIProject to give at least one file path for output destination file.");
		}
		else {
			FP_InferenceTree mytree=new FP_InferenceTree("itemsets","summarization");
			mytree.prepareToReceiveItemList();
			int lineID=0;
			System.out.println("start reading from path "+inputPath+" and receiving itemsets.");
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(new File(inputPath)));
				long start=System.nanoTime();
				FeatureVector featurevector;
				int linecount=0;
				String line;
				try {
					while((line=br.readLine())!=null){
						lineID++;
						//parse into feature vector
						featurevector=FeatureVector.readFeatureVectorFromFormattedString(line);
						if(!featurevector.isEmpty()){
							mytree.consumeItemList(featurevector, lineID);
							linecount++;
						}
					}
					long end=System.nanoTime();
					mytree.finishReceivingItemList();
					br.close();
					System.out.println("itemset reception finished,Time used: "+(end-start)/1000000+" milliseconds");
					System.out.println("total number of itemsets feeded : "+linecount);
					System.out.println();

					System.out.println("start building FP Tree.");
					start=System.nanoTime();
					mytree.buildTree();
					end=System.nanoTime();
					System.out.println("FP Tree building finished,Time used: "+(end-start)/1000000+" milliseconds");
					System.out.println();

					System.out.println("start getting frequent patterns with support threshold: "+supportThreshold);
					start=System.nanoTime();
					HashMap<SortableItemSet,Integer> patterns= mytree.getFrequentPatterns(supportThreshold);

					end=System.nanoTime();
					System.out.println(patterns.size()+" frequent patterns got,Time used: "+(end-start)/1000000+" milliseconds");
					System.out.println();
					
					//If -WFIoutputPath or -WFIProjectedPath parameter given                    
					if(WFIoutputPath!=null||WFIProjectedPath!=null){
						LinkedHashMap <SortableItemSet,Double> WFImap=turnFPSetIntoWFISet(patterns,linecount);						
						//WFI
						if(WFIoutputPath!=null){
							File f=new File(WFIoutputPath);
							System.out.println("starting writing result to WFIoutput destination: "+f.getAbsolutePath()+" given by -WFIoutput command.");
							start=System.nanoTime();
							PrintWriter pw=new PrintWriter(f);
							for(Entry<SortableItemSet,Double> en: WFImap.entrySet()){
								String inputline=en.getKey().toString()+":"+en.getValue();							
								if(!inputline.isEmpty())
									pw.println(inputline);								
							} 
							pw.close();
							end=System.nanoTime();
							System.out.println("Weighted Frequent patterns saved to destination file,Time used: "+(end-start)/1000000+" milliseconds");
							System.out.println();	
						}
                         //WFI projected
						if(WFIProjectedPath!=null){
							File f=new File(WFIProjectedPath);
							System.out.println("starting writing result to WFIProjection destination: "+f.getAbsolutePath()+" given by -WFIProjected command.");
							start=System.nanoTime();
							PrintWriter pw=new PrintWriter(f);
							br = new BufferedReader(new FileReader(new File(inputPath)));						
							while((line=br.readLine())!=null){							
								FeatureVector vector=FeatureVector.readFeatureVectorFromFormattedString(line);
								if(!vector.isEmpty()){
									//System.out.println("------");
									//System.out.println(vector);
									FeatureVector projectedVector=projectFeatureVectorOnWFISet(vector,WFImap);
									//System.out.println(projectedVector);
									pw.println(projectedVector.toFormattedString());
								}                             
							}											
							br.close();								 
							pw.close();
							end=System.nanoTime();
							System.out.println("WFI projected feature vectors saved to destination file,Time used: "+(end-start)/1000000+" milliseconds");
							System.out.println();
							if(WFIoutputPath==null){
							f=new File("WFISets");
							pw=new PrintWriter(f);					
							for(Entry<SortableItemSet,Double> en: WFImap.entrySet()){
								String inputline=en.getKey().toString()+":"+en.getValue();							
								if(!inputline.isEmpty())
									pw.println(inputline);								
							} 																		 
							System.out.println("WFI sets have been put into an ordered list and saved to path "+f.getAbsolutePath());
							pw.close();
							}
						}
					}
				} catch (IOException e) {						
					e.printStackTrace();
				}

			} catch (FileNotFoundException e1) {
				System.out.println("cannot find file at path: "+inputPath);
				e1.printStackTrace();
			}
		}
	}
	
	public static LinkedHashMap<SortableItemSet,Double> turnFPSetIntoWFISet(HashMap<SortableItemSet,Integer> input){
		//try to remove redundant sets in FP set
		TreeMap<Integer,HashSet<SortableItemSet>> map=new TreeMap<Integer,HashSet<SortableItemSet>>();
		HashMap<SortableItemSet,Integer> newpatterns=new HashMap<SortableItemSet,Integer>();
		//put them into groups by their set size
		for (Entry<SortableItemSet,Integer> en: input.entrySet()){
			int size=en.getKey().size();
			HashSet<SortableItemSet> group=map.get(size);
			if(group==null){
				group=new HashSet<SortableItemSet>();
				map.put(size, group);
			}
			group.add(en.getKey());
		}
		
		int sum=0;
		//each group only need to check with the group with next higher size
		for (Entry<Integer,HashSet<SortableItemSet>> en: map.entrySet()){
			int size=en.getKey();
			HashSet<SortableItemSet> content=en.getValue();
			Entry<Integer,HashSet<SortableItemSet>> nexten=map.higherEntry(size);
			//if there is higher size group
			if(nexten!=null){
				HashSet<SortableItemSet> nextcontent=nexten.getValue();
				for (SortableItemSet set:content){
					int mycount=input.get(set);					
					for (SortableItemSet nextGroupSet:nextcontent){
						if(nextGroupSet.containsAll(set)){
						 	mycount-=input.get(nextGroupSet);
						}
					}
					if(mycount>0){
						newpatterns.put(set,mycount);
					    sum+=mycount;
					}
				}
			}
			//if current group has the highest size, then keep them intact
			else {
				for (SortableItemSet set:content){
					int occur=input.get(set);
					sum+=occur;
					newpatterns.put(set, occur);
				}
			}
		}
	    
	    LinkedHashMap<SortableItemSet,Double> result=new LinkedHashMap<SortableItemSet,Double>();
	    double dsum=(double)sum;
	    for (Entry<SortableItemSet, Integer> en: newpatterns.entrySet()){
	    		result.put(en.getKey(), (double)en.getValue()/dsum);
	    }
	    return result;
	}
	
	public static LinkedHashMap<SortableItemSet,Double> turnFPSetIntoWFISet(HashMap<SortableItemSet,Integer> input,int totalcount){
			    
	    LinkedHashMap<SortableItemSet,Double> result=new LinkedHashMap<SortableItemSet,Double>();
	    double dsum=(double)totalcount;
	    for (Entry<SortableItemSet, Integer> en: input.entrySet()){
	    		result.put(en.getKey(), -Math.log((double)en.getValue()/dsum));
	    }
	    return result;
	}
	

	public static FeatureVector projectFeatureVectorOnWFISet(FeatureVector vector,LinkedHashMap<SortableItemSet,Double> WFImap){
		FeatureVector newvector=new FeatureVector();
		int order=0;
		for (Entry<SortableItemSet,Double> en: WFImap.entrySet()){			
			boolean contain=vectorContainsSortableItemSet(vector,en.getKey());
			if(contain)
			newvector.addOneFeatureIn(order);			
			order++;
		}
		return newvector;
	}
	
	public static boolean vectorContainsSortableItemSet(FeatureVector vector, SortableItemSet set){

		HashMap<Integer,Integer> IDset=set.getSet();
		for (Entry<Integer,Integer> en:IDset.entrySet()){
			int vectoroccur=vector.getFeatureOccurrence(en.getKey());
			if(vectoroccur==0||vectoroccur<en.getValue())
				return false;
		}	
		return true;
	}
	
}
