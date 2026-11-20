package group18.demo;

import group18.demo.DemoSession.CompileError;
import group18.model.ExecutionEvent.ExitType;

import java.time.Duration;
import java.util.List;

/**
 * The scripts of the three demo sessions. Each step is what a student would do in BlueJ; the comments give the
 * telemetry it produces.
 */
final class DemoScripts {

    private static final String NORMAL = ExitType.NORMAL_EXIT.name();
    private static final String EXCEPTION = ExitType.EXCEPTION_EXIT.name();

    private static final Duration FAST = Duration.ofMillis(230);
    private static final Duration STEADY = Duration.ofMillis(320);
    private static final Duration SLOW = Duration.ofMillis(520);

    private DemoScripts() {
    }

    private static Duration seconds(long seconds) {
        return Duration.ofSeconds(seconds);
    }

    /**
     * High activity: the student builds a calculator in small steps. Every compilation succeeds and every call
     * returns normally; there is one short break.
     *
     * @param s the session driver
     */
    static void fullyCorrect(DemoSession s) {
        List<String> files = List.of("Calculator.java");
        s.open();
        s.waitFor(seconds(20));
        s.setSource("Calculator", DemoSources.template("Calculator"));
        s.waitFor(seconds(12));
        s.type("Calculator", 190, FAST);
        s.delete("Calculator", 24, FAST);
        s.setSource("Calculator", DemoSources.CALCULATOR_V1);
        s.waitFor(seconds(6));
        s.compile(files);
        s.run("Calculator", null, NORMAL, "", "", null);
        s.run("Calculator", "add", NORMAL, "", "", "7");
        s.run("Calculator", "subtract", NORMAL, "", "", "3");
        s.waitFor(seconds(25));
        s.type("Calculator", 420, FAST);
        s.setSource("Calculator", DemoSources.CALCULATOR_V2);
        s.waitFor(seconds(5));
        s.compile(files);
        s.run("Calculator", "main", NORMAL, "2 + 3 = 5\n7 - 4 = 3\n6 * 7 = 42\n9 / 3 = 3\n", "", null);
        s.run("Calculator", "divide", NORMAL, "Cannot divide by zero\n", "", "0");
        s.waitFor(seconds(150));
        s.type("Calculator", 210, STEADY);
        s.delete("Calculator", 12, FAST);
        s.setSource("Calculator", DemoSources.CALCULATOR_V3);
        s.waitFor(seconds(4));
        s.compile(files);
        s.run("Calculator", "power", NORMAL, "", "", "1024");
        s.run("Calculator", "main", NORMAL, "2 + 3 = 5\n7 - 4 = 3\n6 * 7 = 42\n9 / 3 = 3\n", "", null);
        s.waitFor(seconds(30));
    }

    /**
     * Medium activity: the student writes a bank account class, meets several compiler errors and a runtime
     * exception, takes a break, and fixes everything by the end.
     *
     * @param s the session driver
     */
    static void partiallyCorrect(DemoSession s) {
        List<String> account = List.of("BankAccount.java");
        s.open();
        s.waitFor(seconds(30));
        s.setSource("BankAccount", DemoSources.template("BankAccount"));
        s.waitFor(seconds(10));
        s.type("BankAccount", 260, STEADY);
        s.paste("BankAccount", 64);
        s.setSource("BankAccount", DemoSources.BANK_ACCOUNT_BROKEN);
        s.waitFor(seconds(5));
        s.compile(account,
                new CompileError("BankAccount.java", 13, "';' expected"),
                new CompileError("BankAccount.java", 23, "cannot find symbol\n  symbol:   variable balanse\n"
                        + "  location: class BankAccount"));
        s.waitFor(seconds(20));
        s.type("BankAccount", 3, STEADY);
        s.delete("BankAccount", 8, FAST);
        s.type("BankAccount", 30, STEADY);
        s.setSource("BankAccount", DemoSources.BANK_ACCOUNT_WRONG_TYPE);
        s.waitFor(seconds(3));
        s.compile(account, new CompileError("BankAccount.java", 23,
                "incompatible types: java.lang.String cannot be converted to double"));
        s.waitFor(seconds(35));
        s.delete("BankAccount", 16, FAST);
        s.type("BankAccount", 190, STEADY);
        s.setSource("BankAccount", DemoSources.BANK_ACCOUNT_V1);
        s.waitFor(seconds(4));
        s.compile(account);
        s.run("BankAccount", null, NORMAL, "", "", null);
        s.run("BankAccount", "deposit", NORMAL, "", "", null);
        s.run("BankAccount", "getBalance", NORMAL, "", "", "100.0");
        s.run("BankAccount", "withdraw", EXCEPTION, "",
                "java.lang.IllegalArgumentException: Insufficient funds\n"
                        + "\tat BankAccount.withdraw(BankAccount.java:31)\n", null);
        s.waitFor(seconds(240));
        s.type("BankAccount", 150, STEADY);
        s.setSource("BankAccount", DemoSources.BANK_ACCOUNT_V2);
        s.waitFor(seconds(4));
        s.compile(account);
        s.run("BankAccount", "withdraw", NORMAL, "", "", "false");
        s.run("BankAccount", "withdraw", NORMAL, "", "", "true");
        s.waitFor(seconds(20));
        s.setSource("Bank", DemoSources.template("Bank"));
        s.waitFor(seconds(10));
        s.type("Bank", 330, STEADY);
        s.setSource("Bank", DemoSources.BANK_BROKEN);
        s.waitFor(seconds(5));
        s.compile(List.of("Bank.java", "BankAccount.java"), new CompileError("Bank.java", 19,
                "method deposit in class BankAccount cannot be applied to given types;\n"
                        + "  required: double\n  found:    no arguments"));
        s.waitFor(seconds(25));
        s.delete("Bank", 28, FAST);
        s.type("Bank", 380, STEADY);
        s.setSource("Bank", DemoSources.BANK_V1);
        s.waitFor(seconds(4));
        s.compile(List.of("Bank.java"));
        s.run("Bank", "main", NORMAL, "Accounts: 2\nTotal balance: 350.0\n", "", null);
        s.waitFor(seconds(20));
    }

