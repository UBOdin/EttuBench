package featureEngineering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.HashMultiset;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;


/**
 * this class resolves all naming problems in the query including aliases removal
 * currently it does not ensure every column has its table name because this functionality
 * requires schema info, but we assume no schema info here for simplicity
 * @author tingxie
 *
 */
public class PlainSelectNamingResolver{

	private SelectNamingResolver ancester;
	//record alias mapping
	public HashMap<String,Object> aliasRegister;
	private PlainSelect ps;
	private PlainSelect transformedPlainSelect;
	private List<PlainSelectNamingResolver> childList;

	public PlainSelectNamingResolver(SelectNamingResolver ancester,PlainSelect input,HashMap<String,Object> register){
		this.ancester=ancester;
		this.aliasRegister=register;		
		this.aliasRegister.putAll(this.ancester.commonregister);
		this.childList=new ArrayList<PlainSelectNamingResolver>();
		this.ps=input;
	}

	public Object searchAliasComplete(String alias,Class<Expression>[] expectedTypes){
		Object obj=this.getAliasedObject(alias, expectedTypes);
		if(obj!=null)
			return obj;
		//search its children
		else{
			for (PlainSelectNamingResolver child:childList){
				obj=child.searchAliasComplete(alias, expectedTypes);
				if(obj!=null)
					return obj;
			}		 
		}
		return null;
	}

	//	/**
	//	 * check if two expressions are across selects
	//	 * @return
	//	 */
	//	public boolean ifAcrossSelect(Expression left,Expression right){
	//		PlainSelectNamingResolver lmappedobj=searchThroughExpressionAndFindMapping(left);
	//		PlainSelectNamingResolver rmappedobj=searchThroughExpressionAndFindMapping(right);
	//		//if one mapped but one does not, then they are across select
	//		if (lmappedobj==null&&rmappedobj==null)
	//			return false;
	//		else if (lmappedobj==null)
	//			return true;
	//		else if (rmappedobj==null)
	//			return true;
	//		else if (lmappedobj!=rmappedobj)
	//			return true;
	//		else
	//			return false;
	//	}
	//
	//	public PlainSelectNamingResolver searchThroughExpressionAndFindMapping(Expression exp){
	//		PlainSelectNamingResolver obj=this.parent.objectBelongMap.get(exp);
	//		if(obj!=null)
	//			return obj;
	//		//search all its child realms
	//		else {
	//			for (PlainSelectNamingResolver en: this.childmap){
	//				obj=en.searchThroughExpressionAndFindMapping(exp);
	//				if(obj!=null)
	//					return obj;
	//			}	
	//		}
	//
	//		if (exp instanceof BinaryExpression){
	//			BinaryExpression bexp=(BinaryExpression) exp;		  
	//			Expression left=bexp.getLeftExpression();
	//			Expression right=bexp.getRightExpression();
	//			obj=searchThroughExpressionAndFindMapping(left);
	//			if(obj!=null)
	//				return obj;
	//
	//			obj=searchThroughExpressionAndFindMapping(right);
	//			if(obj!=null)
	//				return obj;
	//		}
	//		else if (exp instanceof Function){
	//			Function f=(Function) exp;
	//			ExpressionList explist=f.getParameters();
	//			if (explist!=null){
	//				List<Expression> elist= f.getParameters().getExpressions();	
	//				for (Expression e:elist){
	//					obj=searchThroughExpressionAndFindMapping(e);
	//					if(obj!=null)
	//						return obj;
	//				}
	//			}
	//		}
	//		else if (exp instanceof Parenthesis){
	//			Parenthesis p=(Parenthesis) exp;
	//			obj=searchThroughExpressionAndFindMapping(p.getExpression());
	//			if(obj!=null)
	//				return obj;
	//		}
	//		else if (exp instanceof InverseExpression){
	//			InverseExpression p=(InverseExpression) exp;
	//			obj=searchThroughExpressionAndFindMapping(p.getExpression());
	//			if(obj!=null)
	//				return obj;
	//		}
	//
	//		//for all other cases, just return null		
	//		return null;
	//	}

