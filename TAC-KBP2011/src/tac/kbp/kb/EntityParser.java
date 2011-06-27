/**
 * @author dsbatista
 */

package tac.kbp.kb;

import java.io.File;
import java.io.IOException;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import tac.kbp.kb.xml.Entity;
import tac.kbp.kb.xml.Fact;
import tac.kbp.kb.xml.FactLink;
import tac.kbp.kb.xml.KnowledgeBase;


public class EntityParser {
	// instantiate Digester and disable XML validation
	private final Digester digester = new Digester();

	/**
	 * Configures Digester rules and actions, parses the XML file specified as
	 * the first argument.
	 */
	public EntityParser() {
		digester.setValidating(false);

		// instantiate EntityParser class
		digester.addObjectCreate("knowledge_base", KnowledgeBase.class);

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
	}
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws SAXException
	 */
	public KnowledgeBase process(String filepath) throws IOException, SAXException {
		// now that rules and actions are configured, start the parsing process
		return (KnowledgeBase) digester.parse(new File(filepath));	
	}
}