package tac.kbp.queries;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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

import redis.clients.jedis.BinaryJedis;
import tac.kbp.bin.Definitions;
import tac.kbp.kb.index.spellchecker.SuggestWord;
import tac.kbp.queries.candidates.Candidate;
import tac.kbp.utils.string.Abbreviations;
import tac.kbp.utils.string.ExtractAbbrev;
import edu.stanford.nlp.util.Triple;

public class KBPQuery {
	
	public String query_id;
	public String name;
	public String docid;
	public String gold_answer;
	public String supportDocument = new String();
	
	// alternative senses for the string name
	public HashSet<String> alternative_names;
	public Vector<Abbreviations> abbreviations;
	
	// named-entities in the support document
	public HashSet<String> persons;
	public HashSet<String> places;
	public HashSet<String> organizations;
	
	// topic-distribution associated with the support document
	public double[] topics_distribution = new double[100];
	public int highest_topic;
	
	// list of candidates retrieved from the Knowledge-Base
	public HashSet<Candidate> candidates;
	
	// ranked-list of candidates
	public ArrayList<Candidate> candidatesRanked;
	
	// list of suggested words from query
	public List<SuggestWord> suggestedwords;

	
	
	// Constructors	
	public KBPQuery() {
		super();
		this.candidates = new HashSet<Candidate>();
	}
	
	public KBPQuery(String query_id) {
		super();
		this.query_id = query_id;
		this.candidates = new HashSet<Candidate>();
	}
	
	public KBPQuery(String query_id, String name, String docid) {
		super();
		this.query_id = query_id;
		this.name = name;
		this.docid = docid;
		this.gold_answer = null;
		this.alternative_names = new HashSet<String>();
		this.abbreviations = new Vector<Abbreviations>();
		this.candidates = new HashSet<Candidate>(); 
		
		this.persons = new HashSet<String>();
		this.places = new HashSet<String>();
		this.organizations = new HashSet<String>();
	}

	
	
	// Methods
	// Reciprocal rank
	public float reciprocalRank(){
		float reciprocalrank = 0;
		for (int i = 0; i < candidatesRanked.size(); i++) {
			if (candidatesRanked.get(i).entity.id.equalsIgnoreCase(this.gold_answer)) {
				reciprocalrank =  (float) 1 / (float) (i+1);
				break;
			}
		}
		return reciprocalrank;
		
	}
	
	@SuppressWarnings("unchecked")
	public void getNamedEntities() throws Exception {
		getSupportDocument();
		
		List<Triple<String, Integer, Integer>> entities = tac.kbp.bin.Definitions.classifier.classifyToCharacterOffsets(supportDocument);
		
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
	
	public void getTopicsDistribution(int index) {
		
		String line = Definitions.queries_topics.get(index);
		String[] topics = line.split(" ");
		
		int max_topic = 0;
		double max_value = 0;
		
		for (int i = 0; i < topics.length ; i++) {
			topics_distribution[i] = Double.parseDouble(topics[i]);
			if (topics_distribution[i] > max_value) {
				max_topic = i;
				max_value = topics_distribution[i];
			}
		}
		
		this.highest_topic = max_topic;
	}
	
	public void getSupportDocument() throws IOException {        
        getSupportDocument(this);
	}
	
	public void getSupportDocument(KBPQuery q) throws IOException {
        Term t = new Term("docid", this.docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.documents.doc(scoredocs[0].doc);        
        this.supportDocument = doc.get("text");
	}
	
	public void getSupportDocumentTerms() throws IOException {
		Map<String, Integer> wordsQ = tac.kbp.utils.string.StringUtils.tokenized(this.supportDocument);
		
		Set<String> keys = wordsQ.keySet();
		System.out.println("number of terms: " + keys.size());
		
		for (String string : keys) {
			System.out.println(string + '\t' + wordsQ.get(string));
		}
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
	
			input = new BufferedReader(new FileReader(tac.kbp.bin.Definitions.named_entities_supportDoc+"/"+this.query_id+"-named-entities.txt"));
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
	
	public void getAlternativeSenses(BinaryJedis binaryjedis) throws UnsupportedEncodingException {
				
		/* Consult alternative senses dictionary */
		
		byte[] queryStringbyteArray = this.name.getBytes("UTF-8");
		byte[] queryStringLowbyteArray = this.name.toLowerCase().getBytes("UTF-8");
			
		byte[] acronyms = binaryjedis.get(queryStringLowbyteArray);
		byte[] senses = binaryjedis.get(queryStringbyteArray);
			
		if (acronyms != null) {						
			String acr = new String(acronyms, "UTF8");
			String[] acronymsArray = acr.split(",\\s");
				
			for (int i = 0; i < acronymsArray.length; i++) {
				String cleaned = tac.kbp.utils.string.StringUtils.cleanString(acronymsArray[i]);
				if (cleaned.compareToIgnoreCase(this.name) != 0) {
					this.alternative_names.add(cleaned);
				}
										
			}
		}
			
		if (senses != null) {
			String ses = new String(senses, "UTF8");
			String[] sensesArray = ses.split(",\\s");
			for (int i = 0; i < sensesArray.length; i++) {
				String cleaned = tac.kbp.utils.string.StringUtils.cleanString(sensesArray[i]);			
				if (cleaned.compareToIgnoreCase(this.name) != 0) {
					this.alternative_names.add(cleaned);
				}		
			}
		}
	}

	
	// builds a query to Lucene with:
	// 	query name string of query: "Cedar Rapids"
	//	individual tokens part of the query name string: "Cedar" + "Rapids"
	//  the same thing as above for each alternative name
	public HashMap<String, HashSet<String>> generateQuery() {
		
		HashSet<String> queryStrings = new HashSet<String>(); 		
		HashSet<String> queryTokens = new HashSet<String>();
			
		HashMap<String, HashSet<String>> query = new HashMap<String,HashSet<String>>();
		
		queryStrings.add('"' + this.name + '"');
		
		String[] tmp = this.name.split("\\s");
		for (int z = 0; z < tmp.length; z++) {
			if (!tmp[z].matches("\\s*-\\s*|.*\\!|\\!.*|.*\\:|\\:.*|\\s*")) {
				queryTokens.add('"' + tmp[z] + '"');
			}
		}
		
		for (Iterator<String> iterator = this.alternative_names.iterator(); iterator.hasNext();) {
			String alternative = (String) iterator.next();
			
			String queryParsed = alternative.replaceAll("\\(", "").replaceAll("\\)","").replaceAll("\\]", "").replaceAll("\\[", "").replaceAll("\"", "");			
			queryStrings.add('"' + queryParsed + '"');
			
			String[] tokens = queryParsed.split("\\s");
			
			for (int i = 0; i < tokens.length; i++) {
				if (!tokens[i].matches("\\s*-\\s*|.*\\!|\\!.*|.*\\:|\\:.*|\\s*")) {
					queryTokens.add('"' + tokens[i].trim() + '"');
				}
			}

		}
		
		query.put("strings", queryStrings);
		query.put("tokens", queryTokens);
				
		return query;
		
	}

}