	/**
	 * solve the problem of reference chain of aliases
	 * major function that check alias
	 * @param alias
	 * @param register
	 * @return
	 */
	private Object getAliasedObject(String alias,Class<Expression>[] expectedTypes){
		Object obj=this.getObjectDeepest(alias,expectedTypes);		
		if(obj!=null)
			return obj;
		else
			return null;
	}

	private Object getObjectNext(String alias,Class<Expression>[] expectedTypes){
		Object obj=this.aliasRegister.get(alias);

		if(obj!=null){
			boolean pass=false;
			for(Class<Expression> c:expectedTypes){				
				if(c.isInstance(obj)){
					pass=true;
					break;
				}
			}
			if(pass)
				return obj;
		}

		//continue searching	
		Object previousObj=null;       

		while (obj!=null&&obj!=previousObj){
			previousObj=obj;
			String key=obj.toString();
			obj=this.aliasRegister.get(key);
			//if obj is null, then we have exhausted our candidates and still no found
			//just return the alias
			if(obj!=null){
				//return the first one that matches the expected types
				boolean pass=false;
				for(Class<Expression> c:expectedTypes){				
					if(c.isInstance(obj)){
						pass=true;
						break;
					}
				}
				if(pass)
					return obj;
			}
		}		
		return null;
	}

	private Object getObjectDeepest(String alias,Class<Expression>[] expectedTypes){
		Object next=this.getObjectNext(alias, expectedTypes);
		Object result=null;
		while (next!=null){
			result=next;
			next=this.getObjectNext(next.toString(), expectedTypes);
			if(next!=null&&next.toString().equals(result.toString()))
				break;
		}
		return result;
	}


	/**
	 * when deal with each plain select it should have it own register
	 * representing it is an independent region that will not affect any others
	 * @param ps
	 * @param addOnRegister
	 */
	public PlainSelect aliasReplacePlainSelect(){

		if(this.transformedPlainSelect==null){
			PlainSelect ps=QueryToolBox.copyPlainSelect(this.ps);
			//remove subjoins
			QueryToolBox.removeSubJoinForPlainSelect(ps);
			//first step, scan through all of its elements remove and and register all possible alias
			scanThroughPlainSelect(ps);
			//alias replace sub-queries first
			aliasReplaiceMySubs(ps);
			//next step, try to replace all terms with its actual content
			replaceAliasInPlainSelect(ps);
			this.transformedPlainSelect=ps;
		}		
		return this.transformedPlainSelect;
	}

	private void aliasReplaiceMySubs(PlainSelect ps){
		//critical must analyze from items first
		//scan through FromItem
		FromItem from=ps.getFromItem();
		if(from instanceof SubSelect){
			SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(((SubSelect) from).getSelectBody(),this.ancester,this.aliasRegister);
			SubSelect newsub=new SubSelect();
			SelectBody newbody=resolver.aliasReplaceSelectBody();
			this.childList.addAll(resolver.getChildPlainSelectResolverList());
			newsub.setSelectBody(newbody);
			ps.setFromItem(newsub);
		}

		@SuppressWarnings("unchecked")
		List<Join> joins=ps.getJoins();
		if (joins!=null){
			//scan through joins for FromItem first
			List<Join> newjoins=new ArrayList<Join>();
			for (Join j: joins){
				Join newj=QueryToolBox.copyJoin(j);
				from=j.getRightItem();
				if(from instanceof SubSelect){
					SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(((SubSelect) from).getSelectBody(),this.ancester,this.aliasRegister);
					SubSelect newsub=new SubSelect();
					SelectBody newbody=resolver.aliasReplaceSelectBody();
					this.childList.addAll(resolver.getChildPlainSelectResolverList());
					newsub.setSelectBody(newbody);
					newj.setRightItem(newsub);
				}
				newjoins.add(newj);
			}	

			List<Join> newjoins1=new ArrayList<Join>();
			//scan through joins for join conditions
			for (Join j: newjoins){
				Join newj=QueryToolBox.copyJoin(j);
				//scan through join condition
				Expression onExp=j.getOnExpression();
				newj.setOnExpression(traverseExpressionAndAliasReplaceSubSelect(onExp));;				
				newjoins1.add(newj);
			}				
			ps.setJoins(newjoins1);			
		}		


		//scan through SelectItems
		@SuppressWarnings("unchecked")
		List<SelectItem> slist=ps.getSelectItems();
		for (SelectItem sitem:slist){
			if (sitem instanceof SelectExpressionItem){
				SelectExpressionItem seitem=(SelectExpressionItem) sitem;
				Expression e=seitem.getExpression();
				traverseExpressionAndAliasReplaceSubSelect(e);
			}
		}

		//scan through where
		Expression where=ps.getWhere();
		if(where!=null){
			traverseExpressionAndAliasReplaceSubSelect(where);
		}
		//scan through having
		Expression having=ps.getHaving();
		if(having!=null){
			traverseExpressionAndAliasReplaceSubSelect(having);

		}		
	}


