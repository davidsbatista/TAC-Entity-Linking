package tac.kbp.queries.features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javassist.compiler.ast.Keyword;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import tac.kbp.bin.Definitions;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.ExactDictionaryChunker;

import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;

// Out-Degree
// N = set of names of entities mentioned in the context g
// C = set of articles of candidates from the Knowledge Base g
// Search for each context name n in N in the article of each candidate c in C, if it's found an edge is made from c to n

// In-Degree
// N = f names of candidates from the Knowledge-Base g
// C = f articles of entities mentioned in the context g
// Search for each candidate name string n in N in the article of each context name c in C, if it's found an edge is made from c to n


public class LinkDisambiguation {

    public static String[] chunk(ExactDictionaryChunker chunker, String[] texts) {
    	
    	List<String> types = new ArrayList<String>();
    	
    	for ( String text : texts ) {
    		for ( String type : chunk(chunker,text) ) types.add(type);
    	}
    	
	return types.toArray(new String[0]);
	
    }

    public static String[] chunk(ExactDictionaryChunker chunker, String text ) {
    	
        Chunking chunking = chunker.chunk(text);
        List<String> types = new ArrayList<String>();
        
        for (Chunk chunk : chunking.chunkSet()) {
            int start = chunk.start();
            int end = chunk.end();
            String type = chunk.type();
            double score = chunk.score();
            String phrase = text.substring(start,end);            
	    types.add(type);
	    
        }
        
        return types.toArray(new String[0]);
    }
    
    public static String getWikiText ( String pageID ) throws IOException, ParseException {
    	
    	KeywordAnalyzer analyzer = new KeywordAnalyzer();
		QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_35,"name",analyzer);
		
    	//String query = "entity:" + '"' + pageID.replaceAll("\"", "").replace("&amp;","&") + '"';
    	String query = "entity:" + '"' + pageID + '"'; //.replaceAll("\"", "").replace("&amp;","&") + 
    	
		TopDocs docs = tac.kbp.bin.Definitions.wikipediaEn.search(queryParser.parse(query), 1);
		Document doc = null;
		
		if (docs.totalHits == 0) {
			System.out.println("Error! " + query + " returned 0 documents");
			return " ";
		}
		else {
			doc = tac.kbp.bin.Definitions.wikipediaEn.doc(docs.scoreDocs[0].doc);
			return doc.getFieldable("wiki_text").stringValue();
		}
    }
    
    public static String getArticleText (String pageTitle) throws IOException, ParseException, WikiApiException {
    	Page p = null;    	
		try {
			
			p = Definitions.wiki.getPage(pageTitle);			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (p!=null)
			return p.getParsedPage().getText();
		else return null;
    }
    
    public static HashMap<String, Integer> getScore ( ExactDictionaryChunker chunker, String queryContext , String candidateWikiText ) throws IOException, ParseException, WikiApiException {
		
    	String pagesInContext[] = chunk(chunker,queryContext);
		String pagesInCandidateWikiText[] = chunk(chunker,candidateWikiText);
		
		int outDegree = 0, inDegree = 0;
		HashMap<String, Integer> scores = new HashMap<String, Integer>();

		//out-degree
		for ( String s1 : pagesInCandidateWikiText )			
			for ( String contextName : pagesInContext ) 
				if ( contextName.equals(s1) ) outDegree++;
		
		//in-degree
		for ( String queryContextName : pagesInContext ) {
			String entity = getWikiText(queryContextName);
			if (entity!=null) {
				String aux[] = chunk(chunker,entity);
				
				for ( String s1 : aux ) 
					for ( String s2 : pagesInContext ) 
						if ( s2.equals(s1) ) inDegree++;
			}
		}
		
		scores.put("outDegree", outDegree);
		scores.put("inDegree", inDegree);
		
		return scores;
    }
    
    /*
    public static Map<String,Double> getScore ( ExactDictionaryChunker chunker, String queryContext , String[] candidateContext ) throws IOException, ParseException {
		Map<String,Double> scores = new HashMap<String,Double>();
		for ( String c : candidateContext ) scores.put(c,getScore(chunker,queryContext,c));
		return scores;
    }
    */

}





