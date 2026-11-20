package group18.demo;

/**
 * Versions of the Java classes written during the demo sessions. Some versions deliberately contain the mistakes
 * the compiler complains about in the scripts.
 */
final class DemoSources {

    private DemoSources() {
    }

    /**
     * The class BlueJ creates for "New Class...".
     *
     * @param name the class name
     * @return the template source
     */
    static String template(String name) {
        return """
                /**
                 * Write a description of class %1$s here.
                 *
                 * @author (your name)
                 * @version (a version number or a date)
                 */
                public class %1$s
                {
                    // instance variables - replace the example below with your own
                    private int x;

                    /**
                     * Constructor for objects of class %1$s
                     */
                    public %1$s()
                    {
                        // initialise instance variables
                        x = 0;
                    }

                    /**
                     * An example of a method - replace this comment with your own
                     *
                     * @param  y  a sample parameter for a method
                     * @return    the sum of x and y
                     */
                    public int sampleMethod(int y)
                    {
                        // put your code here
                        return x + y;
                    }
                }
                """.formatted(name);
    }

    /** Calculator with add and subtract, replacing the template method. */
    static final String CALCULATOR_V1 = """
            /**
             * A calculator for whole numbers.
             */
            public class Calculator
            {
                public Calculator()
                {
                }

                public int add(int a, int b)
                {
                    return a + b;
                }

                public int subtract(int a, int b)
                {
                    return a - b;
                }
            }
            """;

    /** Calculator with multiply, divide (guarding against zero) and a main method. */
    static final String CALCULATOR_V2 = """
            /**
             * A calculator for whole numbers.
             */
            public class Calculator
            {
                public Calculator()
                {
                }

                public int add(int a, int b)
                {
                    return a + b;
                }

                public int subtract(int a, int b)
                {
                    return a - b;
                }

                public int multiply(int a, int b)
                {
                    return a * b;
                }

                public int divide(int a, int b)
                {
                    if (b == 0) {
                        System.out.println("Cannot divide by zero");
                        return 0;
                    }
                    return a / b;
                }

                public static void main(String[] args)
                {
                    Calculator c = new Calculator();
                    System.out.println("2 + 3 = " + c.add(2, 3));
                    System.out.println("7 - 4 = " + c.subtract(7, 4));
                    System.out.println("6 * 7 = " + c.multiply(6, 7));
                    System.out.println("9 / 3 = " + c.divide(9, 3));
                }
            }
            """;

    /** Calculator version 2 plus a power method. */
    static final String CALCULATOR_V3 = CALCULATOR_V2.replace("""
                public static void main(String[] args)""", """
                public int power(int base, int exponent)
                {
                    int result = 1;
                    for (int i = 0; i < exponent; i++) {
                        result = multiply(result, base);
                    }
                    return result;
                }

                public static void main(String[] args)""");

    /** BankAccount with a missing semicolon (line 13) and a misspelt field (line 23). */
    static final String BANK_ACCOUNT_BROKEN = """
            /**
             * A bank account with a balance that cannot go below zero.
             */
            public class BankAccount
            {
                private String owner;
                private double balance;

                public BankAccount(String owner)
                {
                    this.owner = owner;
                    balance = 0
                }

                public void deposit(double amount)
                {
                    balance = balance + amount;
                }

                public double getBalance()
                {
                    return balanse;
                }
            }
            """;

    /** BankAccount with the first two mistakes fixed but returning a String where a double is expected. */
    static final String BANK_ACCOUNT_WRONG_TYPE = BANK_ACCOUNT_BROKEN
            .replace("balance = 0\n", "balance = 0;\n")
            .replace("return balanse;", "return \"Balance: \" + balance;");

    /** Working BankAccount whose withdraw method throws when funds are insufficient. */
    static final String BANK_ACCOUNT_V1 = """
            /**
             * A bank account with a balance that cannot go below zero.
             */
            public class BankAccount
            {
                private String owner;
                private double balance;

                public BankAccount(String owner)
                {
                    this.owner = owner;
                    balance = 0;
                }

                public void deposit(double amount)
                {
                    balance = balance + amount;
                }

                public double getBalance()
                {
                    return balance;
                }

                public void withdraw(double amount)
                {
                    if (amount > balance) {
                        throw new IllegalArgumentException("Insufficient funds");
                    }
                    balance = balance - amount;
                }
            }
            """;

