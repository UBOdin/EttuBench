
import java.util.ArrayList;

import featureEngineering.CombinedRegularizer;
import featureEngineering.SelectNamingResolver;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

public class Test {

	public static void main(String[] args) {
		Utility.CreateDataFile("bombay", "data/bombay_query.csv");
		ArrayList<String> queryList = Utility.getQueryList("bombay");
		
		ArrayList<Statement> statementList1 = Utility.convertToStatement(queryList, false);
		
		ArrayList<SelectBody> statementList2=new ArrayList<SelectBody>();
		//do a pass of alias removing
		for(Statement stmt: statementList1){
			if(stmt instanceof Select){
				Select s=(Select) stmt;
		    	SelectNamingResolver resolver=new SelectNamingResolver(s,true);
		    	statementList2.add(resolver.aliasReplaceSelect());
			}
		}
		//System.out.println(SelectNamingResolver.antiSchemaMap);
		//System.out.println(SelectNamingResolver.schemaMap);
		//do a pass of Table name appending for select bodies
		for (SelectBody body:statementList2){
			SelectNamingResolver.giveTableNamesInSelectBody(body);
		}
		
		//do a pass of regularization			
		ArrayList<Statement> statementList = new ArrayList<Statement>(); 
		for(SelectBody body:statementList2){
			Select s=new Select();
			s.setSelectBody(CombinedRegularizer.regularize(body));
			statementList.add(s);	
		}
		
		// method name can be either aouiche, makiyama or aligon
		double[][] matrix = Utility.createDistanceMatrix("Aouiche", statementList);
		// write distance matrix to file
		Utility.WriteDistanceMatrixToFile(matrix, "data/bombay_Aouiche.csv");
	}

}
