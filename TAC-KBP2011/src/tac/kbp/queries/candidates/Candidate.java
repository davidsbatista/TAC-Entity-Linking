package tac.kbp.queries.candidates;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.queryParser.ParseException;

import tac.kbp.bin.Definitions;
import tac.kbp.bin.Definitions.NERType;
import tac.kbp.kb.index.xml.Entity;
import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.features.Features;
import tac.kbp.queries.features.LinkDisambiguation;
import tac.kbp.queries.features.TextSimilarities;
import edu.stanford.nlp.util.Triple;

public class Candidate {
	
	public Entity entity = null;
	public int indexID;
	
	public HashSet<String> persons = null;
	public HashSet<String> places = null;
	public HashSet<String> organizations = null;
	
	//logistic regression
	public double[] conditionalProbabilities;
	
	public Features features;
	
	
	// Constructors
	public Candidate(String eid, Features features) {
		super();
		entity = new Entity();
		this.entity.id = eid;
		this.features = features;
	}
	
	public Candidate() {
		super();
		this.entity = new Entity();
		this.conditionalProbabilities = new double[2];
		this.features = new Features();
	}
	
	public Candidate(org.apache.lucene.document.Document doc, int indexID) {
		
		entity = new Entity();
		entity.id = doc.getField("id").stringValue();			
		entity.type = doc.getField("type").stringValue();
		entity.name = doc.getField("name").stringValue();
		entity.wiki_text = doc.getField("wiki_text").stringValue();
		
		this.persons = new HashSet<String>();
		this.places = new HashSet<String>();
		this.organizations = new HashSet<String>();
		this.features = new Features();
		
		this.indexID = indexID;
	}
	
	
	
	// Methods
	
	public void extractFeatures(KBPQuery q) throws Exception{
		
		/* first get named entities and topics distribution */
		getNamedEntities();
		getTopicsDistribution();
		
		topicalSimilarities(q);
		nameSimilarities(q.name);
		textualSimilarities(q);
		linkDisambiguation(q);
		isCorrect(q);
	}
	
	
	// LDA topic similarities
	public void cosine_similarity(double[] query_topics){
		
		double dot_product = 0;
		double candidate_norm = 0;
		double query_norm = 0;
		
		for (int i = 0; i < query_topics.length; i++) {
			
			candidate_norm += Math.pow(query_topics[i], 2); 
			query_norm += Math.pow(this.features.topics_distribution[i], 2);
			dot_product += this.features.topics_distribution[i] * query_topics[i];
		}
		
		
		this.features.topic_cosine_similarity = dot_product / (Math.sqrt(candidate_norm) * Math.sqrt(query_norm)); 
  
	}
	
	public void topicalSimilarities(KBPQuery q){
		
		divergence(q.topics_distribution);
		cosine_similarity(q.topics_distribution);
		if (q.highest_topic == this.features.highest_topic) {
			features.topicMatch = true;
		}		
	}
		
	public void getTopicsDistribution() {		
		
		String line = Definitions.kb_topics.get(this.indexID);
		String[] topics = line.split(" ");
		
		int max_topic = 0;
		double max_value = 0.0;
		
		for (int i = 0; i < topics.length ; i++) {
			features.topics_distribution[i] =  Double.parseDouble(topics[i]);
			if (features.topics_distribution[i] > max_value) {
				max_topic = i;
				max_value = features.topics_distribution[i];
			}
		}
		
		this.features.highest_topic = max_topic;
	}
	
	public void divergence(double[] lda_query) {
		this.features.kldivergence = com.aliasi.stats.Statistics.klDivergence(lda_query, this.features.topics_distribution);
	}
	
	
	// Graph Structure
	public void linkDisambiguation(KBPQuery q) throws IOException, ParseException{		
		HashMap<String, Integer> scores = LinkDisambiguation.getScore(Definitions.chunker, q.supportDocument, this.entity.wiki_text);
		this.features.inDegree = scores.get("inDegree");
		this.features.outDegree = scores.get("outDegree");
	}
		
	
	// Textual Similarities
	public void textualSimilarities(KBPQuery q) throws IOException {
		this.features.cosine_similarity = TextSimilarities.INSTANCE.getSimilarity(q.supportDocument, this.entity.wiki_text);
		this.features.queryStringInWikiText = queryStringInWikiText(q);
		this.features.candidateNameInSupportDocument = candidateNameInSupportDocument(q);
		this.features.candidateType = determineType();
		this.namedEntitiesIntersection(q);
	}

	
	
	// Name Similarities
	public void nameSimilarities(String query) {
		
		this.features.similarities = tac.kbp.utils.string.StringSimilarities.compareStrings(query,this.entity.name);
		
		features.exactMatch = query.equalsIgnoreCase(entity.name);
		features.querySubStringOfCandidate = entity.name.toLowerCase().contains(query.toLowerCase());
		features.candidateSubStringOfQuery = query.toLowerCase().contains(entity.name.toLowerCase());		
		features.queryStartsCandidateName = query.toLowerCase().startsWith(entity.name.toLowerCase());
		features.queryEndsCandidateName = query.toLowerCase().endsWith(entity.name.toLowerCase());		
		features.candidateNameStartsQuery = query.toLowerCase().startsWith(entity.name);		
		features.candidateNameEndsQuery = query.toLowerCase().endsWith(entity.name);
		
		if (tac.kbp.utils.string.StringUtils.isUpper(query)) {			
			if (isAcroynym(query, entity.name)) {
				features.queryStringAcronymOfCandidate = true;
			}
			if (isAcroynym(entity.name, query)) {
				features.candidateAcronymOfqueryString = true;
			}	
		}
	}

