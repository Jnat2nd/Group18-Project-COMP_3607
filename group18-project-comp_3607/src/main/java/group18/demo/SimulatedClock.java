package group18.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public final class SimulatedClock extends Clock {

    private final ZoneId zone;

    private volatile Instant now;

    public SimulatedClock(Instant start, ZoneId zone) {
        this.now = Objects.requireNonNull(start, "start");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public void advance(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("A simulated clock cannot go backwards");
        }
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new SimulatedClock(now, newZone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}