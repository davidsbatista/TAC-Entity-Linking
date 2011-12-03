package tac.kbp.queries;

import java.util.HashMap;
import java.util.Set;

import tac.kbp.utils.Definitions.NERType;

public class Features {
	
	/* semantic */
	public NERType queryType; 
	public NERType candidateType;
	public boolean typeMtach;
	public float namedEntitiesIntersection;
	public boolean queryStringInWikiText;
	public boolean candidateNameInSupportDocument;
	public double[] topics_distribution = new double[100];  
	public double kldivergence;
	
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
	public boolean correct_answer;
	
	public double[] inputVector(){
		
		double[] inputVector = new double[19];
		
		if (this.queryType == candidateType) {
			inputVector[0] = 1;
		}

		inputVector[1] = namedEntitiesIntersection;
		
		if (this.queryStringInWikiText) {
			inputVector[2] = 1;
		}
		
		if (this.candidateNameInSupportDocument) {
			inputVector[3] = 1;
		}
				
		inputVector[4] = this.kldivergence;
		
		if (this.exactMatch) {
			inputVector[5] = 1;
		}
		
		if (this.querySubStringOfCandidate) {
			inputVector[6] = 1;
		}
		
		if (this.candidateSubStringOfQuery) {
			inputVector[7] = 1;
		}
		
		if (this.queryStartsCandidateName) {
			inputVector[8] = 1;
		}
		
		if (this.queryEndsCandidateName) {
			inputVector[9] = 1;
		}
		
		if (this.candidateNameStartsQuery) {
			inputVector[10] = 1;
		}
		
		if (this.candidateNameEndsQuery) {
			inputVector[11] = 1;
		}
		
		if (this.queryStringAcronymOfCandidate) {
			inputVector[12] = 1;
		}
		
		if (this.candidateAcronymOfqueryString) {
			inputVector[13] = 1;
		}
		
		inputVector[14] = (double) similarities.get("DiceSimilarity");
		inputVector[15] = (double) similarities.get("JaccardSimilarity");
		inputVector[16] = (double) similarities.get("Jaro");
		inputVector[17] = (double) similarities.get("JaroWinkler");
		inputVector[18] = (double) similarities.get("Levenshtein");
		
		return inputVector;
	}

	public int output() {
		if (this.correct_answer) {
			return 1;
		}
		else return 0;
	}
	
	/* calculates the average of the string similarities */
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