	private boolean isAcroynym(String query, String name) {		
		String [] words = name.split(" ");
		char [] accro = query.toLowerCase().toCharArray();
		
		if (words.length == accro.length) {
			for (int i = 0; i < accro.length; i++) {		
				if (accro[i] != (words[i]).toLowerCase().charAt(0)) {
					return false;
				}	
			}
			return true;
		}
		return false;
	}

	public boolean queryStringNamedEntity() {
		System.out.println("entity: " + entity.name);
		
		for (String p : persons) {
			System.out.print(p + " ");
		}
		
		System.out.println();
		
		System.out.print("PLACES: ");
		for (String p : places) {
			System.out.print(p + " ");
		}
		
		System.out.println();
		
		System.out.print("ORG: ");
		for (String p : organizations) {
			System.out.print(p + " ");
		}	
		return persons.contains(entity.name) || places.contains(entity.name) || organizations.contains(entity.name);
	}
	
	public NERType determineType(){
		if (entity.type.equalsIgnoreCase("PER")) {
			return tac.kbp.bin.Definitions.NERType.PERSON;
		}
		
		if (entity.type.equalsIgnoreCase("ORG")) {
			return tac.kbp.bin.Definitions.NERType.ORGANIZATION;
		}
			
		if (entity.type.equalsIgnoreCase("GPE")) {
			return tac.kbp.bin.Definitions.NERType.PLACE;
		}
		else return tac.kbp.bin.Definitions.NERType.UNK;
			
			
	}
	
	public boolean queryStringInWikiText(KBPQuery q){
		
		boolean nameInWikiText = entity.wiki_text.toUpperCase().indexOf(q.name.toUpperCase()) != -1;
		boolean alternativeWikiText = false;
		
		for (String sense : q.alternative_names) {
			if (entity.wiki_text.toUpperCase().indexOf(sense.toUpperCase()) != -1) {
				alternativeWikiText = true;
				break;
			}
		}
		
		return (nameInWikiText || alternativeWikiText);
	}
	
	public boolean candidateNameInSupportDocument(KBPQuery q) {
		//TODO: entity.name can have several tokens, suggestion:
		//TODO: try to match each token, instead of the whole string		
		return q.supportDocument.toUpperCase().indexOf(entity.name.toUpperCase()) != -1;
	}
	
	
	
	// Named-Entities related methods
	@SuppressWarnings("unchecked")
	public void getNamedEntities() throws Exception {
		
		List<Triple<String, Integer, Integer>> entities = tac.kbp.bin.Definitions.classifier.classifyToCharacterOffsets(entity.getWiki_text());
		
		for (Triple<String, Integer, Integer> triple : entities) {			
			String ename = entity.getWiki_text().substring(triple.second,triple.third);
			addEntitiesToCandidate(ename, triple.first);
		}
		
		// calculates the candidateType based on the number and type of named-entities
		int[] numbers = {persons.size(),places.size(),organizations.size()};
		
		int maxValue = numbers[0];
		int maxPos = 0;
		
		for(int i=1;i < numbers.length;i++){
			if(numbers[i] > maxValue){
				maxValue = numbers[i];
				maxPos = i;
			}
		}
		
		switch (maxPos) {
		
		case 0:
			this.features.queryType = NERType.PERSON;
			break;

		case 1:
			this.features.queryType = NERType.PLACE;
			break;
			
		case 2:
			this.features.queryType = NERType.ORGANIZATION;
			break;
		}
	}
	
	public void addEntitiesToCandidate(String ename, String tag) {		   
		
		String entity = ename.replaceAll("\\*", "").replaceAll("\\n", " ").replaceAll("\\!", "");
		
		if (entity.length()>1) {
			
			   if (tag.equalsIgnoreCase("PERSON"))
				   this.persons.add('"' + entity.trim() + '"');
			      
			   if (tag.equalsIgnoreCase("ORGANIZATION"))
				   this.organizations.add('"' + entity.trim() + '"');
			      
			   if (tag.equalsIgnoreCase("LOCATION"))
				   this.places.add('"' + entity.trim() + '"');
		   }
	}
	
	public void namedEntitiesIntersection(KBPQuery q){
		
		Set<String> all = new HashSet<String>();
		all.addAll(places);
		all.addAll(organizations);
		all.addAll(persons);
		
		Set<String> queryAll = new HashSet<String>();
		queryAll.addAll(q.places);
		queryAll.addAll(q.organizations);
		queryAll.addAll(q.persons);
			
		Set<String> intersection = new HashSet<String>(queryAll);
		intersection.retainAll(all);
			
		this.features.namedEntitiesIntersection = intersection.size();
		
	}

	
	// Other
	public void isCorrect(KBPQuery q) {
		if (this.entity.id.equalsIgnoreCase(q.gold_answer)) {
			this.features.correct_answer = true;
		}
		else this.features.correct_answer = false;
	}

	public double[] getConditionalProbabilities() {
		return conditionalProbabilities;
	}
}

