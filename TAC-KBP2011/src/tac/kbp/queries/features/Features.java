package tac.kbp.queries.features;

import java.util.HashMap;
import java.util.Set;

import tac.kbp.bin.Definitions;
import tac.kbp.bin.Definitions.NERType;

public class Features {
	
	/* other variables to hold values or used in features value's generation */
	public double[] topics_distribution = new double[100]; // LDA topics distribution
	public int highest_topic;
	public NERType queryType; 
	public NERType candidateType;
	public String eid; 
	public boolean correct_answer;
	public double[] features;
		
	/* textual similarities */
	public double cosine_similarity; 				 	// cosine similarity #1
	public boolean typeMatch; 						 	// 1 if candidateType and queryType are the same  #2
	public float namedEntitiesIntersection; 		 	// number of common named entities #3
	public boolean queryStringInWikiText; 			 	// 1 if the query string in candidate's text #4
	public boolean candidateNameInSupportDocument; 	 	// 1 if the candidate's string is in the support document #5
	
	/* topical similarity */
	public double  kldivergence; 						// Kullback-Leibler Divergence between LDA topics distribution #6
	public boolean topicMatch;  						// 1 if the topic with the higest probability is the same in the query topic's distribution #7
	public double  topic_cosine_similarity;  			// 1 if the topic with the higest probability is the same in the query topic's distribution #8
	
	/* name string similarities */
	public boolean exactMatch; 				     		// 1 if query string is equal to candidate's string #9
	public boolean querySubStringOfCandidate;     		// 1 if the query string is a substring of the candidate's string #10
	public boolean candidateSubStringOfQuery;    	 	// 1 if the candidate's string is a substring of query string #11
	public boolean queryStartsCandidateName;      		// 1 if the query string starts the candidate string #12
	public boolean queryEndsCandidateName;    	  		// 1 if the query string ends candidate string #13
	public boolean candidateNameStartsQuery;  	  		// 1 if candidate string starts query string #14 (NULL)
	public boolean candidateNameEndsQuery;    	  		// 1 if candidate string ends the query string #15 (NULL)
	public boolean queryStringAcronymOfCandidate; 		// 1 if query string is an acronym of the candidate #16
	public boolean candidateAcronymOfqueryString;	 	// 1 if candidate's string is an acronym of the query string #17 (NULL)
	
	public HashMap<String, Float> similarities = new HashMap<String, Float>();
	
	/* Hashtable containing the following keys
	 *  
	 * DiceSimilarity 		#18
	 * JaccardSimilarity	#19
	 * Jaro					#20
	 * JaroWinkler			#21
	 * Levenshtein 			#22
	 */
	
	public float average_similarities = 0;				// average of the 5 similarity string measures, to be used for NIL Detector
	
	/* link disambiguation */
	public int outDegree;  			// the out-degree measure according to <http://aclweb.org/anthology/I/I11/I11-1113.pdf> #23 
	public int inDegree;	  		// the in-degree measure according to <http://aclweb.org/anthology/I/I11/I11-1113.pdf>  #24
	
	public double outDegreeNormalized;
	public double inDegreeNormalized;
	

	/* Methods */ 
	
	/* constuctor */
	public Features() {
		super();
	}
	
	/* returns a feature vector */
	public double[] featuresVector(){
		
		double[] inputVector = new double[24];
		
		if (Definitions.textualSimilarities) {
			
			// cosine
			inputVector[0] = this.cosine_similarity;
			
			// type match
			if (this.queryType == candidateType) {
				inputVector[1] = 1;
			}
			// named-entities intersection
			inputVector[1] = namedEntitiesIntersection;
			
			// query is in the wiki text
			if (this.queryStringInWikiText) {
				inputVector[2] = 1;
			}
			
			// candidate name string is in the support document
			if (this.candidateNameInSupportDocument) {
				inputVector[3] = 1;
			}
			
			if (this.topicMatch) {
				inputVector[4] = 1;
			}
			
		}
		
		if (Definitions.topicalSimilarities) {
			
			inputVector[5] = this.kldivergence;
			
			if (this.topicMatch) {
				inputVector[6] = 1;
			}

			inputVector[7] = this.topic_cosine_similarity;
			
		}
		
		if (Definitions.nameSimilarities) {
			
			if (this.exactMatch) {
				inputVector[8] = 1;
			}
			
			if (this.querySubStringOfCandidate) {
				inputVector[9] = 1;
			}
			
			if (this.candidateSubStringOfQuery) {
				inputVector[10] = 1;
			}
			
			if (this.queryStartsCandidateName) {
				inputVector[11] = 1;
			}
			
			if (this.queryEndsCandidateName) {
				inputVector[12] = 1;
			}
			
			if (this.candidateNameStartsQuery) {
				inputVector[13] = 1;
			}
			
			if (this.candidateNameEndsQuery) {
				inputVector[14] = 1;
			}
			
			if (this.queryStringAcronymOfCandidate) {
				inputVector[15] = 1;
			}
			
			if (this.candidateAcronymOfqueryString) {
				inputVector[16] = 1;
			}
			
			inputVector[17] = (double) similarities.get("DiceSimilarity");
			inputVector[18] = (double) similarities.get("JaccardSimilarity");
			inputVector[19] = (double) similarities.get("Jaro");
			inputVector[20] = (double) similarities.get("JaroWinkler");
			inputVector[21] = (double) similarities.get("Levenshtein");
			
		}
		
		if (Definitions.linkDisambiguation) {
			inputVector[22] = this.inDegree;
			inputVector[23] = this.outDegree;			
		}
		
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
}