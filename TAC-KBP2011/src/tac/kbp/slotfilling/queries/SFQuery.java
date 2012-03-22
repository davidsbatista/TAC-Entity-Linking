package tac.kbp.slotfilling.queries;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import tac.kbp.configuration.Definitions;
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
	
	public String query_id;
	public String name;
	public String docid;
	public String etype;
	public String nodeid;
	public String ignore;
	public String supportDocument;
	
	public Set<String> persons;
	public Set<String> places;
	public Set<String> organizations;
	
	public HashSet<String> alternative_names;
	public Vector<Abbreviations> abbreviations;
	
	public Set<Chunk> sentences;

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
	}
	
	public void getSupportDocument() throws IOException {
        Term t = new Term("docid", this.docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.documents.doc(scoredocs[0].doc);        
        this.supportDocument = doc.get("text");
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
	
}
