package tac.kbp.utils.lda;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import tac.kbp.queries.KBPQuery;
import tac.kbp.utils.Definitions;

public class SupportDocLDA {
	
	public static List<KBPQuery> queries = new LinkedList<KBPQuery>();
	public static EnglishPorterStemmer stemmer = new EnglishPorterStemmer();
	public static int num_docs = 0;
	
	public static void main(String[] args) throws Exception {
		
		tac.kbp.utils.Definitions.loadAll(args[0], args[1]);

		System.out.println(tac.kbp.utils.Definitions.queries.size() + " queries loaded");
		System.out.println(tac.kbp.utils.Definitions.docslocations.size() + " documents locations loaded");

		//use buffering
		File aFile = new File(args[2]);					
		Writer output = new BufferedWriter(new FileWriter(aFile));
		
		for (KBPQuery query : queries) {
			query.getSupportDocument(query, tac.kbp.utils.Definitions.docslocations);
			String text_parsed = parse(query.supportDocument);
			System.out.println(text_parsed);
			output.write( text_parsed + "\n");
			num_docs++;
		}
		
		//command = "/home/dsbatista/GibbsLDA++-0.2/src/lda"
		//args = " -inf -dir /collections/TAC-2011/lda_trained_model/ -model model-final -niters 20 -dfile " + query.id+"/"+query.doc_id+'_lda_format'
		output.close();
		
		System.out.println(num_docs + " docs parsed");
	}
	
	public static String parse(String text) {
		
		String text_no_new_line = text.replaceAll("\\n", " ");
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
