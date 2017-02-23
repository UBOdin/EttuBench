package booleanFormulaEntities;

import java.util.HashSet;
import java.util.Iterator;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;

/**
 * either a boolean conjunctive or disjunctive clause represented as 
 * a hash set of boolean literals
 * no tautology or contradiction is allowed and it checks implicit tautology like A IS NULL OR (A IS NOT NULL)
 * @author tingxie
 *
 */
public class BooleanNormalClause{
	private HashSet<BooleanLiteral> content;
	private boolean isDisjunctive;
	private int checkvalue=0;
	private int hashcode=Integer.MIN_VALUE;

	/**
	 * constructor for creating a clause from scratch without context
	 * @param input
	 * @param isDisjunctive
	 */
	private BooleanNormalClause(BooleanLiteral input,boolean isDisjunctive){
		this.content=new HashSet<BooleanLiteral>();
		this.content.add(input);
		this.isDisjunctive=isDisjunctive;
	}

	/**
	 * constructor for creating a clause from a set without context
	 * @param input
	 * @param isDisjunctive
	 */
	private BooleanNormalClause(HashSet<BooleanLiteral> input, boolean isDisjunctive){
		this.content=input;
		this.isDisjunctive=isDisjunctive;
	}


	public BooleanNormalClause(int checkvalue){
		this.checkvalue=checkvalue;
	}

	public HashSet<BooleanLiteral> getContent(){
		if (this.checkvalue==0)
		return this.content;
		else if (this.checkvalue==1){
			HashSet<BooleanLiteral> content=new HashSet<BooleanLiteral>();
			content.add(new BooleanLiteral(new FixedOrderExpression(ToolBox.tautology)));
			return content;
		}
		else{
			HashSet<BooleanLiteral> content=new HashSet<BooleanLiteral>();
			content.add(new BooleanLiteral(new FixedOrderExpression(ToolBox.contradiction)));
			return content;
		}
	}

	/**
	 * this method controls the creation of BooleanNormalClause without context
	 * @param input
	 * @param isDisjunctive
	 * @return
	 */
	public static BooleanNormalClause createClause(BooleanLiteral input,boolean isDisjunctive){
		if (input.ifTautology()==0){
			BooleanNormalClause result=new BooleanNormalClause (input,isDisjunctive);
			return result;
		}
		else {
			return new BooleanNormalClause(input.ifTautology());			
		}
	}

	/**
	 * element containment check
	 * @param bl
	 * @return
	 */
	public boolean ifContains(BooleanLiteral bl){
		return content.contains(bl);
	}

	public boolean isDisjunctive(){
		return this.isDisjunctive;
	}
	/**
	 * add a boolean literal to it
	 */
	public void add(BooleanLiteral bl){

		int checkvalue=bl.ifTautology();
		//ORed together
		if(this.isDisjunctive){
			//if itself is already a tautology, then no need to Ored it with anything
			if (this.checkvalue!=1){
				if (checkvalue==1){
					this.checkvalue=1;
					this.content=null;
				}
				else if (checkvalue==0){
					if (this.checkvalue==-1){
						this.checkvalue=0;
						this.content=new HashSet<BooleanLiteral>();
						this.content.add(bl);
					}
					else {
						Iterator<BooleanLiteral> it=this.content.iterator();
						while (it.hasNext()){
							BooleanLiteral mylit=it.next();
							RegionRelationship regioncheckValue; 							
							regioncheckValue=BooleanLiteral.regionContainmentCheck(bl,mylit);
							
							switch(regioncheckValue){
							case Overlap: this.content=null;this.checkvalue=1;return;
							case NonOverLapButMeet:this.content=null;this.checkvalue=1;return;
							case Contained:return;
							case Contains:it.remove();break;
							default:break;
							}
						}
						this.content.add(bl);
					}
				}
			}
		}
		//Anded together
		else {
			//if it is not contraction
			if (this.checkvalue!=-1){
				if (checkvalue==-1){
					this.checkvalue=-1;
					this.content=null;
				}
				else if (checkvalue==0){
					if (this.checkvalue==1){
						this.checkvalue=0;
						this.content=new HashSet<BooleanLiteral>();
						this.content.add(bl);	
					}
					else{
						Iterator<BooleanLiteral> it=this.content.iterator();
						while (it.hasNext()){
							BooleanLiteral mylit=it.next();
							RegionRelationship regioncheckValue;
							regioncheckValue=BooleanLiteral.regionContainmentCheck(bl,mylit);	
							switch(regioncheckValue){
							case Contained:it.remove();break;
							case Contains:return;
							case NonOverLapButMeet:this.content=null;this.checkvalue=-1;return;	
							case NonOverLapNoMeet:this.content=null;this.checkvalue=-1;return;
							default:break;
							}
						}
						this.content.add(bl);
					}
				}
			}
		}
	}

	/**
	 * set containment check, returns true is content of input set is fully contained in this clause
	 * @param set
	 * @return
	 */
	public boolean ifContainsAll(BooleanNormalClause set){
		return content.containsAll(set.content);
	}

	/**
	 * negate this clause and flip its type
	 * @return
	 */
	public BooleanNormalClause getNegatedClause(){
		if (this.checkvalue==0){
			HashSet<BooleanLiteral> newset=new HashSet<BooleanLiteral>();
			for (BooleanLiteral bl: this.content){
				newset.add(BooleanLiteral.negateBooleanLiteral(bl));
			}
			//flip the type isDisjunctive
			BooleanNormalClause DC=new BooleanNormalClause(newset,!this.isDisjunctive);
			return DC;
		}
		else if (this.checkvalue==1){
			return new BooleanNormalClause(-1);
		}
		else 
			return new BooleanNormalClause(1);
	}

