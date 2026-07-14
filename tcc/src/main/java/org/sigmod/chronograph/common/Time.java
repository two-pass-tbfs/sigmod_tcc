package org.sigmod.chronograph.common;

public interface Time extends Comparable<Time> {

	/**
	 * Compares this time with the specified time for order. Returns a negative
	 * integer, zero, or a positive integer as this time is less than, equal to, or
	 * greater than the specified time.
	 *
	 * @param o
	 *            the time to be compared
	 * @return a negative integer, zero, or a positive integer as this time is less
	 *         than, equal to, or greater than the specified time.
	 */
	default int compareTo(Time o) {
		if (this.getTime() < o.getTime())
			return -1;
		else if (this.getTime() > o.getTime())
			return +1;
		else
			return 0;
	}

	/**
	 * return a time in time-instant or a start time in time-period used for
	 * compareTo, due to disjointness, it is not ambiguous.
	 *
	 * @return a time in time-instant or a start time in time-period
	 */
    long getTime();

	/**
	 * @return a time in time-instant or a start time in time-period
	 */
    long getStartTime();

	/**
	 * @return a time in time-instant or a finish time in time-period
	 */
    long getFinishTime();

	/**
	 * @return finishTime - startTime
	 */
    long getDuration();

	/**
	 * Checks whether a temporal relation is true between two time instances e.g.
	 * this: 5, time: 5, 9, tr: starts, return true this (5) tr (starts) time (5,9)
	 *
	 * @param time
	 *            the time to be compared
	 * @param tr
	 *            the temporal relation to check
	 */
    boolean checkTemporalRelation(Time time, TemporalRelation tr);

	/**
	 * get the temporal relation for time from this
	 * <p>
	 * e.g., this: 3 time: 5 return isAfter
	 *
	 * @param time
	 *            the time to be checked
	 * @return the temporal relation for time from this
	 */
    TemporalRelation getTemporalRelation(Time time);
}
