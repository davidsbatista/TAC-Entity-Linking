/**
 * 
 */
package tac.kbp.kb.xml;

import java.util.ArrayList;
import java.util.List;


public class KnowledgeBase {
	
	private List<Entity> entities = new ArrayList<Entity>();

	public void addEntity(Entity entity) {
		entities.add(entity);
	}
	
	public Iterable<Entity> getEntities() {
		return entities;
	}
}