	/**
	 * merges two disjunctive or conjunctive clauses
	 * need to be of the same type
	 * it makes no sense to merge different types
	 * need to consider tautology
	 * @param other
	 * @return
	 */
	public static BooleanNormalClause mergeTwoBooleanNormalClauses(BooleanNormalClause left,BooleanNormalClause right,boolean isDisjunctive){
		int leftcheck=left.checkvalue;
		int rightcheck=right.checkvalue;
		//ORed together
		if (isDisjunctive){
			if (leftcheck==1)
				return left;
			else if (rightcheck==1)
				return right;
			else if (leftcheck==-1){
				return right;
			}
			else if (rightcheck==-1)
				return left;				
		}
		//ANDed together
		else {
			if (leftcheck==-1)
				return left;
			else if (rightcheck==-1)
				return right;
			else if (leftcheck==1){
				return right;
			}
			else if (rightcheck==1)
				return left;	
		}

		if (left.isDisjunctive==right.isDisjunctive&&right.isDisjunctive==isDisjunctive){
			//since there is guarantee that these two contains no tautologies nor contradictions
			BooleanNormalClause newclause=new BooleanNormalClause(new HashSet<BooleanLiteral>(left.content),left.isDisjunctive);
			if(left.isDisjunctive){
				for(BooleanLiteral lit:right.getContent()){
					if (newclause.ifTautology()==1) return newclause;
					else
						newclause.add(lit);		
				}
			}
			else {
				for(BooleanLiteral lit:right.getContent()){
					if (newclause.ifTautology()==-1) return newclause;
					else
						newclause.add(lit);		
				}
			}
			return newclause;
		}
		else {
			System.out.println("inconsistent type when merging two normal clause");
			return null;
		}			
	}

	/**
	 * reform it into an jsqlparser expression
	 */
	public Expression reformExpression(){	
		
			if (this.checkvalue==1){
				 return ToolBox.tautology;
			}
			else if (this.checkvalue==-1){
				 return ToolBox.contradiction;
			}
			else {
				Iterator<BooleanLiteral> it=this.content.iterator();
				BooleanLiteral first,rest;
				first=it.next();
				Expression result;
				result=first.getExpression().getExpression();

				while(it.hasNext()){
					rest=it.next();
					BinaryExpression bexp;
					if (this.isDisjunctive)
						bexp=new OrExpression();
					else
						bexp=new AndExpression();

					bexp.setLeftExpression(result);
					bexp.setRightExpression(rest.getExpression().getExpression());
					result=bexp;
				}
				return result;
			}

	}


	/**
	 *  print itself
	 */
	public String toString(){
		return reformExpression().toString();
	}

	public boolean equals(Object o){	
		BooleanNormalClause bnc=(BooleanNormalClause) o;
		if (this.checkvalue==bnc.checkvalue){
			if (this.checkvalue!=0)
				return true;			
			else if (!(bnc.isDisjunctive^this.isDisjunctive)){
				return this.content.equals(bnc.content);
			}
			else
				return false;
		}
		else
			return false;				
	}

	public int hashCode(){
		if (this.checkvalue==0){
			if(this.hashcode==Integer.MIN_VALUE)
				this.hashcode=this.toString().hashCode();
			return this.hashcode;
		}
		else if (this.checkvalue==1)
			return ToolBox.tautology.toString().hashCode();
		else
			return ToolBox.contradiction.toString().hashCode();
	}

	public int ifTautology(){
		return this.checkvalue;
	}

	public int getSize(){
		if(this.checkvalue==0)
			return this.content.size();
		else 
			return 1;
	}

	/**
	 * check whether two booleanNormalClause region containment relationship without context
	 * left side is input, right side is myself
	 * currently only check for conjunctive clauses
	 * it must makes sure that literals that it is comparing are not across different sub-queries
	 * @param clause1
	 * @param clause2
	 * @return
	 */
	public RegionRelationship regionContainmentCheck(BooleanNormalClause inputclause){
		//for special case
		if (inputclause.content.size()==1&&this.content.size()==1){
			BooleanLiteral left=inputclause.content.iterator().next();
			BooleanLiteral right=this.content.iterator().next();			
			return BooleanLiteral.regionContainmentCheck(left, right);
		}
		//else we only deal with conjunctive clauses and only do containment check
		//for other relationships, we treat it as non-comparable
		else if (!this.isDisjunctive&&!inputclause.isDisjunctive){
			int selfcontainInput=0;//# of self contains input
			int Inputcontainself=0; //# of input contains self
			for (BooleanLiteral myliteral: this.content){
				for (BooleanLiteral inputliteral: inputclause.content){
					RegionRelationship checkvalue=BooleanLiteral.regionContainmentCheck(myliteral, inputliteral);
					if (checkvalue==RegionRelationship.Contained){
						Inputcontainself++;
						//check whether identical
						RegionRelationship checkv=BooleanLiteral.regionContainmentCheck(inputliteral, myliteral);
						if(checkv==RegionRelationship.Contained)
							selfcontainInput++;
						break;
					}
					else if (checkvalue==RegionRelationship.Contains){
						selfcontainInput++;
						//check whether identical
						RegionRelationship checkv=BooleanLiteral.regionContainmentCheck(inputliteral, myliteral);
						if(checkv==RegionRelationship.Contains)
							Inputcontainself++;
						break;
					}
				}
			}
			//check the count
			//if input is totally contained in myself, then all of self literals should contain some literal in input
			if (selfcontainInput==this.content.size()){
				return RegionRelationship.Contained;
			}
			//if myself is totally contained in input, then all of input literal should contains some literal in self
			else if (Inputcontainself==inputclause.content.size()){
				return RegionRelationship.Contains;
			}
			else 
				return RegionRelationship.NotComparable;

		}		
		return RegionRelationship.NotComparable;		
	}



}