	/**
	 * traverse an expression and DNF normalize any potential sub-select
	 * @param exp
	 */
	private Expression traverseExpressionAndAliasReplaceSubSelect(Expression exp){
		if (exp instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) exp;
			bexp.setLeftExpression(traverseExpressionAndAliasReplaceSubSelect(bexp.getLeftExpression()));
			bexp.setRightExpression(traverseExpressionAndAliasReplaceSubSelect(bexp.getRightExpression()));
			return bexp;
		}
		else if (exp instanceof IsNullExpression){
			IsNullExpression isnull=(IsNullExpression) exp;
			Expression left=isnull.getLeftExpression();
			isnull.setLeftExpression(traverseExpressionAndAliasReplaceSubSelect(left));
			return isnull;
		}
		else if (exp instanceof ExistsExpression){
			ExistsExpression exists=(ExistsExpression) exp;
			SubSelect sub=(SubSelect) exists.getRightExpression();
			SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(sub.getSelectBody(),this.ancester,this.aliasRegister);
			SelectBody newbody=resolver.aliasReplaceSelectBody();
			sub.setSelectBody(newbody);
			return exists;			
		}
		else if (exp instanceof InExpression){
			InExpression inexp=(InExpression) exp;
			inexp.setLeftExpression(traverseExpressionAndAliasReplaceSubSelect(inexp.getLeftExpression()));
			ItemsList ilist=inexp.getItemsList();
			if(ilist instanceof SubSelect){
				SubSelect sub=(SubSelect) ilist;
				SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(sub.getSelectBody(),this.ancester,this.aliasRegister);
				SelectBody newbody=resolver.aliasReplaceSelectBody();
				sub.setSelectBody(newbody);
			}
			return inexp;
		}
		else if (exp instanceof SubSelect){			
			SubSelect sub=(SubSelect) exp;
			SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(sub.getSelectBody(),this.ancester,this.aliasRegister);
			SelectBody newbody=resolver.aliasReplaceSelectBody();
			sub.setSelectBody(newbody);
			return sub;
		}
		else if (exp instanceof AllComparisonExpression){
			AllComparisonExpression all=(AllComparisonExpression) exp;
			SubSelect sub=all.getSubSelect();
			SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(sub.getSelectBody(),this.ancester,this.aliasRegister);
			SelectBody newbody=resolver.aliasReplaceSelectBody();
			sub.setSelectBody(newbody);
			return all;
		}
		else if (exp instanceof AnyComparisonExpression){
			AnyComparisonExpression any=(AnyComparisonExpression) exp;
			SubSelect sub=any.getSubSelect();
			SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(sub.getSelectBody(),this.ancester,this.aliasRegister);
			SelectBody newbody=resolver.aliasReplaceSelectBody();
			sub.setSelectBody(newbody);
			return any;
		}
		else if (exp instanceof Parenthesis)	{
			return traverseExpressionAndAliasReplaceSubSelect(((Parenthesis) exp).getExpression());
		}
		else if (exp instanceof InverseExpression){
			return traverseExpressionAndAliasReplaceSubSelect(((InverseExpression) exp).getExpression());
		}
		else return exp;

	}

	private void registerAlias(String alias,Object target){
		Object prev_target=this.aliasRegister.get(alias);
		if(target instanceof Table||target instanceof SubSelect)
			this.aliasRegister.put(alias, target);
		else if(prev_target!=null){			
			if(!(prev_target instanceof Table)&&!(prev_target instanceof SubSelect)){
				this.aliasRegister.put(alias, target);
			}
		}
		else {
			this.aliasRegister.put(alias, target);
		}
	}

	private void scanThroughPlainSelect(PlainSelect ps){
		//alias can only appear in select items and from items

		//scan through SelectItems
		@SuppressWarnings("unchecked")
		List<SelectItem> slist=ps.getSelectItems();
		for (SelectItem sitem:slist){
			if (sitem instanceof SelectExpressionItem){
				SelectExpressionItem seitem=(SelectExpressionItem) sitem;
				String alias=seitem.getAlias();
				Expression e=seitem.getExpression();
				if (alias!=null){
					//register it
					if(e instanceof SubSelect){
						SubSelect sub=(SubSelect) e;
						//register its select body
						registerAlias(alias, sub.getSelectBody());
						//remove it
						sub.setAlias(null);
					}
					else
						registerAlias(alias, e);
					//remove alias
					seitem.setAlias(null);
				}
			}
		}

		//scan through FromItem
		FromItem from=ps.getFromItem();
		scanThroughFromItem(from);
		//scan through joins
		@SuppressWarnings("unchecked")
		List<Join> joins=ps.getJoins();
		if (joins!=null){
			for (Join j: joins){
				from=j.getRightItem();
				scanThroughFromItem(from);				
			}
		}
	}

	private void scanThroughFromItem(FromItem from){
		HashMap<String,Object> register=new HashMap<String,Object>();
		if (from instanceof Table){
			String alias=from.getAlias();
			if (alias!=null){
				//register it
				registerAlias(alias, from);
				//remove it
				from.setAlias(null);
			}
		}
		else if (from instanceof SubSelect){
			SubSelect sub=(SubSelect) from;
			//we do not care aliases in sub-select, just the alias of the sub-select itself
			String alias=sub.getAlias();
			if (alias!=null){
				//register its select body
				registerAlias(alias, sub.getSelectBody());
				//remove it
				sub.setAlias(null);
			}			
		}
		else {
			SubJoin subj=(SubJoin) from;
			String alias=subj.getAlias();
			if (alias!=null){
				//register it
				register.put(alias, subj);
				//remove it
				subj.setAlias(null);
			}
			FromItem leftfrom=subj.getLeft();
			scanThroughFromItem(leftfrom);
			Join j=subj.getJoin();
			FromItem rightfrom=j.getRightItem();			
			scanThroughFromItem(rightfrom);
			//on Expression should not contain aliases
		}
	}

	/**
	 * major function that replaces all aliased terms in plainselect
	 * @param ps
	 * @param register
	 */
	private void replaceAliasInPlainSelect(PlainSelect ps){
		//go through selectitems
		@SuppressWarnings("unchecked")
		List<SelectItem> slist=ps.getSelectItems();
		Iterator<SelectItem> it=slist.iterator();
		while (it.hasNext()){
			SelectItem sitem=it.next();
			if (sitem instanceof AllTableColumns){
				AllTableColumns all=(AllTableColumns) sitem;
				Table t=all.getTable();
				//expecting tables
				Object obj=getAliasedObject(t.toString(),QueryToolBox.TableTypes);
				if (obj!=null&&obj instanceof Table){
					t.setName(obj.toString());
				}
			}
			else if (sitem instanceof SelectExpressionItem){
				SelectExpressionItem seitem=(SelectExpressionItem) sitem;
				seitem.setExpression(replaceAliasedInExpression(seitem.getExpression()));			
			}
		}

		//go through group by
		@SuppressWarnings("unchecked")
		List<Expression> groupbylist=ps.getGroupByColumnReferences();
		if(groupbylist!=null){
			List<Expression> newgroupbylist=new ArrayList<Expression> ();
			for (Expression exp: groupbylist){
				newgroupbylist.add(replaceAliasedInExpression(exp));
			}
			ps.setGroupByColumnReferences(newgroupbylist);
		}

		//go through order by columns
		@SuppressWarnings("unchecked")
		List<OrderByElement> orderby=ps.getOrderByElements();
		if (orderby!=null){
			for (OrderByElement ele: orderby){
				ele.setExpression(replaceAliasedInExpression(ele.getExpression()));        		 
			}
		}

		//go through from item
		ps.setFromItem(replaceAliasedInFromItem(ps.getFromItem()));

		//go through joins
		@SuppressWarnings("unchecked")
		List<Join> joins =ps.getJoins();
		if (joins!=null){
			for (Join j: joins){
				j.setRightItem(replaceAliasedInFromItem(j.getRightItem()));
				j.setOnExpression(replaceAliasedInExpression(j.getOnExpression()));				
			}
		}
		//go through having
		ps.setHaving(replaceAliasedInExpression(ps.getHaving()));

		//go through where clause
		ps.setWhere(replaceAliasedInExpression(ps.getWhere()));
	}


	private Expression replaceAliasedInExpression(Expression e){
		if (e instanceof Function){
			Function f=(Function) e;
			ExpressionList elist=f.getParameters();	
			if(elist!=null){
				List<Expression> newlist=new ArrayList<Expression>();
				@SuppressWarnings("unchecked")
				List<Expression> list=elist.getExpressions();
				for (Expression exp: list){
					Expression newexp=this.replaceAliasedInExpression(exp);
					if(!newexp.equals(f))
						newlist.add(newexp);
					else
						newlist.add(exp);
				}
				elist.setExpressions(newlist);				
			}
			return f;
		}
		else if (e instanceof Column){
			Column c=(Column) e;
			String key=c.toString();
			//expecting column or constants			
			Object obj=this.searchAliasComplete(key,QueryToolBox.ColTypes);
			if (obj!=null){
				Expression exp=(Expression) obj;
				if(!c.toString().equals(exp.toString())){
					if(exp instanceof Function){
						Function f=(Function) exp;
						ExpressionList elist=f.getParameters();
						boolean pass=true;
						if(elist!=null){
							@SuppressWarnings("unchecked")
							List<Expression> explist=elist.getExpressions();
							for (Expression para:explist){
								if(para.toString().equals(key))
									pass=false;
							}
						}
						if(pass)
							return this.replaceAliasedInExpression(exp);
						else 
							return f;
					}
					else
						return this.replaceAliasedInExpression(exp);
				}
				else {
					registerColumn(c);
					return c;
				}
			}
			//if partly aliased or no alias
			else{ 
				Table t=c.getTable();
				//if the column has a table name
				if (t!=null&&t.getName()!=null){
					//expecting tables
					obj=getAliasedObject(t.getName(),QueryToolBox.TableTypes);
					//if table name aliased
					if(obj!=null){
						if (obj instanceof Table){
							c.setTable((Table) obj);
							//try to see if the column name is an alias									
							Object o=this.getAliasedObject(c.getColumnName(),QueryToolBox.ColTypes);
							if(o!=null&&!o.toString().contains(((Table) obj).getName())&&!o.toString().contains(".")){
								c.setColumnName(o.toString());
							}
							registerColumn(c);
							return c;									
						}
						//if the alias is pointing to a selectbody
						else if (obj instanceof SelectBody){			
							SelectBody body=(SelectBody) obj;
							//get the NamingResolver for this sub-select
							SelectBodyNamingResolver resolver=this.ancester.selectBodyBelongMap.get(body);
							//if no resolver found, then it means that it is pointing to another sibling selectbody under its parent
							//and its parent has only scanned its alias without doing any alias replace to that silbing
							//just create a resolver
							if(resolver==null){  
								resolver=new SelectBodyNamingResolver(body,this.ancester,this.aliasRegister);
								body=resolver.aliasReplaceSelectBody();
							}
							List<PlainSelectNamingResolver> rlist=resolver.getChildPlainSelectResolverList();
							for (PlainSelectNamingResolver res: rlist){									
								//try to see if the column name is an alias									
								Object o=res.getAliasedObject(c.getColumnName(),QueryToolBox.ColTypes);
								if(o!=null&&o!=c){
									Expression exp=(Expression) o;
									if(exp instanceof Column){
										registerColumn((Column) exp);
									}
									return exp;
								}							
								//then try to see if there is any column in the mapped select body matches this column name	
								PlainSelect ps=res.transformedPlainSelect;
								//if the selectbody that this table refers to is its parent
								//then it has not done alias replace yet
								if(ps==null)
									ps=res.ps;

								Expression bestmatch=findBestMatchedSelectItemFromPlainSelect(ps,c);

								//if a match is found, then just use the found name				
								if(bestmatch!=null){
									Expression result=bestmatch;
									//see if this expression is a column
									if(result instanceof Column){
										Column col=(Column) result;
										//try to see if it is directly aliased
										Object colobject=res.getAliasedObject(col.toString(),QueryToolBox.ColTypes);
										if(colobject!=null){
											Expression exp=(Expression) colobject;
											if(exp instanceof Column)
												registerColumn((Column) exp);
											return exp;
										}

										//take a look at its table name
										Table table=col.getTable();
										//if it has table name
										if(table!=null&&table.getName()!=null){
											Object tableobject=res.getAliasedObject(col.getTable().getName(),QueryToolBox.TableTypes);
											//if table is aliased
											if(tableobject!=null){
												if(tableobject instanceof Table){
													col.setTable((Table) tableobject);
												}
												//then it must be a selectbody
												else{
													SelectBody mybody=(SelectBody) tableobject;
													//alias normalize the selectbody first
													boolean selfloop=mybody.toString().contains(col.getTable().getName());
													if(!selfloop){
													SelectBodyNamingResolver mybodyres=new SelectBodyNamingResolver(mybody,this.ancester,this.aliasRegister);
													mybody=mybodyres.aliasReplaceSelectBody();
												    }
												    	
													//find the best match
													Expression mybestmatch=findBestMatchedSelectItemFromSelectBody(mybody,col);
													if(mybestmatch!=null){													
														if(mybestmatch instanceof Column){
															if(selfloop)
															((Column) mybestmatch).setTable(new Table());
															else
															registerColumn((Column) mybestmatch);
														}
														return mybestmatch;
													}
													else{
														//if search fails, give its table name a canonical name
														Table cname=this.ancester.CanonicalNames.get(mybody);
														if(cname==null){
															col.setTable(this.createCanonicalNameForSelectBody(mybody,false));
														}
														else
															col.setTable(cname);
														//since its column name is not aliased, just keep it intact
														registerColumn(col);
														return col;
													}													
												}
											}
											//if table is not aliased,check its column name is aliased or not
											else {
												Object colobj=res.getAliasedObject(col.getColumnName(),QueryToolBox.ColTypes);
												if(colobj!=null&&!colobj.toString().contains(table.getName())&&!colobj.toString().contains("."))
													col.setColumnName(colobj.toString());
												registerColumn(col);
												return col;
											}
										}
										else{
											registerColumn(col);
											return col;	
										}
									}
									else
										return result; 
								}
								//if no match found, may be the sub-select is of the form Select *
								else{
									//if search fails, give its table name a canonical name
									Table cname=this.ancester.CanonicalNames.get(body);
									if(cname==null){
										c.setTable(this.createCanonicalNameForSelectBody(body,false));
									}
									else
										c.setTable(cname);
									//since its column name is not aliased, just keep it intact
									registerColumn(c);
									return c;
								}
							}							
                            registerColumn(c);
							return c;							
						}
						else{
							System.out.println("error, column table name "+t.getName()+" alias pointing to unexpected target "+obj.getClass()+obj);
							return null;
						}
					}
					//if table name is not aliased try to resolve its column name alone
					else {
						Object o=this.getAliasedObject(c.getColumnName(),QueryToolBox.ColTypes);
						if(o!=null&&!o.toString().contains(t.getName())&&!o.toString().contains(".")){							
							c.setColumnName(o.toString());
						}
						registerColumn(c);
						return c;
					}
				}
				//if this column does not have a table name, then we are sure it has no no alias
				else {
					registerColumn(c);
					return c;
				}
			}
		}	
		else if (e instanceof CaseExpression){
			CaseExpression caseexp=(CaseExpression) e;
			Expression sw=caseexp.getSwitchExpression();
			if (sw!=null){
				caseexp.setSwitchExpression(replaceAliasedInExpression(sw));
			}		

			@SuppressWarnings("unchecked")
			List<WhenClause> wclist=caseexp.getWhenClauses();			
			if (wclist!= null){
				for (WhenClause wc:wclist){
					wc.setWhenExpression(replaceAliasedInExpression(wc.getWhenExpression()));
					wc.setThenExpression(replaceAliasedInExpression(wc.getThenExpression()));
				}
			}
			Expression elseexp=caseexp.getElseExpression();
			if (elseexp!=null){
				caseexp.setElseExpression(replaceAliasedInExpression(caseexp.getElseExpression()));
			}
			return caseexp;
		}
		else if (e instanceof Parenthesis){
			Parenthesis p=(Parenthesis) e;
			p.setExpression(replaceAliasedInExpression(p.getExpression()));
			return p;
		}
		else if (e instanceof BinaryExpression){
			BinaryExpression bexp=(BinaryExpression) e;
			bexp.setLeftExpression(replaceAliasedInExpression(bexp.getLeftExpression()));
			bexp.setRightExpression(replaceAliasedInExpression(bexp.getRightExpression()));
			return bexp;
		}
		else if (e instanceof InverseExpression){
			InverseExpression p=(InverseExpression) e;
			p.setExpression(replaceAliasedInExpression(p.getExpression()));
			return p;
		}
		else if (e instanceof InExpression){
			InExpression inexp=(InExpression) e;
			inexp.setLeftExpression(replaceAliasedInExpression(inexp.getLeftExpression()));
			return inexp;
		}
		else if (e instanceof IsNullExpression){
			IsNullExpression isnull=(IsNullExpression) e;
			isnull.setLeftExpression(replaceAliasedInExpression(isnull.getLeftExpression()));
			return isnull;
		}
		else if (e instanceof Between){
			Between between=(Between) e;
			between.setLeftExpression(replaceAliasedInExpression(between.getLeftExpression()));
			between.setBetweenExpressionStart(replaceAliasedInExpression(between.getBetweenExpressionStart()));
			between.setBetweenExpressionEnd(replaceAliasedInExpression(between.getBetweenExpressionEnd()));
			return between;
		}
		else return e;
	}



	private Expression findBestMatchedSelectItemFromSelectBody(SelectBody body,Column c){
		if(body instanceof PlainSelect)
			return findBestMatchedSelectItemFromPlainSelect((PlainSelect) body,c);
		else {
			List<Expression> resultlist=new ArrayList<Expression>();
			Union u=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=u.getPlainSelects();
			for (PlainSelect ps:plist){
				Expression tempresult=findBestMatchedSelectItemFromPlainSelect(ps,c);
				if(tempresult!=null)
					resultlist.add(tempresult);   			
			}
			int minlengthDiff=Integer.MAX_VALUE;
			Expression bestResult=null;
			for (Expression exp:resultlist){
				int lengthDiff=Math.abs(exp.toString().length()-c.toString().length());
				if(lengthDiff<minlengthDiff){
					minlengthDiff=lengthDiff;
					bestResult=exp;
				}
			}
			return bestResult;
		}
	}

	private Expression findBestMatchedSelectItemFromPlainSelect(PlainSelect ps,Column c){
		@SuppressWarnings("unchecked")
		List<SelectItem> sitemlist=ps.getSelectItems();
		SelectExpressionItem bestmatch=null;
		for(SelectItem sitem:sitemlist){
			if(sitem.toString().equals(c.getColumnName())){
				bestmatch=(SelectExpressionItem)sitem;
				break;
			}
			else if (sitem instanceof SelectExpressionItem){
				SelectExpressionItem seitem=(SelectExpressionItem) sitem;
				Expression exp=seitem.getExpression();
				if(exp instanceof Column){
					Column cc=(Column) exp;
					if(cc.getColumnName().equals(c.getColumnName())){
						bestmatch=(SelectExpressionItem)sitem;
						break;
					}
				}									
			}
		}
		if(bestmatch!=null)
			return bestmatch.getExpression();
		else 
			return null;
	}

	private Table createCanonicalNameForSelectBody(SelectBody body,boolean skipNamingResolve){		
		//simplfied version is to give empty table name
		if(this.ancester.isSimplified())
			return new Table();
		//common way is to give the sub-select a canonical name
		else {
			Table t= QueryToolBox.traverseAndGetCanonicalTableName(body,skipNamingResolve);
			//register it 
			this.ancester.CanonicalNames.put(body, t);
			return t;
		}
	}


	/**
	 * major function that replaces aliased from item
	 * @param item
	 * @param register
	 * @param commonreg
	 * @return
	 */
	private FromItem replaceAliasedInFromItem(FromItem item){
		if (item instanceof Table){
			Table t=(Table) item;
			String key=t.toString();
			//expecting tables			
			Object obj=getAliasedObject(key,QueryToolBox.TableTypes);
			//it must be a select body
			if (obj!=null){
				if(obj instanceof Table){
					return (Table) obj;
				}
				else if (obj instanceof SelectBody){
					SelectBody body=(SelectBody) obj;
					//object that it is pointing to cannot form a circle
					if(!body.toString().contains(" "+key+" ")){
						SubSelect newsub=new SubSelect();
						newsub.setSelectBody(body);
						return newsub;
					}
					else return t;
				} 
				else {
					return item;
				}
			}				
			else
				return t;
		}
		else if (item instanceof SubSelect){
			return item;	
		}
		else if (item==null){
			System.out.println("error, input for replaceAliasedInFromItem cannot be null");
			return null;
		}
		else {
			SubJoin subj=(SubJoin) item;
			subj.setLeft(replaceAliasedInFromItem(subj.getLeft()));
			Join j=subj.getJoin();
			j.setOnExpression(replaceAliasedInExpression(j.getOnExpression()));
			j.setRightItem(replaceAliasedInFromItem(j.getRightItem()));
			return subj;
		}
	}

	//	@Override
	//	public boolean ifAcrossContext(Expression left, Expression right) {
	//		return this.ifAcrossSelect(left, right);		
	//	}
	public void registerColumn(Column c){
		Table t=c.getTable();
		if(t!=null&&t.getName()!=null&&c.getColumnName()!=null){
			//register table-col map
			HashMultiset<String> columnset=SelectNamingResolver.schemaMap.get(t.getName());
			if(columnset==null){
				columnset=HashMultiset.create();
				SelectNamingResolver.schemaMap.put(t.getName(), columnset);
			}    	
			columnset.add(c.getColumnName());
			//register col-table map
			HashMultiset<String> tableset=SelectNamingResolver.antiSchemaMap.get(c.getColumnName());
			if(tableset==null){
				tableset=HashMultiset.create();
				SelectNamingResolver.antiSchemaMap.put(c.getColumnName(), tableset);
			}    	
			tableset.add(t.getName());
		}
	}
	
}

