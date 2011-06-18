/**
 * @author dsbatista
 */

package tac.kbp.kb;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Parses the contents of KB XML file. The name of the file to parse must be
 * specified as the first command line argument.
 */

public class EntityParser {

	private static Vector<EntityParser.Entity> entities = new Vector<Entity>();

	public void addEntity(Entity entity) {
		
		/*
		System.out.println("wiki_title: " + entity.getWiki_title());
		System.out.println("type: " + entity.getType());
		System.out.println("id: " + entity.getId());
		System.out.println("name: " + entity.getName());
		System.out.println("infobox_class: " + entity.getInfobox_class());
		System.out.println("facts: ");
		
		for (Iterator<Fact> iterator = entity.facts.iterator(); iterator.hasNext();) {
			Fact fact = (Fact) iterator.next();
			
			if (fact.fact != null) {
				System.out.print(fact.name + ": " + fact.fact);
			}
			
			else System.out.print(fact.name + ": ");			
			
			if (fact.factlink.size()>0) {
				for (Iterator<FactLink> iterator2 = fact.factlink.iterator(); iterator2.hasNext();) {
					FactLink factlink = (FactLink) iterator2.next();
					if (factlink.e_id == null) {
						System.out.println(factlink.link);
					}
					else System.out.print(factlink.e_id + ": " + factlink.link);
				}
			}
		System.out.println("\n");	
		}
		
		System.out.println("\n");
		*/
		
		this.entities.add(entity);
	}

	/**
	 * Configures Digester rules and actions, parses the XML file specified as
	 * the first argument.
	 */

	/**
	 * @param args
	 * @throws IOException
	 * @throws SAXException
	 */

	public static Vector<EntityParser.Entity> process(String filepath) throws IOException, SAXException {

		// instantiate Digester and disable XML validation
		Digester digester = new Digester();
		digester.setValidating(false);

		// instantiate EntityParser class
		digester.addObjectCreate("knowledge_base", EntityParser.class);

		// instantiate Entity class
		digester.addObjectCreate("knowledge_base/entity", Entity.class);

		// set different properties of Entity instance using specified methods
		digester.addCallMethod("knowledge_base/entity/wiki_text","setWiki_text", 0);

		// set properties of Entity instance when attributes are found
		digester.addSetProperties("knowledge_base/entity", "wiki_title","wiki_title");
		digester.addSetProperties("knowledge_base/entity", "type", "type");
		digester.addSetProperties("knowledge_base/entity", "id", "id");
		digester.addSetProperties("knowledge_base/entity", "name", "name");
		digester.addSetProperties("knowledge_base/entity/facts", "class", "infobox_class");
		 
		digester.addObjectCreate("knowledge_base/entity/facts/fact", Fact.class);
		digester.addSetNext("knowledge_base/entity/facts/fact", "addFact");
		digester.addCallMethod("knowledge_base/entity/facts/fact/", "setFact", 0);
        digester.addSetProperties("knowledge_base/entity/facts/fact", "setName", "name");

        digester.addObjectCreate("knowledge_base/entity/facts/fact/link", FactLink.class);
        digester.addSetNext("knowledge_base/entity/facts/fact/link", "setFactLink");
        
        digester.addCallMethod("knowledge_base/entity/facts/fact/link", "setLink", 0);
		digester.addSetProperties("knowledge_base/entity/facts/fact/link", "entity_id","e_id");

		
		// call 'addEntity' method when the next 'knowledge_base/entity' pattern is seen
		digester.addSetNext("knowledge_base/entity", "addEntity");
		
		// now that rules and actions are configured, start the parsing process
		EntityParser entity = (EntityParser) digester.parse(new File(filepath));
		
		return entities;
	}
	
	public static class FactLink {

		public String e_id = null;
		public String link = null;
		
		public String getE_id() {
			return e_id;
		}
		
		public void setE_id(String e_id) {
			this.e_id = e_id;
		}
		
		public String getLink() {
			return link;
		}
		
		public void setLink(String link) {
			this.link = link;
		}
		

	}
	
	public static class Fact {
		
		public String name;
		public String fact;
		public Vector<FactLink> factlink = new Vector<EntityParser.FactLink>();
		
		public void setFactLink(FactLink factlink){
			this.factlink.add(factlink);
		}
		
		public void setName(String name){
			this.name = name;
		}
		
		public void setFact(String fact){
			this.fact = fact;
		}
		
		/*
		public String toString(){
			StringBuilder result = new StringBuilder();
 			
			if (fact.length()==0) {
				result.append(name+":"+fact);
			}
			
			return result.toString();
			
			/*
			else if (fact.length()>0) {

				for (Iterator iterator = factlink.iterator(); iterator.hasNext();) {
					FactLink fact = (FactLink) iterator.next();
					result.append()
					
				}
			}
			*/

	}
	
	public static class Entity {

		private String wiki_title;
		private String type;
		private String id;
		private String name;
		private Vector<Fact> facts = new Vector<EntityParser.Fact>();
		private String infobox_class;
		private String wiki_text;
		
		public void addFact(Fact fact){
			this.facts.add(fact);
		}
		
		public String getWiki_title() {
			return wiki_title;
		}


		public void setWiki_title(String wiki_title) {
			this.wiki_title = wiki_title;
		}

		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * @param type
		 *            the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}

		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * @param id
		 *            the id to set
		 */
		public void setId(String id) {
			this.id = id;
		}


		public String getName() {
			return name;
		}


		public void setName(String name) {
			this.name = name;
		}


		public String getWiki_text() {
			return wiki_text;
		}


		public void setWiki_text(String wiki_text) {
			this.wiki_text = wiki_text;
		}

		public String getInfobox_class() {
			return infobox_class;
		}


		public void setInfobox_class(String infobox_class) {
			this.infobox_class = infobox_class;
		}

		public Vector<Fact> getFacts() {
			return this.facts;
		}

	}
}