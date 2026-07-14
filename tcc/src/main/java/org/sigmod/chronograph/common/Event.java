package org.sigmod.chronograph.common;

import com.tinkerpop.blueprints.Element;

import java.util.Map;
import java.util.Set;

public interface Event {

	String getId();
	
	String getElementId();
	
	Map<String, Object> getProperties();
	
	<T> T getProperty(String key);
	
	Set<String> getPropertyKeys();
	
	void setProperty(String key, Object value);
	
	<T> T removeProperty(String key);
	
	Time getTime();
	
	long getStartTime();
	
	long getFinishTime();
	
	long getDuration();

	Element getElement();

}
