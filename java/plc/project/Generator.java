package plc.project;

import java.awt.*;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;


public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    //public class Main {

    //   int x = null;
    //   double y = null;
    //   string z = null;
    //public static void main(String[] args) {
    //   System.exit(new Main().main());
    //}

    //int f() {
    //   return x;
    // }
    // double g() {
    //   return y;
    // }
    // String h() {
    //   return z;
    // }
    // int main() {}
    //
    // }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        indent++;
        if(!ast.getGlobals().isEmpty()) {
            for (int i = 0; i < ast.getGlobals().size(); i++) {
                newline(indent);
                print(ast.getGlobals().get(i));
            }
            newline(0);
        }
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        print("System.exit(new Main().main());");
        indent--;
        newline(indent);
        print("}");
        newline(0);
        if(!ast.getFunctions().isEmpty()) {
            newline(indent);
            for(int i = 0; i < ast.getFunctions().size(); i++) {
                if(i > 0) {
                    newline(0);
                    newline(indent);
                }
                print(ast.getFunctions().get(i));
            }
        }
        newline(0);
        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (!ast.getMutable()) {
            print("final ");
        }
        print(Environment.getType(ast.getVariable().getType().getName()).getJvmName());

//        switch (ast.getTypeName()) {
//            case "String":
//                print("String");
//                break;
//            case "Boolean":
//                print("boolean");
//                break;
//            case "Decimal":
//                print("double");
//                break;
//            case "Integer":
//                print("int");
//                break;
//            default:
//                break;
//        }

        if (ast.getValue().isPresent()) {
            if (ast.getValue().get() instanceof Ast.Expression.PlcList) {
                // If the value is a list
                print("[]");
            }
        }
        print(" ", ast.getVariable().getName());
        if(ast.getValue().isPresent()) {
            print(" = ");
            if(ast.getValue().get() instanceof Ast.Expression.PlcList) {
                Ast.Expression.PlcList list = (Ast.Expression.PlcList) ast.getValue().get();
                print("{");
                List<Ast.Expression> vals = list.getValues();
                for(int i = 0; i < vals.size(); i++) {
                    if(i > 0) {
                        print(", ");
                    }
                    print(vals.get(i));
                }
                print("}");
            }
            else {
                print(ast.getValue().get());
            }
        }
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Function ast) {
        Environment.Type funcType;
        if(ast.getReturnTypeName().isPresent()) {
            funcType = Environment.getType(ast.getReturnTypeName().get());
        } else {
            funcType = ast.getFunction().getReturnType();
        }
        print(funcType.getJvmName());
        print(" ");

        print(ast.getName());
        print("(");
        if(!ast.getParameters().isEmpty()){
            for(int i =0; i < ast.getParameters().size(); i++){
                if (i > 0) {
                    print(", ");
                }
                print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName());
                print(" ");
                print(ast.getParameters().get(i));
            }}
            print(") {");
            if (!ast.getStatements().isEmpty()) {
                indent++;
                for (int i = 0; i < ast.getStatements().size(); i++) {
                    newline(indent);
                    print(ast.getStatements().get(i));
                }
                indent--;
                newline(indent);
            }

            print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if(ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if", " (", ast.getCondition(), ") {");
        newline(++indent);
        for(int i = 0; i < ast.getThenStatements().size(); i++) {
            if(i > 0) {
                newline(indent);
            }
            print(ast.getThenStatements().get(i));
        }
        indent--;
        newline(indent);
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for(int i = 0; i < ast.getElseStatements().size(); i++) {
                if(i > 0) {
                    newline(indent);
                }
                print(ast.getElseStatements().get(i));
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (", ast.getCondition(), ") {");
        newline(++indent);

        for(Ast.Statement.Case caseStmt : ast.getCases()) {
            print(caseStmt);
        }
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if(ast.getValue().isPresent()) {
            print("case ", ast.getValue().get(), ":");
        } else {
            print("default:");
        }
        newline(++indent);
        for(int i = 0; i < ast.getStatements().size(); i++) {
            print(ast.getStatements().get(i));
            if(i < (ast.getStatements().size() - 1)) {
                newline(indent);
            }
        }
        if(ast.getValue().isPresent()) {
            newline(indent);
            print("break;");
        }
        indent--;
        if(ast.getValue().isPresent()) {
            newline(indent);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ") {");
        if(!ast.getStatements().isEmpty()) {
            newline(++indent);
            for(int i = 0; i < ast.getStatements().size(); i++) {
                if(i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
            print("}");
        } else {
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object val = ast.getLiteral();
        try {
            if(val instanceof Character) {
                print("\'" + val + "\'");
            } else if(val instanceof String) {
                print("\"" + val + "\"");
            } else if (val instanceof BigDecimal) {
                print(val);
            }
            else {
                print(val.toString());
            }
        } catch (NullPointerException e) {
            print("null;");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        if("^".equals(operator)) {
            print("Math.pow(", ast.getLeft(), ", ", ast.getRight(), ")");
        } else {
            print(ast.getLeft(), " ", operator, " ", ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()) {
            print(ast.getVariable().getJvmName(), "[", ast.getOffset().get(), "]");
        } else {
            print(ast.getVariable().getJvmName());
        }
        return null;
    }
    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        print("(");
        if(!ast.getArguments().isEmpty()){
            for(int i = 0; i < ast.getArguments().size()-1; i++){
                print(ast.getArguments().get(i));
                print(", ");
            }
            print(ast.getArguments().getLast());
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        for(int i = 0 ; i < ast.getValues().size()-1; i++){
            print(ast.getValues().get(i));
            print(", ");
        }
        print(ast.getValues().getLast());
        print("}");
        return null;
    }

}
