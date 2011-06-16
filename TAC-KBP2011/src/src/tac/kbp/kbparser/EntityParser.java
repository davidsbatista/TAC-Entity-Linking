/**
 * @author dsbatista
 */

package src.tac.kbp.kbparser;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Parses the contents of KB XML file. The name of the file to parse must be
 * specified as the first command line argument.
 */

public class EntityParser {

	Vector entities = new Vector();

	public void addEntity(Entity entity) {
		System.out.println("wiki_title: " + entity.getWiki_title());
		System.out.println("type: " + entity.getType());
		System.out.println("id: " + entity.getId());
		System.out.println("name: " + entity.getName());
		System.out.println("infobox_class: " + entity.getInfobox_class());
		System.out.println("facts: ");

		HashMap facts = entity.getFacts();
		
		System.out.println("number of facts: " + facts.size());
		Iterator it = facts.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}

		//System.out.println("wiki_text: " + entity.getWiki_text());
		System.out.println("\n");

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

	public static void main(String[] args) throws IOException, SAXException {

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
		
		// instantiate an Hashmap		
		digester.addObjectCreate("knowledge_base/entity/facts", HashMap.class);
		digester.addSetNext("knowledge_base/entity/facts", "setFacts");
		digester.addCallMethod("knowledge_base/entity/facts/fact", "put", 2);
		digester.addCallParam("knowledge_base/entity/facts/fact/", 0, "name");
		digester.addCallParam("knowledge_base/entity/facts/fact", 1);
		
		digester.addObjectCreate("knowledge_base/entity/fact/link", HashMap.class);
		//digester.addSetNext("knowledge_base/entity/fact/link", "setFacts");
		digester.addCallParam("knowledge_base/entity/facts/fact/link", 0, "entity_id");
		digester.addCallParam("knowledge_base/entity/facts/fact/link", 1);
		
		
		
		// call 'addEntity' method when the next 'knowledge_base/entity' pattern is seen
		digester.addSetNext("knowledge_base/entity", "addEntity");
		
		// now that rules and actions are configured, start the parsing process
		EntityParser entity = (EntityParser) digester.parse(new File(args[0]));
	}

	/**
	 * JavaBean class that holds properties of each Contact entry. It is
	 * important that this class be public and static, in order for Digester to
	 * be able to instantiate it.
	 */

	public static class Entity {

		private String wiki_title;
		private String type;
		private String id;
		private String name;
		private HashMap<String, String> facts;
		private String infobox_class;
		private String wiki_text;

		/**
		 * @return the wiki_title
		 */
		public String getWiki_title() {
			return wiki_title;
		}

		/**
		 * @param wiki_title
		 *            the wiki_title to set
		 */
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

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the wiki_text
		 */
		public String getWiki_text() {
			return wiki_text;
		}

		/**
		 * @param wiki_text
		 *            the wiki_text to set
		 */
		public void setWiki_text(String wiki_text) {
			this.wiki_text = wiki_text;
		}

		/**
		 * @return the infobox_class
		 */
		public String getInfobox_class() {
			return infobox_class;
		}

		/**
		 * @param infobox_class
		 *            the infobox_class to set
		 */
		public void setInfobox_class(String infobox_class) {
			this.infobox_class = infobox_class;
		}

		public HashMap getFacts() {
			return this.facts;
		}

		public void setFacts(HashMap facts) {
			this.facts = facts;

		}
	}
}