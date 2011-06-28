/**
 * 
 */
package tac.kbp.kb.index.xml;

import java.util.Vector;


public class Entity {

	private String wiki_title;
	private String type;
	private String id;
	private String name;
	private Vector<Fact> facts = new Vector<Fact>();
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