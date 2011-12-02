package tac.kbp.queries;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;

import tac.kbp.kb.index.xml.Entity;
import tac.kbp.utils.Definitions;
import tac.kbp.utils.Definitions.NERType;
import tac.kbp.utils.misc.JavaRunCommand;

import edu.stanford.nlp.util.Triple;

public class Candidate {
	
	public Entity entity = null;
	public int indexID;
	
	public HashSet<String> persons = null;
	public HashSet<String> places = null;
	public HashSet<String> organizations = null;
	
	public Features features;
	
	public Candidate() {
		super();
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
	
	public void extractFeatures(KBPQuery q) throws Exception{
		getNamedEntities();
		nameSimilarities(q.name);
		semanticFeatures(q);
		getTopicsDistribution();
		divergence(q.topics_distribution);
		
	}
	
	public void getNamedEntities() throws Exception {
		
		List<Triple<String, Integer, Integer>> entities = tac.kbp.utils.Definitions.classifier.classifyToCharacterOffsets(entity.getWiki_text());
		
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

	public void semanticFeatures(KBPQuery q) throws IOException {
		
		features.queryStringInWikiText = queryStringInWikiText(q);
		features.candidateNameInSupportDocument = candidateNameInSupportDocument(q);
		features.queryStringIsNamedEntity = queryStringNamedEntity();
		features.candidateType = determineType();
		
		/*
		// get the vector of candidate in the KB
		TermFreqVector wiki_text_vector = Definitions.searcher.getIndexReader().getTermFreqVector(indexID, "wiki_text");
				
		// get the vector of the query's support document
		Term t = new Term("docid", q.docid); 
		Query query = new TermQuery(t); 		
		TopDocs docs = Definitions.documents.search(query, 1);
		ScoreDoc[] scoredocs = docs.scoreDocs;
		TermFreqVector support_doc_vector = Definitions.documents.getIndexReader().getTermFreqVector(scoredocs[0].doc, "text");
		
		// get vectors for candidates wiki_text and for support document
		// normalize both vectors: common words only, same dimension
		// calculate sin(v1,v2)
		this.normalizeVectors(support_doc_vector, wiki_text_vector);
		*/
	}
	
	public void normalizeVectors(TermFreqVector support_doc_vector, TermFreqVector wiki_text_vector) {
		
		System.out.println(support_doc_vector);
		System.out.println(wiki_text_vector);
		
		int[] support_doc_termFreq = support_doc_vector.getTermFrequencies();
		String[] support_doc_terms = support_doc_vector.getTerms();
		
		int[] wiki_text_termFreq = wiki_text_vector.getTermFrequencies();
		String[] wiki_text_terms = wiki_text_vector.getTerms();
		
		System.out.println("Wiki_text: ");
		for (int i = 0; i < wiki_text_terms.length; i++) {
			System.out.println(wiki_text_terms[i] + "\t" + wiki_text_termFreq[i]);
		}
		
		System.out.println("Support Doc: ");
		for (int i = 0; i < support_doc_terms.length; i++) {
			System.out.println(support_doc_terms[i] + "\t" + support_doc_termFreq[i]);
		}
		
		
		
	}
	
	public boolean queryStringNamedEntity() {
		return persons.contains(entity.name) || places.contains(entity.name) || organizations.contains(entity.name);
	}
	
	public NERType determineType(){
		if (entity.type.equalsIgnoreCase("PER")) {
			return tac.kbp.utils.Definitions.NERType.PERSON;
		}
		
		if (entity.type.equalsIgnoreCase("ORG")) {
			return tac.kbp.utils.Definitions.NERType.ORGANIZATION;
		}
			
		if (entity.type.equalsIgnoreCase("GPE")) {
			return tac.kbp.utils.Definitions.NERType.PLACE;
		}
		else return tac.kbp.utils.Definitions.NERType.UNK;
			
			
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
		return q.supportDocument.toUpperCase().indexOf(entity.name.toUpperCase()) != -1;
	}

	public void getTopicsDistribution() {
		
		String[] entity_id = entity.id.split("E");
		
		String command = "head -n " + (Integer.parseInt(entity_id[1])-1) + " " + Definitions.KB_lda_topics+"/model-final.theta | tail -n 1";		
		String output = JavaRunCommand.run(command);
		
		String[] parsed_output = output.split("<==");
		
		String[] topics = parsed_output[1].split(" ");
		
		for (int i = 0; i < topics.length ; i++) {
			features.topics_distribution[i] =  Double.parseDouble(topics[i]);
		}
	}
	
	public void divergence(double[] lda_query) {
		this.features.kldivergence = com.aliasi.stats.Statistics.klDivergence(lda_query, this.features.topics_distribution);		
	}
			
}