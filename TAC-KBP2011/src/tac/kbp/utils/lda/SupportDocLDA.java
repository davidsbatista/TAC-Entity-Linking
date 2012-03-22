package tac.kbp.utils.lda;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import tac.kbp.entitylinking.bin.Definitions;
import tac.kbp.entitylinking.queries.KBPQuery;


public class SupportDocLDA {
	
	public static EnglishPorterStemmer stemmer = new EnglishPorterStemmer();
	public static int num_docs = 0;
	
	public static void process(String queries, String stopwords, String dcIndex, String outputFile) throws Exception {
		
		//TODO: this method needs to created this Definitions
		//tac.kbp.bin.Definitions.loadAll(queries);
		
		System.out.println(tac.kbp.entitylinking.bin.Definitions.queriesTrain.size() + " queries loaded");
		System.out.println(tac.kbp.entitylinking.bin.Definitions.stop_words.size() + " stopwords");

		//use buffering
		File aFile = new File(outputFile);					
		Writer output = new BufferedWriter(new FileWriter(aFile));
		
		System.out.println("parsing support documents...");
		
		for (KBPQuery query : tac.kbp.entitylinking.bin.Definitions.queriesTrain) {
			query.getSupportDocument(query);			
			String text_parsed = parse(query.supportDocument);
			output.write( text_parsed + "\n");
			num_docs++;
		}
		output.close();
		System.out.println(num_docs + " docs parsed");
		
	}
	
	public void getSupportDocument(KBPQuery q) throws IOException {
        Term t = new Term("docid", q.docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = tac.kbp.entitylinking.bin.Definitions.documents.doc(scoredocs[0].doc);        
        q.supportDocument = doc.get("text");
	}
	
	public static String parse(String text) {
		
		String text_no_tags = tac.kbp.utils.string.StringUtils.removeTags(text);
		String text_no_new_line = text_no_tags.replaceAll("\\n", " ");
		String text_no_stopwords = removeStopWords(text_no_new_line);
		String text_stemmed = stemm(text_no_stopwords);		
		return text_stemmed;
	}

	public static String removeStopWords(String text) {
		
		StringBuffer words_only = new StringBuffer();				
		String text_cleaned = text.replaceAll("[^\\w|^\\s|[0-9]]", "");				
		String[] words = text_cleaned.split("[\\s]");
		
		for (int i = 0; i < words.length; i++) {
			if (!Definitions.stop_words.contains(words[i].toLowerCase()) && words[i].length()>0) {
				words_only.append(words[i].toLowerCase()+" ");
			}
		}
		
		return words_only.toString();		
	}
	
	public static String stemm(String word) {
		
		word.toLowerCase();
		stemmer.add(word.toCharArray(), word.toCharArray().length);
		stemmer.stem();
				
		return stemmer.toString();	
	}
	
}
