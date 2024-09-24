package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;

import java.util.Arrays;

import java.util.Comparator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        for(Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for(Ast.Function function : ast.getFunctions()) {
            visit(function);
        }

        return scope.lookupFunction("main", 0).invoke(Arrays.asList());

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(),ast.getMutable(), visit(ast.getValue().get()));
        }
        else
            scope.defineVariable(ast.getName(), ast.getMutable(),Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        if(ast.getName().isEmpty()) {
            throw new RuntimeException("undefined function");
        }
        final Scope definingScope = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), (args) -> {
            Scope originalScope = scope;
            try {
                scope = new Scope(definingScope);

                int i = 0;
                for (String var : ast.getParameters()) {
                    scope.defineVariable(var, true, args.get(i));
                    i++;
                }

                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } catch (Return returnValue) {
                return returnValue.value;
            } finally {
                scope = originalScope;
            }

            return Environment.NIL;
        });

        return Environment.NIL;



    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {

            if(ast.getValue().isPresent()) {
                scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
            } else {
                //CHECK return null value
                scope.defineVariable(ast.getName(), true,Environment.NIL);
            }
            return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Only variables and list elements can be assigned to.");
        }

        Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();

        Environment.PlcObject value = visit(ast.getValue());


        if (!access.getOffset().isPresent()) {
            Environment.Variable variable = scope.lookupVariable(access.getName());

            if (!variable.getMutable()) {
                throw new RuntimeException("Cannot assign to an immutable variable.");
            }

            variable.setValue(value);
        } else {

            Environment.Variable listVariable = scope.lookupVariable(access.getName());
            if (!(listVariable.getValue().getValue() instanceof List<?>)) {
                throw new RuntimeException("Attempted to access a non-list variable as a list.");
            }

            List<Object> list = (List<Object>) listVariable.getValue().getValue();

            Environment.PlcObject offsetObject = visit(access.getOffset().get());
            if (!(offsetObject.getValue() instanceof BigInteger)) {
                throw new RuntimeException("List offset must be a BigInteger.");
            }

            int index = ((BigInteger) offsetObject.getValue()).intValueExact();


            if (index < 0 || index >= list.size()) {
                throw new RuntimeException("List index out of bounds.");
            }


            list.set(index, value.getValue());
        }

        return Environment.NIL;
        }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Environment.PlcObject condition = visit(ast.getCondition());
        boolean checkCondition = requireType(Boolean.class, condition);

        if(checkCondition) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } else {
            try {
                scope = new Scope(scope);
                for(Ast.Statement stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Environment.PlcObject switchCondition = visit(ast.getCondition()); //switch
        boolean match = false;

        try {
            scope = new Scope(scope);
            for(Ast.Statement.Case caseVal : ast.getCases()) {
                if(caseVal.getValue().isPresent()) {
                    Environment.PlcObject caseValue = visit(caseVal.getValue().get());
                    //case 1: condition = case value --> visit case statements
                    if((switchCondition.getValue()).equals(caseValue.getValue())) {
                        match = true;
                        for (Ast.Statement stmt : caseVal.getStatements()) {
                            visit(stmt);
                        }
                    }
                }
            }
            //default statements
            if(!match) {
                for (Ast.Statement.Case caseVal : ast.getCases()) {
                    if(!caseVal.getValue().isPresent()) {
                        for(Ast.Statement stmt : caseVal.getStatements()) {
                            visit(stmt);
                        }
                    }
                }
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        if(ast.getValue().isPresent()) {
            return visit(ast.getValue().get());
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
//        Environment.PlcObject val = visit(ast.getValue());
//        throw new Return(val);
        Environment.PlcObject val = Environment.NIL;
        if(ast.getValue() != null) {
            val = visit(ast.getValue());
        }
        throw new Return(val);
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left;
        Environment.PlcObject right;
        String operator = ast.getOperator();

        if("&&".equals(operator)) {
            left = visit(ast.getLeft());
            boolean leftCheck = requireType(Boolean.class, left);
            if(!leftCheck) {
                return Environment.create(false);
            }
            right = visit(ast.getRight());
            boolean rightCheck = requireType(Boolean.class, right);
            return Environment.create(rightCheck);
        }

        else if("||".equals(operator)) {
            left = visit(ast.getLeft());
            boolean leftCheck = requireType(Boolean.class, left);
            if(leftCheck) {
                return Environment.create(true);
            }
            right = visit(ast.getRight());
            boolean rightCheck = requireType(Boolean.class, right);
            return Environment.create(rightCheck);
        }

        else if("<".equals(operator) || ">".equals(operator)) {
            left = visit(ast.getLeft());
            right = visit(ast.getRight());

            if(left.getValue() instanceof Comparable && right.getValue() instanceof Comparable) {
                Comparable leftVal = (Comparable) left.getValue();
                Comparable rightVal = (Comparable) right.getValue();

                if(leftVal.getClass().equals(rightVal.getClass())) {
                    int result = leftVal.compareTo(rightVal);
                    //leftVal < rightVal
                    if("<".equals(operator)) {
                        return Environment.create(result < 0);
                    }
                    else if(">".equals(operator)) {
                        return Environment.create(result > 0);
                    }
                    else {
                        throw new RuntimeException("They are equal");
                    }
                } else {
                    throw new RuntimeException("Different types");
                }
            } else {
                throw new RuntimeException("Invalid Comparable Values");
            }
        }


        else if("==".equals(operator) || "!=".equals(operator)) {
            left = visit(ast.getLeft());
            right = visit(ast.getRight());
            if("==".equals(operator)) {
                return Environment.create(Objects.equals(left.getValue(), right.getValue()));
            } else if("!=".equals(operator)) {
                return Environment.create(!Objects.equals(left.getValue(), right.getValue()));
            } else {
                throw new RuntimeException("Invalid Case");
            }
        }


        else if("+".equals(operator)) {
            left = visit(ast.getLeft());
            right = visit(ast.getRight());

            //Case 1: either expr a string --> concatenate
            if(left.getValue() instanceof String || right.getValue() instanceof String) {
                return Environment.create(left.getValue().toString() + right.getValue().toString());
            }
            //Case 2: both BigInteger --> add
            else if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                return Environment.create(((BigInteger) left.getValue()).add((BigInteger) right.getValue()));
            }
            //Case 3: both BigDecimal --> add
            else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                return Environment.create(((BigDecimal) left.getValue()).add((BigDecimal) right.getValue()));
            }
            else{
                throw new RuntimeException("Invalid Case");
            }
        }

        else if("-".equals(operator) || "*".equals(operator)) {
            left = visit(ast.getLeft());
            right = visit(ast.getRight());
            if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                if("-".equals(operator)) {
                    return Environment.create(((BigInteger) left.getValue()).subtract((BigInteger) right.getValue()));
                } else if ("*".equals(operator)) {
                    return Environment.create(((BigInteger) left.getValue()).multiply((BigInteger) right.getValue()));
                } else {
                    throw new RuntimeException("Invalid Operator");
                }

            }
            else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                if("-".equals(operator)) {
                    return Environment.create(((BigDecimal) left.getValue()).subtract((BigDecimal) right.getValue()));
                } else if ("*".equals(operator)) {
                    return Environment.create(((BigDecimal) left.getValue()).multiply((BigDecimal) right.getValue()));
                } else {
                    throw new RuntimeException("Invalid Operator");
                }
            }
            else {
                throw new RuntimeException("Invalid Case");
            }
        }

        else if("/".equals(operator)) {
            left = visit(ast.getLeft());
            right = visit(ast.getRight());
            //Case 1: both BigInteger
            if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                BigInteger leftVal = (BigInteger) left.getValue();
                BigInteger rightVal = (BigInteger) right.getValue();
                if(rightVal.equals(BigInteger.ZERO)) {
                    throw new RuntimeException("Cannot be divide by 0");
                }
                return Environment.create(leftVal.divide(rightVal));
            }
            //Case 2: both BigDecimal
            else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                BigDecimal leftVal = (BigDecimal) left.getValue();
                BigDecimal rightVal = (BigDecimal) right.getValue();
                if(rightVal.equals(BigDecimal.ZERO)) {
                    throw new RuntimeException("Cannot be divide by 0");
                }
                BigDecimal roundedResult =  leftVal.divide(rightVal, RoundingMode.HALF_EVEN);
                return Environment.create(roundedResult);
            }
            else {
                throw new RuntimeException("Invalid division");
            }
        }

        else if("^".equals(operator)) {
            left = visit(ast.getLeft());
            right = visit(ast.getRight());
            BigInteger leftVal = requireType(BigInteger.class, left);
            BigInteger rightVal = requireType(BigInteger.class, right);

            if(rightVal.compareTo(BigInteger.ZERO) > 0) {
                BigInteger result = leftVal.pow(rightVal.intValueExact());
                return Environment.create(result);
            } else {
                BigDecimal base = new BigDecimal(leftVal);
                BigDecimal exp = new BigDecimal(rightVal.negate());
                BigDecimal result = BigDecimal.ONE.divide(base.pow(exp.intValueExact()), RoundingMode.HALF_EVEN);
                return Environment.create(result.toBigInteger());
            }
        }
        throw new RuntimeException("Invalid Binary Operator");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (!ast.getOffset().isPresent()) {
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            return variable.getValue();
        }

        else {
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            Environment.PlcObject listObject = variable.getValue();

            if (!(listObject.getValue() instanceof List<?>)) {
                throw new RuntimeException("Attempted to access a non-list variable as a list.");
            }

            List<?> list = (List<?>) listObject.getValue();

            Environment.PlcObject offsetObject = visit(ast.getOffset().get());
            if (!(offsetObject.getValue() instanceof BigInteger)) {
                throw new RuntimeException("List offset must be a BigInteger.");
            }

            BigInteger offset = (BigInteger) offsetObject.getValue();
            int index = offset.intValueExact();

            if (index < 0 || index >= list.size()) {
                throw new RuntimeException("List index out of bounds.");
            }

            Object listElement = list.get(index);
            return Environment.create(listElement);
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        try {
            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            List<Environment.PlcObject> evaluatedArgs = new ArrayList<>();
            for (Ast.Expression argument : ast.getArguments()) {
                evaluatedArgs.add(visit(argument));
            }
            return function.invoke(evaluatedArgs);
        } catch (Exception e) {
            System.err.println("Error invoking function '" + ast.getName() + "': " + e.getMessage());
            return Environment.NIL;
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> args = new ArrayList<>();
        for(Ast.Expression expr: ast.getValues()){
            try {
                Environment.PlcObject result = visit(expr);
                if (result != null && result.getValue() != null) {
                    args.add(result.getValue());
                } else {
                    throw new RuntimeException("Expression evaluation returned null.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error evaluating expression: " + e.getMessage(), e);
            }
        }
        try {
            return Environment.create(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create list PlcObject: " + e.getMessage(), e);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
