package org.sigmod.chronograph.common;

import java.util.Map;

public interface LoopFunction<T> {
	
	/**
	 * A function determines the termination condition of loop based on current
	 * element, current path, the number of loops
	 * 
	 * @param argument
	 * @param currentPath
	 * @param loopCount
	 * @return
	 */
    boolean apply(T argument, Map<Object, Object> currentPath, int loopCount);
}
