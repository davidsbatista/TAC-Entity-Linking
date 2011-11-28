package tac.kbp.queries;

import java.util.HashMap;

public class Features {
	
	public enum NERType {
	    PERSON, ORGANIZATION, PLACE
	}
	
	/* semantic */
	
	public NERType queryType; 
	public NERType candidateType;
	public float namedEntitiesIntersection;
	public boolean queryStringInWikiText;
	public boolean candidateNameInSupportDocument;
			
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

}
