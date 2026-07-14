package org.sigmod.chronograph.common;

import java.util.Map;

public interface HistoryEnabledFunction<T, R> {
	/**
	 * A function determines the termination condition of loop based on current
	 * element, current path, the number of loops
	 * 
	 * @param argument
	 * @param currentPath
	 * @return
	 */
    R apply(T argument, Map<Object, Object> currentPath);
}
