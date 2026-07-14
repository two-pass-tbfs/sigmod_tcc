package org.sigmod.chronograph.common;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public interface EdgeEvent extends Event {

	/**
	 * Get a vertex event
	 * 
	 * @param direction the direction of the event
	 * @return the vertex event
	 */
    VertexEvent getVertexEvent(Direction direction);

	Vertex getVertex(Direction direction);

	String getLabel();
}
