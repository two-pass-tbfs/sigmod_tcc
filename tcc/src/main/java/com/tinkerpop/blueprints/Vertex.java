package com.tinkerpop.blueprints;

import org.sigmod.chronograph.common.TemporalRelation;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.common.Tokens.EventComparator;
import org.sigmod.chronograph.common.VertexEvent;

import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;

public interface Vertex extends Element {

	/**
	 * Return the edges incident to the vertex according to the provided direction
	 * and edge labels. The resulting collection would have multiple e(i|j) with
	 * multiple labels. labels of candidate edges should be included in labels if
	 * labels are empty, all the candidate edges should be returned.
	 *
	 * @param direction the direction of the edges to retrieve
	 * @param labels    the labels of the edges to retrieve
	 * @return a collection of incident edges
	 */
    Collection<Edge> getEdges(Direction direction, List<String> labels);

	/**
	 * Return the vertices adjacent to the vertex according to the provided
	 * direction and edge labels. The resulting collection does not have redundancy
	 * <p>
	 * This method would remove duplicate vertices according to the definition of
	 * Vertex.getEdges
	 *
	 * @param direction the direction of the edges of the adjacent vertices
	 * @param labels    the labels of the edges of the adjacent vertices
	 * @return a collection of adjacent vertices
	 */
    Collection<Vertex> getVertices(Direction direction, List<String> labels);

	/**
	 * Add a new outgoing edge from this vertex to the parameter vertex with
	 * provided edge label.
	 *
	 * @param label    the label of the edge
	 * @param inVertex the vertex to connect to with an incoming edge
	 * @return the newly created edge
	 */
    Edge addEdge(String label, Vertex inVertex);

	/**
	 * Remove the element from the graph.
	 */
    void remove();

	/**
	 * Explicitly add a vertex event of this graph element valid at time.
	 * 
	 * @param time TimeInstant or TimePeriod
	 * @return the created vertex event
	 */
    VertexEvent addEvent(Time time);

	/**
	 * Return a vertex event of this graph element valid at time.
	 * 
	 * @param time TimeInstant or TimePeriod
	 * @return VertexEvent valid at time
	 */
    VertexEvent getEvent(Time time);

	/**
	 * Return events of this element that are matched with tr for time. In addition
	 * to getEvents(time, tr), the method includes in-going vertex event for
	 * out-going edge events if aware out events and out-going vertex event for
	 * in-going edge events if aware in events.
	 * 
	 * 
	 * @param time           the time to check
	 * @param tr             the temporal relation to match with time
	 * @param awareOutEvents include in-going vertex events for out-going edge
	 *                       events
	 * @param awareInEvents  include out-going vertex events for in-going edge
	 *                       events
	 * @return NavigableSet of VertexEvent or EdgeEvent
	 */
    NavigableSet<VertexEvent> getEvents(Time time, TemporalRelation tr, boolean awareOutEvents,
                                        boolean awareInEvents, EventComparator eventComparator);
	
	/**
	 * Remove all the events that are matched with tr for time
	 * 
	 * @param time the time to check
	 * @param tr   the temporal relation to match with time
	 */
    void removeEvents(Time time, TemporalRelation tr);

}
