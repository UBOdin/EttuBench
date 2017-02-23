package toolsForMetrics;


import java.util.ArrayList;
import java.util.Iterator; 
import java.util.List;

import net.sf.jsqlparser.expression.Expression; 
import net.sf.jsqlparser.expression.ExpressionVisitor; 
import net.sf.jsqlparser.schema.Column; 
import net.sf.jsqlparser.schema.Table; 
import net.sf.jsqlparser.statement.select.AllColumns; 
import net.sf.jsqlparser.statement.select.AllTableColumns; 
import net.sf.jsqlparser.statement.select.FromItem; 
import net.sf.jsqlparser.statement.select.FromItemVisitor; 
import net.sf.jsqlparser.statement.select.Join; 
import net.sf.jsqlparser.statement.select.Limit; 
import net.sf.jsqlparser.statement.select.OrderByElement; 
import net.sf.jsqlparser.statement.select.OrderByVisitor; 
import net.sf.jsqlparser.statement.select.PlainSelect; 
import net.sf.jsqlparser.statement.select.SelectExpressionItem; 
import net.sf.jsqlparser.statement.select.SelectItem; 
import net.sf.jsqlparser.statement.select.SelectItemVisitor; 
import net.sf.jsqlparser.statement.select.SelectVisitor; 
import net.sf.jsqlparser.statement.select.SubJoin; 
import net.sf.jsqlparser.statement.select.SubSelect; 
import net.sf.jsqlparser.statement.select.Union;
import toolsForMetrics.ColumnExpressionVisitor;
import toolsForMetrics.Util; 

/**
 * A class to de-parse (that is, tranform from JSqlParser hierarchy into a string) a 
 * {@link net.sf.jsqlparser.statement.select.Select} 
 */ 
public class SubSelectVisitor implements SelectVisitor, OrderByVisitor, SelectItemVisitor, FromItemVisitor { 
	protected StringBuilder buffer; 
	protected ColumnExpressionVisitor expressionVisitor; 

	public SubSelectVisitor() { 
	} 

	/**
	 * @param expressionVisitor 
	 *            a {@link ExpressionVisitor} to de-parse expressions. It has to share the same<br> 
	 *            StringBuilder (buffer parameter) as this object in order to work 
	 * @param buffer 
	 *            the buffer that will be filled with the select 
	 */ 
	public SubSelectVisitor(ColumnExpressionVisitor expressionVisitor, StringBuilder buffer) { 
		if (buffer == null) {
			buffer = new StringBuilder();
		}
		
		this.buffer = buffer;
		this.expressionVisitor = expressionVisitor; 
	} 
	
	private List<Column> columns = new ArrayList<Column>(); 

	public List<Column> getColumns(){ 
		return columns;
	} 


