package tac.kbp.queries;

import java.util.HashSet;
import java.util.List;

import tac.kbp.kb.index.xml.Entity;
import edu.stanford.nlp.util.Triple;

public class Candidate {
	
	Entity entity = null;
	
	public HashSet<String> persons = null;
	public HashSet<String> places = null;
	public HashSet<String> organizations = null;
	
	public float personsIntersction;
	public float organizationsIntersction;
	public float placesIntersction;
	
	public Features features;
	
	public Candidate(org.apache.lucene.document.Document doc) {
		entity = new Entity();
		entity.id = doc.getField("id").stringValue();			
		entity.type = doc.getField("type").stringValue();
		entity.name = doc.getField("name").stringValue();
		entity.wiki_text = doc.getField("wiki_text").stringValue();
		
		this.persons = new HashSet<String>();
		this.places = new HashSet<String>();
		this.organizations = new HashSet<String>();
		this.features = new Features();
	}
	
	public void getNamedEntities() throws Exception {
		
		List<Triple<String, Integer, Integer>> entities = tac.kbp.utils.Definitions.classifier.classifyToCharacterOffsets(entity.getWiki_text());
		
		for (Triple<String, Integer, Integer> triple : entities) {			
			String ename = entity.getWiki_text().substring(triple.second,triple.third);
			addEntitiesToCandidate(ename, triple.first);
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
		
		this.features.similarities = tac.kbp.utils.StringSimilarities.compareStrings(query,this.entity.name);
		
		features.exactMatch = query.equalsIgnoreCase(entity.name);
		features.querySubStringOfCandidate = entity.name.toLowerCase().contains(query.toLowerCase());
		features.candidateSubStringOfQuery = query.toLowerCase().contains(entity.name.toLowerCase());		
		features.queryStartsCandidateName = query.toLowerCase().startsWith(entity.name.toLowerCase());
		features.queryEndsCandidateName = query.toLowerCase().endsWith(entity.name.toLowerCase());		
		features.candidateNameStartsQuery = query.toLowerCase().startsWith(entity.name);		
		features.candidateNameEndsQuery = query.toLowerCase().endsWith(entity.name);
		
		if (tac.kbp.utils.StringUtils.isUpper(query)) {			
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

	public void semanticFeatures(KBPQuery q) {
		
		features.queryStringInWikiText = queryStringInWikiText(q);
		features.candidateNameInSupportDocument = candidateNameInSupportDocument(q);
		
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
}
