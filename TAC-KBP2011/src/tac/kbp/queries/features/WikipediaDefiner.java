package tac.kbp.queries.features;

import java.io.File;

import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;

public class WikipediaDefiner {

	public static void main(String args[]) throws Exception {
		
	    WikipediaConfiguration conf = new WikipediaConfiguration(new File("/home/dsbatista/wikipedia-miner-1.2.0/custom_configs/configuration-wikipedia-en.xml")) ;
			
	    Wikipedia wikipedia = new Wikipedia(conf, false) ;
	    
	    Article article = wikipedia.getArticleByTitle("Wikipedia") ;
	    
	    System.out.println(article.getSentenceMarkup(0)) ;
	    
	    wikipedia.close() ;
	}
  }