    /**
     * Low activity: the student pastes code they do not understand, takes long breaks, keeps failing to compile,
     * and the one version that compiles throws an exception every time it runs.
     *
     * @param s the session driver
     */
    static void completelyIncorrect(DemoSession s) {
        List<String> student = List.of("Student.java");
        s.open();
        s.waitFor(seconds(150));
        s.setSource("Student", DemoSources.template("Student"));
        s.waitFor(seconds(25));
        s.delete("Student", 40, FAST);
        s.paste("Student", 430);
        s.type("Student", 55, SLOW);
        s.setSource("Student", DemoSources.STUDENT_BROKEN);
        s.waitFor(seconds(5));
        s.compile(student,
                new CompileError("Student.java", 20, "cannot find symbol\n  symbol:   method length()\n"
                        + "  location: variable grades of type int[]"),
                new CompileError("Student.java", 21, "cannot find symbol\n  symbol:   variable total\n"
                        + "  location: class Student"),
                new CompileError("Student.java", 23, "missing return statement"));
        s.waitFor(seconds(420));
        s.type("Student", 45, SLOW);
        s.delete("Student", 30, SLOW);
        s.setSource("Student", DemoSources.STUDENT_UNBALANCED);
        s.waitFor(seconds(5));
        s.compile(student,
                new CompileError("Student.java", 24, "incompatible types: possible lossy conversion from double "
                        + "to int"),
                new CompileError("Student.java", 30, "reached end of file while parsing"));
        s.waitFor(seconds(120));
        s.type("Student", 25, SLOW);
        s.setSource("Student", DemoSources.STUDENT_MISPLACED);
        s.waitFor(seconds(5));
        s.compile(student,
                new CompileError("Student.java", 5, "';' expected"),
                new CompileError("Student.java", 22, "class, interface, enum, or record expected"));
        s.waitFor(seconds(90));
        s.delete("Student", 130, FAST);
        s.type("Student", 40, SLOW);
        s.setSource("Student", DemoSources.STUDENT_COMPILES);
        s.setSource("GradeBook", DemoSources.GRADE_BOOK);
        s.waitFor(seconds(5));
        s.compile(List.of("Student.java", "GradeBook.java"));
        s.run("Student", null, NORMAL, "", "", null);
        s.run("Student", "getAverage", EXCEPTION, "",
                "java.lang.ArithmeticException: / by zero\n\tat Student.getAverage(Student.java:23)\n", null);
        s.run("Student", "printReport", EXCEPTION, "",
                "java.lang.NullPointerException: Cannot invoke \"String.toUpperCase()\" because \"this.name\" "
                        + "is null\n\tat Student.printReport(Student.java:28)\n", null);
        s.run("GradeBook", "main", EXCEPTION, "",
                "java.lang.NullPointerException: Cannot invoke \"String.toUpperCase()\" because \"this.name\" "
                        + "is null\n\tat Student.printReport(Student.java:28)\n\tat GradeBook.main(GradeBook.java:8)\n",
                null);
        s.waitFor(seconds(200));
        s.type("Student", 20, SLOW);
        s.setSource("Student", DemoSources.STUDENT_MISSING_RETURN);
        s.waitFor(seconds(5));
        s.compile(student, new CompileError("Student.java", 25, "missing return statement"));
        s.waitFor(seconds(360));
    }
}
