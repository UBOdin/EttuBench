package toolsForMetrics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.jsqlparser.expression.BooleanValue;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.LeafValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.select.AllTableColumns;


//FINISHED
//Stores some Global information about tables and their schemas
public class Global {
	//public static Environment env = null;
	
	public static final double eps=0.000001;
	public static File dataDir = null;
	public static File swapDir = null;
	public static File permanentDir = null;
	public static List<File> sqlFiles = null;
    public static long MaxMem;
//    public static Statistics statistics = new Statistics();
	public  static HashMap<String, CreateTable> tables = new HashMap<String, CreateTable>();
	public  static HashMap<String, String> tableAlias = null;
	
//	public static ProgramMode programMode = ProgramMode.QUERY_MODE;

	public  static HashMap<String, ColDataType[]> tableColDataType = null;

	public static HashMap<String, Long> tableSize = new HashMap<String, Long>();

	public  static HashMap<String, HashMap<String, Integer>> tableSchemas = null;
	
	/**
	 * String is tableName
	 * List<Index> is primary keys
	 */
	public static HashMap<String, List<Index>> tableIndex = null;
	
//	public static HashSet<EnvironmentHandler> handlerRegistry = new HashSet<EnvironmentHandler>();

	public static HashMap<String, List<Integer>> tablePrimaryKeys = null;
	
	public static HashMap<String, HashMap<Integer, String>> tableForeignKeys = null;

	/**
	 * Function that kick out duplicate column in the list
	 * @param list
	 * @return
	 */
	 public static List<Column> unique(List<Column> list){
		 HashMap <String,Integer> map=new HashMap<String,Integer>();
		 String line;
		 List<Column> newlist=new ArrayList<Column>();
		 
		 Column c;
		 for (int i=0;i<list.size();i++){
			 c=list.get(i);
			 line="";
			 if (c.getTable()!=null&&c.getTable().getName()!=null)
			 line+=c.getTable().getName().toUpperCase()+".";
			 line+=c.getColumnName();
			 if (!map.containsKey(line)){
				 newlist.add(list.get(i));
				 map.put(line, 1);
			 }				 			 
		 }
		 list.clear();
		return newlist;
	 }
	 
//	 public static Environment getEnvironemt(EnvironmentConfig config) {
//		 if (env == null) {
//			 env = new Environment(permanentDir, config);
//		 }
//		 return env;
//	 }
//	 
//	 public static Environment getCurrentEnvironment() {
//		 return env;
//	 }
	 
//	 /**
//	  * Function that turn Token read from the disk into tuple
//	  * @param tokens
//	  * @param type
//	  * @return
//	  */
//	public static Tuple turnTokenIntoLeafValue(String[] tokens,List<Dtype> type){
//		int size=tokens.length;
//		Tuple tuple=new Tuple();
//		
//		for (int i=0;i<size;i++){
//		if (tokens[i].toUpperCase().equals("NULL")) {
//			tuple.addValue(new NullValue());
//		} else {
//			if (type.get(i)==Dtype.LongValue) {
//				tuple.addValue(new LongValue(tokens[i]));
//			} else if (type.get(i)==Dtype.DoubleValue) {
//				tuple.addValue(new DoubleValue(tokens[i]));
//			} else if (type.get(i)==Dtype.StringValue) {
//				tuple.addValue(new StringValue(tokens[i]));
//			} else if (type.get(i)==Dtype.DateValue) {
//				tuple.addValue(new DateValue(tokens[i]));
//			} else if (type.get(i)==Dtype.BooleanValue) {
//				if (tokens[i].equals(BooleanValue.TRUE.toString()))
//					tuple.addValue(BooleanValue.TRUE);
//				else if(tokens[i].equals(BooleanValue.FALSE.toString()))
//					tuple.addValue(BooleanValue.FALSE);
//				else
//					tuple.addValue(new NullValue());
//			} else if (type.get(i)==Dtype.TimestampValue) {
//				tuple.addValue(new TimestampValue(tokens[i]));
//			} else if (type.get(i)==Dtype.TimeValue)  {
//				tuple.addValue(new TimeValue(tokens[i]));
//			} else if (type.get(i)==Dtype.NullValue)
//				tuple.addValue(new NullValue());
//		}
//		}
//		return tuple;
//	}
//	/**
//	 * Function that gets the data type of each element of this tuple
//	 * @param tuple
//	 * @return
//	 */
//	public static List<Dtype> getTupleElementType(Tuple tuple){
//		List<Dtype> fulltype = null;
//		if (tuple!=null){
//	    	int size=tuple.getValues().size();
//			fulltype=new ArrayList<Dtype>(size);
//	    	LeafValue value;
//		for (int i = 0; i < size; i++){
//			value=tuple.getValues().get(i);
//			if (value instanceof LongValue)
//			fulltype.add(Dtype.LongValue);
//			else if (value instanceof StringValue)
//				fulltype.add(Dtype.StringValue);
//			else if (value instanceof DoubleValue)
//			   fulltype.add(Dtype.DoubleValue);
//			else if (value instanceof DateValue)
//				fulltype.add(Dtype.DateValue);
//			else if (value instanceof BooleanValue)
//				fulltype.add(Dtype.BooleanValue);
//			else if (value instanceof TimeValue)
//				fulltype.add(Dtype.TimeValue);
//			else if (value instanceof TimestampValue)
//				fulltype.add(Dtype.TimestampValue);
//			else if (value instanceof NullValue)
//				fulltype.add(Dtype.NullValue);
//			else System.out.println("Unknow type in typelist");
//		}    
//
//	    }
//		return fulltype;
//		
//	}
	/**
	 * Function that returns a HashMap with (Column in the Schema,Index of this Column in the Schema)
	 * @param schema
	 * @return
	 */
	public static HashMap<String, Integer> getSchemaMap(Schema schema) {
		// create a hashmap for knowning which columnname corresponding to which
		// array index in the tuple
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		String tableName;
		Column col;
		String fullName;

		for (int i = 0; i < schema.getValues().size(); i++) {
			col = schema.getValues().get(i);
			if (col.getTable().getName() != null) {
				tableName = col.getTable().getName();
				fullName = tableName + "." + col.getColumnName();
			} else {
				fullName = col.getColumnName();
			}
			map.put(fullName,i);
		}

		return map;
	}

