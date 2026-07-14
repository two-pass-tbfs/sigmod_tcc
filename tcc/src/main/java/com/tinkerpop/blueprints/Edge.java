package com.tinkerpop.blueprints;

import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.Event;
import org.sigmod.chronograph.common.TemporalRelation;
import org.sigmod.chronograph.common.Time;

import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableSet;

public interface Edge extends Element {

	/**
	 * Return the tail/out or head/in vertex.
	 *
	 * @param direction whether to return the tail/out or head/in vertex
	 * @return the tail/out or head/in vertex
	 * @throws IllegalArgumentException is thrown if a direction of both is provided
	 */
    Vertex getVertex(Direction direction) throws IllegalArgumentException;

	/**
	 * Return the label associated with the edge.
	 *
	 * @return the edge label
	 */
    String getLabel();

	/**
	 * Remove the element from the graph.
	 */
    void remove();

	/**
	 * Add an event valid at time. The caller (Element) keeps distinct events
	 * regarding their valid time.
	 * <p>
	 * If time is an instance of TimeInstant and a time instant t is equal to
	 * existing time instant or is in a range of existing time-period, the method
	 * fails and return null.
	 * <p>
	 * If time is an instance of TimeInstant and a time instant t is not equal to
	 * any existing time instant or is not in a range of any existing time-period,
	 * return a newly created event.
	 * <p>
	 * If time is an instance of TimePeriod, the method may return a newly created
	 * event. If the time-period p covers any time-instants, the instants are merged
	 * to p. If the time-period p is overlapped with other time-periods, p extends.
	 * If the time-period p is exactly equal to an existing time-period, the method
	 * fails and returns null.
	 * 
	 * @param time the new time to be added
	 * @return VertexEvent or EdgeEvent
	 */
    EdgeEvent addEvent(Time time);

	/**
	 * Return an event of this graph element valid at time
	 * 
	 * @param time the time to match
	 * @return EdgeEvent
	 */
    EdgeEvent getEvent(Time time);

	/**
	 * Return events of this element that are matched with tr for time
	 *
	 * @param time the time to check in the events
	 * @param temporalRelation the temporal relation of time
	 * @return NavigableSet of VertexEvent or EdgeEvent
	 */
    NavigableSet<EdgeEvent> getEvents(Time time, TemporalRelation temporalRelation,
                                      Comparator<Event> eventComparator);
	
	/**
	 * 
	 * @return events of this element
	 */
    Collection<EdgeEvent> getEvents();
	
	/**
	 * Return the event of this element that is matched with tr for time
	 *
	 * @param time the time to check in the events
	 * @return EdgeEvent
	 */
    EdgeEvent getEvent(Time time, TemporalRelation temporalRelation);

	/**
	 * Remove all the events that are matched with temporalRelation for time
	 * 
	 * @param time the time to check
	 * @param temporalRelation the temporal relation to match
	 */
    void removeEvents(Time time, TemporalRelation temporalRelation);
}
