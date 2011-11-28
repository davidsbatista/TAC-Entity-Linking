package tac.kbp.queries;

import java.util.HashMap;
import java.util.Set;

import tac.kbp.utils.Definitions.NERType;

public class Features {
	
	/* semantic */
	
	public NERType queryType; 
	public NERType candidateType;
	public float namedEntitiesIntersection;
	public boolean queryStringInWikiText;
	public boolean candidateNameInSupportDocument;
	public boolean queryStringIsNamedEntity;
	public Float[] topics_distribution = new Float[100];  
			
	/* string similarities */
	
	public boolean exactMatch;
	public boolean querySubStringOfCandidate;
	public boolean candidateSubStringOfQuery;
	public boolean queryStartsCandidateName;
	public boolean queryEndsCandidateName;
	public boolean candidateNameStartsQuery;
	public boolean candidateNameEndsQuery;
	public boolean queryStringAcronymOfCandidate;
	public boolean candidateAcronymOfqueryString;
	public HashMap<String, Float> similarities = new HashMap<String, Float>();	
	
	/* other */
	public float lucene_score;
	
	
	public float average_similarities() {
		Set<String> keys = similarities.keySet();
		float average = 0;
		
		for (String similarity : keys) {
			average += similarities.get(similarity);
		}
		
		return average / similarities.size();
	}
	
	@Override
	public String toString() {
		 
		String string = "queryType: " + this.queryType + "\n" +
		"candidateType: " + this.candidateType + "\n" +
		"namedEntitiesIntersection: " + this.namedEntitiesIntersection + "\n" +
		"queryStringInWikiText: " + this.queryStringInWikiText  + "\n" +
		"candidateNameInSupportDocument: " + this.candidateNameInSupportDocument + "\n" +
		"exactMatch: " + this.exactMatch + "\n" +
		"querySubStringOfCandidate: " + this.querySubStringOfCandidate + "\n" +
		"candidateSubStringOfQuery: " + this.candidateSubStringOfQuery + "\n" + 
		"queryStartsCandidateName: " + this.queryStartsCandidateName + "\n" +
		"queryEndsCandidateName: " + this.queryEndsCandidateName + "\n" + 
		"candidateNameStartsQuery: " + this.candidateNameStartsQuery + "\n" +
		"candidateNameEndsQuery: " + this.candidateNameEndsQuery + "\n" +
		"queryStringAcronymOfCandidate: " + this.queryStringAcronymOfCandidate + "\n" +
		"candidateAcronymOfqueryString: " + this.candidateAcronymOfqueryString + "\n";
		
		
		Set<String> similarities_keys = similarities.keySet();
		
		for (String similiarity : similarities_keys) {
			string += similiarity + " " + similarities.get(similiarity) + "\n";
		}
		
		return string;
		
	}

}
