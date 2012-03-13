package tac.kbp.wikipedia;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryJedis;
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
				
		int redis_port = 6379;
		String redis_host = "127.0.0.1";
		Jedis jedis = new Jedis(redis_host, redis_port);
		
		hyperlinks = com.google.common.collect.LinkedHashMultimap.create();
		disambiguation = com.google.common.collect.LinkedHashMultimap.create();
		
		System.out.println("Loading disambiguation pages mappings file");
		disambiguation = loadDisambiguation(disambiguation_file);
		
		System.out.println("Loading anchor links mappings file");
		hyperlinks = loadArticlesLinks(anchors_file);
		
		System.out.println("Loading redirect pages mappings file");
		redirectsAndNormalized = loadRedirects(redirects_file);
		
		System.out.println("Loading disambiguation pages mappings");
		
		Set<String> keys = disambiguation.keySet();
		int x = 0;
		for (String k : keys) {
			Set<String> values = disambiguation.get(k);			
			for (String v : values)
				jedis.set(k, v);
			x++;
			System.out.println(x+'/'+keys.size());
		}		
		disambiguation = null;

		
		System.out.println("Loading redirect pages mappings");
		
		keys = redirectsAndNormalized.keySet();
		x = 0;
		for (String k : keys) {
			jedis.set(k, redirectsAndNormalized.get(k));
			x++;
			System.out.println(x+'/'+keys.size());
		}
		redirectsAndNormalized = null;
		
		
		System.out.println("Loading anchor links mappings");
		
		keys = hyperlinks.keySet();
		x = 0;
		for (String k : keys) {
			Set<String> values = hyperlinks.get(k);
			for (String v : values) {
				jedis.set(k, v);
			}			
			x++;
			System.out.println(x+'/'+keys.size());
		}
		hyperlinks = null;
		
		System.out.println(jedis.info());
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
	}
}













