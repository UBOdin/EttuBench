package booleanFormulaEntities;

import java.util.HashSet;
import java.util.Iterator;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;

/**
 * this class maintains a CNF or DNF formula, a hash set of BooleanNormalClauses
 * it needs to make sure that none of its normal clause is a tautology or contradiction
 * it checks implicit tautology between clauses like X ISNULL OR X ISNOTNULL
 * @author tingxie
 *
 */
public class DisjunctiveNormalForm{
	private HashSet<BooleanNormalClause> DNFcontent;
	private int checkvalue=0;
	private DisjunctiveNormalForm(HashSet<BooleanNormalClause> DNFinput){
		this.DNFcontent=DNFinput;			
	}

	private DisjunctiveNormalForm(BooleanNormalClause DNFinput,BooleanLiteral originalExpression){
		this.DNFcontent=new HashSet<BooleanNormalClause>();
		if(DNFinput.isDisjunctive())
			System.out.println("error, please input a conjunctive clause to initialize a DNF form");
		else
			this.DNFcontent.add(DNFinput);
	}

	public DisjunctiveNormalForm(int checkvalue){
		if(checkvalue==0)
			System.out.println("cannot input 0 for this special constructor");
		else
		this.checkvalue=checkvalue;		
	}

	/**
	 * DNF, after negation, will be a CNF form and vice versa
	 * @return
	 */
	public DisjunctiveNormalForm getNegatedForm(){
		if (this.checkvalue==0){
			BooleanLiteral tautology=new BooleanLiteral(new FixedOrderExpression(ToolBox.tautology));
			DisjunctiveNormalForm newDNF=new DisjunctiveNormalForm(BooleanNormalClause.createClause(tautology, false),tautology);

			BooleanNormalClause disjunctive;
			HashSet<BooleanLiteral> content;
			HashSet<BooleanNormalClause> newcontent;
			//for each negated conjunctive clause we get a separate DNF from it
			for (BooleanNormalClause conjunctive: this.DNFcontent){
				disjunctive=conjunctive.getNegatedClause();
				content=disjunctive.getContent();
				newcontent=new HashSet<BooleanNormalClause>();
				for (BooleanLiteral lit:content){   
					//create a conjunctive clause from each literal in disjunctive clause
					newcontent.add(BooleanNormalClause.createClause(lit,false));
				}
				//merge DNFs
				newDNF=DisjunctiveNormalForm.AndTogetherTwoNormalForms(newDNF,new DisjunctiveNormalForm(newcontent));
			}
			return newDNF;
		}
		else if (this.checkvalue==1){
			return new DisjunctiveNormalForm(-1);
		}
		else {
			return new DisjunctiveNormalForm(1);
		}
	}

	/**
	 * publicly it only accepts BooleanLiteral
	 * this method controls the creation of BooleanNormalForm
	 */
	public static DisjunctiveNormalForm createForm(BooleanLiteral bl){
		BooleanNormalClause DNFinput;				
		DNFinput=BooleanNormalClause.createClause(bl, false);

		int DNFcheckvalue=DNFinput.ifTautology();
		if (DNFcheckvalue==0)
			return new DisjunctiveNormalForm(DNFinput,bl);
		else {
			return new DisjunctiveNormalForm(DNFcheckvalue);
		}		
	}


	/**
	 * AND together two expressions and create a new normal form
	 * @param left
	 * @param right
	 * @return
	 */
	public static DisjunctiveNormalForm AndTogetherTwoNormalForms(DisjunctiveNormalForm left,DisjunctiveNormalForm right){
		int leftcheck=left.checkvalue;
		int rightcheck=right.checkvalue;

		//ANded together
		if (leftcheck==1)
			return right;
		else if (rightcheck==1)
			return left;
		else if (leftcheck==-1)
			return left;
		else if (rightcheck==-1)
			return right;

		//if code reaches this line then we are safe to merge
		//since there is guarantee that these two contains no tautologies nor contradictions
		DisjunctiveNormalForm newDNF=new DisjunctiveNormalForm(-1);

		for (BooleanNormalClause leftDNFclause: left.DNFcontent){
			for (BooleanNormalClause rightDNFclause: right.DNFcontent){
				BooleanNormalClause result=BooleanNormalClause.mergeTwoBooleanNormalClauses(leftDNFclause, rightDNFclause, false);
				newDNF.add(result);	
				if(newDNF.ifTautology()==1)
					return newDNF;
			}
		}
		return newDNF;
	}

	/**
	 * add one clause to it
	 */
	public void add(BooleanNormalClause input){

		if (input.ifTautology()==1){
			this.DNFcontent=null;
			this.checkvalue=1;
		}
		else if (input.ifTautology()==0){
			if(this.checkvalue==-1){
				this.checkvalue=0;
				this.DNFcontent=new HashSet<BooleanNormalClause>();
				this.DNFcontent.add(input);
			}
			else if (this.checkvalue==0){
				Iterator<BooleanNormalClause> it=this.DNFcontent.iterator();				
				while (it.hasNext()){
					BooleanNormalClause clause=it.next();
					RegionRelationship checkvalue=clause.regionContainmentCheck(input);
				
					switch(checkvalue){
					//if input contains clause, remove this clause as duplicate and keep checking
					case Contains:it.remove();break;
					//if input is contained in self, just ignore it
					case Contained:return;
					//if Overlap
					case Overlap:this.DNFcontent=null;this.checkvalue=1;return;
					//if non-overlap but meet
					case NonOverLapButMeet:this.DNFcontent=null;this.checkvalue=1;return;
					default:break;
					}
				}				
				this.DNFcontent.add(input);
			}
		}
	}

