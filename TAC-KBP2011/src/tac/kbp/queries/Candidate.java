package tac.kbp.queries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tac.kbp.kb.index.xml.Entity;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;

public class Candidate {
	
	Entity entity = null;
	
	public HashSet<String> persons = null;
	public HashSet<String> places = null;
	public HashSet<String> organizations = null;

	public float diceSimilarity;
	public float jaccardSimilarity;
	public float jaro;
	public float jaroWinkler;
	public float levenshtein;
	
	public Candidate(org.apache.lucene.document.Document doc) {
		entity = new Entity();
		entity.id = doc.getField("id").stringValue();			
		entity.type = doc.getField("type").stringValue();
		entity.name = doc.getField("name").stringValue();
		entity.wiki_text = doc.getField("wiki_text").stringValue();
		
		this.persons = new HashSet<String>();
		this.places = new HashSet<String>();
		this.organizations = new HashSet<String>();
	}

	public void getCandidateData() {
		
	}
	
	public void getNamedEntities(AbstractSequenceClassifier classifier) throws Exception {
		
		String wiki_text_classified = classifier.classifyWithInlineXML(entity.getWiki_text());
		Document doc = tac.kbp.utils.XMLUtils.loadXMLFromString(wiki_text_classified);
		
		NodeList persons = doc.getElementsByTagName("PERSON");
		NodeList organizations = doc.getElementsByTagName("ORGANIZATION");
		NodeList locations = doc.getElementsByTagName("LOCATION");
		
		addEntitiesToCandidate(persons, "PERSONS");
		addEntitiesToCandidate(organizations, "PERSONS");
		addEntitiesToCandidate(locations, "PERSONS");
	}
	
	public void addEntitiesToCandidate(NodeList nodeList, String tag) {
		
		for (int temp = 0; temp < nodeList.getLength(); temp++) { 
		   Node nNode = nodeList.item(temp);
		   String name = nNode.getTextContent();	
		   
		   String entity = name.replace("*", "").replace("\n", " ").replace("!", "");
		   
		   if (entity.length()>1) {
			   
			   if (tag.equalsIgnoreCase("PERSON"))
				   this.persons.add('"' + entity.trim() + '"');
			      
			   if (tag.equalsIgnoreCase("ORGANIZATION"))
				   this.organizations.add('"' + entity.trim() + '"');
			      
			   if (tag.equalsIgnoreCase("LOCATION"))
				   this.places.add('"' + entity.trim() + '"');
		   }
		}
	}
		
	public void nameSimilarities(String query) {
		
		HashMap<String, Float> similarities = tac.kbp.utils.StringSimilarities.compareStrings(query,this.entity.name);
		Set<String> keys = similarities.keySet();
								
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String key = iterator.next();
			System.out.println(key + ":" + similarities.get(key));					
		}
	}
}