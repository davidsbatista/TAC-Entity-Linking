package tac.kbp.collection.index;

/**
 * @author dsbatista
 * 
 */
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;


public class Index {

	public static IndexWriter createIndex(String dir) throws CorruptIndexException, LockObtainFailedException, IOException {
		
		IndexWriter indexDir = new IndexWriter(FSDirectory.open(new File(dir)), new WhitespaceAnalyzer(),IndexWriter.MaxFieldLength.UNLIMITED);	
		
		return indexDir;
	}

	
	public static void indexEntities(IndexWriter index, Article a) throws CorruptIndexException, IOException{
			
		Document doc = new Document();
		
		//genre
		doc.add(new Field("genre", a.getGenre(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		//text
		doc.add(new Field("text", a.getText(), Field.Store.YES, Field.Index.ANALYZED));
		
		//id
		doc.add(new Field("docid", a.getDoc_id(), Field.Store.YES, Field.Index.ANALYZED));
		
		index.addDocument(doc);
	}
}

