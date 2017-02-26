package Regularization;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.WithItem;

/**
 * this class resolves all naming problems in the query including aliases removal and replacement
 * @author tingxie
 *
 */
public class SelectNamingResolver{

	//records fromItem's realm,global
	public IdentityHashMap<SelectBody,SelectBodyNamingResolver> selectBodyBelongMap;
	//record alias mapping, global
	public  HashMap<String,Object> commonregister;
	private Select select;
	private boolean simplified;
	//canonical names for select body,global
	public HashMap<SelectBody, Table> CanonicalNames;
	public Select transformedSelect;
	
	public SelectNamingResolver(Select input,boolean simplified){
		selectBodyBelongMap=new IdentityHashMap<SelectBody,SelectBodyNamingResolver>();
		commonregister =new HashMap<String,Object>();
		CanonicalNames=new HashMap<SelectBody, Table>();
		this.select=input;
	    this.simplified=simplified;
	}
	
	public boolean isSimplified(){
		return this.simplified;
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
//		PlainSelectNamingResolver obj=objectBelongMap.get(exp);
//		if(obj!=null)
//			return obj;
//
//		if (exp instanceof BinaryExpression){
//			BinaryExpression bexp=(BinaryExpression) exp;		  
//			Expression left=bexp.getLeftExpression();
//			Expression right=bexp.getRightExpression();
//			obj=searchThroughExpressionAndFindMapping(left);
//			if(obj!=null)
//				return obj;
//			
//				obj=searchThroughExpressionAndFindMapping(right);
//				if(obj!=null)
//					return obj;
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
	 * replace all aliases with its referred Structure in the query
	 * (1) alias in FromItem
	 * (1) alias in SelectItem
	 * (1) alias in Boolean Formula
	 */		
	public Select aliasReplaceSelect(){
         if(this.transformedSelect==null){
		//deal with With statement
		List<WithItem> withlist= select.getWithItemsList();
		List<WithItem> newwithlist= new ArrayList<WithItem>();
		HashMap<String,Object> addon=new HashMap<String,Object>();
		if (withlist!=null){
			for (WithItem witem: withlist){
				WithItem newwitem=new WithItem();
				List<SelectExpressionItem> wlist=witem.getWithItemList();
				if (wlist!=null){
					for (SelectExpressionItem sitem: wlist){
						//register alias of select items
						String alias=sitem.getAlias();
						if (alias!=null)
							commonregister.put(alias, sitem.getExpression());
					}
				}
				SelectBody b=witem.getSelectBody();
				SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(b,this,addon);
				addon.putAll(resolver.aliasRegister);
				
				//alias replace with statement first
				SelectBody newbody=resolver.aliasReplaceSelectBody();
				//register alias mapping and selectbody mapping
				String wname=witem.getName();
				this.commonregister.put(wname, newbody);				
				newwitem.setName(witem.getName());
				newwitem.setWithItemList(witem.getWithItemList());
				newwitem.setSelectBody(newbody);
				newwithlist.add(newwitem);
			}
		}

		//deal with its own select body
		SelectBody body=select.getSelectBody();	
		SelectBodyNamingResolver resolver=new SelectBodyNamingResolver(body,this,addon);
		SelectBody newbody=resolver.aliasReplaceSelectBody();
		Select newselect=new Select();
		newselect.setSelectBody(newbody);
		newselect.setWithItemsList(newwithlist);
		this.transformedSelect=newselect;
		
         }
		
		return this.transformedSelect;
	}




//
//	@Override
//	public boolean ifAcrossContext(Expression left, Expression right) {
//		return this.ifAcrossSelect(left, right);		
//	}

}
