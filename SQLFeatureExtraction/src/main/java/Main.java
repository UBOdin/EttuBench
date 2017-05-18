import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import Regularization.CombinedRegularizer;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

/**
 * Main class that extracts features from queries,
 * using these features to do clustering and save the result
 * in the format on which users can use R code provided in the repo directly
 * to evaluate the clustering performance. 
 * @author tingxie
 *
 */
public class Main {
	public static String datapath=null;

	public static void main(String[] args) {
		try {
			String current = new java.io.File( "." ).getCanonicalPath();
			File f=new File(current);
			String parent=f.getParent();
			datapath=parent+"/data/";
			System.out.println("data path : "+datapath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] data={"ub","bombay","googleplus"};
		String[] method={"aligon","makiyama","aouiche"};
		HashSet<Integer> modules=new HashSet<Integer>();		

		for(int i=0;i<args.length;i++){
			if(args[i].equalsIgnoreCase("-input")){
				if(i+1<args.length){
					data=new String[1];
					String name=args[i+1].toLowerCase();
					if(name.contains("ub"))
						data[0]="ub";
					else if (name.contains("bombay"))
						data[0]="bombay";
					else if (name.contains("googleplus"))
						data[0]="googleplus"; 
				}					
			}
			if(args[i].equalsIgnoreCase("-metric")){
				if(i+1<args.length){
					method=new String[1];
					String name=args[i+1].toLowerCase();
					if(name.contains("aligon"))
						method[0]="aligon";
					else if (name.contains("aouiche"))
						method[0]="aouiche";
					else if (name.contains("makiyama"))
						method[0]="makiyama";
				}
			}
			if(args[i].equalsIgnoreCase("-modules")){
				if(i+1<args.length){
					String content=args[i+1];
					String[] tokens=content.split("\\&");
					modules=new HashSet<Integer>();					
					for(int j=0;j<tokens.length;j++){
						modules.add(Integer.parseInt(tokens[j]));
					}
				}
			}
			if(args[i].equalsIgnoreCase("-datapath")){
				if(i+1<args.length){
					datapath = args[i+i];
				}
			}
		}

		ArrayList<ArrayList<String> > queryLists=new ArrayList<ArrayList<String>> ();
		System.out.println("---begin query retrieval------");
		System.out.println();
		//long start=System.nanoTime();

		for(int i=0;i<data.length;i++){
			System.out.println("data set using is "+data[i]);
			System.out.println();			
			System.out.println("begin preparing queries from data set "+data[i]);
			Utility.CreateDataFile(datapath,data[i],data[i]+"_query.csv");
			System.out.println("query data created and saved to "+datapath+data[i]+"_query.csv. ");
			System.out.println();			
			System.out.println("begin reading text queries into memory.");
			System.out.println();
			ArrayList<String> queryList = Utility.getQueryList(datapath,data[i]);
			System.out.println(queryList.size()+" queries in total");
			queryLists.add(queryList);	
			System.out.println();
		}
		//long end=System.nanoTime();
		System.out.println("query parsing finished.");
		System.out.println();

		System.out.println("---begin query regulairization and comparison------");
		System.out.println();



		for(int i=0;i<data.length;i++){
			for(int j=0;j<method.length;j++){
				if(!modules.isEmpty()){
					queryComparison(data[i],method[j],queryLists.get(i),modules);
				}
				else{
					HashSet<Integer>	newmodules;
					for(int k=0;k<5;k++){
					newmodules=new HashSet<Integer>();
						newmodules.add(k);						
						queryComparison(data[i],method[j],queryLists.get(i),newmodules);
					}
					newmodules=new HashSet<Integer>();
					newmodules.add(1);newmodules.add(2);newmodules.add(3);newmodules.add(4);
					queryComparison(data[i],method[j],queryLists.get(i),newmodules);
				}					
			}
		}		
	}


	public static void queryComparison(String data,String method,ArrayList<String> queryList,HashSet<Integer> modules){		
		System.out.println("~~~~~~~~");
		System.out.println("data using "+data);
		System.out.println("metric using "+method);
		if(modules.contains(1))
			System.out.println("naming module will be applied");
		if (modules.contains(2))
			System.out.println("Expression Regularization module will be applied");
		if(modules.contains(3))
			System.out.println("Flattening From-Nesting module will be applied");
		if (modules.contains(4))
			System.out.println("Union pull-out module will be applied");
		boolean regu=true;
		if(modules.size()==1&&modules.iterator().next()==0){
			regu=false;
			System.out.println("no regularization module will be applied");
		}
		System.out.println();
		//parse queries
		System.out.println("nonparsable queries are saved into "+datapath+data+"_nonparsable.txt");
		ArrayList<Statement> statementList1 = Utility.convertToStatement(data,datapath,queryList);
		ArrayList<Statement> statementList = new ArrayList<Statement>();
		//do a pass of regularization	
		if(regu){
			System.out.println("begin regularization");		
			//long start=System.nanoTime();
			for(int k=0;k<statementList1.size();k++){		
				Select s=(Select) statementList1.get(k);
				statementList.add(CombinedRegularizer.regularize(s,modules));
			}
			//long end=System.nanoTime();
			System.out.println("regularization ended.");
			System.out.println();
		}

		System.out.println("begin query comparison and clustering. ");
		//long start=System.nanoTime();
		// method name can be either aouiche, makiyama or aligon
		double[][] matrix;
		if(regu)
			matrix = Utility.createDistanceMatrix(method, statementList);
		else
			matrix = Utility.createDistanceMatrix(method, statementList1);	

		//long end=System.nanoTime();
		// write distance matrix to file
		String outputpath;
		if(!regu)
			outputpath=datapath+data+"_"+method+".csv";
		else {
			ArrayList<Integer> mds=new ArrayList<Integer>();
			for(Integer m:modules)
				mds.add(m);
			Collections.sort(mds);
			if(mds.size()==4)
				outputpath=datapath+data+"_"+method+"_regularization";
			else{
				outputpath=datapath+data+"_"+method+"_regularization_";
				for(Integer m:mds)
					outputpath+=m.toString();
			}
			outputpath+=".csv";
		}

		Utility.WriteDistanceMatrixToFile(matrix, outputpath);	
		System.out.println("comparison finished, distance matrix saved to "+outputpath);
		System.out.println();
	}
}