	public void visit(PlainSelect plainSelect) { 
		
		List<Table> tables = new ArrayList<Table>();
		
		FromItem fromitem = plainSelect.getFromItem();
		
		if (fromitem != null) {
			@SuppressWarnings("unchecked")
			List<Join> joinlist = plainSelect.getJoins();

			if (joinlist == null || joinlist.isEmpty()) {
				//consumeFromItem(fromitem, tables, schemas);
				consumeFromItem(fromitem, tables);
			}
			// if multiple tables
			else {
				//consumeFromItem(fromitem, tables, schemas);
				consumeFromItem(fromitem, tables);
				for (int i = 0; i < joinlist.size(); i++) {
					consumeFromItem(joinlist.get(i).getRightItem(), tables);
					Expression sss = joinlist.get(i).getOnExpression();
					//System.out.println(sss);
					if (sss != null) {
						// pop out the top iter
						SelectItemListParser.correct(sss, tables);
						//breaking selection operators with AND
						List<Expression> selects = Util.processSelect(sss);

						for (int j = 0; j < selects.size(); j++) {
							Schema selectSchema = Util.processExpression(selects.get(j));
							for (int k = 0; k < selectSchema.getValues().size(); k++) {
								columns.add(selectSchema.getValues().get(k));
							}
						}
					}
				}
				//Collections.sort(this.scanList);
			}

		} else
			System.out.println("no from item found,please check");		

		List<SelectItem> distinctItems = null;
		
		if (plainSelect.getDistinct() != null) {
			@SuppressWarnings("unchecked")
			List<SelectItem> items=plainSelect.getDistinct().getOnSelectItems();
			distinctItems = items;
		}
		
		if (distinctItems != null) {
			
//			SelectItemListParser parserDistinct = new SelectItemListParser(distinctItems, tables);

			// check whether there is any function
			//int flip = 0;
			for (int i = 0; i < distinctItems.size(); i++) {
				SelectItem ss = distinctItems.get(i);
				if (ss instanceof SelectExpressionItem) {
					Expression sss = ((SelectExpressionItem) ss)
							.getExpression();
					Schema selectSchema = Util.processExpression(sss);
					for (int j = 0; j < selectSchema.getValues().size(); j++) {
						columns.add(selectSchema.getValues().get(j));
					}
				}
				else {
					System.out.println(ss);
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		List<SelectItem> selectItems = plainSelect.getSelectItems();
		
//		SelectItemListParser parserSelect = new SelectItemListParser(selectItems, tables);

		if (selectItems != null) {
			// check whether there is any function
			//int flip = 0;
			for (int i = 0; i < selectItems.size(); i++) {
				SelectItem ss = selectItems.get(i);
				if (ss instanceof SelectExpressionItem) {
					Expression sss = ((SelectExpressionItem) ss)
							.getExpression();
					Schema selectSchema = Util.processExpression(sss);
					for (int j = 0; j < selectSchema.getValues().size(); j++) {
						columns.add(selectSchema.getValues().get(j));
					}
				}
				else {
					System.out.println(ss);
				}
			}
		}

		// 2.check where condition and do selection
				Expression where = plainSelect.getWhere();
				if (where != null) {
					// pop out the top iter
					SelectItemListParser.correct(where, tables);
					//breaking selection operators with AND
					List<Expression> selects = Util.processSelect(where);

					for (int i = 0; i < selects.size(); i++) {
						Schema selectSchema = Util.processExpression(selects.get(i));
						for (int j = 0; j < selectSchema.getValues().size(); j++) {
							columns.add(selectSchema.getValues().get(j));
						}
					}
				}

				// 3. check group by and corresponding aggregation
				@SuppressWarnings("unchecked")
				List<Expression> groupbyRef = plainSelect.getGroupByColumnReferences();
				if (groupbyRef != null) {
					// pop out the top iter
					for (int i = 0; i < groupbyRef.size(); i++) {
						SelectItemListParser.correct(groupbyRef.get(i), tables);
						//breaking selection operators with AND
						List<Expression> columns = Util.processSelect(groupbyRef.get(i));
						for (int j = 0; j < columns.size(); j++) {
							Schema selectSchema = Util.processExpression(columns.get(j));
							for (int k = 0; k < selectSchema.getValues().size(); k++) {
								this.columns.add(selectSchema.getValues().get(k));
								
							}
						}
					}
				}
				
				// 4. check Having clause
				Expression having = plainSelect.getHaving();
				if (having != null) {
					// pop out the top iter
					SelectItemListParser.correct(having, tables);
					//breaking selection operators with AND
					List<Expression> selects = Util.processSelect(having);

					for (int i = 0; i < selects.size(); i++) {
						Schema selectSchema = Util.processExpression(selects.get(i));
						for (int j = 0; j < selectSchema.getValues().size(); j++) {
							columns.add(selectSchema.getValues().get(j));
						}
					}
				}

	} 

	public void visit(Union union) { 
		for (@SuppressWarnings("unchecked")
		Iterator<PlainSelect> iter = union.getPlainSelects().iterator(); iter.hasNext();) { 
			buffer.append("("); 
			PlainSelect plainSelect = iter.next(); 
			plainSelect.accept(this); 
			buffer.append(")"); 
			if (iter.hasNext()) { 
				buffer.append(" UNION "); 
				if (union.isAll()) { 
					buffer.append("ALL ");// should UNION be a BinaryExpression ? 
				} 
			} 

		} 

		if (union.getOrderByElements() != null) { 
			@SuppressWarnings("unchecked")
			List<OrderByElement> olist=union.getOrderByElements();
			deparseOrderBy(olist); 
		} 

		if (union.getLimit() != null) { 
			deparseLimit(union.getLimit()); 
		} 

	} 

	public void visit(OrderByElement orderBy) { 
		orderBy.getExpression().accept(expressionVisitor); 
		if (!orderBy.isAsc()) 
			buffer.append(" DESC"); 
	} 

	public void visit(Column column) { 
		buffer.append(column.getWholeColumnName()); 
	} 

	public void visit(AllColumns allColumns) { 
		buffer.append("*"); 
	} 

	public void visit(AllTableColumns allTableColumns) { 
		buffer.append(allTableColumns.getTable().getWholeTableName() + ".*"); 
	} 

	public void visit(SelectExpressionItem selectExpressionItem) { 
		selectExpressionItem.getExpression().accept(expressionVisitor); 
		if (selectExpressionItem.getAlias() != null) { 
			buffer.append(" AS " + selectExpressionItem.getAlias()); 
		} 

	} 

	public void visit(SubSelect subSelect) { 
		buffer.append("("); 
		subSelect.getSelectBody().accept(this); 
		buffer.append(")"); 
		String alias = subSelect.getAlias(); 
		if (alias != null) { 
			buffer.append(" AS ").append(alias); 
		} 
	} 

	public void visit(Table tableName) { 
		buffer.append(tableName.getWholeTableName()); 
		String alias = tableName.getAlias(); 
		if (alias != null && !alias.isEmpty()) { 
			buffer.append(" AS " + alias); 
		} 
	} 

	public void deparseOrderBy(List<OrderByElement> orderByElements) { 
		buffer.append(" ORDER BY "); 
		for (Iterator<OrderByElement> iter = orderByElements.iterator(); iter.hasNext();) { 
			OrderByElement orderByElement = iter.next(); 
			orderByElement.accept(this); 
			if (iter.hasNext()) { 
				buffer.append(", "); 
			} 
		} 
	} 

	public void deparseLimit(Limit limit) { 
		// LIMIT n OFFSET skip 
		if (limit.isRowCountJdbcParameter()) { 
			buffer.append(" LIMIT "); 
			buffer.append("?"); 
		} else if (limit.getRowCount() != 0) { 
			buffer.append(" LIMIT "); 
			buffer.append(limit.getRowCount()); 
		} 

		if (limit.isOffsetJdbcParameter()) { 
			buffer.append(" OFFSET ?"); 
		} else if (limit.getOffset() != 0) { 
			buffer.append(" OFFSET " + limit.getOffset()); 
		} 

	} 

	public StringBuilder getBuffer() { 
		return buffer; 
	} 

	public void setBuffer(StringBuilder buffer) { 
		this.buffer = buffer; 
	} 

	public ExpressionVisitor getExpressionVisitor() { 
		return expressionVisitor; 
	} 

	public void setExpressionVisitor(ColumnExpressionVisitor visitor) { 
		expressionVisitor = visitor; 
	} 

	public void visit(SubJoin subjoin) { 
		buffer.append("("); 
		subjoin.getLeft().accept(this); 
		deparseJoin(subjoin.getJoin()); 
		buffer.append(")"); 
	} 

	public void deparseJoin(Join join) { 
		if (join.isSimple()) 
			buffer.append(", "); 
		else { 

			if (join.isRight()) 
				buffer.append(" RIGHT"); 
			else if (join.isNatural()) 
				buffer.append(" NATURAL"); 
			else if (join.isFull()) 
				buffer.append(" FULL"); 
			else if (join.isLeft()) 
				buffer.append(" LEFT"); 

			if (join.isOuter()) 
				buffer.append(" OUTER"); 
			else if (join.isInner()) 
				buffer.append(" INNER"); 

			buffer.append(" JOIN "); 

		} 

		FromItem fromItem = join.getRightItem(); 
		fromItem.accept(this); 
		if (join.getOnExpression() != null) { 
			buffer.append(" ON "); 
			join.getOnExpression().accept(expressionVisitor); 
		} 
		if (join.getUsingColumns() != null) { 
			buffer.append(" USING ("); 
			for (@SuppressWarnings("unchecked")
			Iterator<Column> iterator = join.getUsingColumns().iterator(); iterator.hasNext();) { 
				Column column = iterator.next(); 
				buffer.append(column.getWholeColumnName()); 
				if (iterator.hasNext()) { 
					buffer.append(", "); 
				} 
			} 
			buffer.append(")"); 
		} 

	} 
	
	private void consumeFromItem(FromItem fromitem, List<Table> tables) {
		if (fromitem instanceof Table) {
			Table t = (Table) fromitem;
			// uppercase it
			t.setName(t.getName().toUpperCase());
			//System.out.println(t.getName());
			//t.setAlias(null);
			tables.add(t);
			
			
			// if there is any alias of this table,create a key to map to original table
			//if (t.getAlias() != null)
			//	Global.tableAlias.put(t.getAlias(), t.getName());
		} else if (fromitem instanceof SubSelect){
			SubSelect temp = (SubSelect) fromitem;
			if (temp.getSelectBody() instanceof PlainSelect) {
				((PlainSelect)temp.getSelectBody()).accept(this);
			} else {
				Union union = (Union)temp.getSelectBody();
				@SuppressWarnings("unchecked")
				List<PlainSelect> select_list = union.getPlainSelects();
				for (PlainSelect select : select_list) {
					select.accept(this);
				}
			}
		}
	}

}