	/**
	 * Function that finds out the index of column c in the schema
	 * @param schema
	 * @param c
	 * @return
	 */
	public static Integer findOutIndex(Schema schema,Column c){
		String fullName1;
		String fullName2;
		if(c.getTable()!=null&&c.getTable().getName()!=null)
			fullName1=c.getTable().getName()+"."+c.getColumnName();
		else
			fullName1=c.getColumnName();

		for (int i=0;i<schema.getValues().size();i++)
		{   
			Column cc = schema.getValues().get(i);
			if(cc.getTable()!=null&&cc.getTable().getName()!=null)
				fullName2=cc.getTable().getName()+"."+cc.getColumnName();
			else
				fullName2=cc.getColumnName();

			if (fullName1.equals(fullName2))
				return i;
		}
		return null;

	}

	/**
	 * Function that gets the File of this table by the table name
	 * @param tableName
	 * @return
	 */
	public static File getFileFromTableName(String tableName){
		return new File(Global.dataDir + "/" + tableName + ".dat");
	}

	/**
	 * tool method to find whether two columns match with each other
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static Boolean ColumnMatch(Column c1,Column c2){
		Boolean result=false;
		String fullName1="";
		String fullName2="";
		if (c1.getTable()!=null&&c1.getTable().getName()!=null){
			fullName1+=c1.getTable().getName();
		}
		if (c2.getTable()!=null&&c2.getTable().getName()!=null){
			fullName2+=c2.getTable().getName();
		}
		fullName1+=c1.getColumnName();
		fullName2+=c2.getColumnName();
		if (fullName1.equals(fullName2))
			result=true;
		return result;
	}

	/**
	 * Function that compares whether two leafValues are equal
	 * @param a
	 * @param b
	 * @return
	 */
	public static Boolean compareLeafValue(LeafValue a,LeafValue b){
		Boolean result=false;
		if (a.getClass().equals(b.getClass())){
			if (a instanceof DoubleValue){
				DoubleValue value1=(DoubleValue) a;
				DoubleValue value2=(DoubleValue) b;
				if (Math.abs(value1.getValue()-value2.getValue())<eps)
					return true;
			}
			else if (a instanceof LongValue){
				LongValue value1=(LongValue) a;
				LongValue value2=(LongValue) b;
				if (value1.getValue()==value2.getValue())
					return true;
			}
			else if (a instanceof StringValue){
				StringValue value1=(StringValue) a;
				StringValue value2=(StringValue) b;
				if (value1.getValue().equals(value2.getValue()))
					return true;
			}
			else if (a instanceof BooleanValue){
				BooleanValue value1=(BooleanValue) a;
				BooleanValue value2=(BooleanValue) b;
				if (value1.getValue()==value2.getValue())
					return true;
			}
			else if (a instanceof TimestampValue){
				TimestampValue value1=(TimestampValue) a;
				TimestampValue value2=(TimestampValue) b;
				if (value1.getValue().equals(value2.getValue()))
					return true;
			}
			else if (a instanceof DateValue){
				DateValue value1=(DateValue) a;
				DateValue value2=(DateValue) b;
				if (value1.getValue().equals(value2.getValue()))
					return true;
			}
			else if (a instanceof TimeValue){
				TimeValue value1=(TimeValue) a;
				TimeValue value2=(TimeValue) b;
				if (value1.getValue().equals(value2.getValue()))
					return true;
			}			
		}


		return result;
	}

