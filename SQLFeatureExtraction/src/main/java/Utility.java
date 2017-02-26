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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import dataset.BankData;
import dataset.PocketData;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import querySimilarityMetrics.Aligon;
import querySimilarityMetrics.Aouiche;
import querySimilarityMetrics.Makiyama;
import toolsForMetrics.ExtendedColumn;
import toolsForMetrics.Global;

/**
 * Utility class that pre-process input file into queries
 * @author tingxie
 *
 */
public class Utility {

	
	public static ArrayList<String> getQueryList(String datapath,String dataset) {
		ArrayList<String> queryList = new ArrayList<>();
		try {
			
			if (dataset.equals("pocket")) {
				PocketData pocketData = new PocketData(datapath);
				queryList = pocketData.readDistinctQueriesFromFile("pocket_data.txt");
			}
			else if (dataset.startsWith("bank")) { // Bank dataset
				BankData bankData = new BankData(datapath, 116);
				queryList = bankData.readDistinctQueriesFromFile("bank_data.txt");
			}
			else if (dataset.contains("bombay")) { // Bombay IIT dataset
				int[] question_per_ass = {5,5,4};
				for (int assignment = 0; assignment <= 2; ++assignment) {
					for (int question = 1; question <= question_per_ass[assignment]; ++question) {
						String fileName = datapath + assignment + "_" + question + ".txt";
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
			} else if (dataset.contains("facebook")) {
				String[] fileList = new String[4];
				fileList[0] = "1924a6a39f09d992514925466c8844462377205d.csv";
				fileList[1] = "2747d54967a32fc95945671b930d57c1d5a9ac02.csv";
				fileList[2] = "9508c8dad8392584f9fc9f185fccb06ee37fdf7f.csv";
				fileList[3] = "6851422d5cc6909eab3be6588846cbf8fe4e7a90.csv";
				HashSet<String> set = new HashSet<>();
				
				for (String fileName : fileList) {
					try {
						BufferedReader input = new BufferedReader(new FileReader(new File(datapath+fileName)));
						String line;
						line = input.readLine();
						String query;
						while (line != null) {
							String[] arr = line.split("\t");
							query = arr[4];
							if ((!query.startsWith("PRAGMA")) && (!query.startsWith("SQLiteProgram"))) {
								query = query.toLowerCase();
								//query = query.replace("key", "col_key");
								InputStream stream = new ByteArrayInputStream(query.getBytes(StandardCharsets.UTF_8));
								CCJSqlParser parser = new CCJSqlParser(stream);
								Statement statement = null;
								try {
									statement = parser.Statement();
									//statement = removeString(statement);
									set.add(statement.toString());
									
								} catch (ParseException e) {
									line = input.readLine();
									continue;
								}
							}
							line = input.readLine();
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				queryList = new ArrayList<>(set);
			} else if (dataset.contains("googleplus")) {
				try {
					BufferedReader input = new BufferedReader(new FileReader(new File(datapath+"ManualClustering.txt")));
					String line;
					line = input.readLine();
					String query;
					while (line != null) {
						String[] arr = line.split("\t");
						query = arr[1];
						query = query.substring(query.indexOf("SELECT"));
						query = query.substring(0, query.length() - 1);
						//query = query.replace("key", "col_key");
						InputStream stream = new ByteArrayInputStream(query.getBytes(StandardCharsets.UTF_8));
						CCJSqlParser parser = new CCJSqlParser(stream);
						Statement statement = null;
						try {
							statement = parser.Statement();
							queryList.add(query);
							
						} catch (ParseException e) {
							line = input.readLine();
							continue;
						}
						line = input.readLine();
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}

			}  else if (dataset.contains("ub")){ // UB exam dataset
				BufferedReader input = new BufferedReader(new FileReader(datapath+"ub_exam_data.txt"));
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
	
	public static void CreateDataFile(String datapath,String dataset, String outputFileName) {
		try {
			BufferedWriter output_id = new BufferedWriter(new FileWriter(new File(datapath+outputFileName)));
			output_id.write("id\tlabel\tquery\n");
			
			if (dataset.contains("bombay")) {// Bombay IIT dataset
				int[] question_per_ass = {5,5,4};
				int count_question = 1;
				for (int assignment = 0; assignment <= 2; ++assignment) {
					for (int question = 1; question <= question_per_ass[assignment]; ++question) {
						String fileName = datapath + assignment + "_" + question + ".txt";
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
			} else if (dataset.contains("ub")) { // UB exam dataset
				BufferedReader input = new BufferedReader(new FileReader(datapath+"ub_exam_data.txt"));
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
			} else if (dataset.contains("facebook")){
				String[] fileList = new String[4];
				fileList[0] = "1924a6a39f09d992514925466c8844462377205d.csv";
				fileList[1] = "2747d54967a32fc95945671b930d57c1d5a9ac02.csv";
				fileList[2] = "9508c8dad8392584f9fc9f185fccb06ee37fdf7f.csv";
				fileList[3] = "6851422d5cc6909eab3be6588846cbf8fe4e7a90.csv";
				HashSet<String> set = new HashSet<>();
				
				for (String fileName : fileList) {
					BufferedReader input = new BufferedReader(new FileReader(new File(datapath+fileName)));
					String line;
					line = input.readLine();
					String query;
					while (line != null) {
						String[] arr = line.split("\t");
						query = arr[4];
						if ((!query.startsWith("PRAGMA")) && (!query.startsWith("SQLiteProgram"))) {
							query = query.toLowerCase();
							//query = query.replace("key", "col_key");
							InputStream stream = new ByteArrayInputStream(query.getBytes(StandardCharsets.UTF_8));
							CCJSqlParser parser = new CCJSqlParser(stream);
							Statement statement = null;
							try {
								statement = parser.Statement();
								//statement = removeString(statement);
								set.add(statement.toString());
								
							} catch (ParseException e) {
								line = input.readLine();
								continue;
							}
						}
						line = input.readLine();
					}
				}
				// read cluster id of each query
				ArrayList<Integer> cluster_id = new ArrayList<>();
				BufferedReader in = new BufferedReader(new FileReader(new File(datapath+"makiyama_clustered.txt")));
				String line = in.readLine();
				while (line != null) {
					cluster_id.add(Integer.parseInt(line));
					line = in.readLine();
				}
				in.close();
				Iterator<String> it = set.iterator();
				int counter = 1;
				while (it.hasNext()) {
					String temp = it.next();
					output_id.write(counter + "\t" + cluster_id.get(counter - 1) + "\t" + temp + "\n");
					counter++;
				}
			} else if (dataset.contains("googleplus")){ 
				BufferedReader input = new BufferedReader(new FileReader(new File(datapath+"ManualClustering.txt")));
				String line;
				line = input.readLine();
				String query, id, label;
				while (line != null) {
					String[] arr = line.split("\t");
					query = arr[1];
					id = query.substring(1, query.indexOf("SELECT")-2);
					System.out.println(id);
					query = query.substring(query.indexOf("SELECT"));
					query = query.substring(0, query.length() - 1);
					if (arr[0].equals("Account")) {
						label = "1";
					} else if (arr[0].equals("Activity")) {
						label = "2";
					} else if (arr[0].equals("Analytics")) {
						label = "3";
					} else if (arr[0].equals("Contacts")) {
						label = "4";
					} else if (arr[0].equals("Feed")) {
						label = "5";
					} else if (arr[0].equals("Housekeeping")) {
						label = "6";
					} else if (arr[0].equals("Media")) {
						label = "7";
					} else if (arr[0].equals("Photo")) {
						label = "8";
					} else {//if (arr[0].equals("Recommendation")) {
						label = "9";
					} 
					output_id.write(id + "\t" + label + "\t" + query + "\n");
					//System.out.println(query);
					//query = query.replace("key", "col_key");
					
					line = input.readLine();
				}
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
		if (method.toLowerCase().contains("aligon")) {
			ArrayList<TreeSet<ExtendedColumn>> queryProjectionSet = new ArrayList<>();
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
		} else if (method.toLowerCase().contains("makiyama")) {
			ArrayList<TreeMap<String, Integer>> queryMap = new ArrayList<>();
			for (int j = 0; j < statementList.size(); j++) {
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
			
		} else if (method.toLowerCase().contains("aouiche")) {
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

	public static ArrayList<Statement> convertToStatement(String data,String datapath,ArrayList<String> queryList) {
		ArrayList<Statement> statementList = new ArrayList<>();
		PrintWriter pw=null;

		
		for (String query : queryList) {
			InputStream stream = new ByteArrayInputStream(query.getBytes(StandardCharsets.UTF_8));
			CCJSqlParser parser = new CCJSqlParser(stream);
			Statement statement = null;
			try {
				statement = parser.Statement();
				statementList.add(statement);
			} catch (ParseException e) {			    
				if(pw==null){
					try {
						pw=new PrintWriter(new File(datapath+data+"_nonparsable.txt"));
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
				}
				pw.println(query);
			}catch(Error  e){
				if(pw==null){
					try {
						pw=new PrintWriter(new File(datapath+data+"_nonparsable.txt"));
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
				}
				pw.println(query);
				pw.close();
			}

			if(pw!=null)
				pw.close();
		}
		return statementList;
	}
	
	
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
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(fileName)));
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
	
	public static Statement removeString(Statement s) {
		if (s instanceof Select) {
			Select select = (Select)s;
			return removeString(select);
		}
		return s;
	}
	
	public static Select removeString(Select s) {
		SelectBody ps = s.getSelectBody();
		s.setSelectBody(removeString(ps));
		return s;
	}
	
	public static SelectBody removeString(SelectBody sb) {
		if (sb instanceof PlainSelect) {
			PlainSelect ps = (PlainSelect)sb;
			return removeString(ps);
		}
		return sb;
	}
	
	public static PlainSelect removeString(PlainSelect ps) {
		Distinct distinct = ps.getDistinct();
		FromItem fromItem = ps.getFromItem();
		List<Expression> groupByList = ps.getGroupByColumnReferences();
		Expression having = ps.getHaving();
		Table table = ps.getInto();
		List<Join> joinList = ps.getJoins();
		List<OrderByElement> orderByList = ps.getOrderByElements();
		List<SelectItem> selectItems = ps.getSelectItems();
		Expression whereExp = ps.getWhere();
		
		if (distinct != null) {
			ps.setDistinct(removeString(distinct));
		}
		if (fromItem != null) {
			ps.setFromItem(removeString(fromItem));
		}
		if (groupByList != null) {
			for (int i = 0; i < groupByList.size(); ++i) {
				groupByList.set(i, removeString(groupByList.get(i)));
			}
			ps.setGroupByColumnReferences(groupByList);
		}
		if (having != null) {
			ps.setHaving(removeString(having));
		}
		if (joinList != null) {
			for (int i = 0; i < joinList.size(); ++i) {
				joinList.set(i, removeString(joinList.get(i)));
			}
			ps.setJoins(joinList);
		}
		if (selectItems != null) {
			for (int i = 0; i < selectItems.size(); ++i) {
				selectItems.set(i, removeString(selectItems.get(i)));
			}
			ps.setSelectItems(selectItems);
		}
		if (whereExp != null) {
			ps.setWhere(removeString(whereExp));
		}
		
		return ps;
	}
	public static Distinct removeString(Distinct distinct) {
		if (distinct == null) {
			return null;
		}
		List<SelectExpressionItem> items = distinct.getOnSelectItems();
		if (items != null) {
			for (int i = 0; i < items.size(); ++i) {
				items.set(i, removeString(items.get(i)));
			}
		}
		return distinct;
	}
	public static SelectExpressionItem removeString(SelectExpressionItem item) {
		if (item == null) {
			return null;
		}
		Expression exp = item.getExpression();
		if (exp != null)
			item.setExpression(removeString(exp));
		return item;
	}
	
	public static Expression removeString(Expression exp) {
		if (exp == null) {
			return null;
		}
		if (exp instanceof StringValue) {
			StringValue val = (StringValue) exp;
			val.setValue("");
			return val;
		} else if (exp instanceof BinaryExpression) {
			BinaryExpression binaryExp = (BinaryExpression) exp;
			binaryExp.setLeftExpression(removeString(binaryExp.getLeftExpression()));
			binaryExp.setRightExpression(removeString(binaryExp.getRightExpression()));
			return binaryExp;
		} else if (exp instanceof AllComparisonExpression) {
			AllComparisonExpression allExp = (AllComparisonExpression)exp;
			AllComparisonExpression newExp = new AllComparisonExpression(removeString(allExp.getSubSelect()));
			return newExp;
		} else if (exp instanceof AnyComparisonExpression) {
			AnyComparisonExpression anyExp = (AnyComparisonExpression)exp;
			AnyComparisonExpression newExp = new AnyComparisonExpression(removeString(anyExp.getSubSelect()));
			return newExp;
		} else if (exp instanceof Between) {
			Between between = (Between) exp;
			between.setBetweenExpressionStart(removeString(between.getBetweenExpressionStart()));
			between.setBetweenExpressionEnd(removeString(between.getBetweenExpressionEnd()));
			between.setLeftExpression(removeString(between.getLeftExpression()));
			return between;
		} else if (exp instanceof CaseExpression) {
			CaseExpression caseExp = (CaseExpression) exp;
			caseExp.setElseExpression(removeString(caseExp.getElseExpression()));
			caseExp.setSwitchExpression(removeString(caseExp.getSwitchExpression()));
			List<Expression> whenClauses = caseExp.getWhenClauses();
			for (int i = 0; i < whenClauses.size(); ++i) {
				whenClauses.set(i, removeString(whenClauses.get(i)));
			}
			caseExp.setWhenClauses(whenClauses);
			return caseExp;
		} else if (exp instanceof ExistsExpression) {
			ExistsExpression existExp = (ExistsExpression)exp;
			existExp.setRightExpression(removeString(existExp.getRightExpression()));
			return existExp;
		} else if (exp instanceof Function) {
			Function f = (Function)exp;
			ExpressionList expList = f.getParameters();
			f.setParameters((ExpressionList)removeString(expList));
			return f;
		} else if (exp instanceof InExpression) {
			InExpression inExp = (InExpression)exp;
			inExp.setItemsList(removeString(inExp.getItemsList()));
			inExp.setLeftExpression(removeString(inExp.getLeftExpression()));
			return inExp;
		} else if (exp instanceof InverseExpression) {
			InverseExpression invExp = (InverseExpression) exp;
			invExp.setExpression(removeString(invExp.getExpression()));
			return invExp;
		} else if (exp instanceof IsNullExpression) {
			IsNullExpression isnullExp = (IsNullExpression)exp;
			isnullExp.setLeftExpression(removeString(isnullExp.getLeftExpression()));
			return isnullExp;
		} else if (exp instanceof Parenthesis) {
			Parenthesis parenthesis = (Parenthesis)exp;
			parenthesis.setExpression(removeString(parenthesis.getExpression()));
			return parenthesis;
		} else if (exp instanceof SubSelect) {
			SubSelect subSelect = (SubSelect)exp;
			subSelect.setSelectBody(removeString(subSelect.getSelectBody()));
			return subSelect;
		} else if (exp instanceof WhenClause) {
			WhenClause when = (WhenClause) exp;
			when.setThenExpression(removeString(when.getThenExpression()));
			when.setWhenExpression(removeString(when.getWhenExpression()));
			return when;
		} else if (exp instanceof Column) {
			Column col = (Column) exp;
			if ((col.getTable() != null) && (col.getTable().getAlias() == null) && (col.getTable().getName() == null) && (col.getTable().getSchemaName() == null)) {
				if ((col.getColumnName() != null) && (col.getColumnName().startsWith("\"")) && (col.getColumnName().endsWith("\""))) {
					return new StringValue("''");
				}
			}
			return col;
		}
		return exp;
	}
	
	public static SubSelect removeString(SubSelect subSelect) {
		if (subSelect == null) return null;
		subSelect.setSelectBody(removeString(subSelect.getSelectBody()));
		return subSelect;
	}
	
	public static ItemsList removeString(ItemsList itemList) {
		if (itemList == null) return null;
		if (itemList instanceof ExpressionList) {
			ExpressionList expList = (ExpressionList)itemList;
			List<Expression> list = expList.getExpressions();
			boolean allTheSame = true;
			for (int i = 0; i < list.size(); ++i) {
				if (!(list.get(i) instanceof StringValue)) {
					allTheSame = false;
				} 
				list.set(i, removeString(list.get(i)));
			}
			if (allTheSame == true) {
				list.clear();
				list.add(new StringValue("''"));
			}
			expList.setExpressions(list);
			return expList;
		} 
		return removeString((SubSelect)itemList);
	}
	public static FromItem removeString(FromItem from) {
		if (from == null) return null;
		if (from instanceof SubSelect) {
			SubSelect subSelect = (SubSelect)from;
			return removeString(subSelect);
		} else if (from instanceof SubJoin) {
			SubJoin subjoin = (SubJoin)from;
			return removeString(subjoin);
		}
		return from;
	}
	public static SubJoin removeString(SubJoin subJoin) {
		if (subJoin == null) return null;
		FromItem left = subJoin.getLeft();
		subJoin.setLeft(removeString(left));
		Join join = subJoin.getJoin();
		subJoin.setJoin(removeString(join));
		return subJoin;
	}
	public static Join removeString(Join join) {
		if (join == null) return null;
		Expression e = join.getOnExpression();
		join.setOnExpression(removeString(e));
		FromItem from = join.getRightItem();
		join.setRightItem(removeString(from));
		return join;
	}
	public static SelectItem removeString(SelectItem selectItem) {
		if (selectItem == null) return null;
		if (selectItem instanceof SelectExpressionItem) {
			SelectExpressionItem item = (SelectExpressionItem)selectItem;
			return removeString(item);
		}
		return selectItem;
	}
	
}
