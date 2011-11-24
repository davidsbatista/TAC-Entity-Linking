package tac.kbp.queries;

import java.util.HashSet;

import tac.kbp.kb.index.xml.Entity;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

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

}
