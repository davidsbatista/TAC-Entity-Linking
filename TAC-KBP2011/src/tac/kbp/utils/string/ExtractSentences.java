package tac.kbp.utils.string;

import java.util.Set;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

public class ExtractSentences {
				
	final static TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
	final static SentenceModel SENTENCE_MODEL = new MedlineSentenceModel();
	final static SentenceChunker SENTENCE_CHUNKER = new SentenceChunker(TOKENIZER_FACTORY, SENTENCE_MODEL);
		
	public static Set<Chunk> extractSentences(String document) {
			
		Chunking chunking = SENTENCE_CHUNKER.chunk(document.toCharArray(),0,document.length());
		Set<Chunk> sentences = chunking.chunkSet();
		
		return sentences;
	}
}