package tac.kbp.queries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tac.kbp.utils.Definitions;
import edu.stanford.nlp.util.Triple;

public class KBPQuery {
	
	public String query_id;
	public String name;
	public String docid;
	public String answer_kb_id;
	public String supportDocument = new String();
	
	// alternative senses for the string name
	public HashSet<String> alternative_names;	
	
	// named-entities in the support document
	public HashSet<String> persons;
	public HashSet<String> places;
	public HashSet<String> organizations;
	
	// topic-distribution associated with the support document
	public double[] topics_distribution = new double[100];
	
	// list of candidates retrieved from the Knowledge-Base
	public HashSet<Candidate> candidates;
	
	// ranked-list of candidates
	public ArrayList<Candidate> candidatesRanked;
	
	public KBPQuery(String query_id, String name, String docid) {
		super();
		this.query_id = query_id;
		this.name = name;
		this.docid = docid;
		this.answer_kb_id = null;
		this.alternative_names = new HashSet<String>();
		this.candidates = new HashSet<Candidate>();  
		
		this.persons = new HashSet<String>();
		this.places = new HashSet<String>();
		this.organizations = new HashSet<String>();
	}
	
	public void getNamedEntities() throws Exception {
		getSupportDocument();
		
		List<Triple<String, Integer, Integer>> entities = tac.kbp.utils.Definitions.classifier.classifyToCharacterOffsets(supportDocument);
		
		for (Triple<String, Integer, Integer> triple : entities) {			
			String ename = this.supportDocument.substring(triple.second,triple.third);
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
	
	public void loadNamedEntitiesXML() throws ParserConfigurationException, SAXException, IOException {
		
		String filename = tac.kbp.utils.Definitions.named_entities_supportDoc+"/"+this.query_id+"-CRF-named-entities.xml";
		
		File fXmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		org.w3c.dom.Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
 
		NodeList persons = doc.getElementsByTagName("PERSON");
		NodeList organizations = doc.getElementsByTagName("ORGANIZATION");
		NodeList locations = doc.getElementsByTagName("LOCATION");
		
		addEntitiesToQuery(persons, "PERSON");
		addEntitiesToQuery(organizations, "ORGANIZATION");
		addEntitiesToQuery(locations, "LOCATION");
	}
	
	public void getTopicsDistribution(int index) {
		
		String line = Definitions.queries_topics.get(index);
		String[] topics = line.split(" ");
		
		for (int i = 0; i < topics.length ; i++) {
			topics_distribution[i] = Double.parseDouble(topics[i]);	
		}
	}
	
	public void getSupportDocument() throws IOException {        
        getSupportDocument(this);
	}
	
	public void getSupportDocument(KBPQuery q) throws IOException {
        Term t = new Term("docid", this.docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = tac.kbp.utils.Definitions.documents.doc(scoredocs[0].doc);        
        this.supportDocument = doc.get("text");
	}
	
	public void addEntitiesToQuery(NodeList nodeList, String tag) {
		
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

	public void loadNamedEntities() throws IOException{
		
		BufferedReader input;

		try {
	
			input = new BufferedReader(new FileReader(tac.kbp.utils.Definitions.named_entities_supportDoc+"/"+this.query_id+"-named-entities.txt"));
			String line = null;
			boolean persons = false;
			boolean org = false;
			boolean place = false;
	        
			while (( line = input.readLine()) != null){
				if (line.equalsIgnoreCase("")) {
					continue;
				}
				
				if (line.equalsIgnoreCase("PERSONS:")) {
					persons = true;
					org = false;
					place = false;
					continue;
				}
				
				if (line.equalsIgnoreCase("PLACES:")) {
					org = true;
					persons = false;
					place = false;
					continue;
				}
				
				if (line.equalsIgnoreCase("ORGANIZATIONS:")) {
					place = true;
					persons = false;
					org = false;
					continue;
				}
				
				if (place)
					this.places.add( '"' + line.trim() + '"');
				
				if (org)
					this.organizations.add('"' + line.trim() + '"');
				
				if (persons)
					this.persons.add('"' + line.trim() + '"');

	        }
	        
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();    	
		}
	}
}