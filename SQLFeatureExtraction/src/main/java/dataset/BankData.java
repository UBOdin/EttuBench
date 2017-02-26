package dataset;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

/**
 * bank data processor class
 * @author tingxie
 *
 */
public class BankData {
	public static final int MESSAGE = 14; // column number for the actual query
	
	public String data_path;
	public int max_num_file = 116;
	public BankData(String data_path, int max_num_file) {
		// TODO Auto-generated constructor stub
		this.data_path = data_path;
		this.max_num_file = max_num_file;
	}
	
	public ArrayList<String> getDistinctQueries() {
		ArrayList<Statement> SyntaxTreeList = new ArrayList<Statement>();
		ArrayList<String> query_list = new ArrayList<String>();
		String line = null;
		int count_notaquery = 0;
		int count_parsingerror = 0;
		int count_empty = 0;
		int count_net = 0;
		int count_queries = 0;
		int count_select = 0;
		int count_parsable = 0;
		try {
			for (int i = 1; i <= this.max_num_file; ++i) {
				String filename = "output_" + i + ".csv";
				System.out.println(filename);
				BufferedReader input = new BufferedReader(new FileReader(new File(this.data_path + filename)));
				input.readLine();
				line = input.readLine();
				
				while (line != null) {
					String[] strArr = line.split(";");
					line = input.readLine();
					
					if (strArr.length < 15)
						continue;
					String query = strArr[MESSAGE];
					
					if (query.equals("NOT A QUERY")) {
						count_notaquery++;
						continue;
					} else if (query.equals("PARSING_ERROR")) {
						count_parsingerror++;
						continue;
					} else if (query.equals("")) {
						count_empty++;
						continue;
					}
					if (query.startsWith("net")) {
						count_net++;
						continue;
					}
					//System.out.println(query);
					InputStream stream = new ByteArrayInputStream(query.getBytes(StandardCharsets.UTF_8));
					CCJSqlParser parser = new CCJSqlParser(stream);
					Statement stmt = null;
					try {
						stmt = parser.Statement(); 
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						count_parsingerror++;
						continue; // continue with other queries if the current query cannot be parsed
					} catch (Exception e) {
						count_parsingerror++;
						System.out.println(e.getClass());
						System.exit(1);
					}
					count_parsable++;
					if (!(stmt instanceof Select))
						continue;
					count_select++;
					Select select = (Select)stmt;
					SelectBody selectBody = select.getSelectBody();
					
					boolean foundSameSyntaxTree = false;
					for (int j = 0; j < SyntaxTreeList.size(); ++j) {
						
						if (QuerySkeletonCheck.compareStatement(stmt, SyntaxTreeList.get(j)) == true) {
							foundSameSyntaxTree = true;
						}
					}
					if (foundSameSyntaxTree == false) {
						SyntaxTreeList.add(stmt);
						query_list.add(query);
					}
				}
				input.close();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(line);
		}
		System.out.println("not a query: " + count_notaquery);
		System.out.println("parsing error: " + count_parsingerror);
		System.out.println("net: " + count_net);
		System.out.println("select: " + count_select);
		System.out.println("parsable: " + count_parsable);
		System.out.println("skeleton: " + query_list.size());
		return query_list;
	}
	
	public void writeDistinctQueriesToFile(String fileName) {
		ArrayList<String> queryList = this.getDistinctQueries();
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(fileName)));
			for (String query : queryList) {
				output.write(query.toString() + "\n");
			}
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public ArrayList<String> readDistinctQueriesFromFile(String fileName) {
		ArrayList<String> queryList = new ArrayList<>();
		try {
			BufferedReader input = new BufferedReader(new FileReader(new File(fileName)));
			String line = input.readLine();
			while (line != null) {
				queryList.add(line);
				line = input.readLine();
			}
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}		
		return queryList;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int max_num_file = 116;
		BankData data = new BankData("/Users/anhduc/study/Insider Threats/bank_data/", max_num_file);
		data.writeDistinctQueriesToFile("/Users/anhduc/study/Insider Threats/bank_data/distinct_queries_" + max_num_file + ".txt");
	}

}

