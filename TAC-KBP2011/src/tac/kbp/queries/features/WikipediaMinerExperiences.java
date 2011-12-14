package tac.kbp.queries.features;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.wikipedia.miner.annotation.Context;
import org.wikipedia.miner.annotation.Disambiguator;
import org.wikipedia.miner.annotation.Topic;
import org.wikipedia.miner.annotation.TopicDetector;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.db.WDatabase.DatabaseType;
import org.wikipedia.miner.model.Disambiguation;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.RelatednessCache;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.util.text.PorterStemmer;

public class WikipediaMinerExperiences {
	
	public static void main(String args[]) throws Exception {
		
		WikipediaConfiguration conf = new WikipediaConfiguration(new File("/home/dsbatista/wikipedia-miner-1.2.0/custom_configs/configuration-wikipedia-en.xml"));
		
		conf.setStopwords(new File("/collections/TAC-2011/resources/stopwords.txt"));
		
		PorterStemmer stemmer = new PorterStemmer();
		conf.setDefaultTextProcessor(stemmer);
		
		conf.addDatabaseToCache(DatabaseType.pageLinksInNoSentences);
		conf.addDatabaseToCache(DatabaseType.pageLinksOutNoSentences);
		conf.addDatabaseToCache(DatabaseType.pageLinkCounts);
		conf.addDatabaseToCache(DatabaseType.label);
		conf.addDatabaseToCache(DatabaseType.pageLabel);
		conf.addDatabaseToCache(DatabaseType.statistics);
		conf.addDatabaseToCache(DatabaseType.page);
		
		Wikipedia wikipedia = new Wikipedia(conf, false);
		
		while (!wikipedia.isReady()) {
			System.out.println(wikipedia.getProgress());
		}
		
	    Disambiguator disambiguator = new Disambiguator(wikipedia);	    
	    
	    TopicDetector detector = new TopicDetector(wikipedia, disambiguator, true, true);
	    
	    ArticleComparer comparer = new ArticleComparer(wikipedia);
	    
	    RelatednessCache rc = new RelatednessCache(comparer);
	    
	    
	    
	    
	    String newsText = "BEIRUT, Lebanon — Last February, the Obama administration accused one of Lebanon’s famously secretive banks of laundering money for an international cocaine ring with ties to the Shiite militant group Hezbollah. Now, in the wake of the bank’s exposure and arranged sale, its ledgers have been opened to reveal deeper secrets: a glimpse at the clandestine methods that Hezbollah — a terrorist organization in American eyes that has evolved into Lebanon’s pre-eminent military and political power — uses to finance its operations. The books offer evidence of an intricate global money-laundering apparatus that, with the bank as its hub, appeared to let Hezbollah move huge sums of money into the legitimate financial system, despite sanctions aimed at cutting off its economic lifeblood. At the same time, the investigation that led the United States to the bank, the  Lebanese Canadian Bank, provides new insights into the murky sources of Hezbollah’s money. While law enforcement agencies around the world have long believed that Hezbollah is a passive beneficiary of contributions from loyalists abroad involved in drug trafficking and a grab bag of other criminal enterprises, intelligence from several countries points to the direct involvement of high-level Hezbollah officials in the South American cocaine trade. One agent involved in the investigation compared Hezbollah to the Mafia, saying, “They operate like the Gambinos on steroids.” On Tuesday, federal prosecutors in Virginia announced the indictment of the man at the center of the Lebanese Canadian Bank case, charging that he had trafficked drugs and laundered money not only for Colombian cartels, but also for the murderous Mexican gang Los Zetas.";
	    Collection<Topic> topics = new LinkedList<Topic>();
	    
	    topics = detector.getTopics(newsText, rc);
	    
	}

}