	/**
	 * Function that get the Column's full name if current name is abbreviated or aliased
	 * @param cc
	 * @param tables
	 * @return
	 */
	public static Column getColumnFullName(Column cc,List<Table>tables){
		Column c=new Column();
		c.setTable(cc.getTable());
		
		c.setColumnName(cc.getColumnName());

		if (c.getTable()==null||c.getTable().getName()==null){				
			//find its global table name

			//first search from its	local table list
			int flip=0;
			for (int i=0;i<tables.size();i++){
				Table t=tables.get(i);
				CreateTable ct=Global.tables.get(t.getName());
				if (ct != null) {
					@SuppressWarnings("unchecked")
					List<ColumnDefinition> colDef=ct.getColumnDefinitions();
					for (int j=0;j<colDef.size();j++){
						if (colDef.get(j).getColumnName().equals(c.getColumnName())){
							flip=1;
							c.setTable(ct.getTable());
						}
					}
				} else {
					if (tables.size() == 1) {
						c.setTable(tables.get(0));
					}
				}
			}
			//if cannot find it at current table list,search globally
			if (flip==0){
				for(HashMap.Entry<String, CreateTable> entry : Global.tables.entrySet()){
					CreateTable ct=entry.getValue();
					@SuppressWarnings("unchecked")
					List<ColumnDefinition> colDef=ct.getColumnDefinitions();
					for (int j=0;j<colDef.size();j++){
						if (colDef.get(j).getColumnName().equals(c.getColumnName()))	
							c.setTable(ct.getTable());
					}
				}
			}
		}
		return c;
	}
	/**
	 * Function that finds out all columns of the corresponding table if we come across character '*'
	 * @param all
	 * @param global
	 * @return
	 */
	public static Schema findOutAllTableColumns(AllTableColumns all,Global global){
		Schema schema=new Schema();
		String tname=all.getTable().getName();
		CreateTable ct=Global.tables.get(tname);
		@SuppressWarnings("unchecked")
		List<ColumnDefinition> colDef=ct.getColumnDefinitions();
		for (int i=0;i<colDef.size();i++){
			Column c=new Column();
			c.setColumnName(colDef.get(i).getColumnName());
			c.setTable(ct.getTable());
		}
		return schema;
	}
	
	/**
	 * Function that finds out all columns of the corresponding table if we come across character '*'
	 * @param all
	 * @param global
	 * @return
	 */
	public static Schema findOutAllTableColumns(AllTableColumns all){
		Schema schema=new Schema();
		String tname=all.getTable().getName();
		CreateTable ct=Global.tables.get(tname);
		@SuppressWarnings("unchecked")
		List<ColumnDefinition> colDef=ct.getColumnDefinitions();
		for (int i=0;i<colDef.size();i++){
			Column c=new Column();
			c.setColumnName(colDef.get(i).getColumnName());
			c.setTable(ct.getTable());
		}
		return schema;
	}

//	public static List<ColumnType> getColumnTypes(List<Column> columns) {
//		
//		List<ColumnType> retVal = new ArrayList<ColumnType>();
//		int a;
//		for (int i = 0; i < columns.size(); i++) {
//			if ((a=findOutIfPrimaryKey(columns.get(i)))!=-1) {
//				if (a==1)
//				retVal.add(ColumnType.PrimaryKey);
//				else if (a==0)
//				retVal.add(ColumnType.ForeignKey);	
//			} else if (findOutIfForeignKey(columns.get(i))) {
//				retVal.add(ColumnType.ForeignKey);
//			} else {
//				retVal.add(ColumnType.Other);
//			}
//		}
//		
//		return retVal;
//	}

//	private static boolean findOutIfForeignKey(Column column) {
//		int index = findOutColumnIndexInTable(column);
//		
//		String dummyValue = tableForeignKeys.get(column.getTable().getName().toUpperCase()).get(index);
//		
//		if (dummyValue == null || dummyValue.equals("")) {
//			return false;
//		}
//		else {
//			return true;
//		}
//		
//	}

//	private static int findOutColumnIndexInTable(Column column) {
//		return tableSchemas.get(column.getTable().getName().toUpperCase()).get(column.getColumnName());
//	}

//	private static int findOutIfPrimaryKey(Column column) {
//		List<Index> listOfIndexes = tableIndex.get(column.getTable().getName());
//		
//		for (Index index : listOfIndexes) {
//			@SuppressWarnings("unchecked")
//			List<String> columnNames = index.getColumnsNames();
//			if (columnNames.contains(column.getColumnName())) {
//				int index1 = findOutColumnIndexInTable(column);
//				String dummyValue = tableForeignKeys.get(column.getTable().getName().toUpperCase()).get(index1);
//				if (dummyValue == null || dummyValue.equals("")) {
//					return 1;
//				}
//				else {
//					return 0;
//				}
//			}
//		}
//		
//		return -1;
//	}
	
	



}