	/**
	 * cross product two forms of the same type
	 * it makes no sense to merge DNF with CNF and vice versa
	 * need to consider tautology
	 * @param left
	 * @param right
	 * @return
	 */
	public static DisjunctiveNormalForm OrTogetherTwoNormalForms(DisjunctiveNormalForm left,DisjunctiveNormalForm right){
		
		int leftcheck=left.checkvalue;
		int rightcheck=right.checkvalue;

		//Orded together
		if (leftcheck==-1)
			return right;
		else if (rightcheck==-1)
			return left;
		else if (leftcheck==1)
			return left;
		else if (rightcheck==1)
			return right;

		DisjunctiveNormalForm newDNF;
		//if code reaches this line then we are safe to merge
		//since there is guarantee that these two contains no tautologies nor contradictions			
		//for its DNF form, we can safely add them together
		if(left.DNFcontent.size()>right.DNFcontent.size()){
			newDNF=new DisjunctiveNormalForm(new HashSet<BooleanNormalClause>(left.DNFcontent));
			for (BooleanNormalClause clause:right.DNFcontent){
				newDNF.add(clause);
				if(newDNF.ifTautology()==1)
					return newDNF;
			}
		}
		else {
			newDNF=new DisjunctiveNormalForm(new HashSet<BooleanNormalClause>(right.DNFcontent));
			for (BooleanNormalClause clause:left.DNFcontent){
				newDNF.add(clause);
				if(newDNF.ifTautology()==1)
					return newDNF;
			}	
		}
		return newDNF;
	}

	/**
	 *  print its original expression
	 */
	public String toString(){
		if(this.checkvalue==0){
			return this.getReformedExpression().toString();
		}
		else if (this.checkvalue==1)
			return ToolBox.tautology.toString();
		else
			return ToolBox.contradiction.toString();	
	}

