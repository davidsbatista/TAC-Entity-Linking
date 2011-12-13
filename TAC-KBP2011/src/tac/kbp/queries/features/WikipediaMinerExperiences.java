package tac.kbp.queries.features;

import java.io.File;

import org.wikipedia.miner.annotation.Disambiguator;
import org.wikipedia.miner.model.Disambiguation;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;

public class WikipediaMinerExperiences {
	
	public static void main(String args[]) throws Exception {
		
		WikipediaConfiguration conf = new WikipediaConfiguration(new File("/home/dsbatista/wikipedia-miner-1.2.0/custom_configs/configuration-wikipedia-en.xml")) ;
		
	    Wikipedia wikipedia = new Wikipedia(conf, false);
	    
	    Disambiguator disambiguator = new Disambiguator(wikipedia);		
	}

}
