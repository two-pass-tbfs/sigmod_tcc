package org.sigmod.chronograph.memstore;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.*;
import org.sigmod.chronograph.common.Tokens.EventComparator;

import java.util.*;

public class ChronoVertexEvent implements VertexEvent, Comparable<ChronoVertexEvent> {

	private final Vertex vertex;
	private final Time time;

	public ChronoVertexEvent(Vertex v, Time time) {
		this.vertex = v;
		this.time = time;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Event))
			return false;
		return this.vertex.equals(((Event) obj).getElement()) && this.getTime().equals(((Event) obj).getTime());
	}

	@Override
	public String toString() {
		return "ChronoVertexEvent{" + "vertex=" + vertex + ", time=" + time + '}';
	}

	/**
	 * Checks the difference of two events by comparing the element, and then the
	 * element
	 * 
	 * @param event
	 *            the event to be compared
	 * @return Integer difference
	 */
	@Override
	public int compareTo(ChronoVertexEvent event) {
		int elementComparison = this.getElement().getId().compareTo(event.getElement().getId());

		if (elementComparison != 0)
			return elementComparison;

		return this.getTime().compareTo(event.getTime());
	}

	@Override
	public String getId() {
		return vertex.getId() + "_" + time;
	}

	@Override
	public String getElementId() {
		return vertex.getId();
	}

	@Override
	public Map<String, Object> getProperties() {
		return vertex.getProperties();
	}

	@Override
	public <T> T getProperty(String key) {
		return vertex.getProperty(key);
	}

	@Override
	public Set<String> getPropertyKeys() {
		return vertex.getPropertyKeys();
	}

	@Override
	public void setProperty(String key, Object value) {
		vertex.setProperty(key, value);

	}

	@Override
	public <T> T removeProperty(String key) {
		return vertex.removeProperty(key);
	}

	@Override
	public Time getTime() {
		return time;
	}

	@Override
	public long getStartTime() {
		return time.getStartTime();
	}

	@Override
	public long getFinishTime() {
		return time.getFinishTime();
	}

	@Override
	public long getDuration() {
		return time.getDuration();
	}

	@Override
	public Element getElement() {
		return vertex;
	}

	@Override
	public NavigableSet<EdgeEvent> getEdgeEvents(Direction direction, TemporalRelation tr, String label) {
		NavigableSet<EdgeEvent> neighborEvents = new TreeSet<>(EventComparator.earlyStartTimeShortDuration);
		for (Edge e : this.vertex.getEdges(direction, List.of(label))) {
			NavigableSet<EdgeEvent> events = e.getEvents(time, tr, EventComparator.earlyStartTimeShortDuration);
			if (events.isEmpty())
				continue;
			neighborEvents.add(events.first());
		}
		return neighborEvents;
	}

	@Override
	public NavigableSet<VertexEvent> getVertexEvents(Direction direction, TemporalRelation tr, String label) {
		NavigableSet<VertexEvent> neighborEvents = new TreeSet<>(EventComparator.earlyStartTimeShortDuration);
		for (Vertex v : this.vertex.getVertices(direction, List.of(label))) {
			NavigableSet<VertexEvent> vertexEvents = v.getEvents(time, tr, false, false,
					EventComparator.earlyStartTimeShortDuration);
			if (vertexEvents.isEmpty())
				continue;
			neighborEvents.add(vertexEvents.first());
		}
		return neighborEvents;
	}
}
