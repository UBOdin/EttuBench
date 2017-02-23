package featureEngineering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.Union;

public class SelectBodyNamingResolver{
	//records object's realm
	SelectNamingResolver ancester;
	//record resolver for each plain select
	public  HashMap<String,Object> aliasRegister;
	private SelectBody body;
	private List<PlainSelectNamingResolver> chidList;
	private SelectBody transformedBody;
	
	public SelectBodyNamingResolver(SelectBody input,SelectNamingResolver ancester,HashMap<String,Object> addon){
		this.body=input;
		this.ancester=ancester;	
		//register this resolver
		this.ancester.selectBodyBelongMap.put(body, this);
		
		this.chidList=new ArrayList<PlainSelectNamingResolver>();
		this.aliasRegister=new HashMap<String,Object>();
		this.aliasRegister.putAll(addon);
		this.aliasRegister.putAll(this.ancester.commonregister);
	}

	public List<PlainSelectNamingResolver> getChildPlainSelectResolverList(){
		return this.chidList;
	}
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
	 * function that replaces alias in any selection body
	 * @param body
	 * @param commonreg
	 */
	public SelectBody aliasReplaceSelectBody(){
		if(this.transformedBody==null){
		//for each plain select we need a resolver
		if (body instanceof PlainSelect){
			PlainSelect ps=(PlainSelect) body;
			PlainSelectNamingResolver resolver=new PlainSelectNamingResolver(this.ancester,ps,this.aliasRegister);
			this.chidList.add(resolver);
			//register this new selectbody
			SelectBody newbody=resolver.aliasReplacePlainSelect();
			this.ancester.selectBodyBelongMap.put(newbody, this);
			this.transformedBody=newbody;
		}
		else {
			Union U=(Union) body;
			@SuppressWarnings("unchecked")
			List<PlainSelect> plist=U.getPlainSelects();
			List<PlainSelect> newplist=new ArrayList<PlainSelect>();
			for (PlainSelect ps: plist){
				PlainSelectNamingResolver resolver=new PlainSelectNamingResolver(this.ancester,ps,this.aliasRegister);
				this.chidList.add(resolver);
				newplist.add(resolver.aliasReplacePlainSelect()); 
			}
			Union u=new Union();
			u.setAll(U.isAll());
			u.setDistinct(U.isDistinct());
			u.setLimit(U.getLimit());
			u.setOrderByElements(U.getOrderByElements());
			u.setPlainSelects(newplist);
			//register this new selectbody
			this.ancester.selectBodyBelongMap.put(u, this);
			this.transformedBody=u;
		}
		}
		
		return this.transformedBody;
	}
	

//	@Override
//	public boolean ifAcrossContext(Expression left, Expression right) {
//		return this.ifAcrossSelect(left, right);		
//	}
//	
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
	
}
