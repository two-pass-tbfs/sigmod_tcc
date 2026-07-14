package org.sigmod.chronograph.memstore;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.sigmod.chronograph.common.EdgeEvent;
import org.sigmod.chronograph.common.Event;
import org.sigmod.chronograph.common.Time;
import org.sigmod.chronograph.common.VertexEvent;

import java.util.Map;
import java.util.Set;

public class ChronoEdgeEvent implements EdgeEvent, Comparable<ChronoEdgeEvent> {
	private final Edge edge;
	private final Time time;

	public ChronoEdgeEvent(Edge e, Time time) {
		this.edge = e;
		this.time = time;
	}

	@Override
	public VertexEvent getVertexEvent(Direction direction) {
		return edge.getVertex(direction).getEvent(time);
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
	public int compareTo(ChronoEdgeEvent event) {
		int elementComparison = this.getElement().getId().compareTo(event.getElement().getId());

		if (elementComparison != 0)
			return elementComparison;

		return this.getTime().compareTo(event.getTime());
	}

	@Override
	public Time getTime() {
		return time;
	}

	@Override
	public Element getElement() {
		return edge;
	}

	@Override
	public String getId() {
		return edge.getId() + "_" + time;
	}

	@Override
	public Map<String, Object> getProperties() {
		return edge.getProperties();
	}

	@Override
	public <T> T getProperty(String key) {
		return edge.getProperty(key);
	}

	@Override
	public Set<String> getPropertyKeys() {
		return edge.getPropertyKeys();
	}

	@Override
	public void setProperty(String key, Object value) {
		edge.setProperty(key, value);
	}

	@Override
	public <T> T removeProperty(String key) {
		return edge.removeProperty(key);
	}

	@Override
	public Vertex getVertex(Direction direction) {
		return edge.getVertex(direction);
	}

	@Override
	public String getLabel() {
		return edge.getLabel();
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
	public String getElementId() {
		return edge.getId();
	}

	@Override
	public long getDuration() {
		return time.getDuration();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EdgeEvent))
			return false;
		return this.edge.equals(((Event) obj).getElement()) && this.getTime().equals(((Event) obj).getTime());
	}

	@Override
	public String toString() {
		return "ChronoEdgeEvent{" + "edge=" + edge + ", time=" + time + '}';
	}
}
