package tac.kbp.queries.features;

import java.util.HashMap;
import java.util.Set;

import tac.kbp.utils.Definitions.NERType;

public class Features {
	
	/* semantic */
	public NERType queryType; 
	public NERType candidateType;
	public boolean typeMatch;  // 1 if candidateType and queryType are the same  #1
	public float namedEntitiesIntersection; // number of common named entities #2
	public boolean queryStringInWikiText; // 1 if the query string in candidate's text #3
	public boolean candidateNameInSupportDocument; // 1 if the candidate's string is in the support document #4
	public double[] topics_distribution = new double[100]; // LDA topics distrubution 
	public double kldivergence; // Kullback-Leibler Divergence between LDA topics distrubution #5
	
	/* string similarities */
	public boolean exactMatch; // 1 if query string is equal to candidate's string #6
	public boolean querySubStringOfCandidate; // 1 if the query string is a substring of the candidate's string #7
	public boolean candidateSubStringOfQuery; // 1 if the candidate's string is a substring of query string #8
	public boolean queryStartsCandidateName;  // 1 if the query string starts the candidate string #9
	public boolean queryEndsCandidateName;    // 1 if the query string ends candidate string #10
	public boolean candidateNameStartsQuery;  // 1 if candidate string starts query string #11 (NULL)
	public boolean candidateNameEndsQuery;    // 1 if candidate string ends the query string #12 (NULL)
	public boolean queryStringAcronymOfCandidate; // 1 if query string is an acronym of the candidate #13
	public boolean candidateAcronymOfqueryString; // 1 if candidate's string is an acronym of the query string #14
	
	/* string similarities features:
	 * 
	 * DiceSimilarity 		#15
	 * JaccardSimilarity	#16
	 * Jaro					#17
	 * JaroWinkler			#18
	 * Levenshtein 			#19
	 */
	public HashMap<String, Float> similarities = new HashMap<String, Float>();
	
	
	/* text similarities */
	public float lucene_score; // the score given by Lucene #20
	public double cosine_similarity; // cosine similarity #21
		
	/* other */
	public String eid; 
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

	/* creates a feature object from a String red from file */
	public Features(String eid, String[] features) {		
		
		this.eid = eid;
		this.typeMatch = Boolean.parseBoolean((features[0]));
		this.namedEntitiesIntersection = Float.parseFloat((features[1]));
		this.queryStringInWikiText = Boolean.parseBoolean((features[2]));
		this.candidateNameInSupportDocument = Boolean.parseBoolean((features[3]));
		this.kldivergence = Double.parseDouble((features[4]));
		this.exactMatch = Boolean.parseBoolean((features[5]));
		this.querySubStringOfCandidate = Boolean.parseBoolean((features[6]));
		this.candidateSubStringOfQuery = Boolean.parseBoolean((features[7]));
		this.queryStartsCandidateName = Boolean.parseBoolean((features[8]));
		this.queryEndsCandidateName = Boolean.parseBoolean((features[9]));
		this.candidateNameStartsQuery = Boolean.parseBoolean((features[10]));
		this.candidateNameEndsQuery = Boolean.parseBoolean((features[11]));
		this.queryStringAcronymOfCandidate = Boolean.parseBoolean((features[12]));
		this.candidateAcronymOfqueryString = Boolean.parseBoolean((features[13]));
		this.similarities.put("DiceSimilarity", Float.parseFloat((features[14])));
		this.similarities.put("JaccardSimilarity", Float.parseFloat((features[15])));
		this.similarities.put("Jaro", Float.parseFloat((features[16])));
		this.similarities.put("JaroWinkler", Float.parseFloat((features[17])));
		this.similarities.put("Levenshtein", Float.parseFloat((features[18])));
	}
	
	public Features() {
		super();
	}
}








