package org.sigmod.chronograph.common;

import com.tinkerpop.blueprints.Direction;

import java.util.Collection;

public interface VertexEvent extends Event {
	
	/**
	 * Return chronologically closest neighbor edge events per a pair (out vertex,
	 * label, in vertex)
	 * 
	 * @param direction the direction of the element from this vertex
	 * @param tr the temporal relation to match with this vertex event's time
	 * @param label the label of the edge
	 * @return Set<EdgeEvent>
	 */
    Collection<EdgeEvent> getEdgeEvents(Direction direction, TemporalRelation tr, String label);

	/**
	 * Return chronologically closest neighbor vertex events per a pair (out vertex,
	 * label, in vertex)
	 * 
	 * @param direction the direction of the element from this vertex
	 * @param tr the temporal relation to match with this vertex event's time
	 * @param label the label of the edge
	 * @return Set<VertexEvent>
	 */
    Collection<VertexEvent> getVertexEvents(Direction direction, TemporalRelation tr, String label);

}
