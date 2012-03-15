package tac.kbp.wikipedia;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;

import com.google.common.collect.LinkedHashMultimap;

import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.PageQuery;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiTitleParsingException;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.NestedListContainer;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;

public class WikipediaMappings {
	
	public static DatabaseConfiguration dbConfig;
	public static Wikipedia wiki;
	public static Map<String,String> redirectsAndNormalized = new HashMap<String, String>();
	public static LinkedHashMultimap<String,String> hyperlinks; 
	public static LinkedHashMultimap<String,String> disambiguation;	
	public static LinkedHashMultimap<String,String> allMappings;
	
	public static void init(String host, String database, String user, String password) throws WikiApiException {
		
		// configure the database connection parameters
		dbConfig = new DatabaseConfiguration();
		dbConfig.setHost(host);
		dbConfig.setDatabase(database);
		dbConfig.setUser(user);
		dbConfig.setPassword(password);
		dbConfig.setLanguage(Language.english);

		// Create the Wikipedia object
		wiki = new Wikipedia(dbConfig);
		
		System.out.println(wiki.getWikipediaId());
	}
	
	public static void getArticlesLink() throws WikiTitleParsingException, IOException {
		
		hyperlinks = com.google.common.collect.LinkedHashMultimap.create();
		
		for (Page page : wiki.getArticles()) {
			
			ParsedPage p;
			
			try {
				
				p = page.getParsedPage();
				
				for (Link link : p.getLinks()) { 
					if (link.getType() == Link.type.INTERNAL) {
						String text = link.getText();
						String target = link.getTarget(); 
						if (!text.equalsIgnoreCase(target.replaceAll("_", " ")) && text.length()>0) {
							System.out.println(link.getText().toLowerCase().replaceAll("_", " ") + "->" + link.getTarget().replaceAll("#.*", ""));
							String text_normalized = link.getText().toLowerCase().replaceAll("_", " ");
							String target_parsed = link.getTarget().replaceAll("#.*", "");
							hyperlinks.put(text_normalized, target_parsed);
							System.out.println(hyperlinks.size());
						}
					}
				}
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		FileOutputStream fos = new FileOutputStream("hyperlinks");
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(hyperlinks);
	    oos.close();
	}
	
	public static void getRedirects() throws WikiApiException, IOException {

		for (Page page : wiki.getArticles()) {
			
		    Set<String> redirects = page.getRedirects();
		    String normalizedTitle = page.getTitle().getPlainTitle().toLowerCase();
		    String originalTitle = page.getTitle().getWikiStyleTitle();
		    
		    redirectsAndNormalized.put(normalizedTitle, originalTitle);
		    System.out.println(normalizedTitle + " -> " + originalTitle);
		    
		    for (String r : redirects) {
		    	String normalizedRedirect = r.toLowerCase().replaceAll("_", " ");
				redirectsAndNormalized.put(normalizedRedirect, originalTitle);
				System.out.println(normalizedRedirect + " -> " + originalTitle);
			}		    
		}
		
		FileOutputStream fos = new FileOutputStream("redirectsAndNormalized");
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(redirectsAndNormalized);
	    oos.close();
	}

	@SuppressWarnings("unchecked")
	public static LinkedHashMultimap<String, String> loadArticlesLinks(String filename) throws IOException, ClassNotFoundException {
		
		hyperlinks = com.google.common.collect.LinkedHashMultimap.create();
		
	    FileInputStream fis = new FileInputStream(filename);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    hyperlinks = (LinkedHashMultimap<String, String>) ois.readObject();
	    ois.close();
	    
	    return hyperlinks;
	    
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> loadRedirects(String filename) throws IOException, ClassNotFoundException {
		
	    FileInputStream fis = new FileInputStream(filename);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    redirectsAndNormalized = (Map<String, String>) ois.readObject();
	    ois.close();
	    
	    return redirectsAndNormalized;
	
	}
    
	public static void getDisambiguations() throws WikiApiException, IOException {
		
		PageQuery pq = new PageQuery();		
		pq.setOnlyDisambiguationPages(true);
		
		disambiguation = com.google.common.collect.LinkedHashMultimap.create();
		
		for (Page p : wiki.getPages(pq)) {
			for (NestedListContainer list : p.getParsedPage().getNestedLists()) {
				for (Link link : list.getLinks()) {
					String text = link.getText();
					String target = link.getTarget(); 
					if (!text.equalsIgnoreCase(target.replaceAll("_", " ")) && text.length()>0) {
						//System.out.println(link.getText().toLowerCase().replaceAll("_", " ") + "->" + link.getTarget().replaceAll("#.*", ""));
						String text_normalized = link.getText().toLowerCase().replaceAll("_", " ");
						String target_parsed = link.getTarget().replaceAll("#.*", "");
						disambiguation.put(text_normalized, target_parsed);
						System.out.println(disambiguation.size());
					}
				}
			}		
		}
		
		FileOutputStream fos = new FileOutputStream("disambiguation");
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(disambiguation);
	    oos.close();
	}
	
	@SuppressWarnings("unchecked")
	public static LinkedHashMultimap<String, String> loadDisambiguation(String filename) throws IOException, ClassNotFoundException {
		
	    FileInputStream fis = new FileInputStream(filename);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    disambiguation = (LinkedHashMultimap<String, String>) ois.readObject();
	    ois.close();
	    
	    return disambiguation;
	
	}

	public static void getEntities() throws WikiApiException, IOException {
		
		PageQuery pq = new PageQuery();				
		pq.setOnlyArticlePages(true);
		
		FileWriter fstream = new FileWriter("entities.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		
		for (Page p : wiki.getPages(pq)) {
			out.write(p.getTitle().getPlainTitle()+'\n');
		}
		out.close();
	}
	
	public static void loadMappings(String disambiguation_file, String redirects_file, String anchors_file) throws IOException, ClassNotFoundException {
		
		hyperlinks = com.google.common.collect.LinkedHashMultimap.create();
		disambiguation = com.google.common.collect.LinkedHashMultimap.create();		
		allMappings = com.google.common.collect.LinkedHashMultimap.create();
		
		System.out.println("Loading disambiguation pages mappings file");
		disambiguation = loadDisambiguation(disambiguation_file);
		
		//System.out.println("Loading anchor links mappings file");
		//hyperlinks = loadArticlesLinks(anchors_file);
		
		System.out.println("Loading redirect pages mappings file");
		redirectsAndNormalized = loadRedirects(redirects_file);
		
		System.out.println("Loading disambiguation pages mappings");
		
		Set<String> keys = disambiguation.keySet();
		for (String k : keys) {
			allMappings.putAll(k, disambiguation.get(k));
		}		
		disambiguation = null;
		
		
		/*
		System.out.println("Loading anchor links mappings");
		
		keys = hyperlinks.keySet();
		for (String k : keys) {
			allMappings.putAll(k, hyperlinks.get(k));			
		}
		hyperlinks = null;
		*/
		
		System.out.println("Loading redirect pages mappings");
		
		keys = redirectsAndNormalized.keySet();
		for (String k : keys) {
			allMappings.put(k, redirectsAndNormalized.get(k));
		}
		redirectsAndNormalized = null;
		
		
		System.out.println(allMappings.size() + " keys loaded");
				
		FileOutputStream fos = new FileOutputStream("allMappings");
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(allMappings);
	    oos.close();		
	}	
	
	public static void dumpMappings() {
		
		System.out.println("Connecting to REDIS server.. ");
		Jedis j = new Jedis("borat", 6379);
        j.connect();
        
        Set<String> keys = j.keys("a*");
        
        for (String k : keys) {
        	
			System.out.println(k);
			
			System.out.println(j.llen(k));
			
			//System.out.println(j.hkeys(k));
			
			
			/*
			long len = j.llen(k);
			System.out.println("llen(\"" + k + "\"):" + len);
			
			for (; ; ) {
			String value = j.lpop(k);
			if (value == null) {
				break;
			}
			System.out.println(("lpop(\"" + k + "\"):"+ value));
	        }
	        */
		}        
	}
	
	@SuppressWarnings("unchecked")
	public static void loadMappingsToREDIS(String filename) throws IOException, ClassNotFoundException {
		
		allMappings = com.google.common.collect.LinkedHashMultimap.create();
		
		FileInputStream fis = new FileInputStream(filename);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    allMappings = (LinkedHashMultimap<String, String>) ois.readObject();
	    ois.close();
	    
	    Jedis j = new Jedis("borat.inesc-id.pt", 6379);
        j.connect();
        
        Set<String> keys = allMappings.keySet();
	    
	    for (String k : keys) {
	    	
	    	Set<String> values = allMappings.get(k);
	    	
	    	for (String v : values) {
	    		System.out.println(k + '\t' + v);
	    		j.sadd(k, v);
			}
	    	
	    	/*
	    	long len = j.llen(k);
	    	System.out.println("llen(\"" + k +"\") :" + len);
	    	
	    	List<String> keyValues = j.lrange(k, 0, len - 1);
	    	System.out.println("lrange(\"" + k +"\", 0, len - 1)" + keyValues.size());
	    	
	    	for (String kv : keyValues) {
				System.out.println(kv);
			}
	    	
	    	for (; ; ) {
	            String value = j.lpop("sequence_steps");
	            if (value == null) {
	                break;
	            }

	            System.out.println("lpop(\"" + k + "\"): " + value);
	    	}
	    	*/
        }
	}
	
	public static void test() {
		
		Jedis j = new Jedis("borat.inesc-id.pt", 6379);
        j.connect();
        
        for (String	e: j.smembers("az")) {
        	System.out.println(e);
        }
	}
	
	
	public static void main(String args[]) throws WikiApiException, IOException, ClassNotFoundException {
		
		String host = args[0];
		String database = args[1];
		String user = args[2];
		String passwd = args[3];
		
		//init(host, database, user, passwd);
		//getRedirects();
		//getArticlesLink();
		//getDisambiguations();
		//loadRedirects(args[4]);
		//loadArticlesLinks(args[4]);
		//loadDisambiguation(args[4]);
		//getEntities();
		loadMappings(args[4],args[5],args[6]);
		loadMappingsToREDIS(args[4]);
	}
}