    /** BankAccount whose withdraw method returns false instead of throwing, plus getOwner. */
    static final String BANK_ACCOUNT_V2 = BANK_ACCOUNT_V1.replace("""
                public void withdraw(double amount)
                {
                    if (amount > balance) {
                        throw new IllegalArgumentException("Insufficient funds");
                    }
                    balance = balance - amount;
                }""", """
                public boolean withdraw(double amount)
                {
                    if (amount > balance) {
                        return false;
                    }
                    balance = balance - amount;
                    return true;
                }

                public String getOwner()
                {
                    return owner;
                }""");

    /** Bank that calls deposit() without an argument. */
    static final String BANK_BROKEN = """
            import java.util.ArrayList;

            /**
             * A bank that holds several accounts.
             */
            public class Bank
            {
                private ArrayList<BankAccount> accounts = new ArrayList<>();

                public void addAccount(BankAccount account)
                {
                    accounts.add(account);
                }

                public double totalBalance()
                {
                    double total = 0;
                    for (BankAccount account : accounts) {
                        account.deposit();
                        total = total + account.getBalance();
                    }
                    return total;
                }
            }
            """;

    /** Working Bank with a main method. */
    static final String BANK_V1 = BANK_BROKEN.replace("""
                        account.deposit();
            """, "") .replace("""
                    return total;
                }
            """, """
                    return total;
                }

                public static void main(String[] args)
                {
                    Bank bank = new Bank();
                    BankAccount a = new BankAccount("Ana");
                    a.deposit(200);
                    BankAccount b = new BankAccount("Ravi");
                    b.deposit(150);
                    bank.addAccount(a);
                    bank.addAccount(b);
                    System.out.println("Accounts: " + bank.accounts.size());
                    System.out.println("Total balance: " + bank.totalBalance());
                }
            """);

    /** Pasted Student class with undefined variables and a missing return. */
    static final String STUDENT_BROKEN = """
            public class Student
            {
                private String name;
                private int[] grades;
                private int count;

                public Student(String name)
                {
                    grades = new int[10];
                }

                public void addGrade(int grade)
                {
                    grades[count] = grade;
                    count++;
                }

                public double getAverage()
                {
                    for (int i = 0; i < grades.length(); i++) {
                        total = total + grades[i];
                    }
                }

                public String getName()
                {
                    return name;
                }
            }
            """;

    /** Student with an unclosed loop and a lossy double-to-int return. */
    static final String STUDENT_UNBALANCED = """
            public class Student
            {
                private String name;
                private int[] grades;
                private int count;

                public Student(String name)
                {
                    grades = new int[10];
                }

                public void addGrade(int grade)
                {
                    grades[count] = grade;
                    count++;
                }

                public int getAverage()
                {
                    double total = 0;
                    for (int i = 0; i < grades.length; i++) {
                        total = total + grades[i];
                    return total / count;
                }

                public String getName()
                {
                    return name;
                }
            """;

    /** Student with a missing semicolon and a method outside the class. */
    static final String STUDENT_MISPLACED = """
            public class Student
            {
                private String name;
                private int[] grades;
                private int count

                public Student(String name)
                {
                    grades = new int[10];
                }

                public double getAverage()
                {
                    double total = 0;
                    for (int i = 0; i < count; i++) {
                        total = total + grades[i];
                    }
                    return total / count;
                }
            }
            }

            public void printReport()
            {
                System.out.println(name.toUpperCase() + ": " + getAverage());
            }
            """;

    /** Student that compiles but divides by zero and uses a null name at run time. */
    static final String STUDENT_COMPILES = """
            public class Student
            {
                private String name;
                private int[] grades;
                private int count;

                public Student(String name)
                {
                    grades = new int[10];
                }

                public void addGrade(int grade)
                {
                    grades[count] = grade;
                    count++;
                }

                public int getAverage()
                {
                    int total = 0;
                    for (int i = 0; i < count; i++) {
                        total = total + grades[i];
                    }
                    return total / count;
                }

                public void printReport()
                {
                    System.out.println(name.toUpperCase() + ": " + getAverage());
                }
            }
            """;

    /** Student whose getAverage lost its return statement. */
    static final String STUDENT_MISSING_RETURN = STUDENT_COMPILES.replace("""
                    return total / count;
            """, "");

    /** GradeBook whose main method has an off-by-one loop and creates students without names. */
    static final String GRADE_BOOK = """
            public class GradeBook
            {
                public static void main(String[] args)
                {
                    String[] names = {"Ana", "Ravi", "Kim"};
                    for (int i = 0; i <= names.length; i++) {
                        Student s = new Student(names[i]);
                        s.printReport();
                    }
                }
            }
            """;
}
