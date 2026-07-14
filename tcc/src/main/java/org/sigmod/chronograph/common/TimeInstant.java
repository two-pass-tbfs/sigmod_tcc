package org.sigmod.chronograph.common;

public class TimeInstant implements Time {
    protected long time;

    public TimeInstant(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return Long.toString(time);
    }

    @Override
    public long getStartTime() {
        return time;
    }

    @Override
    public long getFinishTime() {
        return time;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TimeInstant oTime))
            return false;
        return time == oTime.getTime();
    }

    @Override
    public boolean checkTemporalRelation(Time ti, TemporalRelation tr) {
        return switch (tr) {
            case isBefore -> time < ti.getStartTime();
            case isAfter -> time > ti.getStartTime();
            case cotemporal -> time == ti.getStartTime();
            default -> false;
        };
    }

    @Override
    public TemporalRelation getTemporalRelation(Time time) {
        if (time instanceof TimeInstant) {
            if (this.getTime() < time.getTime())
                return TemporalRelation.isAfter;
            if (this.getTime() > time.getTime())
                return TemporalRelation.isBefore;
            if (this.equals(time))
                return TemporalRelation.cotemporal;
        }

        return null;
    }

    @Override
    public long getDuration() {
        return 0;
    }
}
