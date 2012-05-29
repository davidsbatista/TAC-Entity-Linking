package tac.kbp.slotfilling.queries;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import tac.kbp.slotfilling.configuration.Definitions;
import tac.kbp.slotfilling.queries.attributes.Attributes;
import tac.kbp.utils.string.Abbreviations;
import tac.kbp.utils.string.ExtractAbbrev;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import edu.stanford.nlp.util.Triple;

public class SFQuery {

	/* query information */
	public String query_id;
	public String name;
	public String docid;
	public String etype;
	public String nodeid;
	public String ignore;
	public String supportDocument;
	public Attributes attributes;
	
	/* query mined information */
	public Set<String> persons;
	public Set<String> places;
	public Set<String> organizations;
	public Set<Chunk> sentences;
	
	public HashSet<String> alternative_names;
	public Vector<Abbreviations> abbreviations;
	public Set<Document> documents; 				/* documents retrived, which hold the answers to the slots */
	
	/* query answers */	
	public LinkedList<HashMap<String, String>> answers;

	public SFQuery() {
		super();
	}
	
	public SFQuery(String query_id, String name, String docid, String etype, String nodeid) {
		
		super();
		this.query_id = query_id;
		this.name = name;
		this.docid = docid;
		this.etype = etype;
		this.nodeid = nodeid;
		
		/* other named-entities in the support document */
		this.persons = new HashSet<String>();
		this.places = new HashSet<String>();
		this.organizations = new HashSet<String>();
		
		this.alternative_names = new HashSet<String>();
		this.abbreviations = new Vector<Abbreviations>();
		this.documents = new HashSet<Document>();
		this.answers = new LinkedList<HashMap<String,String>>();
	}
	
	
	public void getSupportDocument() throws IOException {
        Term t = new Term("docid", this.docid); 
        Query query = new TermQuery(t);
        TopDocs docs = Definitions.documents.search(query, 1);        
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.documents.doc(scoredocs[0].doc);        
        this.supportDocument = doc.get("text");
	}
	
	public Document getKBEntry() throws IOException {        
		Term t = new Term("id", this.nodeid);         
		Query query = new TermQuery(t);		
        TopDocs docs = Definitions.knowledge_base.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.knowledge_base.doc(scoredocs[0].doc);        
        return doc;
	}
	
	public void extractSentences(){
		
		final TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
		final SentenceModel SENTENCE_MODEL = new MedlineSentenceModel();
		final SentenceChunker SENTENCE_CHUNKER = new SentenceChunker(TOKENIZER_FACTORY, SENTENCE_MODEL);
		
		Chunking chunking = SENTENCE_CHUNKER.chunk(supportDocument.toCharArray(),0,supportDocument.length());
		sentences = chunking.chunkSet();
		
		/*
		String slice = chunking.charSequence().toString();
		
		int i = 1;
		for (Chunk sentence : sentences) {
		    int start = sentence.start();
		    int end = sentence.end();
		    System.out.println("SENTENCE "+(i++)+":");
		    System.out.println(slice.substring(start,end));
		   
		}
		*/
	}
	
	public void getNamedEntities() throws Exception {
		
		List<Triple<String, Integer, Integer>> entities = Definitions.classifier.classifyToCharacterOffsets(supportDocument);
		
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
	
	public void getAlternativeSenses() throws UnsupportedEncodingException {
		
		for (String	e: Definitions.jedis.smembers(this.name.toLowerCase())) {
	    	this.alternative_names.add(e.toLowerCase());
	    }		
	}
	
	public void extracAcronyms() {
		
		/* Schwartz and Hirst abbreviations and acronyms extractor*/
		ExtractAbbrev extractAbbrv =  new ExtractAbbrev();
		this.abbreviations = extractAbbrv.extractAbbrPairs(this.supportDocument);			
		
		boolean acronym = true;
		
		for (int j = 0; j < this.name.length(); j++) {
			if (Character.isLowerCase(this.name.charAt(j))) {
				acronym = false;
			}
		}
		
		if (acronym) {
			 for (Abbreviations abbreviation : this.abbreviations) {					
				if (abbreviation.getShortForm().equalsIgnoreCase(this.name)) {
					this.alternative_names.add(abbreviation.getLongForm());
				}
			}
		}
		
	}
	
	public void queryCollection() throws ParseException, IOException {
		//query document collection
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_35);
		QueryParser parser = new QueryParser(Version.LUCENE_35, "text", analyzer);
		
		// create the query
		//Joiner orJoiner = Joiner.on(" OR ");
		String terms = '"' + name + '"' + " ";
		
		//terms += orJoiner.join(q.alternative_names);
		//terms += orJoiner.join(q.abbreviations);
		//terms.replaceAll("\\_", " ");
		
		Query query = parser.parse(terms);			
		//System.out.println(query);
		
	    TopDocs docs = Definitions.documents.search(query, 50);
	    ScoreDoc[] scoredocs = docs.scoreDocs;
	    
	    //System.out.println("documents returned: " + docs.totalHits);	        
	    //System.out.println("documents searched: " + scoredocs.length);
	    //System.out.println("");
	    
	    for (int i = 0; i < scoredocs.length; i++) {
			Document doc = Definitions.documents.doc(scoredocs[i].doc);
			documents.add(doc);
	    }
	}

	/* methods to be used for trainning queries */
	
	public void getAnswerDocument() throws IOException {
        Term t = new Term("docid", this.docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.documents.doc(scoredocs[0].doc);        
        this.supportDocument = doc.get("text");
	}
	
}
