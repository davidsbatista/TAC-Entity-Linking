/**
 * 
 */
package tac.kbp.kb.xml;

import java.util.Vector;


public class Fact {
	
	public String name;
	public String fact;
	public Vector<FactLink> factlink = new Vector<FactLink>();
	
	public void setFactLink(FactLink factlink){
		this.factlink.add(factlink);
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void setFact(String fact){
		this.fact = fact;
	}
	
	/*
	public String toString(){
		StringBuilder result = new StringBuilder();
		
		if (fact.length()==0) {
			result.append(name+":"+fact);
		}
		
		return result.toString();
		
		/*
		else if (fact.length()>0) {

			for (Iterator iterator = factlink.iterator(); iterator.hasNext();) {
				FactLink fact = (FactLink) iterator.next();
				result.append()
				
			}
		}
		*/

}