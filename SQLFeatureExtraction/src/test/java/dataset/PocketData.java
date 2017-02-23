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
import net.sf.jsqlparser.parser.TokenMgrError;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class PocketData {
	public String data_path;
	public PocketData(String data_path) {
		// TODO Auto-generated constructor stub
		this.data_path = data_path;
	}
	
	public ArrayList<String> getDistinctQueries() {
		int count_parsable = 0;
		int count_parse_error = 0;
		int count_select = 0;
		ArrayList<Statement> SyntaxTreeList = new ArrayList<Statement>();
		ArrayList<String> query_list = new ArrayList<>();
		File file = new File(data_path);
		String[] filename_list = file.list();
		for (String filename : filename_list) {
			//System.out.println(data_path + filename);
			file = new File(data_path + filename);
			if (file.isDirectory()) {
				// go into that directory
				String path = data_path + filename + "/tag/SQLite-Query-PhoneLab/2015/03/";
				File folder = new File(path);
				String[] file_list = folder.list();
				for (String f : file_list) {		
					if (!f.equals(".DS_Store")) {
						try {	
							System.out.println(path + f);
							BufferedReader input = new BufferedReader(new FileReader(new File(path + f)));
							String line = input.readLine();
							while (line != null) {
								String old_line = line;
								line = input.readLine();
								
								if ((old_line.indexOf('{') != -1) && (old_line.indexOf('}') != -1)) {
									
									String json = old_line.substring(old_line.indexOf('{') + 1, old_line.indexOf('}'));
									if (json.indexOf("\"Results\":") != -1) {
										
										String remaining = json.substring(json.indexOf("\"Results\":") + 11, json.length());
										
										String query = remaining.substring(0, remaining.indexOf("\""));
										if (query.startsWith("SQLiteProgram")) {
											query = query.substring(15, query.length());
										}
										if (query.indexOf("\\n") != -1) {
											query = query.replace("\\n", " ");
										}
										// parse the query
										//System.out.println(query);
										InputStream stream = new ByteArrayInputStream(query.getBytes(StandardCharsets.UTF_8));
										CCJSqlParser parser = new CCJSqlParser(stream);
										Statement stmt = null;
										try {
											stmt = parser.Statement(); 
										} catch (ParseException e) {
											count_parse_error++;
											continue; // continue with other queries if the current query cannot be parsed
										} catch (TokenMgrError e) {
											count_parse_error++;
											continue;
										} catch (NumberFormatException e) {
											count_parse_error++;
											continue;
										}
										count_parsable++;
										if (!(stmt instanceof Select)) {
											continue;
										}
										count_select++;
										Select select = (Select)stmt;
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
								}
							}
							input.close();
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}
			}
		}
		System.out.println("un-parsable queries: " + count_parse_error);
		System.out.println("parsable queries: " + count_parsable);
		System.out.println("select: " + count_select);
		System.out.println("skeletons: " + query_list.size());
		
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
		PocketData data = new PocketData("/Users/anhduc/study/Insider Threats/PhoneLab_data/");
		//data.writeDistinctQueriesToFile("/Users/anhduc/study/Insider Threats/PhoneLab_data/distinct_queries.txt");
		data.readDistinctQueriesFromFile("/Users/anhduc/study/Insider Threats/PhoneLab_data/distinct_queries.txt");
	}

}

