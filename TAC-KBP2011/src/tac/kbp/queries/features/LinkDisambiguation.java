package tac.kbp.queries.features;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

// Out-Degree
// N = set of names of entities mentioned in the context g
// C = set of articles of candidates from the Knowledge Base g
// Search for each context name n in N in the article of each candidate c in C, if it's found an edge is made from c to n

// In-Degree
// N = f names of candidates from the Knowledge-Base g
// C = f articles of entities mentioned in the context g
// Search for each candidate name string n in N in the article of each context name c in C, if it's found an edge is made from c to n


public class LinkDisambiguation {

    static final double CHUNK_SCORE = 1.0;

    public static ExactDictionaryChunker buildDictionary ( String path ) throws IOException {
    	
    	System.out.println("Loading dictionary...");
    	
    	BufferedReader input = new BufferedReader( new FileReader(path) );
    	MapDictionary<String> dictionary = new MapDictionary<String>();	
    	String aux = null;
    	
		while ((aux=input.readLine())!=null) {
			// TODO : remove the two lines below if reading from a proper dictionary
			if (aux.length()==0)
				continue;			
			aux = aux.split("\\s")[2].split("wiki_title:")[1];
			aux.replaceAll("([A-Z])"," $1").trim();
			
			String str = new String(aux.replace("_"," "));
			String clas = new String(aux);
	        dictionary.addEntry(new DictionaryEntry<String>(str,clas,CHUNK_SCORE));
		}
		
        ExactDictionaryChunker dictionaryChunkerTT = new ExactDictionaryChunker(dictionary,
                                                         IndoEuropeanTokenizerFactory.INSTANCE,
                                                         true,true);
	
        System.out.println("Dictionary contains " + dictionary.size() + " entries.");
        
        return dictionaryChunkerTT;
    }

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

    
    public static String getWikiText ( String pageID ) {
    	// TODO : get Wiki Text
    	return pageID.replace("_"," ");
    }

    public static double getScore ( ExactDictionaryChunker chunker, String queryContext , String candidateContext ) {
		String pagesQuery[] = chunk(chunker,queryContext);
		String pagesCandidate[] = chunk(chunker,candidateContext);
		int cnt1 = 0, cnt2 = 0;
		for ( String s1 : pagesCandidate ) for ( String s2 : pagesQuery ) if ( s2.equals(s1) ) cnt1++;
		for ( String s : pagesQuery ) {
			String aux[] = chunk(chunker,getWikiText(s));
			for ( String s1 : aux ) for ( String s2 : pagesQuery ) if ( s2.equals(s1) ) cnt2++;
		}
		// TODO : Return just one metric or normalize before doing the combination
		return cnt1 + cnt2;
    }

    public static Map<String,Double> getScore ( ExactDictionaryChunker chunker, String queryContext , String[] candidateContext ) {
		Map<String,Double> scores = new HashMap<String,Double>();
		for ( String c : candidateContext ) scores.put(c,getScore(chunker,queryContext,c));
		return scores;
    }

    public static void main(String[] args) throws Exception {
		ExactDictionaryChunker chunker = buildDictionary ( args[0] );
		// TODO : Serializar o chunker, para arrancar mais rápido da próxima vez
		String queryContext = "The best type of government is Anarchism, but people have Amnesia.";
		String candidateContext = "Anarchism is a type of government.";
		System.out.println( "Score: " + getScore(chunker,queryContext,candidateContext));
    }

}
