package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Tests have been provided for a few selective parts of the AST, and are not
 * exhaustive. You should add additional tests for the remaining parts and make
 * sure to handle all of the cases defined in the specification which have not
 * been tested here.
 */
public final class AnalyzerTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testSource(String test, Ast.Source ast, Ast.Source expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            expected.getGlobals().forEach(global -> Assertions.assertEquals(global.getVariable(), analyzer.scope.lookupVariable(global.getName())));
            expected.getFunctions().forEach(fun -> Assertions.assertEquals(fun.getFunction(), analyzer.scope.lookupFunction(fun.getName(), fun.getParameters().size())));
        }
    }
    private static Stream<Arguments> testSource() {
        return Stream.of(
                // VAR value: Boolean = TRUE; FUN main(): Integer DO RETURN value; END
                Arguments.of("Invalid Return",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("value", "Boolean", true, Optional.of(new Ast.Expression.Literal(true)))
                                ),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Return(new Ast.Expression.Access(Optional.empty(), "value")))
                                        )
                                )
                        ),
                        null
                ),
                // FUN main() DO RETURN 0; END
                Arguments.of("Missing Integer Return Type for Main",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                                            new Ast.Statement.Return(new Ast.Expression.Literal(new BigInteger("0"))))
                                        )
                                )
                        ),
                        null
                ),
                Arguments.of("Invalid Global Use",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("num", "Integer", true, Optional.of(new Ast.Expression.Literal(new BigInteger("1"))))
                                ),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                                new Ast.Statement.Expression(
                                                        new Ast.Expression.Function(
                                                                "print",
                                                                Arrays.asList(
                                                                        new Ast.Expression.Binary("+",
                                                                                new Ast.Expression.Access(Optional.empty(), "num"),
                                                                                new Ast.Expression.Literal(new BigDecimal("1.0"))
                                                                        )
                                                                )
                                                        )
                                                )
                                        ))
                                )
                        ),
                        null
                ),
                Arguments.of("Invalid Return Type",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.empty(), Arrays.asList(
                                                new Ast.Statement.Expression(
                                                        new Ast.Expression.Function(
                                                                "print",
                                                                Arrays.asList(
                                                                        new Ast.Expression.Literal("Hello, World!")
                                                                )
                                                        )
                                                )
                                        ))
                                )
                        ),
                        null
                ),
                Arguments.of(
                        "Valid Main",
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(
                                        new Ast.Function(
                                                "main",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("Integer"),
                                                Arrays.asList()
                                        )
                                )
                        ),
                        init(new Ast.Source(
                                        Arrays.asList(),
                                        Arrays.asList(
                                                new Ast.Function(
                                                        "main",
                                                        Arrays.asList(),
                                                        Arrays.asList(),
                                                        Optional.of("Integer"),
                                                        Arrays.asList()
                                                )
                                        )), ast -> {
                                    // Simulate what would be expected in a successful case
                                    // For example, setting a function that's found in the scope with the correct return type
                                    Environment.Function function = new Environment.Function("main", "main", Arrays.asList(), Environment.getType("Integer"), args -> Environment.NIL);
                                    ast.getFunctions().get(0).setFunction(function);
                                }
                        )
                ),
                Arguments.of(
                        "Global Use in Function",
                        new Ast.Source(
                                Arrays.asList(
                                        new Ast.Global("num", "Integer", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))
                                ),
                                Arrays.asList(
                                        new Ast.Function(
                                                "main",
                                                Arrays.asList(),
                                                Arrays.asList(),
                                                Optional.of("Integer"),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                new Ast.Expression.Function(
                                                                        "print",
                                                                        Arrays.asList(new Ast.Expression.Access(Optional.empty(), "num"))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),
                        init(new Ast.Source(
                                        Arrays.asList(
                                                new Ast.Global("num", "Integer", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE)))
                                        ),
                                        Arrays.asList(
                                                new Ast.Function(
                                                        "main",
                                                        Arrays.asList(),
                                                        Arrays.asList(),
                                                        Optional.of("Integer"),
                                                        Arrays.asList(
                                                                new Ast.Statement.Expression(
                                                                        init(new Ast.Expression.Function(
                                                                                "print",
                                                                                Arrays.asList(
                                                                                        init(new Ast.Expression.Access(Optional.empty(), "num"), ast -> {
                                                                                            Environment.Variable numVar = new Environment.Variable("num", "num", Environment.Type.INTEGER, true, new Environment.PlcObject(Environment.Type.INTEGER, new Scope(null), BigInteger.ONE));
                                                                                            ast.setVariable(numVar);
                                                                                        })
                                                                                )
                                                                        ), ast -> {
                                                                            // Assuming print is a valid function and registered in the scope
                                                                            ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.INTEGER), Environment.Type.NIL, args -> Environment.NIL));
                                                                        })
                                                                )
                                                        )
                                                )
                                        )), ast -> {
                                    // Assuming the setting up of the global variable is correct in the environment
                                    Environment.Variable numVar = new Environment.Variable("num", "num", Environment.Type.INTEGER, true, new Environment.PlcObject(Environment.Type.INTEGER, new Scope(null), BigInteger.ONE));
                                    ast.getGlobals().get(0).setVariable(numVar);
                                    // Setup function in environment if needed
                                    Environment.Function mainFunction = new Environment.Function("main", "main", Arrays.asList(), Environment.getType("Integer"), args -> Environment.NIL);
                                    ast.getFunctions().get(0).setFunction(mainFunction);
                                }
                        )
                )



        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testGlobal(String test, Ast.Global ast, Ast.Global expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }

    private static Stream<Arguments> testGlobal() {
        return Stream.of(
                Arguments.of("Declaration",
                        // VAR name: Integer;
                        new Ast.Global("name", "Integer", true, Optional.empty()),
                        init(new Ast.Global("name", "Integer", true, Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL));
                        })
                ),
                Arguments.of("Variable Type Mismatch",
                        // VAR name: Decimal = 1;
                        new Ast.Global("name", "Decimal", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        null
                ),
                Arguments.of("Unknown Type",
                        // VAR name: Unknown;
                        new Ast.Global("name", "Unknown", true, Optional.empty()),
                        null
                ),
                Arguments.of(
                        "Initialization with Integer",
                        //VAR name: Integer = 1;
                        new Ast.Global("name", "Integer", true, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Global("name", "Integer", true, Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL));
                        })
                ),

                //                Arguments.of("Correct List Declaration",
                //                        new Ast.Global("list", "Integer", true,
                //                                Optional.of(new Ast.Expression.PlcList(Arrays.asList(
                //                                        new Ast.Expression.Literal(BigInteger.ONE),
                //                                        new Ast.Expression.Literal(BigInteger.valueOf(2)),
                //                                        new Ast.Expression.Literal(BigInteger.valueOf(3)))))),
                //                        null
                //                ),
                Arguments.of(
                        "List Initialization with Integer Values",
                        new Ast.Global("list", "Integer", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.valueOf(2)),
                                new Ast.Expression.Literal(BigInteger.valueOf(3))
                        )))),
                        init(new Ast.Global("list", "Integer", true, Optional.empty()), ast -> {
                            // Assuming Environment.Type.INTEGER correctly represents the type object for integers.
                            Environment.Type integerType = Environment.getType("Integer");
                            List<Environment.PlcObject> integerValues = Arrays.asList(
                                    new Environment.PlcObject(integerType, new Scope(null), BigInteger.ONE),
                                    new Environment.PlcObject(integerType, new Scope(null), BigInteger.valueOf(2)),
                                    new Environment.PlcObject(integerType, new Scope(null), BigInteger.valueOf(3))
                            );
                            Environment.PlcObject listValue = new Environment.PlcObject(integerType, new Scope(null), integerValues);
                            Environment.Variable listVariable = new Environment.Variable("list", "list", integerType, true, listValue);
                            ast.setVariable(listVariable);
                        })
                ),
                Arguments.of(
                        "List Type Mismatch with Decimals in Integer List",
                        // LIST list: Integer = [1.0, 2.0, 3.0];
                        new Ast.Global("list", "Integer", true, Optional.of(new Ast.Expression.PlcList(Arrays.asList(
                                new Ast.Expression.Literal(new BigDecimal("1.0")),
                                new Ast.Expression.Literal(new BigDecimal("2.0")),
                                new Ast.Expression.Literal(new BigDecimal("3.0"))
                        )))),
                        RuntimeException.class  // Expecting a RuntimeException due to type mismatch
                ),
                Arguments.of(
                        "Immutable Decimal Initialization",
                        new Ast.Global(
                                "name",
                                "Decimal",
                                false,  // false indicates it is immutable (VAL keyword)
                                Optional.of(new Ast.Expression.Literal(new BigDecimal("1.0")))
                        ),
                        init(new Ast.Global("name", "Decimal", false, Optional.empty()), ast -> {
                            Environment.Type decimalType = Environment.getType("Decimal");
                            Environment.PlcObject decimalValue = new Environment.PlcObject(decimalType, new Scope(null), new BigDecimal("1.0"));
                            Environment.Variable decimalVariable = new Environment.Variable("name", "name", decimalType, false, decimalValue);
                            ast.setVariable(decimalVariable);
                        })
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunction(String test, Ast.Function ast, Ast.Function expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getFunction(), analyzer.scope.lookupFunction(expected.getName(), expected.getParameters().size()));
        }
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
                Arguments.of("Hello World",
                        // FUN main(): Integer DO print("Hello, World!"); END
                        // Recall note under Ast.Function, we do not check for missing RETURN
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(
                                        new Ast.Expression.Literal("Hello, World!")
                                )))
                         )),
                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.Expression(init(new Ast.Expression.Function("print", Arrays.asList(
                                        init(new Ast.Expression.Literal("Hello, World!"), ast -> ast.setType(Environment.Type.STRING))
                                )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                        )), ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Return Type Mismatch",
                        // FUN increment(num: Integer): Decimal DO RETURN num + 1; END
                        new Ast.Function("increment", Arrays.asList("num"), Arrays.asList("Integer"), Optional.of("Decimal"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                ))
                        )),
                        null
                ),

                Arguments.of("Return 0",
                        // FUN main(): Integer DO RETURN 0; END
                        new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"),
                                Arrays.asList(
                                        new Ast.Statement.Return(new Ast.Expression.Literal(new BigInteger("0")))
                                )
                        ),
                        init(new Ast.Function("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                                new Ast.Statement.Return(init(new Ast.Expression.Literal(new BigInteger("0")), ast -> ast.setType(Environment.Type.INTEGER)))
                        )),
                        ast -> ast.setFunction(new Environment.Function("main", "main", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                ),
                Arguments.of("Return Type Mismatch",
                        // FUN increment(num: Integer): Decimal DO RETURN num + 1; END
                        new Ast.Function("increment", Arrays.asList("num"), Arrays.asList("Integer"), Optional.of("Decimal"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "num"),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                ))
                        )),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Ast.Statement.Declaration expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()),
                        init(new Ast.Statement.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL));
                        })
                ),
                Arguments.of("Initialization",
                        // LET name = 1;
                        new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                        init(new Ast.Statement.Declaration("name", Optional.empty(), Optional.of(
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )), ast -> ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, true, Environment.NIL)))
                ),
                Arguments.of(
                        "Initialization with Explicit Type and Value",
                        // LET name: Integer = 1;
                        new Ast.Statement.Declaration(
                                "name",
                                Optional.of("Integer"),  // Explicit type declaration
                                Optional.of(new Ast.Expression.Literal(BigInteger.ONE))  // Explicit initialization with value '1'
                        ),
                        init(new Ast.Statement.Declaration(
                                "name",
                                Optional.of("Integer"),
                                Optional.of(new Ast.Expression.Literal(BigInteger.ONE))
                        ), ast -> {
                            ast.setVariable(new Environment.Variable(
                                    "name",
                                    "name",
                                    Environment.Type.INTEGER,  // The type of the variable
                                    true,  // Assuming variables are mutable by default, adjust if your context is different
                                    new Environment.PlcObject(Environment.Type.INTEGER, new Scope(null), BigInteger.ONE)  // Properly creating a PLCObject with value
                            ));
                            // Properly setting the type of the literal to match the declared type
                            ((Ast.Expression.Literal)ast.getValue().get()).setType(Environment.Type.INTEGER);
                        })
                ),
                Arguments.of(
                        "Invalid Type",
                        // LET name: Integer = 1.0;
                        new Ast.Statement.Declaration(
                                "name",
                                Optional.of("Integer"),  // Variable is declared to be of type Integer
                                Optional.of(new Ast.Expression.Literal(new BigDecimal("1.0")))  // Initialization with a Decimal value
                        ),
                        null
                ),
                Arguments.of(
                        "Self-Referencing Initialization",
                        // LET name: Integer = name;
                        new Ast.Statement.Declaration(
                                "name",
                                Optional.of("Integer"),  // Declares the variable as an Integer
                                Optional.of(new Ast.Expression.Access(Optional.empty(), "name"))  // Attempts to initialize with itself
                        ),
                        null  // Expected outcome is failure, so we provide `null` to indicate that the test should expect an exception or specific error
                )

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAssignmentStatement(String test, Ast.Statement.Assignment ast, Ast.Statement.Assignment expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL);
        }));
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Variable",
                        // variable = 1;
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "variable"),
                                new Ast.Expression.Literal(BigInteger.ONE)
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                        )
                ),
                Arguments.of("Invalid Type",
                        // variable = "string";
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.empty(), "variable"),
                                new Ast.Expression.Literal("string")
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testListAssignmentStatement(String test, Ast.Statement.Assignment ast, Ast.Statement.Assignment expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("list", "list", Environment.Type.INTEGER, true, Environment.NIL);
        }));
    }
    private static Stream<Arguments> testListAssignmentStatement() {
        return Stream.of(
                Arguments.of(
                        "List Assignment",
                        // list[1] = 7;
                        //add listVar to scope used by analyzer
                        new Ast.Statement.Assignment(
                                new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.ONE)), "list"),
                                new Ast.Expression.Literal(BigInteger.valueOf(7))
                        ),
                        new Ast.Statement.Assignment(
                                init(new Ast.Expression.Access(Optional.of(new Ast.Expression.Literal(BigInteger.ONE)), "list"), ast -> ast.setVariable(new Environment.Variable("list", "list", Environment.Type.INTEGER, true, Environment.NIL))),
                                init(new Ast.Expression.Literal(BigInteger.valueOf(7)), ast -> ast.setType(Environment.Type.INTEGER))
                        )
                )

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testIfStatement(String test, Ast.Statement.If ast, Ast.Statement.If expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("Valid Condition",
                        // IF TRUE DO print(1); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function("print", Arrays.asList(
                                                new Ast.Expression.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        new Ast.Statement.If(
                                init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(new Ast.Statement.Expression(
                                        init(new Ast.Expression.Function("print", Arrays.asList(
                                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Invalid Condition",
                        // IF "FALSE" DO print(1); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal("FALSE"),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function("print", Arrays.asList(
                                            new Ast.Expression.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Invalid Statement",
                        // IF TRUE DO print(9223372036854775807); END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Statement.Expression(
                                        new Ast.Expression.Function("print", Arrays.asList(
                                                new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE))
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Empty Statements",
                        // IF TRUE DO END
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(Boolean.TRUE),
                                Arrays.asList(),
                                Arrays.asList()
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testSwitchStatement(String test, Ast.Statement.Switch ast, Ast.Statement.Switch expected) {
        test(ast, expected,
                init(new Scope(null),
                        scope -> {
                            // we need letter and number to be defined within the scope in order to analyze the switch examples

                            // note:  recall during the Analyzer, letter and number could have been initialized Environment.NIL,
                            //        the types are what we are concerned with in the Analyzer and not the evaluation of what is stored within the variables.
                            scope.defineVariable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y'));
                            scope.defineVariable("number", "number", Environment.Type.INTEGER, true, Environment.create(new BigInteger("1")));
                        }
                )
        );
    }

    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Condition Value Type Match",
                        // SWITCH letter CASE 'y': print("yes"); letter = 'n'; DEFAULT print("no"); END
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(),"letter"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Literal('y')),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                                                        new Ast.Statement.Assignment(
                                                                new Ast.Expression.Access(Optional.empty(), "letter"),
                                                                new Ast.Expression.Literal('n')
                                                        )
                                                )
                                       ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no"))))
                                                )
                                        )
                                )
                        ),
                        new Ast.Statement.Switch(
                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(init(new Ast.Expression.Literal('y'), ast -> ast.setType(Environment.Type.CHARACTER))),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("yes"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                      ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        ),
                                                        new Ast.Statement.Assignment(
                                                                init(new Ast.Expression.Access(Optional.empty(), "letter"), ast -> ast.setVariable(new Environment.Variable("letter", "letter", Environment.Type.CHARACTER, true, Environment.create('y')))),
                                                                init(new Ast.Expression.Literal('n'), ast -> ast.setType(Environment.Type.CHARACTER))
                                                        )
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(
                                                                init(new Ast.Expression.Function("print", Arrays.asList(init(new Ast.Expression.Literal("no"), ast -> ast.setType(Environment.Type.STRING)))),
                                                                        ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                Arguments.of("Condition Value Type Mismatch",
                        // SWITCH number CASE 'y': print("yes"); letter = 'n'; DEFAULT: print("no"); END
                        new Ast.Statement.Switch(
                                new Ast.Expression.Access(Optional.empty(),"number"),
                                Arrays.asList(
                                        new Ast.Statement.Case(
                                                Optional.of(new Ast.Expression.Literal('y')),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("yes")))),
                                                        new Ast.Statement.Assignment(
                                                                new Ast.Expression.Access(Optional.empty(), "letter"),
                                                                new Ast.Expression.Literal('n')
                                                        )
                                                )
                                        ),
                                        new Ast.Statement.Case(
                                                Optional.empty(),
                                                Arrays.asList(
                                                        new Ast.Statement.Expression(new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal("no"))))
                                                )
                                        )
                                )
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testLiteralExpression(String test, Ast.Expression.Literal ast, Ast.Expression.Literal expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean",
                        // TRUE
                        new Ast.Expression.Literal(true),
                        init(new Ast.Expression.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Integer Valid",
                        // 2147483647
                        new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)),
                        init(new Ast.Expression.Literal(BigInteger.valueOf(Integer.MAX_VALUE)), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Invalid",
                        // 9223372036854775807
                        new Ast.Expression.Literal(BigInteger.valueOf(Long.MAX_VALUE)),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testGroupExpressionProvider")
    public void testGroupExpression(String test, Ast.Expression.Group ast, Class<? extends Throwable> expectedException, Environment.Type expectedType) {
        Analyzer analyzer = new Analyzer(new Scope(null));
        if (expectedException != null) {
            Assertions.assertThrows(expectedException, () -> analyzer.visit(ast));
        } else {
            analyzer.visit(ast);
            Assertions.assertEquals(expectedType, ast.getType());
        }
    }

    private static Stream<Arguments> testGroupExpressionProvider() {
        return Stream.of(
                Arguments.of(
                        "Grouped Literal",
                        new Ast.Expression.Group(new Ast.Expression.Literal(BigInteger.ONE)),
                        RuntimeException.class,
                        null
                ),
                Arguments.of(
                        "Grouped Binary",
                        new Ast.Expression.Group(new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        )),
                        null,
                        Environment.Type.INTEGER
                )
        );
    }

    @ParameterizedTest
    @MethodSource("expressionTestProvider")
    public void testExpressionStatement(Ast.Statement.Expression input, boolean shouldThrowException) {
        Analyzer analyzer = new Analyzer(new Scope(null));
        if (shouldThrowException) {
            Assertions.assertThrows(RuntimeException.class, () -> analyzer.visit(input));
        } else {
            Assertions.assertDoesNotThrow(() -> analyzer.visit(input));
        }
    }

    private static Stream<Arguments> expressionTestProvider() {
        return Stream.of(
                //print(1);
                Arguments.of(new Ast.Statement.Expression(
                        new Ast.Expression.Literal(BigInteger.ONE)
                ), true),
                //1;
                Arguments.of(new Ast.Statement.Expression(
                        new Ast.Expression.Function("print", Arrays.asList(new Ast.Expression.Literal(BigInteger.ONE)))
                ), null)
        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testBinaryExpression(String test, Ast.Expression.Binary ast, Ast.Expression.Binary expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Logical AND Valid",
                        // TRUE && FALSE
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(Boolean.TRUE),
                                new Ast.Expression.Literal(Boolean.FALSE)
                        ),
                        init(new Ast.Expression.Binary("&&",
                                init(new Ast.Expression.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expression.Literal(Boolean.FALSE), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Equal Same Types ",
                        // 1 == 10
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("==",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Not Equal Same Types",
                        // 1 != 10
                        new Ast.Expression.Binary("!=",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("!=",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Not Equal Same Types",
                        // 1 < 10
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("<",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Logical AND Invalid",
                        // TRUE && "FALSE"
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(Boolean.TRUE),
                                new Ast.Expression.Literal("FALSE")
                        ),
                        null
                ),
                Arguments.of("String Concatenation",
                        // "Ben" + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("Ben"),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING))
                ),
                Arguments.of("Integer Addition",
                        // 1 + 10
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expression.Binary("+",
                                init(new Ast.Expression.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expression.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Decimal Addition",
                        // 1 + 1.0
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigDecimal.ONE)
                        ),
                        null
                )

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testAccessExpression(String test, Ast.Expression.Access ast, Ast.Expression.Access expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineVariable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL);
        }));
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        // variable
                        new Ast.Expression.Access(Optional.empty(), "variable"),
                        init(new Ast.Expression.Access(Optional.empty(), "variable"), ast -> ast.setVariable(new Environment.Variable("variable", "variable", Environment.Type.INTEGER, true, Environment.NIL)))
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testFunctionExpression(String test, Ast.Expression.Function ast, Ast.Expression.Function expected) {
        test(ast, expected, init(new Scope(null), scope -> {
            scope.defineFunction("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL);
        }));
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Function",
                        // function()
                        new Ast.Expression.Function("function", Arrays.asList()),
                        init(new Ast.Expression.Function("function", Arrays.asList()), ast -> ast.setFunction(new Environment.Function("function", "function", Arrays.asList(), Environment.Type.INTEGER, args -> Environment.NIL)))
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testRequireAssignable(String test, Environment.Type target, Environment.Type type, boolean success) {
        if (success) {
            Assertions.assertDoesNotThrow(() -> Analyzer.requireAssignable(target, type));
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> Analyzer.requireAssignable(target, type));
        }
    }

    private static Stream<Arguments> testRequireAssignable() {
        return Stream.of(
                Arguments.of("Integer to Integer", Environment.Type.INTEGER, Environment.Type.INTEGER, true),
                Arguments.of("Integer to Decimal", Environment.Type.DECIMAL, Environment.Type.INTEGER, false),
                Arguments.of("Integer to Comparable", Environment.Type.COMPARABLE, Environment.Type.INTEGER,  true),
                Arguments.of("Integer to Any", Environment.Type.ANY, Environment.Type.INTEGER, true),
                Arguments.of("Any to Integer", Environment.Type.INTEGER, Environment.Type.ANY, false)
        );
    }

    /**
     * Helper function for tests. If {@param expected} is {@code null}, analysis
     * is expected to throw a {@link RuntimeException}.
     */
    private static <T extends Ast> Analyzer test(T ast, T expected, Scope scope) {
        Analyzer analyzer = new Analyzer(scope);
        if (expected != null) {
            analyzer.visit(ast);
            Assertions.assertEquals(expected, ast);
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> analyzer.visit(ast));
        }
        return analyzer;
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
