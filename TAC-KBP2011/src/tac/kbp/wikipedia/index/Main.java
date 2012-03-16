package tac.kbp.wikipedia.index;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;

public class Main {
	
	public static DatabaseConfiguration dbConfig;
	public static Wikipedia wiki;
	public static MediaWikiParserFactory pf;
	public static MediaWikiParser parser;	
	public static IndexWriter indexW;
	
	public static void main(String[] args) throws CorruptIndexException, IOException, WikiApiException {
		
		long start = new Date().getTime();
		
		//connects to JWLP Wikipedia Interface
		init();
		
		//creates Lucene index
		createIndex(args[0]);
		
		//loads Pages from JWPL API		
		for (Page page : wiki.getArticles()) {
			
			WikiPage wikiPage = ParseWikiPage.getInfo(page);
			
			if (wikiPage != null) {				
				Document doc = null;
				doc = wikiPage.luceneDoc();
				indexW.addDocument(doc);
			}
		}
		
		System.out.println("Processed entities: " + indexW.numDocs());
		indexW.commit();
		indexW.close();
			
		long end = new Date().getTime();		
		float secs = (end - start) / 1000;
		System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
	}

	
	public static void createIndex(String index) throws IOException {
	
		System.out.println("Writing to index: " + index);
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_35);
		IndexWriterConfig indexCfg = new IndexWriterConfig(Version.LUCENE_35, analyzer);
		Directory wikipediaEN = FSDirectory.open(new File(index));
		indexW = new IndexWriter(wikipediaEN,indexCfg);
	}
		
	public static void init() throws WikiApiException {
		
		// configure the database connection parameters
		dbConfig = new DatabaseConfiguration();
		dbConfig.setHost("");
		dbConfig.setDatabase("");
		dbConfig.setUser("");
		dbConfig.setPassword("");
		dbConfig.setLanguage(Language.english);
	
		// Create the Wikipedia object
		wiki = new Wikipedia(dbConfig);
	
		pf = new MediaWikiParserFactory(Language.english);
		pf.setTemplateParserClass(FlushTemplates.class); // Filtering TEMPLATE-Elements
		parser = pf.createParser();
		
		System.out.println(wiki.getWikipediaId());
	}
}
