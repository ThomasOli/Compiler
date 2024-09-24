package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Single Char", "a", true),
                Arguments.of("Hyphenated", "a-b-c", true),
                Arguments.of("Leading @", "@uf", true),
                Arguments.of("Mixed identifiers", "@h3llo_w0rld-", true),

                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Leading Underscore", "_abc", false),
                Arguments.of("Underscores", "___", false),
                Arguments.of("@ Within", "c@t", false),
                Arguments.of("Special Char", "P^ak", false),
                Arguments.of("Empty", " ", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Trailing Zeros", "1000", true),
                Arguments.of("Zeros", "0", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Negative Zero", "-0", false), //FIX
                Arguments.of("Negative Only", "-", false), //FIX
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Positive Int", "+314", false),
                Arguments.of("Char", "two", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Negative Decimal", "-0.0", true),
                Arguments.of("Zero Leading", "0.5", true),

                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "'c'", true),
                Arguments.of("Newline Escape", "'\\n'", true),
                Arguments.of("Backslash Escape", "'\\\\'", true),

                Arguments.of("Empty", "''", false),
                Arguments.of("No single quotes", " ", false),
                Arguments.of("one single quotes", "\'hello", false),
                Arguments.of("Single quote char", "'''", false),
                Arguments.of("newline", "'\n'", false),
                Arguments.of("Newline Escape then multiple", "'\\nabc'", false),
                Arguments.of("Multiple", "'abc'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("symbols", "\"!@#$%^&*()\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Quote Escape", "\"He said, \\\"Hi!\\\"\"", true),

                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("newline unterminated", "\"unterminated\n\"", false),
                Arguments.of("double quote string", "\"\"\"", false),
                Arguments.of("multiple lines", "\"Hello\nworld\"", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Logical AND", "&&", true),
                Arguments.of("Logical OR", "||", true),
                Arguments.of("Equal", "==", true),
                Arguments.of("Assignment", "=", true),
                Arguments.of("just !", "-", true),
                Arguments.of("free form", "\f", true),
                Arguments.of("vertical tab", "\u000B", true),
                Arguments.of("not whitespace", "\u000B\f", true),
                Arguments.of("just !", "!", true),
                Arguments.of("Space", " ", false),
                Arguments.of("mixed operators", "!=&&", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Newline", "\n", false)


        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                // Add new example here
                Arguments.of("Complex Example", "if (x > 100) { x = 100; }", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "if", 0),
                        new Token(Token.Type.OPERATOR, "(", 3),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, ">", 6),
                        new Token(Token.Type.INTEGER, "100", 8),
                        new Token(Token.Type.OPERATOR, ")", 11),
                        new Token(Token.Type.OPERATOR, "{", 13),
                        new Token(Token.Type.IDENTIFIER, "x", 15),
                        new Token(Token.Type.OPERATOR, "=", 17),
                        new Token(Token.Type.INTEGER, "100", 19),
                        new Token(Token.Type.OPERATOR, ";", 22),
                        new Token(Token.Type.OPERATOR, "}", 24)
                )),
                Arguments.of("Example 6", "x + 1 == y / 2.0 - 3", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "x", 0),
                        new Token(Token.Type.OPERATOR, "+", 2),
                        new Token(Token.Type.INTEGER, "1", 4),
                        new Token(Token.Type.OPERATOR, "==", 6),
                        new Token(Token.Type.IDENTIFIER, "y", 9),
                        new Token(Token.Type.OPERATOR, "/", 11),
                        new Token(Token.Type.DECIMAL, "2.0", 13),
                        new Token(Token.Type.OPERATOR, "-", 17),
                        new Token(Token.Type.INTEGER, "3", 19)
                )),
                Arguments.of("FizzBuzz Program", "LET i = 1;\n" +
                        "WHILE i != 100 DO\n" +
                        "    IF rem(i, 3) == 0 AND rem(i, 5) == 0 DO\n" +
                        "        print(\"FizzBuzz\");\n" +
                        "    ELSE IF rem(i, 3) == 0 DO\n" +
                        "        print(\"Fizz\");\n" +
                        "    ELSE IF rem(i, 5) == 0 DO\n" +
                        "        print(\"Buzz\");\n" +
                        "    ELSE\n" +
                        "        print(i);\n" +
                        "    END END END\n" +
                        "    i = i + 1;\n" +
                        "END", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "i", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "1", 8),
                        new Token(Token.Type.OPERATOR, ";", 9),
                        new Token(Token.Type.IDENTIFIER, "WHILE", 11),
                        new Token(Token.Type.IDENTIFIER, "i", 17),
                        new Token(Token.Type.OPERATOR, "!=", 19),
                        new Token(Token.Type.INTEGER, "100", 22),
                        new Token(Token.Type.IDENTIFIER, "DO", 26),
                        new Token(Token.Type.IDENTIFIER, "IF", 33),
                        new Token(Token.Type.IDENTIFIER, "rem", 36),
                        new Token(Token.Type.OPERATOR, "(", 39),
                        new Token(Token.Type.IDENTIFIER, "i", 40),
                        new Token(Token.Type.OPERATOR, ",", 41),
                        new Token(Token.Type.INTEGER, "3", 43),
                        new Token(Token.Type.OPERATOR, ")", 44),
                        new Token(Token.Type.OPERATOR, "==", 44),
                        new Token(Token.Type.INTEGER, "0", 47),
                        new Token(Token.Type.IDENTIFIER, "AND", 47),
                        new Token(Token.Type.IDENTIFIER, "rem", 51),
                        new Token(Token.Type.OPERATOR, "(", 54),
                        new Token(Token.Type.IDENTIFIER, "i", 55),
                        new Token(Token.Type.OPERATOR, ",", 56),
                        new Token(Token.Type.INTEGER, "5", 58),
                        new Token(Token.Type.OPERATOR, ")", 59),
                        new Token(Token.Type.OPERATOR, "==", 61),
                        new Token(Token.Type.INTEGER, "0", 64),
                        new Token(Token.Type.IDENTIFIER, "DO", 66),
                        new Token(Token.Type.IDENTIFIER, "print", 69),
                        new Token(Token.Type.OPERATOR, "(", 74),
                        new Token(Token.Type.STRING, "\"FizzBuzz\"", 75),
                        new Token(Token.Type.OPERATOR, ")", 74),
                        new Token(Token.Type.OPERATOR, ";", 74),

                        new Token(Token.Type.IDENTIFIER, "ELSE", 85),
                        new Token(Token.Type.IDENTIFIER, "IF", 86),
                        new Token(Token.Type.IDENTIFIER, "rem", 86),
                        new Token(Token.Type.OPERATOR, "(", 86),
                        new Token(Token.Type.IDENTIFIER, "i", 86),
                        new Token(Token.Type.OPERATOR, ",", 86),
                        new Token(Token.Type.INTEGER, "3", 86),
                        new Token(Token.Type.OPERATOR, ")", 86),
                        new Token(Token.Type.OPERATOR, "==", 86),
                        new Token(Token.Type.INTEGER, "0", 86),
                        new Token(Token.Type.IDENTIFIER, "DO", 86),
                        new Token(Token.Type.IDENTIFIER, "print", 86),
                        new Token(Token.Type.OPERATOR, "(", 86),
                        new Token(Token.Type.STRING, "\"Fizz\"", 86),
                        new Token(Token.Type.OPERATOR, ")", 86),
                        new Token(Token.Type.OPERATOR, ";", 86),

                        new Token(Token.Type.IDENTIFIER, "ELSE", 85),
                        new Token(Token.Type.IDENTIFIER, "IF", 86),
                        new Token(Token.Type.IDENTIFIER, "rem", 86),
                        new Token(Token.Type.OPERATOR, "(", 86),
                        new Token(Token.Type.IDENTIFIER, "i", 86),
                        new Token(Token.Type.OPERATOR, ",", 86),
                        new Token(Token.Type.INTEGER, "5", 86),
                        new Token(Token.Type.OPERATOR, ")", 86),
                        new Token(Token.Type.OPERATOR, "==", 86),
                        new Token(Token.Type.INTEGER, "0", 86),
                        new Token(Token.Type.IDENTIFIER, "DO", 86),
                        new Token(Token.Type.IDENTIFIER, "print", 86),
                        new Token(Token.Type.OPERATOR, "(", 86),
                        new Token(Token.Type.STRING, "\"Buzz\"", 86),
                        new Token(Token.Type.OPERATOR, ")", 86),
                        new Token(Token.Type.OPERATOR, ";", 86),

                        new Token(Token.Type.IDENTIFIER, "ELSE", 85),
                        new Token(Token.Type.IDENTIFIER, "print", 86),
                        new Token(Token.Type.OPERATOR, "(", 86),
                        new Token(Token.Type.IDENTIFIER, "i", 86),
                        new Token(Token.Type.OPERATOR, ")", 86),
                        new Token(Token.Type.OPERATOR, ";", 86),

                        new Token(Token.Type.IDENTIFIER, "END", 85),
                        new Token(Token.Type.IDENTIFIER, "END", 85),
                        new Token(Token.Type.IDENTIFIER, "END", 85),

                        new Token(Token.Type.IDENTIFIER, "i", 85),
                        new Token(Token.Type.OPERATOR, "=", 85),
                        new Token(Token.Type.IDENTIFIER, "i", 85),
                        new Token(Token.Type.OPERATOR, "+", 85),
                        new Token(Token.Type.INTEGER, "1", 85),
                        new Token(Token.Type.OPERATOR, ";", 85),
                        new Token(Token.Type.IDENTIFIER, "END", 85)
                ))

        );
    }


    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}