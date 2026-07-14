package org.sigmod.chronograph.common;

import java.util.Comparator;

public class Tokens {

	public enum TimeComparator implements Comparator<Time> {
		earlyFinishTimeShortDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return -1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return +1;
		}), earlyFinishTimeLongDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return -1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return +1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return +1;
		}), earlyStartTimeShortDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return -1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return +1;
		}), earlyStartTimeLongDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return -1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return 1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return +1;
		}), lateFinishTimeShortDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return +1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return -1;
		}), lateFinishTimeLongDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return +1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return +1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return -1;
		}), lateStartTimeShortDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return +1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return -1;
		}), lateStartTimeLongDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return +1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return +1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return -1;
		});

		private final Comparator<Time> comparator;

		TimeComparator(Comparator<Time> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Time o1, Time o2) {
			return comparator.compare(o1, o2);
		}
	}

	public enum EventComparator implements Comparator<Event> {
		earlyStartTimeShortDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return -1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return +1;
		});

		private final Comparator<Event> comparator;

		EventComparator(Comparator<Event> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Event o1, Event o2) {
			return comparator.compare(o1, o2);
		}
	}
}
