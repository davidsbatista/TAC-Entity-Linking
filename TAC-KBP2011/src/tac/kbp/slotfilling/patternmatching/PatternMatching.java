package tac.kbp.slotfilling.patternmatching;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import tac.kbp.slotfilling.queries.SFQuery;
import tac.kbp.slotfilling.queries.attributes.Attribute;
import tac.kbp.slotfilling.queries.attributes.ORG_Attributes;
import tac.kbp.slotfilling.queries.attributes.PER_Attributes;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class PatternMatching {
	
	static TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
	static SentenceModel SENTENCE_MODEL = new MedlineSentenceModel();
	static SentenceChunker SENTENCE_CHUNKER = new SentenceChunker(TOKENIZER_FACTORY, SENTENCE_MODEL);
	
	//collect query-answers pairs from the training queries
	//and gather sentences from the document collection  where query and answer co-occur or answers occurs
	public static Multimap<String, Pattern> qaPairs(Map<String, SFQuery> train_queries) {
		
		Multimap<String, Pattern> patterns = HashMultimap.create();
		Set<String> qkeys = train_queries.keySet();
		
		int sentences_with_q_and_r = 0;
		int sentences_with_r = 0;
		int sentences_with_answer = 0;
		
		for (String qkey : qkeys) {
			
			SFQuery q = train_queries.get(qkey);
						
			Set<String> keys = null;
			HashMap<String, Attribute> attributes = null;
			
			if (q.etype.equalsIgnoreCase("PER")) {
				attributes = ((PER_Attributes) q.attributes).attributes;
				keys = ((PER_Attributes) q.attributes).attributes.keySet();
				
			}				
			else if (q.etype.equalsIgnoreCase("ORG")){
				attributes = ((ORG_Attributes) q.attributes).attributes;
				keys = ((ORG_Attributes) q.attributes).attributes.keySet();
			}
			
			/*
			System.out.println(q.query_id);
			System.out.println(q.etype);
			System.out.println(q.name);
			*/
			
			for (String k : keys) {
				
				if (attributes.get(k).sentence_with_answers.size()>0) {
					sentences_with_answer++;
					
					String response = attributes.get(k).response.get(0);
					String text = attributes.get(k).sentence_with_answers.get(0).trim();
					
					Chunking chunking = SENTENCE_CHUNKER.chunk(text.toCharArray(),0,text.length());
					Set<Chunk> sentences = chunking.chunkSet();
					String slice = chunking.charSequence().toString();
					
					for (Chunk sentence : sentences) {
						
						int start = sentence.start();
						int end = sentence.end();
						String s = slice.substring(start,end);
						
						if (s.contains(response) && s.contains(q.name)) {
							Pattern p = new Pattern(k, q.name, response, s);
							sentences_with_q_and_r++;
							patterns.put(k, p);
						}
							
						
						if (s.contains(response)) {
							Pattern p = new Pattern(k, q.name, response, s);
							sentences_with_r++;
							patterns.put(k, p);
						}
					}
				}
			}
		}
		
		System.out.println("sentences identified with answers: " + sentences_with_answer);
		System.out.println("sentences with query name and answer: " + sentences_with_q_and_r);
		System.out.println("sentences with response: " + sentences_with_r);
		
		return patterns;
	}
	
	public static void parsePatterns(Multimap<String, Pattern> patterns) {
		Set<String> keys = patterns.keySet();
		
		for (String k : keys)
			System.out.println(k + '\t' + patterns.get(k).size());
		
		
		for (String k : keys) {
			
			System.out.println("sentences for slot: " + k);
			System.out.println();

			Collection<Pattern> p = patterns.get(k);
			
			for (Pattern pattern : p) {
				System.out.println("slot_name: "  + '\t' + pattern.slot_name);
				System.out.println("query name: " + '\t' + pattern.query_name);
				System.out.println("response: "   + '\t' + pattern.response_value);
				System.out.println(pattern.sentence);
				
				//TODO: tokenize sentence
				//		replace answer and value for ANSWER/VALUE
				//		extract pattern
				
				/*
				String str1 = StringUtils.replace(pattern.sentence, pattern.response_value.trim(), "ANSWER");
				String str2 = 	StringUtils.replace(str1, pattern.query_name.trim(), "TARGET");
				
				System.out.println(str2);
				*/
				
				System.out.println();
				System.out.println("===================================");
			}		
		}
	}	
}