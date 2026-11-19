package group18.listener;

import group18.testsupport.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MethodScanner finds method and constructor declarations")
class MethodScannerTest {

    @Test
    void findsEveryDeclarationInARealisticClass() {
        Set<String> methods = MethodScanner.scan(TestData.resource("sources/Shapes.java.txt"));
        assertEquals(Set.of(
                "Shapes()",
                "Shapes(int)",
                "compareTo(Shapes)",
                "largest(List<? extends T>, T)",
                "group(String...)",
                "sizes()",
                "Shape.area()",
                "Shape.describe(String, int)",
                "Visitor.visit(Shape)",
                "Visitor.accepts(Object)",
                "Colour.Colour(String)",
                "Colour.code()",
                "Point.distance(Point)"), methods);
    }

    @Test
    void ignoresCodeInCommentsStringsLambdasAndAnonymousClasses() {
        Set<String> methods = MethodScanner.scan(TestData.resource("sources/Shapes.java.txt"));
        assertFalse(methods.contains("fakeMethod()"));
        assertFalse(methods.contains("notAMethod()"));
        assertFalse(methods.contains("insideLambda()"));
        assertFalse(methods.contains("run()"));
    }

    @Test
    void copesWithHalfTypedCode() {
        Set<String> methods = MethodScanner.scan(TestData.resource("sources/BrokenStudent.java.txt"));
        assertTrue(methods.contains("getAverage()"));
        // The constructor follows a field with a missing semicolon, and getName() is swallowed by an unclosed loop.
        assertFalse(methods.contains("Student(String)"));
        assertFalse(methods.contains("getName()"));
    }

    @Test
    void parameterNamesDoNotMatterButTypesDo() {
        Set<String> first = MethodScanner.scan("class A { void f(int a, String b) {} }");
        Set<String> renamed = MethodScanner.scan("class A { void f(final int x, String  y) {} }");
        Set<String> retyped = MethodScanner.scan("class A { void f(long a, String b) {} }");
        assertEquals(Set.of("f(int, String)"), first);
        assertEquals(first, renamed);
        assertEquals(Set.of("f(long, String)"), retyped);
    }

    @Test
    void handlesArraysVarargsAndGenerics() {
        assertEquals(Set.of("m(int[], String..., Map<String, List<Integer>>)"),
                MethodScanner.scan("class A { void m(int x[], String ... s, Map<String, List<Integer>> m) {} }"));
    }

    @Test
    void readsAbstractAndInterfaceMethodsWithoutBodies() {
        assertEquals(Set.of("area()", "name(int)"),
                MethodScanner.scan("interface Shape { double area(); String name(int i); int SIDES = 4; }"));
    }

    @Test
    void ignoresConstructorLikeHeadersWithTheWrongName() {
        assertEquals(Set.of(), MethodScanner.scan("class Calculator { public Calc() { } }"));
    }

    @Test
    void parameterTypesAreNormalised() {
        assertEquals("", MethodScanner.parameterTypes("  "));
        assertEquals("Map<K, V>, int[]", MethodScanner.parameterTypes("Map< K , V > map, final int[] values"));
    }

    @Test
    void stripsCommentsAndLiterals() {
        String code = MethodScanner.stripCommentsAndLiterals("a /* x */ b // y\n\"s{\" 'c' \"\"\"\ntext{\n\"\"\" end");
        assertFalse(code.contains("x"));
        assertFalse(code.contains("y"));
        assertFalse(code.contains("{"));
        assertTrue(code.contains("end"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not java at all", "}}}"})
    void returnsNothingForEmptyOrMeaninglessInput(String source) {
        assertTrue(MethodScanner.scan(source).isEmpty());
    }
}
