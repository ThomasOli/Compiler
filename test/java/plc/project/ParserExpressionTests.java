package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {
    @ParameterizedTest
    @MethodSource
    void testSource(List<Token> tokens, Ast.Source expected) {
        Parser parser = new Parser(tokens);
        Ast.Source result = parser.parseSource();
        assertEquals(expected, result);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of(
                        Arrays.asList(),
                        new Ast.Source(Arrays.asList(), Arrays.asList())
                ),
                Arguments.of(
                        Arrays.asList(
                                //VAR x = expr; VAR y = expr; VAR z = expr;
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.OPERATOR, ";", 10),
                                new Token(Token.Type.IDENTIFIER, "VAR", 11),
                                new Token(Token.Type.IDENTIFIER, "y", 14),
                                new Token(Token.Type.OPERATOR, "=", 15),
                                new Token(Token.Type.IDENTIFIER, "expr", 16),
                                new Token(Token.Type.OPERATOR, ";", 20),
                                new Token(Token.Type.IDENTIFIER, "VAR", 21),
                                new Token(Token.Type.IDENTIFIER, "z", 24),
                                new Token(Token.Type.OPERATOR, "=", 25),
                                new Token(Token.Type.IDENTIFIER, "expr", 26),
                                new Token(Token.Type.OPERATOR, ";", 30)
                        ),
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("x", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))),
                                        new Ast.Global("y", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr"))),
                                        new Ast.Global("z", true, Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                                ),
                                Arrays.asList()
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGlobalDeclaration(String test, List<Token> tokens, Ast.Global expected, Class<? extends Exception> expectedException, String expectedMessage) {
        Parser parser = new Parser(tokens);
        if (expectedException == null) {
            Ast.Global result = parser.parseGlobal();
            assertEquals(expected, result);
        } else {
            Exception exception = assertThrows(expectedException, parser::parseGlobal);
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    private static Stream<Arguments> testGlobalDeclaration() {
        return Stream.of(
                Arguments.of("Declaration",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, ";", 5)),
                        new Ast.Global("x", true, Optional.empty()),
                        null, null),

                Arguments.of("Missing ;",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4)),
                        new Ast.Global("x", true, Optional.empty()),
                        null, null),


                Arguments.of("Initialization",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "VAL", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.INTEGER, "1", 8),
                                new Token(Token.Type.OPERATOR, ";", 9)),
                        new Ast.Global("x", false, Optional.of(new Ast.Expression.Literal(BigInteger.valueOf(1)))),
                        null, null),
                Arguments.of("List Single Expression",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LIST", 0),
                                new Token(Token.Type.IDENTIFIER, "list", 5),
                                new Token(Token.Type.OPERATOR, "=", 10),
                                new Token(Token.Type.OPERATOR, "[", 12),
                                new Token(Token.Type.IDENTIFIER, "expr", 13),
                                new Token(Token.Type.OPERATOR, "]", 17),
                                new Token(Token.Type.OPERATOR, ";", 18)),
                        new Ast.Global("list", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Access(Optional.empty(), "expr"))))),
                        null, null),
                Arguments.of("List Multiple Expressions",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LIST", 0),
                                new Token(Token.Type.IDENTIFIER, "list", 5),
                                new Token(Token.Type.OPERATOR, "=", 10),
                                new Token(Token.Type.OPERATOR, "[", 12),
                                new Token(Token.Type.IDENTIFIER, "e1", 13),
                                new Token(Token.Type.OPERATOR, ",", 15),
                                new Token(Token.Type.IDENTIFIER, "e2", 16),
                                new Token(Token.Type.OPERATOR, ",", 18),
                                new Token(Token.Type.IDENTIFIER, "e3", 19),
                                new Token(Token.Type.OPERATOR, "]", 21),
                                new Token(Token.Type.OPERATOR, ";", 22)),
                        new Ast.Global("list", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(new Ast.Expression.Access(Optional.empty(), "e1"), new Ast.Expression.Access(Optional.empty(), "e2"), new Ast.Expression.Access(Optional.empty(), "e3"))))),
                        null, null),


                Arguments.of("Missing Expression",
                        //LIST list = [];
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LIST", 0),
                                new Token(Token.Type.IDENTIFIER, "list", 5),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.OPERATOR, "[", 10),
                                new Token(Token.Type.OPERATOR, "]", 11),
                                new Token(Token.Type.OPERATOR, ";", 12)),
                        null,
                        ParseException.class, "Missing Expression in List")
        );
    }







    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Statement.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Function("name", Arrays.asList()))
                ),
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, ";", 1)
                        ),
                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "name"))
                )

        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Statement.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "name"),
                                new Ast.Expression.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testDeclarationStatement")
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Statement.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of(
                        "Definition",
                        //LET name;
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ";", 8)),

                        new Ast.Statement.Declaration("name", Optional.empty())
                ),
                Arguments.of(
                        "Initialization",
                        //LET name = expr;
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)),
                        new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")))
                )

        );
    }

    @ParameterizedTest
    @MethodSource("testIfStatement")
    void testIfStatement(String test, List<Token> tokens, Ast.Statement.If expected) {
        test(tokens, expected, Parser::parseStatement);
    }
    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of(
                        "If",
                        //IF expr DO stmt; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 7),
                                new Token(Token.Type.IDENTIFIER, "stmt", 9),
                                new Token(Token.Type.OPERATOR, ";", 13),
                                new Token(Token.Type.IDENTIFIER, "END", 14)
                        ),
                        new Ast.Statement.If(new Ast.Expression.Access(Optional.empty(), "expr"), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))), Arrays.asList())
                ),
                Arguments.of(
                        "Else",
                        //IF expr DO stmt1; ELSE stmt2; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 7),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 9),
                                new Token(Token.Type.OPERATOR, ";", 14),
                                new Token(Token.Type.IDENTIFIER, "ELSE", 15),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 19),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 25)
                        ),
                        new Ast.Statement.If(new Ast.Expression.Access(Optional.empty(), "expr"), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1"))), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2"))))
                ),
                // IF expr stmt; END
                Arguments.of(
                        "Missing DO",
                        //IF expr stmt; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "stmt", 7),
                                new Token(Token.Type.OPERATOR, ";", 11),
                                new Token(Token.Type.IDENTIFIER, "END", 12)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testSwitchStatement")
    void testSwitchStatement(String test, List<Token> tokens, Ast.Statement.Switch expected) {
        // Assuming a method test(tokens, expected, Parser::parseStatement) similar to your 'if' test
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testSwitchStatement() {
        Collections Collections = null;
        return Stream.of(
                Arguments.of(
                        "Basic Switch",
                        //SWITCH expr DEFAULT stmt; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 12),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Case(Optional.empty(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt"))))
                                )
                        )
                ),

                Arguments.of(
                        "Case Switch",
                        //SWITCH expr1 CASE expr2 : stmt1; DEFAULT stmt2; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 13),
                                new Token(Token.Type.IDENTIFIER, "expr2", 18),
                                new Token(Token.Type.OPERATOR, ":", 23),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 25),
                                new Token(Token.Type.OPERATOR, ";", 30),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 32),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 40),
                                new Token(Token.Type.OPERATOR, ";", 45),
                                new Token(Token.Type.IDENTIFIER, "END", 47)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                Arrays.asList(
                                        new Ast.Statement.Case(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr2")), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")))),
                                        new Ast.Statement.Case(Optional.empty(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")
                                        )
                                        )
                                        )
                                )
                        )
                ),


                Arguments.of(
                        "Multiple Cases",
                        // SWITCH expr CASE expr1 : stmt1; CASE expr2 : stmt2; CASE expr3 : stmt3; DEFAULT stmt4; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 11),
                                new Token(Token.Type.IDENTIFIER, "expr1", 16),
                                new Token(Token.Type.OPERATOR, ":", 22),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 24),
                                new Token(Token.Type.OPERATOR, ";", 29),
                                new Token(Token.Type.IDENTIFIER, "CASE", 31),
                                new Token(Token.Type.IDENTIFIER, "expr2", 36),
                                new Token(Token.Type.OPERATOR, ":", 42),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 44),
                                new Token(Token.Type.OPERATOR, ";", 49),
                                new Token(Token.Type.IDENTIFIER, "CASE", 51),
                                new Token(Token.Type.IDENTIFIER, "expr3", 56),
                                new Token(Token.Type.OPERATOR, ":", 62),
                                new Token(Token.Type.IDENTIFIER, "stmt3", 64),
                                new Token(Token.Type.OPERATOR, ";", 69),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 71),
                                new Token(Token.Type.IDENTIFIER, "stmt4", 79),
                                new Token(Token.Type.OPERATOR, ";", 84),
                                new Token(Token.Type.IDENTIFIER, "END", 86)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr1")),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")))),
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr2")),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")))),
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Access(Optional.empty(), "expr3")),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt3")))),
                                        new Ast.Statement.Case(
                                                Optional.empty(), Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt4")
                                                )
                                        )
                                        )
                                )
                        )
                ),

                Arguments.of(
                        "Empty Switch",
                        //SWITCH expr DEFAULT END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 11),
                                // Assuming the DEFAULT block is empty; no statements inside
                                new Token(Token.Type.IDENTIFIER, "END", 18)
                        ),
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(), "expr"), // The condition
                                Collections.singletonList(
                                        new Ast.Statement.Case(
                                                Optional.empty(), // No expression for DEFAULT case
                                                Collections.emptyList() // Empty list of statements for DEFAULT case
                                        )
                                )
                        )
                ),
                Arguments.of(
                        "Double ::",
                        //SWITCH expr1 CASE expr2 :: stmt1; DEFAULT stmt2; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                                new Token(Token.Type.IDENTIFIER, "expr2", 18),
                                new Token(Token.Type.OPERATOR, ":", 23),
                                new Token(Token.Type.OPERATOR, ":", 23),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 25),
                                new Token(Token.Type.OPERATOR, ";", 30),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 32),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 40),
                                new Token(Token.Type.OPERATOR, ";", 45),
                                new Token(Token.Type.IDENTIFIER, "END", 47)
                        ),
                        null
                )
        );
    }
    @ParameterizedTest
    @MethodSource("testWhileStatement")
    void testWhileStatement(String testName, List<Token> tokens, Ast.Statement expected) {
        Parser parser = new Parser(tokens);
        Ast.Statement result = parser.parseStatement(); // Assuming parseStatement can handle while statements
        assertEquals(expected, result);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of(
                        "Simple While",
                        //WHILE expr DO stmt; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt")))
                        )
                ),
                Arguments.of(
                        "While With Multiple Statements",
                        //WHILE expr DO stmt1; stmt2; stmt3; END
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 14),
                                new Token(Token.Type.OPERATOR, ";", 19),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 21),
                                new Token(Token.Type.OPERATOR, ";", 26),
                                new Token(Token.Type.IDENTIFIER, "stmt3", 28),
                                new Token(Token.Type.OPERATOR, ";", 33),
                                new Token(Token.Type.IDENTIFIER, "END", 35)
                        ),
                        new Ast.Statement.While(
                                new Ast.Expression.Access(Optional.empty(), "expr"),
                                Arrays.asList(
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt1")),
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt2")),
                                        new Ast.Statement.Expression(new Ast.Expression.Access(Optional.empty(), "stmt3"))
                                )
                        )
                ),

                Arguments.of(
                        "While Missing END",
                        //WHILE expr DO stmt;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18)
                        ),
                        null
                )
        );
    }










    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
                Arguments.of(
                        "Return",
                        //RETURN expr;
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)),
                        new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of(
                        "Missing Value",
                        //RETURN;
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.OPERATOR, ";", 7)),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testReturnStatement")
    void testReturnStatement(String test, List<Token> tokens, Ast.Statement.Return expected) {
        test(tokens, expected, Parser::parseStatement);
    }







    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expression.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expression.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expression.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expression.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expression.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expression.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expression.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expression.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        ))
                ),
                Arguments.of("Grouped Integers",
                        Arrays.asList(
                                //(expr1 + expr2)

                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "5", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "6", 9),
                                new Token(Token.Type.OPERATOR, ")", 10)

                        ),
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "5"),
                                new Ast.Expression.Access(Optional.empty(), "6")
                        )
                        )
                ),
                Arguments.of("Missing closing parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "abc", 1)
                        ), null
                ),
                Arguments.of("Missing closing parenthesis",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "abc", 1),
                                new Token(Token.Type.IDENTIFIER, "]", 2)
                        ), null
                )

        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expression.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 && expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "&&", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 11),
                                new Token(Token.Type.IDENTIFIER, "||", 13),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18)
                        ),
                new Ast.Expression.Binary("||",
                        new Ast.Expression.Binary("&&", new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")),
                        new Ast.Expression.Access(Optional.empty(), "expr3")
                )
                ),

                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Missing",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6)
                        ),
                       null
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 AND expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expression.Binary("AND",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary: <",
                        Arrays.asList(
                                // expr1 < expr2 < expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 7),
                                new Token(Token.Type.OPERATOR, "<", 12),
                                new Token(Token.Type.IDENTIFIER, "expr3", 3)
                        ),
                        new Ast.Expression.Binary("<=",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Access(Optional.empty(), "expr2"),
                                        new Ast.Expression.Access(Optional.empty(), "expr3")))
                ),
                Arguments.of("Missing Operands",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6)
                        ),
                        null
                ),
                Arguments.of("Basic Arithmetic",
                        Arrays.asList(
                                //expr1 + expr2 - expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 7)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Chain Arithmetic",
                        Arrays.asList(
                                //1 + 2 * 3
                                new Token(Token.Type.INTEGER, "1", 0),
                                new Token(Token.Type.OPERATOR, "+", 1),
                                new Token(Token.Type.INTEGER, "2", 2),
                                new Token(Token.Type.OPERATOR, "*", 3),
                                new Token(Token.Type.INTEGER, "3", 4)
                        ),
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(new BigInteger("1")),
                                new Ast.Expression.Binary("*",
                                        new Ast.Expression.Literal(new BigInteger("2")),
                                        new Ast.Expression.Literal(new BigInteger("3")))
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "variable", 0),
                                new Token(Token.Type.OPERATOR, "-", 7),
                                new Token(Token.Type.INTEGER, "8", 8),
                                new Token(Token.Type.OPERATOR, "+", 9),
                                new Token(Token.Type.INTEGER, "5", 10)
                        ),
                        new Ast.Expression.Binary(
                                "+",
                                new Ast.Expression.Binary("-", new Ast.Expression.Access(Optional.empty(), "variable"), new Ast.Expression.Literal(8)),
                                new Ast.Expression.Literal(5)
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expression.Binary("*",
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Missing Expression for Binary",
                        Arrays.asList(
                                //expr == &&
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.OPERATOR, "&&", 8)
                        ),
                        null,
                        new ParseException("Invalid", 8)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expression.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expression.Access(Optional.empty(), "name")
                ),
                Arguments.of("List Index Access",
                        Arrays.asList(
                                //list[expr]
                                new Token(Token.Type.IDENTIFIER, "list", 0),
                                new Token(Token.Type.OPERATOR, "[", 4),
                                new Token(Token.Type.IDENTIFIER, "expr", 5),
                                new Token(Token.Type.OPERATOR, "]", 9)
                        ),
                        new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "expr")), "list")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expression.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expression.Function("name", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "expr1"),
                                new Ast.Expression.Access(Optional.empty(), "expr2"),
                                new Ast.Expression.Access(Optional.empty(), "expr3")
                        ))
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "x", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "c", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "b", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "a", 19),
                                new Token(Token.Type.OPERATOR, ",", 24),
                                new Token(Token.Type.IDENTIFIER, "d", 26),
                                new Token(Token.Type.OPERATOR, ",", 31),
                                new Token(Token.Type.IDENTIFIER, "e", 33),
                                new Token(Token.Type.OPERATOR, ")", 40)
                        ),
                        new Ast.Expression.Function("x", Arrays.asList(
                                new Ast.Expression.Access(Optional.empty(), "c"),
                                new Ast.Expression.Access(Optional.empty(), "b"),
                                new Ast.Expression.Access(Optional.empty(), "a"),
                                new Ast.Expression.Access(Optional.empty(), "d"),
                                new Ast.Expression.Access(Optional.empty(), "e")
                        ))
                )
        );
    }

    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            assertEquals(expected, function.apply(parser));
        } else {
            assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

    private static <T extends Ast> void testParseException(List<Token> tokens, Exception exception, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        ParseException pe = assertThrows(ParseException.class, () -> function.apply(parser));
        assertEquals(exception, pe);
    }



    @ParameterizedTest
    @MethodSource
    void testScenarioParseException(String test, List<Token> tokens, ParseException exception) {
        testParseException(tokens, exception, Parser::parseExpression);
    }
    private static Stream<Arguments> testScenarioParseException() {
        return Stream.of(
                Arguments.of("Missing Closing Parenthesis",
                        Arrays.asList(
                                //012345
                                //(expr
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        new ParseException("Expected closing parenthesis `)`.", 5)
                ),
                Arguments.of("Missing Closing Bracket",
                        Arrays.asList(
                                //012345
                                //(expr
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, "[", 4)
                        ),
                        new ParseException("Expected closing bracket `]`.", 5)
                ),
                Arguments.of("Missing Closing Parenthesis ",
                        Arrays.asList(
                                //012345
                                //(expr
                                new Token(Token.Type.IDENTIFIER, "expr", 0),
                                new Token(Token.Type.OPERATOR, "(", 4)
                        ),
                        new ParseException("Expected closing parenthesis `)`.", 5)
                ),
                Arguments.of("Missing Value ",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5)
                        ),
                        new ParseException("Expected value", 5)
                ),
                Arguments.of("Incorrect Index",
                        Arrays.asList(
                                new Token(Token.Type.OPERATOR, "[", 5),
                                new Token(Token.Type.IDENTIFIER, "expr", 3)
                        ),
                        new ParseException("Incorrect Index.", 5)
                ),
                Arguments.of("Trailing Comma",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.OPERATOR, ")", 11)

                        ),
                        new ParseException("Expected an Expression.", 10)
                ),
                Arguments.of("Operator with no value",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "+", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, "-", 10)

                        ),
                        new ParseException("Expected an Expression.", 10)
                ),
                Arguments.of("Operator with no value",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 4),
                                new Token(Token.Type.OPERATOR, ";", 5)


                        ),
                        new ParseException("Expected an Expression.", 5)
                )

        );


    }
    @ParameterizedTest
    @MethodSource
    void testInvalidStatements(String description, List<Token> tokens, Class<? extends Exception> expectedException, String expectedMessage) {
        Parser parser = new Parser(tokens);
        Exception exception = assertThrows(expectedException, parser::parseStatement);
        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Stream<Arguments> testInvalidStatements() {
        return Stream.of(
                Arguments.of("LET Missing Identifier",
                        //LET ;
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.OPERATOR, ";", 1)),
                        ParseException.class, "Expected identifier after LET"),

                Arguments.of("LET Missing Semicolon",
                        //LET x
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4)),
                        ParseException.class, "Expected ';'"),

                Arguments.of("LET Missing Expression",
                        //LET x = ;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.OPERATOR, ";", 8)),
                        ParseException.class, "Expected an Expression."),

                Arguments.of("LET Wrong Operator",
                        //LET x == expr;
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "x", 4),
                                new Token(Token.Type.IDENTIFIER, "=", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.OPERATOR, "expr", 6),
                                new Token(Token.Type.OPERATOR, ";", 8)),
                        ParseException.class, "Expected an Expression."),

                // Expression tests
                Arguments.of("Expression Missing Value",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "x", 0),
                                new Token(Token.Type.OPERATOR, "=", 2),
                                new Token(Token.Type.OPERATOR, ";", 4)),
                        ParseException.class, "Expected expression after '='"),

                // RETURN tests
                Arguments.of("RETURN Missing Expression",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER,
                                        "RETURN", 0),
                                new Token(Token.Type.OPERATOR, ";", 7)),
                        ParseException.class, "Expected an Expression."),

                // SWITCH tests
                Arguments.of(
                        "SWITCH Missing DEFAULT",
                        //SWITCH expr END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "END", 11)),
                        ParseException.class, "Expected DEFAULT before END"),

                Arguments.of("SWITCH Missing END",
                        //SWITCH expr DEFAULT
                        Arrays.asList(new Token(Token.Type.IDENTIFIER,
                                        "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 11)),
                        ParseException.class, "Expected END after SWITCH condition"),

                Arguments.of("SWITCH Missing expr",
                        //SWITCH DEFAULT stmt; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 7),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.IDENTIFIER, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 19)
                        ),
                        ParseException.class, "Expected expr after IF8"),

                Arguments.of("Missing CASE ':'",
                        //SWITCH expr1 CASE expr2 stmt1; DEFAULT stmt2; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                                new Token(Token.Type.IDENTIFIER, "expr2", 16),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 21),
                                new Token(Token.Type.IDENTIFIER, ";", 26),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 27),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 34),
                                new Token(Token.Type.IDENTIFIER, ";", 39),
                                new Token(Token.Type.IDENTIFIER, "END", 42)
                        ),
                        ParseException.class, "Expected : after CASE conditions21"),

                Arguments.of("Missing expr in CASE",
                        //SWITCH expr1 CASE : stmt1; DEFAULT stmt2; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 7),
                                new Token(Token.Type.IDENTIFIER, "CASE", 12),
                                new Token(Token.Type.IDENTIFIER, ":", 16),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 17),
                                new Token(Token.Type.IDENTIFIER, ";", 22),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 23),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 30),
                                new Token(Token.Type.IDENTIFIER, ";", 35),
                                new Token(Token.Type.IDENTIFIER, "END", 36)
                        ),
                        ParseException.class, "Expected : after CASE conditions17"),


                Arguments.of("Missing CASE ;",
                        //SWITCH expr1 CASE expr2 : stmt1 DEFAULT stmt2; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 11),
                                new Token(Token.Type.IDENTIFIER, "CASE", 11),
                                new Token(Token.Type.IDENTIFIER, "expr2", 11),
                                new Token(Token.Type.IDENTIFIER, ":", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 11),
                                new Token(Token.Type.IDENTIFIER, ";", 11),
                                new Token(Token.Type.IDENTIFIER, "END", 11)
                        ),
                        ParseException.class, "Expected ';' 2"),

                Arguments.of("Missing DEFAULT in CASE ",
                        //SWITCH expr1 CASE expr2 : stmt1; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 11),
                                new Token(Token.Type.IDENTIFIER, ";", 11),
                                new Token(Token.Type.IDENTIFIER, "CASE", 11),
                                new Token(Token.Type.IDENTIFIER, "expr2", 11),
                                new Token(Token.Type.IDENTIFIER, ":", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.IDENTIFIER, ";", 11),
                                new Token(Token.Type.IDENTIFIER, "END", 11)
                        ),
                        ParseException.class, "Expected DEFAULT before END"),


                Arguments.of("Missing END in CASE",
                        //SWITCH expr1 CASE expr2 : stmt1; DEFAULT stmt2;
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "SWITCH", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 11),
                                new Token(Token.Type.IDENTIFIER, "CASE", 11),
                                new Token(Token.Type.IDENTIFIER, "expr2", 11),
                                new Token(Token.Type.IDENTIFIER, ":", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.IDENTIFIER, ";", 11),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 11),
                                new Token(Token.Type.IDENTIFIER, ";", 11)
                        ),
                        ParseException.class, "Expected END after SWITCH conditions"),


                // WHILE tests
                Arguments.of(
                        "WHILE Missing END",
                        //WHILE expr DO stmt;
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.IDENTIFIER, ";", 17)
                        ),
                        ParseException.class, "Expected END after WHILE conditions"),

                Arguments.of(
                        "WHILE Missing expr",
                        //WHILE DO stmt; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "DO", 6),
                                new Token(Token.Type.IDENTIFIER, "stmt", 8),
                                new Token(Token.Type.IDENTIFIER, ";", 12),
                                new Token(Token.Type.IDENTIFIER, "END", 13)
                        ),
                        ParseException.class, "Expected expr after WHILE7"),

                Arguments.of(
                        "WHILE Missing DO",
                        //WHILE expr stmt; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.IDENTIFIER, ";", 17),
                                new Token(Token.Type.IDENTIFIER, "END", 18)
                        ),
                        ParseException.class, "Expected DO after WHILE conditions"),
                Arguments.of(
                        "WHILE Missing ;",
                        //WHILE expr DO stmt END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.IDENTIFIER, "DO", 14),
                                new Token(Token.Type.IDENTIFIER, "stmt", 17),
                                new Token(Token.Type.IDENTIFIER, "END", 18)
                        ),
                        ParseException.class, "Expected ';' 2"),

                // IF tests
                Arguments.of(
                        "IF Missing DO",
                        //IF expr
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3)),
                        ParseException.class, "Expected DO after IF7"),
                Arguments.of(
                        "IF Missing expr",
                        //IF DO stmt; END
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "DO", 3),
                                new Token(Token.Type.IDENTIFIER, "stmt", 5),
                                new Token(Token.Type.IDENTIFIER, ";", 9),
                                new Token(Token.Type.IDENTIFIER, "END", 10)
                        ),
                        ParseException.class, "Expected expr after IF4"),


                // General
                Arguments.of(
                        "Mixmatched",
                        //IF expr DO SWITCH expr DEFAULT
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 7),
                                new Token(Token.Type.IDENTIFIER, "SWITCH", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 15),
                                new Token(Token.Type.IDENTIFIER, "DEFAULT", 19),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        ParseException.class, "Expected DO after condition")


        );
    }
}
