package group18.analytics.calculators;

import group18.model.TelemetryEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static group18.testsupport.TestData.compile;
import static group18.testsupport.TestData.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Shared calculator helpers filter events and round figures")
class EventsTest {

    @ParameterizedTest(name = "{0} of {1} = {2}%")
    @CsvSource({"1, 3, 33.3", "2, 3, 66.7", "0, 5, 0.0", "5, 5, 100.0", "3, 0, 0.0"})
    void percentagesAreRoundedToOneDecimalPlace(long part, long whole, double expected) {
        assertEquals(expected, Events.percent(part, whole));
    }

    @Test
    void roundsAndConvertsUnits() {
        assertEquals(12.5, Events.round1(12.46));
        assertEquals(2, Events.seconds(1_500));
        assertEquals(1, Events.seconds(1_499));
    }

    @Test
    void filtersEventsByTypeKeepingOrder() {
        List<TelemetryEvent> events = List.of(compile(1, true), run(2, "a"), compile(3, false));
        assertEquals(2, Events.ofType(events, group18.model.CompilationEvent.class).size());
        assertEquals(events.get(1), Events.ofType(events, group18.model.ExecutionEvent.class).get(0));
    }

    @Test
    void errorsWithoutACodeAreCountedAsUnclassified() {
        var summary = new CompilationMetricsCalculator().calculate(List.of(
                compile(1, false, new group18.model.CompilationError("A.java", 1, null, "odd"))));
        assertEquals("UNCLASSIFIED", summary.errorFrequency().get(0).errorCode());
    }
}
