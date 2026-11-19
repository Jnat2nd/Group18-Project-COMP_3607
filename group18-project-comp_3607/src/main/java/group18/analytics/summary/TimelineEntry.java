package group18.analytics.summary;

import java.time.Instant;

import group18.model.LifecycleEvent;

public record TimelineEntry(
    Instant timestamp,
    LifecycleEvent.ActionType action,
    String target) {}
