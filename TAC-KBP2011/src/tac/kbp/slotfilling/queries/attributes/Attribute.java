package tac.kbp.slotfilling.queries.attributes;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

public class Attribute {
	
	public String answer_doc;
	public String slot_name;
	public int start_char;
	public int end_char;
	public List<String> answer;
	public List<String> normalized_answer;
	public List<String> sentence_with_answers; 
	
	public Attribute() {
		super();
		this.answer_doc = null;
		this.answer = new LinkedList<String>();
		this.sentence_with_answers = new LinkedList<String>();
		this.normalized_answer = new LinkedList<String>();
	}
	
	public void extractSentences() {
		
		TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
		SentenceModel SENTENCE_MODEL = new MedlineSentenceModel();
		SentenceChunker SENTENCE_CHUNKER = new SentenceChunker(TOKENIZER_FACTORY, SENTENCE_MODEL);
					
		Chunking chunking = SENTENCE_CHUNKER.chunk(answer_doc.toCharArray(),0,answer_doc.length());
		Set<Chunk> sentences = chunking.chunkSet();
		String slice = chunking.charSequence().toString();
		
		//System.out.println("answer: " + start_char + '\t' + end_char + '\t' + this.answer.get(0));
		
		int i = 1;
		for (Chunk sentence : sentences) {			
			int start = sentence.start();
			int end = sentence.end();		
			String answer_sentence = slice.substring(start,end);
			
			String sentence_answer = slice.substring(start_char,end_char);
			System.out.println(sentence_answer);
			
			/*
			if ( (start_char>=start) && (start_char<=end) && 
				 (end_char>=start) && (end_char<=end) && (answer_sentence.contains(answer.get(0))) ) {
				
				System.out.println("slot_name: " + this.slot_name);
				System.out.println("answer: " + start_char + '\t' + end_char + '\t' + this.answer.get(0));
				System.out.println("sentence boundings: " + start + '\t' + end );				
				System.out.println("sentence:" + slice.substring(start,end).replaceAll("\n", " "));
				System.out.println();
			}
			*/
		}
	}


	public void extractSentencewithAnswer() {
		
		String answer_doc_normalized = answer_doc.replaceAll("\n", " ").replaceAll("\t", " ");
		
		System.out.println("normalized");
		System.out.println(answer_doc_normalized);
		
		System.out.println("unnormalized");
		System.out.println(answer_doc);
		
		System.out.println("document lenght: " + answer_doc.length());
		System.out.println("slot_name: \t" + slot_name);
		System.out.println("answer: \t" + answer.get(0));
		System.out.println("start_char: \t" + start_char);
		System.out.println("end_char: \t" + end_char);
		System.out.println("sentence extracted: \t" + answer_doc.substring(start_char+33, end_char-33));
		
		System.out.println();
	}
}
