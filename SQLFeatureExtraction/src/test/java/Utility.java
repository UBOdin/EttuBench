
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import dataset.BankData;
import dataset.PocketData;
import featureEngineering.CombinedRegularizer;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import querySimilarityMetrics.Aligon;
import querySimilarityMetrics.Aouiche;
import querySimilarityMetrics.Makiyama;
import toolsForMetrics.ExtendedColumn;
import toolsForMetrics.Global;


public class Utility {
	public static void WriteQueryToFile(String dataset) {
		try {
			BufferedWriter output = null;
			
			if (dataset.equals("bombay")) {// Bombay IIT dataset
				output = new BufferedWriter(new FileWriter(new File("outputBombayIIT/bombay_queries.csv")));
				output.write("query\n");
				int[] question_per_ass = {5,5,4};
				for (int assignment = 0; assignment <= 2; ++assignment) {
					for (int question = 1; question <= question_per_ass[assignment]; ++question) {
						String fileName = "outputBombayIIT/" + assignment + "_" + question + ".txt";
						System.out.println(fileName);
						BufferedReader input = new BufferedReader(new FileReader(new File(fileName)));
						String line = input.readLine();
						while (line != null) {
							String[] arr = line.split(";");
							output.write(arr[1] + "\n");
							line = input.readLine();
						}
						input.close();
					}
				}
			} else { // UB exam dataset
				output = new BufferedWriter(new FileWriter(new File("outputBombayIIT/ub_queries.csv")));
				output.write("query\n");
				BufferedReader input = new BufferedReader(new FileReader("ub_exam_data.txt"));
				input.readLine();
				String line = input.readLine();
				String query = null;
				while (line != null) {
					String[] arr = line.split("\t");
					query = arr[5];
					line = input.readLine();
					
					if (query.equals("empty")) {
						continue;
					} else if (!arr[6].equals("empty")) {
						query = arr[6];
					}
					
					if (!arr[3].contains("invalid syntax")) {
						output.write(query + "\n");
					}
				}
				input.close();
			}
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> getQueryList(String dataset) {
		ArrayList<String> queryList = new ArrayList<>();
		try {
			
			if (dataset.equals("pocket")) {
				PocketData pocketData = new PocketData("/Users/anhduc/study/Insider Threats/PhoneLab_data/");
				queryList = pocketData.readDistinctQueriesFromFile("data/pocket_data.txt");
			}
			else if (dataset.startsWith("bank")) { // Bank dataset
				BankData bankData = new BankData("/Users/anhduc/study/Insider Threats/bank_data/", 116);
				queryList = bankData.readDistinctQueriesFromFile("data/bank_data.txt");
			}
			else if (dataset.equals("bombay")) { // Bombay IIT dataset
				int[] question_per_ass = {5,5,4};
				for (int assignment = 0; assignment <= 2; ++assignment) {
					for (int question = 1; question <= question_per_ass[assignment]; ++question) {
						String fileName = "data/" + assignment + "_" + question + ".txt";
						System.out.println(fileName);
						BufferedReader input = new BufferedReader(new FileReader(new File(fileName)));
						String line = input.readLine();
						while (line != null) {
							String[] arr = line.split(";");
							queryList.add(arr[1]);
							line = input.readLine();
						}
						input.close();
					}
				}
			} else { // UB exam dataset
				BufferedReader input = new BufferedReader(new FileReader("data/ub_exam_data.txt"));
				input.readLine();
				String line = input.readLine();
				String query = null;
				while (line != null) {
					String[] arr = line.split("\t");
					query = arr[5];
					
					line = input.readLine();
					
					if (query.equals("empty")) {
						continue;
					} else if (!arr[6].equals("empty")) {
						query = arr[6];
					}
					if (query.startsWith("\"")) {
						query = query.substring(1, query.length() - 1);
					}
					query = query.replace("when", "col_when");
					int label = Integer.parseInt(arr[0])-2013;
					int score = Integer.parseInt(arr[2]);
					if (label == 1) {
						if (score * 100.0 / 15.0 < 50.0) {
							continue;
						}
					} else {
						if (score * 100.0 / 8.0 < 50.0) {
							continue;
						}
					}
					if (!arr[3].contains("invalid syntax")) {
						queryList.add(query);
					}
				}
				//System.out.println(count14);
				//System.out.println(count15);
				input.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return queryList;
	}
	
	public static void CreateDataFile(String dataset, String outputFileName) {
		try {
			BufferedWriter output_id = new BufferedWriter(new FileWriter(new File(outputFileName)));
			output_id.write("id\tlabel\tquery\n");
			
			if (dataset.equals("bombay")) {// Bombay IIT dataset
				int[] question_per_ass = {5,5,4};
				int count_question = 1;
				for (int assignment = 0; assignment <= 2; ++assignment) {
					for (int question = 1; question <= question_per_ass[assignment]; ++question) {
						String fileName = "data/" + assignment + "_" + question + ".txt";
						System.out.println(fileName);
						BufferedReader input = new BufferedReader(new FileReader(new File(fileName)));
						String line = input.readLine();
						int count_line = 0;
						while (line != null) {
							count_line++;
							output_id.write(assignment + "_" + question + "_" + count_line + "\t");
							output_id.write(count_question + "\t");
							output_id.write(line.split(";")[1] + "\n");
							line = input.readLine();
						}
						input.close();
						count_question++;
					}
				}
			} else { // UB exam dataset
				BufferedReader input = new BufferedReader(new FileReader("data/ub_exam_data.txt"));
				input.readLine();
				String line = input.readLine();
				String query = null;
				int id, label;
				int count = 1;
				while (line != null) {
					String[] arr = line.split("\t");
					query = arr[5];
					/*
					if (query.startsWith("\"")) {
						//System.out.println(query);
						query = query.substring(1, query.length()-1);
						System.out.println(query);
					}*/
					label = Integer.parseInt(arr[0])-2013;
					
					id = Integer.parseInt(arr[1]);
					line = input.readLine();
					
					if (query.equals("empty")) {
						continue;
					} else if (!arr[6].equals("empty")) {
						query = arr[6];
					}
					
					int score = Integer.parseInt(arr[2]);
					if (label == 1) {
						if (score * 100.0 / 15.0 < 50.0) {
							continue;
						}
					} else {
						if (score * 100.0 / 8.0 < 50.0) {
							continue;
						}
					}
					if (!arr[3].contains("invalid syntax")) {
						output_id.write(id + "\t" + label + "\t" + query + "\n");
					}
				}
				input.close();
			}
			output_id.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static double[][] createDistanceMatrix(String method, ArrayList<Statement> statementList) {
		Global.tableAlias = new HashMap<String, String>();

		double[][] matrix = new double[statementList.size()][statementList.size()];
		
		for (int j = 0; j < statementList.size(); ++j) {
			matrix[j][j] = 0;
		}
		if (method.toLowerCase().equals("aligon")) {
			ArrayList<TreeSet<toolsForMetrics.ExtendedColumn>> queryProjectionSet = new ArrayList<>();
			ArrayList<TreeSet<ExtendedColumn>> queryGroupBySet = new ArrayList<>();
			ArrayList<TreeSet<ExtendedColumn>> querySelectionSet = new ArrayList<>();
			
			for (int j = 0; j < statementList.size(); j++) {
				Aligon.createQueryVector(statementList.get(j));
				queryProjectionSet.add(j, Aligon.getProjectionList());
				queryGroupBySet.add(j, Aligon.getGroupByList());
				querySelectionSet.add(j, Aligon.getSelectionList());
			}
			
			for (int j = 0; j < statementList.size() - 1; ++j) {
				for (int k = 0; k < statementList.size(); ++k) {
					if (j != k) {
						matrix[j][k] = 1 - Aligon.getDistanceAsRatio(
								queryProjectionSet.get(j), queryGroupBySet.get(j), querySelectionSet.get(j),
								queryProjectionSet.get(k), queryGroupBySet.get(k), querySelectionSet.get(k));
						matrix[k][j] = matrix[j][k];
					}
				}
			}
		} else if (method.toLowerCase().equals("makiyama")) {
			ArrayList<TreeMap<String, Integer>> queryMap = new ArrayList<>();
			for (int j = 0; j < statementList.size(); j++) {
				//System.out.println(statementList.get(j));
				/*
				if (statementList.get(j).toString().startsWith("SELECT external_quest_id, event_instance_id, metadata_version, secondary_category, (SELECT sum(total) FROM (SELECT ei2.value ")) {
					System.out.println("debug");
				}*/
				queryMap.add(j, Makiyama.getQueryVector(statementList.get(j)));
			}
			
			for (int j = 0; j < queryMap.size() - 1; ++j) {
				for (int k = 0; k < queryMap.size(); ++k) {
					if (j != k) {
						matrix[j][k] = 1-Makiyama.getDistanceAsRatio(queryMap.get(j), queryMap.get(k));
						matrix[k][j] = matrix[j][k];
					}
				}
			}
			
		} else if (method.toLowerCase().equals("aouiche")) {
			ArrayList<TreeSet<ExtendedColumn>> queryMap = new ArrayList<>();
			
			for (int j = 0; j < statementList.size(); j++) {
				queryMap.add(j, Aouiche.getQuerySet(statementList.get(j)));
			}
			
			for (int j = 0; j < queryMap.size() - 1; ++j) {
				for (int k = 0; k < queryMap.size(); ++k) {
					if (j != k) {
						matrix[j][k] = 1-Aouiche.getDistanceAsRatio(queryMap.get(j), queryMap.get(k));
						matrix[k][j] = matrix[j][k];
					}
				}
			}
		} else {
			System.err.println("don't know the metric name");
			System.exit(1);
		}
		
		return matrix;
	}

	public static ArrayList<Statement> convertToStatement(ArrayList<String> queryList, boolean preprocessed) {
		ArrayList<Statement> statementList = new ArrayList<>();
		
		for (String query : queryList) {
			InputStream stream = new ByteArrayInputStream(query.getBytes(StandardCharsets.UTF_8));
			CCJSqlParser parser = new CCJSqlParser(stream);
			Statement statement = null;
			try {
				statement = parser.Statement();
			} catch (ParseException e) {
				System.out.println(query);
				e.printStackTrace();
				System.exit(1);
			}
			statementList.add(statement);
		}
		if (preprocessed == true) {
			String path="/Users/tingxie/Documents/bank_data/";
			PrintWriter wr = null;
			try {
				wr = new PrintWriter(path+"testout");
			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
			}

			ArrayList<Statement> newStatementList = new ArrayList<>();
			for (Statement stmt : statementList) {
				newStatementList.add(Utility.getSelect(stmt, true));
			}
			
			wr.close();
			
			return newStatementList;
			/*
			ArrayList<Statement> newStatementList = new ArrayList<>();
			for (Statement stmt : statementList) {
				newStatementList.add(TingRegularizer.regularize((Select) stmt));
			}
			return newStatementList;*/
		}
		return statementList;
	}
	
	
	
//	public static ArrayList<ArrayList<Integer>> getFeatureVectorFromWLAlgo(ArrayList<String> queryList, boolean preprocessed) {
//		ArrayList<ArrayList<Integer>> sparseMatrix = new ArrayList<>();
//		StatementToTreeEncoder encoder = new StatementToTreeEncoder();
//		for (int j = 0; j < queryList.size(); ++j) {
//			String query = queryList.get(j);
//			Tree tree = encoder.ParseSqlLine(query, preprocessed);
//			HashMap<Integer, Integer> hashMap = TreeToLabelListEncoder.parseTree(tree);
//			for (Entry<Integer, Integer> entry : hashMap.entrySet()) {
//				ArrayList<Integer> row = new ArrayList<>();
//				row.add(j + 1);
//				row.add(entry.getKey() + 1);
//				row.add(entry.getValue());
//				sparseMatrix.add(row);
//			}
//		}
//		return sparseMatrix;
//	}
	
	public static void WriteFeatureMatrixToFile(ArrayList<ArrayList<Integer>> sparseMatrix, String fileName) {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(fileName)));
			output.write("row,column,value\n");
			for (ArrayList<Integer> row : sparseMatrix) {
				output.write(row.get(0) + "," + row.get(1) + "," + row.get(2) + "\n");
			}
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void WriteDistanceMatrixToFile(double[][] distanceMatrix, String fileName) {
		try {
			File f=new File(fileName);
			//System.out.println(f.getAbsolutePath());
			BufferedWriter output = new BufferedWriter(new FileWriter(f));
			for (int i = 0; i < distanceMatrix.length; ++i) {
				for (int j = 0; j < distanceMatrix.length; ++j) {
					if (j == distanceMatrix.length - 1) {
						output.write(distanceMatrix[i][j] + "\n");
					} else {
						output.write(distanceMatrix[i][j] + ",");
					}
				}
			}
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
	public static Tree<String> convertToTree(Operator root) {
		Tree<String> tree = new Tree<String>(convertToNode(root));
		return tree;
	}
	
	public static Node<String> convertToNode(Operator root) {
		Node<String> node = new Node<String>(root.toString());
		if (root instanceof ScanOperator) {
			node.setLeaf(true);
		}
		ArrayList<Operator> children = root.getChildren();
		if (children.size() == 1) {
			node.addChildren(convertToNode(children.get(0)));
		} else if (children.size() == 2) {
			node.addChildren(convertToNode(children.get(0))); 
			node.addChildren(convertToNode(children.get(1)));
		}
		return node;
	}*/
	
	/*
	public static Calendar convertStringToCalendar(String str) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.US);
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(format.parse(str));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return cal;
	}
	*/
	
	public static void printExpressionTreeRecursive(Expression exp, int level) {
		if (exp instanceof Parenthesis) {
			printExpressionTreeRecursive(((Parenthesis) exp).getExpression(), level);
			return;
		}
		for (int i = 0; i < level; ++i) {
			System.out.print("\t");
		}
		if (exp instanceof BinaryExpression) {
			if (exp instanceof Addition) 
				System.out.println("Addition");
			else if (exp instanceof AndExpression) 
				System.out.println("AND");
			else if (exp instanceof Division) 
				System.out.println("Division");
			else if (exp instanceof EqualsTo) 
				System.out.println("EqualsTo");
			else if (exp instanceof GreaterThan) 
				System.out.println("GreaterThan");
			else if (exp instanceof GreaterThanEquals) 
				System.out.println("GreaterThanEquals");
			else if (exp instanceof LikeExpression) 
				System.out.println("LIKE");
			else if (exp instanceof MinorThan) 
				System.out.println("MinorThan");
			else if (exp instanceof MinorThanEquals) 
				System.out.println("MinorThanEquals");
			else if (exp instanceof Multiplication) 
				System.out.println("Multiplication");
			else if (exp instanceof NotEqualsTo) 
				System.out.println("NotEqualsTo");
			else if (exp instanceof OrExpression) 
				System.out.println("OR");
			else if (exp instanceof Subtraction) 
				System.out.println("Subtraction");
			printExpressionTreeRecursive(((BinaryExpression) exp).getLeftExpression(), level + 1);
			printExpressionTreeRecursive(((BinaryExpression) exp).getRightExpression(), level + 1);
		}
		else {
			System.out.println(exp);
		}
	}
	
	static long parseLong(String str) throws Exception {
		long result = 0;
        char[] arr = str.toCharArray();
        int startIndex = 0;
        if ((arr[0] == '-') || (arr[0] == '+')) {
            startIndex = 1;
        }
        for (int i = startIndex; i < arr.length; ++i) {
        	int digit = Character.getNumericValue(arr[i]);
            if ((digit < 0) || (digit > 9)) {
                throw new Exception("invalid input");
            }
            result = result * 10 + digit;
        }
        if ((startIndex == 1) && (arr[0] == '-')) {
            result = -result;
        }
        return result;
    }
	
	/**
	 * get the list of plain select queries from the statement
	 * @param newline
	 * @return
	 */
	public static Select getSelect(Statement stmt,boolean doPreprocess){	
		// currently only process select statements
		if (stmt instanceof Select) {	
			Select s = (Select) stmt;
			//preprocessing
			if(doPreprocess){
			s.setSelectBody(CombinedRegularizer.regularize(s.getSelectBody()));
			
			}
			return s;
			
		}
		else if(stmt  instanceof Insert||stmt instanceof Update){
			
		}
		else if (stmt!=null&&!(stmt instanceof Delete)){
			System.out.println("strange sql queries found : "+stmt);
		}		
		return null;
	}
}
