
import java.util.ArrayList;

import featureEngineering.CombinedRegularizer;
import featureEngineering.SelectNamingResolver;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

public class Test {

	public static void main(String[] args) {
		Utility.CreateDataFile("ub", "data/ub_query.csv");
		ArrayList<String> queryList = Utility.getQueryList("ub");
		
		ArrayList<Statement> statementList1 = Utility.convertToStatement(queryList, false);
		ArrayList<String> statementList1Strings=new ArrayList<String>();
		ArrayList<String> statementList2Strings=new ArrayList<String>();
		ArrayList<String> statementList3Strings=new ArrayList<String>();
		ArrayList<String> statementList4Strings=new ArrayList<String>();
		
		ArrayList<SelectBody> statementList2=new ArrayList<SelectBody>();
		//do a pass of alias removing
		for(Statement stmt: statementList1){
			statementList1Strings.add(((Select)stmt).getSelectBody().toString());
			if(stmt instanceof Select){
				Select s=(Select) stmt;
		    	SelectNamingResolver resolver=new SelectNamingResolver(s,true);
		    	SelectBody body=resolver.aliasReplaceSelect();
		    	statementList2Strings.add(body.toString());
		    	statementList2.add(body);
			}
		}
		System.out.println(SelectNamingResolver.antiSchemaMap);
		System.out.println(SelectNamingResolver.schemaMap);
		//do a pass of Table name appending for select bodies
		for (int i=0;i<statementList2.size();i++){
			SelectNamingResolver.giveTableNamesInSelectBody(statementList2.get(i),i);
			statementList3Strings.add(statementList2.get(i).toString());
		}
		
		//do a pass of regularization			
		ArrayList<Statement> statementList = new ArrayList<Statement>(); 
		for(SelectBody body:statementList2){
			Select s=new Select();
			s.setSelectBody(CombinedRegularizer.regularize(body));
			statementList.add(s);
			statementList4Strings.add(s.getSelectBody().toString());
		}
		for(Integer index:SelectNamingResolver.indices){
			System.out.println("------------");
			System.out.println(statementList1Strings.get(index));
			System.out.println(statementList2Strings.get(index));
			System.out.println(statementList3Strings.get(index));
			System.out.println(statementList4Strings.get(index));
		}
		
		// method name can be either aouiche, makiyama or aligon
		double[][] matrix = Utility.createDistanceMatrix("Aouiche", statementList);
		// write distance matrix to file
		Utility.WriteDistanceMatrixToFile(matrix, "data/ub_Aouiche.csv");
	}

}