	/**
	 * major DNF decomposition algorithm, DNF without awarer
	 * does not consider implicit tautology X OR (NOT X)
	 * @param e
	 * @return
	 */
	public static DisjunctiveNormalForm DNFDecomposition(Expression e){
		if (e instanceof OrExpression){
			OrExpression or=(OrExpression) e;
			if (or.isNot())
				System.out.println("please deal with not in Or Expression: "+or);
			Expression left=or.getLeftExpression();
			Expression right=or.getRightExpression();
			return OrTogetherTwoNormalForms(DNFDecomposition(left), DNFDecomposition(right));            
		}
		else if (e instanceof AndExpression){
			AndExpression and=(AndExpression) e;
			if (and.isNot())
				System.out.println("please deal with not in And Expression: "+and);
			Expression left=and.getLeftExpression();
			Expression right=and.getRightExpression();
			return AndTogetherTwoNormalForms(DNFDecomposition(left), DNFDecomposition(right));            
		}
		else if (e instanceof Parenthesis){
			Parenthesis p=(Parenthesis) e;			
			if (!p.isNot()){
				Expression newexp=ToolBox.PeelParenthesis(p);
				return DNFDecomposition(newexp);
			}
			else{
				Expression ee=p.getExpression();
				//if nested parenthesis
				if (ee instanceof Parenthesis){
					//if nested Not,then they cancel each other
					if (((Parenthesis) ee).isNot())
						return DNFDecomposition(((Parenthesis) ee).getExpression());						
					else 
						return DNFDecomposition(ToolBox.PeelParenthesis((Parenthesis) ee)).getNegatedForm();					
				}
				//if there is no nested parenthesis
				//simple case first do CNF and negate all terms
				else{
					return DNFDecomposition(ee).getNegatedForm();
				}	
			}
		}
		else if (e instanceof EqualsTo||e instanceof NotEqualsTo||e instanceof GreaterThan||e instanceof GreaterThanEquals||e instanceof MinorThan||e instanceof MinorThanEquals||e instanceof LikeExpression){
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e)));		
		}
		else if (e instanceof Between)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e)));
		else if (e instanceof IsNullExpression)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e)));
		else if (e instanceof InExpression)									
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e)));
		else if (e instanceof CaseExpression)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e)));
		//if it is an inverseExpression then
		else if (e instanceof InverseExpression){
			InverseExpression inv=(InverseExpression) e;
			Expression ee=inv.getExpression();
			if (ee instanceof InverseExpression)
				return DNFDecomposition(((InverseExpression) ee).getExpression());
			else
				return DNFDecomposition(ee).getNegatedForm();
		}
		else if (e instanceof ExistsExpression)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e)));
		else if (e!=null){   
			if(e instanceof LongValue){
				LongValue lv=(LongValue) e;
				long value=lv.getValue();
				if(value>0)
					return new DisjunctiveNormalForm(1);
				else {
					return new DisjunctiveNormalForm(-1);
				}
			}
			else{
			System.out.println("dnf do not know how to process: "+e.getClass()+":"+e);
			return null;
			}
		}
		else{
			System.out.println("input of normalization cannot be null");
			return null;
		}
	}

	/**
	 * major DNF decomposition algorithm, DNF with ContextAwarer
	 * does not consider implicit tautology X OR (NOT X)
	 * @param e
	 * @return
	 */
	public static DisjunctiveNormalForm DNFDecomposition(Expression e,ExpressionContextAware awarer){

		if (e instanceof OrExpression){
			OrExpression or=(OrExpression) e;
			if (or.isNot())
				System.out.println("please deal with not in Or Expression: "+or);
			Expression left=or.getLeftExpression();
			Expression right=or.getRightExpression();
			return OrTogetherTwoNormalForms(DNFDecomposition(left,awarer), DNFDecomposition(right,awarer));            
		}
		else if (e instanceof AndExpression){
			AndExpression and=(AndExpression) e;
			if (and.isNot())
				System.out.println("please deal with not in And Expression: "+and);
			Expression left=and.getLeftExpression();
			Expression right=and.getRightExpression();
			return AndTogetherTwoNormalForms(DNFDecomposition(left,awarer), DNFDecomposition(right,awarer));            
		}
		else if (e instanceof Parenthesis){

			Parenthesis p=(Parenthesis) e;			
			if (!p.isNot()){
				Expression newexp=ToolBox.PeelParenthesis(p);
				return DNFDecomposition(newexp,awarer);
			}
			else{
				Expression ee=p.getExpression();
				//if nested parenthesis
				if (ee instanceof Parenthesis){
					//if nested Not,then they cancel each other
					if (((Parenthesis) ee).isNot())
						return DNFDecomposition(((Parenthesis) ee).getExpression(),awarer);						
					else 
						return DNFDecomposition(ToolBox.PeelParenthesis((Parenthesis) ee),awarer).getNegatedForm();					
				}
				//if there is no nested parenthesis
				//simple case first do CNF and negate all terms
				else{
					return DNFDecomposition(ee,awarer).getNegatedForm();
				}	
			}
		}
		else if (e instanceof EqualsTo||e instanceof NotEqualsTo||e instanceof GreaterThan||e instanceof GreaterThanEquals||e instanceof MinorThan||e instanceof MinorThanEquals||e instanceof LikeExpression){
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e),awarer));		
		}
		else if (e instanceof Between)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e),awarer));
		else if (e instanceof IsNullExpression)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e),awarer));
		else if (e instanceof InExpression)									
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e),awarer));
		else if (e instanceof CaseExpression)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e),awarer));
		//if it is an inverseExpression then
		else if (e instanceof InverseExpression){
			InverseExpression inv=(InverseExpression) e;
			Expression ee=inv.getExpression();
			if (ee instanceof InverseExpression)
				return DNFDecomposition(((InverseExpression) ee).getExpression(),awarer);
			else
				return DNFDecomposition(ee,awarer).getNegatedForm();
		}
		else if (e instanceof ExistsExpression)
			return DisjunctiveNormalForm.createForm(new BooleanLiteral(new FixedOrderExpression(e),awarer));
		else if (e!=null){   
			if(e instanceof LongValue){
				LongValue lv=(LongValue) e;
				long value=lv.getValue();
				if(value>0)
					return new DisjunctiveNormalForm(1);
				else {
					return new DisjunctiveNormalForm(-1);
				}
			}
			else{
			System.out.println("dnf do not know how to process: "+e.getClass()+":"+e);
			return null;
			}
		}
		else{
			System.out.println("input of normalization cannot be null");
			return null;
		}
	}

	public int getDNFSize(){
		if(this.checkvalue==0)
			return this.DNFcontent.size();
		else return 1;
	}

	public int ifTautology(){
		return this.checkvalue;
	}

	/**
	 * output its content
	 * @param value
	 * @return
	 */
	public HashSet<BooleanNormalClause> getContent(){
		if(this.checkvalue==0)
		return this.DNFcontent;
		else {
			HashSet<BooleanNormalClause> content=new HashSet<BooleanNormalClause>();
			content.add(new BooleanNormalClause(this.checkvalue));
			return content;
		}
	}

	/**
	 * print DNF form of its original expression
	 * @param isDNF
	 * @return
	 */
	public Expression getReformedExpression(){
			if (this.checkvalue==1)
				return ToolBox.tautology;
			else if (this.checkvalue==-1)
				return ToolBox.contradiction;
			else{			
				Iterator<BooleanNormalClause> it=this.DNFcontent.iterator();				
				BooleanNormalClause first=it.next();
				Expression result=first.reformExpression();
				while(it.hasNext()){
					BooleanNormalClause clause=it.next();
					result=new OrExpression(result,clause.reformExpression());
				}
				return result;
			}
	}

}
