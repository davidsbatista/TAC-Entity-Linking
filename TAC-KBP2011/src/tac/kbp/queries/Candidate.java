package tac.kbp.queries;

import java.util.HashSet;

import com.sun.xml.internal.stream.Entity;

public class Candidate {
	
	Entity entity;
	
	public HashSet<String> persons;
	public HashSet<String> places;
	public HashSet<String> organizations;

	float diceSimilarity;
	float jaccardSimilarity;
	float jaro;
	float jaroWinkler;
	float levenshtein;
	
	
	public void extractNamedEntities(){
		
	}
	
}
