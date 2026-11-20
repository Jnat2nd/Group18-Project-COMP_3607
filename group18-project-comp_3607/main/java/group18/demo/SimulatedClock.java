package group18.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * A clock that only moves when told to. The demo sessions and the tests use it so that a 30-minute session runs in
 * a fraction of a second and always produces the same timestamps.
 */
public final class SimulatedClock extends Clock {

    /** Time zone reported by the clock. */
    private final ZoneId zone;

    /** The current time. */
    private volatile Instant now;

    /**
     * Creates a clock.
     *
     * @param start the initial time
     * @param zone  the time zone reported by the clock
     */
    public SimulatedClock(Instant start, ZoneId zone) {
        this.now = Objects.requireNonNull(start, "start");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /**
     * Moves the clock forward.
     *
     * @param duration how far; must not be negative
     * @throws IllegalArgumentException if the duration is negative
     */
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
