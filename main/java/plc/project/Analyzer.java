package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for(Ast.Global global : ast.getGlobals()) {
            visit(global);
        }

        boolean main = false;
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
            if (function.getName().equals("main") && function.getParameters().isEmpty()) {
                if (function.getReturnTypeName().orElse("").equals("Integer")) {
                    main = true;
                } else {
                    throw new RuntimeException("The main function must have an Integer return type.");
                }
            }
        }
        if (!main) {
            throw new RuntimeException("A main/0 function does not exist.");
        }
        return null;
    }



    @Override
    public Void visit(Ast.Global ast) {
        Environment.Type declaredType = Environment.getType(ast.getTypeName());

        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), declaredType, ast.getMutable(), Environment.NIL);
        if (ast.getValue().isPresent()) {
            Ast.Expression value = ast.getValue().get();
            Object res = visit(value);

            requireAssignable(declaredType, value.getType());

            Environment.PlcObject evalValue = new Environment.PlcObject(declaredType, scope, res);
            variable.setValue(evalValue);
        }
        ast.setVariable(variable);
        return null;
    }





    @Override
    public Void visit(Ast.Function ast) {
        final Scope definingScope = scope;
        try {
            scope = new Scope(definingScope);
            List<String> parameters = ast.getParameterTypeNames();
            Environment.Type[] parameterType = new Environment.Type[parameters.size()];
            if(!parameters.isEmpty()){
                for(int i = 0; i < parameters.size(); i++){
                    parameterType[i] = Environment.getType(parameters.get(i));
                    scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterType[i], true, Environment.NIL);
                }
            }
            Environment.Type expectedReturnType = Environment.Type.NIL;
            if (ast.getReturnTypeName().isPresent()){
                expectedReturnType = Environment.getType(ast.getReturnTypeName().get());

                requireAssignable(expectedReturnType, (Environment.getType(ast.getReturnTypeName().get())));
//                if (!expectedReturnType.equals(Environment.getType(ast.getReturnTypeName().get()))) {
//                    throw new RuntimeException("Return Type Mismatch: Expected , received ");
//                }
                scope.defineVariable("returnType", "returnType", Environment.getType(ast.getReturnTypeName().get()), false, Environment.NIL);
                scope.defineFunction(ast.getName(), ast.getName(), Arrays.asList(parameterType), Environment.getType(ast.getReturnTypeName().get()), (args) -> {
                    try {
                        scope = new Scope(definingScope);
                        int i = 0;
                        for (String var : ast.getParameters()) {
                            scope.defineVariable(var, var, parameterType[i], true, args.get(i));
                            i++;
                        }
                    }
                    catch (RuntimeException r) {
                        throw new RuntimeException(r);
                    }
                    finally {
                        scope = definingScope;
                    }
                    return null;
                });
            }
            else{
                scope.defineVariable("returnType", "returnType", Environment.Type.INTEGER, false, Environment.NIL);
                scope.defineFunction(ast.getName(), ast.getName(), Arrays.asList(parameterType), Environment.Type.NIL, (args) -> {
                    try {
                        scope = new Scope(definingScope);
                        int i = 0;
                        for (String var : ast.getParameters()) {
                            scope.defineVariable(var, var, parameterType[i], true, args.get(i));
                            i++;
                        }
                    }
                    catch (RuntimeException r) {
                        throw new RuntimeException(r);
                    }
                    finally {
                        scope = definingScope;
                    }
                    return null;
                });
            }
            if (!ast.getStatements().isEmpty()) {
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                    if(statement instanceof Ast.Statement.Return) {
                        Ast.Expression val = ((Ast.Statement.Return) statement).getValue();
                        visit(val);
                        Environment.Type actualType = val.getType();
                        requireAssignable(expectedReturnType, actualType);
                    }
                    }
            }
            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if(!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Must be a function");
        } else {
            visit(ast.getExpression());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type;

        if(ast.getTypeName().isPresent()) {
            String typeName = ast.getTypeName().get();
            type = Environment.getType(typeName);
        } else if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = ast.getValue().get().getType(); //uses value's type
        } else {
            throw new RuntimeException("Invalid type");
        }
        Environment.Variable variable;
        if(ast.getValue().isPresent()) {
            Ast.Expression val = ast.getValue().get();
            Object res = visit(val);
            requireAssignable(type, val.getType());

            Environment.PlcObject plcValue = new Environment.PlcObject(type, scope, res);
            variable = scope.defineVariable(ast.getName(), ast.getName(), type, true, plcValue);
        } else {
            variable = scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);
        }

        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        try {
            if(!(ast.getReceiver() instanceof Ast.Expression.Access)) {
                throw new RuntimeException("Invalid receiver type");
            }
            Ast.Expression.Access accessVal = (Ast.Expression.Access) ast.getReceiver();
            visit(accessVal);
            Environment.Variable receiverVal = accessVal.getVariable();
            visit(ast.getValue());
            requireAssignable(receiverVal.getType(), ast.getValue().getType());
        } catch (Exception e) {
            throw new RuntimeException("Invalid");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        Environment.Type ifType = ast.getCondition().getType();
        requireAssignable(Environment.Type.BOOLEAN, ifType);
        if(!ast.getThenStatements().isEmpty()) {
            try {
                scope = new Scope(scope);
                for(Ast.Statement stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } else {
            throw new RuntimeException("Then list is empty");
        }
        try {
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        Environment.Type conditionType = ast.getCondition().getType();
        boolean defaultCase = false;

        for(Ast.Statement.Case caseVal : ast.getCases()) {
            if(defaultCase) {
                throw new RuntimeException("Default with value");
            }
            if(caseVal.getValue().isPresent()) {
                visit(caseVal.getValue().get());
                requireAssignable(caseVal.getValue().get().getType(), conditionType);
            }
            try {
                scope = new Scope(scope);
                for(Ast.Statement stmt : caseVal.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        for(Ast.Statement stmt : ast.getStatements()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for(Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        try {
            visit(ast.getValue());
            Environment.Type target = ast.getValue().getType();
            visit(ast.getValue());
            Environment.Type type = ast.getValue().getType();
            requireAssignable(target, type);
        } catch (Exception e) {
            throw new RuntimeException("Invalid return");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
         Object literalVal = ast.getLiteral();

         if(literalVal == null) {
             ast.setType(Environment.Type.NIL);
         } else if(literalVal instanceof Boolean) {
             ast.setType(Environment.Type.BOOLEAN);
         } else if(literalVal instanceof Character) {
             ast.setType(Environment.Type.CHARACTER);
         } else if (literalVal instanceof String) {
             ast.setType(Environment.Type.STRING);
         } else if (literalVal instanceof BigInteger) {
             BigInteger range = (BigInteger) literalVal;
             if(range.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 || range.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                 throw new RuntimeException("Out of range");
             } else {
                 ast.setType(Environment.Type.INTEGER);
             }
         } else if(literalVal instanceof BigDecimal) {
             BigDecimal range = (BigDecimal) literalVal;
             double doubleVal = range.doubleValue();
             if(doubleVal > Float.MAX_VALUE || doubleVal < Float.MIN_VALUE) {
                 throw new RuntimeException("Out of range");
             } else {
                 ast.setType(Environment.Type.DECIMAL);
             }
         } else {
             throw new RuntimeException("Invalid literal type");
         }
         return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if(!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Must be binary expression");
        } else {
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();
        String operator = ast.getOperator();

        if(operator.equals("&&") || operator.equals("||")) {
            if(leftType.equals(Environment.Type.BOOLEAN) && rightType.equals(Environment.Type.BOOLEAN)) {
                ast.setType(Environment.Type.BOOLEAN);
            } else {
                throw new RuntimeException("Both operands must be Boolean");
            }
        } else if(operator.equals("<") || operator.equals(">") || operator.equals("==") || operator.equals("!=")) {
            requireAssignable(Environment.Type.COMPARABLE, leftType);
            requireAssignable(Environment.Type.COMPARABLE, rightType);
            ast.setType(Environment.Type.BOOLEAN);
        } else if(operator.equals("+")) {
            // case 1: either side string (other side is anything)
            if(leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            }
            else if(leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                ast.setType(Environment.Type.INTEGER);
            }
            else if(leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Invalid Types");
            }
        } else if (operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            if(leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                ast.setType(Environment.Type.INTEGER);
            } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Invalid Types");
            }
        } else if(operator.equals("^")) {
            requireAssignable(Environment.Type.INTEGER, leftType);
            requireAssignable(Environment.Type.INTEGER, rightType);
            ast.setType(Environment.Type.INTEGER);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        try{
            if (!ast.getOffset().isPresent()) {
                Environment.Variable variable = scope.lookupVariable(ast.getName());
                ast.setVariable(variable);
            }
        else {
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            Environment.PlcObject listObject = variable.getValue();
            visit(ast.getOffset().get());
//            if (!(listObject.getValue() instanceof List<?>)) {
//                throw new RuntimeException("Attempted to access a non-list variable as a list.");
//            }
            requireAssignable(ast.getOffset().get().getType(), Environment.Type.INTEGER);
            ast.setVariable(scope.lookupVariable(ast.getName()));

        }}
        catch(Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        try {
            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            List<Environment.Type> expectedTypes = function.getParameterTypes();
            for(int i = 0; i < ast.getArguments().size(); i++) {
                Ast.Expression arg = ast.getArguments().get(i);
                visit(arg);
                requireAssignable(expectedTypes.get(i), arg.getType());
            }
            ast.setFunction(function);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        if (!ast.getValues().isEmpty()) {
            Ast.Expression firstType = ast.getValues().get(0);
            visit(firstType);
            Environment.Type listType = firstType.getType();
            for (Ast.Expression expr : ast.getValues()) {
                visit(expr);
                requireAssignable(expr.getType(), listType);
            }
        ast.setType(listType);
    } else {
            ast.setType(Environment.Type.NIL);
        }
        return null;
    }


    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        //same types
        if(target.equals(type)) {
            return;
        }
        //target is any
        if(target.equals(Environment.Type.ANY)) {
            return;
        }
        //target is comparable, types can be comparable types
        if(target.equals(Environment.Type.COMPARABLE) && ((type.equals(Environment.Type.INTEGER)) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING))) {
            return;
        }
        throw new RuntimeException("Mixmatched assignments");
    }

}
