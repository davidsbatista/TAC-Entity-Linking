/**
 * @author dsbatista
 * 
 */

package tac.kbp.index;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.*;

public class CreateIndex {

	public static void main(String[] args) throws CorruptIndexException,
			LockObtainFailedException, IOException, ParseException {

		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

		// Store the index in memory:
		Directory directory = new RAMDirectory();

		// To store an index on disk, use this instead:
		// Directory directory = FSDirectory.open("/tmp/testindex");

		IndexWriter iwriter = new IndexWriter(directory, analyzer, true,
				new IndexWriter.MaxFieldLength(25000));
		Document doc = new Document();
		String text = "This is the text to be indexed.";
		doc.add(new Field("fieldname", text, Field.Store.YES,
				Field.Index.ANALYZED));
		iwriter.addDocument(doc);
		iwriter.close();

		/*
		 * // Now search the index: IndexSearcher isearcher = new
		 * IndexSearcher(directory, true); // read-only=true // Parse a simple
		 * query that searches for "text":
		 * 
		 * @SuppressWarnings("deprecation") QueryParser parser = new
		 * QueryParser("fieldname", analyzer); Query query =
		 * parser.parse("text"); ScoreDoc[] hits = isearcher.search(query, null,
		 * 1000).scoreDocs;
		 * 
		 * isearcher.close(); directory.close();
		 */

	}
}
