package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;


    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {

        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();
        while(tokens.has(0)) {
            if(peek("VAL") || peek("LIST") || peek("VAR")) {
                Ast.Global global = parseGlobal();
                globals.add(global);
            }
            else if(peek("FUN")) {
                Ast.Function func = parseFunction();
                functions.add(func);
                if(peek("VAR") || peek("VAL")) {
                    throw new ParseException("Invalid after function", tokens.indexError());
                }
            }
            else {
                throw new ParseException("Expected Valid Source ", tokens.get(0).getIndex());
            }
        }
        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if(peek("LIST")){
            return parseList();
        }
        else if(peek("VAR")){
            return parseMutable();
        }
        else if(peek("VAL")){
            return parseImmutable();
        }
        else{
            return null;
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        match("LIST");
        if(match(Token.Type.IDENTIFIER)){
            String name = tokens.previous().getLiteral();
            if(!match(":")) {
                throw new ParseException("Expected :1", tokens.indexError());
            }
            if(!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected indentifier", tokens.indexError());
            }
            String type = tokens.previous().getLiteral();
            if(match("=")){
                if(match("[")){
                    List<Ast.Expression> args = new ArrayList<>();
                    if(peek("]")) {
                        throw new ParseException("Missing Expression in List" + tokens.indexError(), tokens.indexError());
                    }
                    while(!peek("]")) {
                        args.add(parseExpression());
                        if(match(",")) {
                            match(",");
                            if (peek("]")){
                                Token lastToken = tokens.previous();
                                int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                                throw new ParseException("Trailing Comma.", tokensChar);
                            }
                        }
                    }
                    if(match("]")){
                        if(!match(";")) {
                            Token lastToken = tokens.previous();
                            int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                            throw new ParseException("Expected ';'", tokensChar);
                        }
                        return new Ast.Global(name, type,true, Optional.of(new Ast.Expression.PlcList(args)));
                    }

                }
            }
        }
        throw new ParseException("Identifier Expected", tokens.get(-1).getIndex());


    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */

    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        if(match(Token.Type.IDENTIFIER)){
            String name = tokens.previous().getLiteral();
            if(match(":")) {
                if(!match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected indentifier", tokens.indexError());
                }
            } else {
                throw new ParseException("Expected :", tokens.indexError());
            }
            String type = tokens.previous().getLiteral();
            Optional<Ast.Expression> initialize = Optional.empty();
            if(match("=")){
                initialize = Optional.of(parseExpression());
            }
            if(!match(";")) {
                throw new ParseException("Expected ;" + tokens.indexError(), tokens.indexError());
            }
            return new Ast.Global(name, type,true, initialize);
        }
        Token lastToken = tokens.previous();
        int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
        throw new ParseException("Expected Identifier 2", tokensChar);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        match("VAL");
        if(match(Token.Type.IDENTIFIER)){
            String name = tokens.get(-1).getLiteral();
            if(!match(":")) {
                throw new ParseException("Expected :3", tokens.indexError());
            }
            if(!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected indentifier", tokens.indexError());
            }
            String type = tokens.previous().getLiteral();
            if(match("=")){
                Ast.Expression x = parseExpression();
                if(match(";")){
                    return new Ast.Global(name, type,false, Optional.of(x));

                }
            }
            return new Ast.Global(name, false, Optional.empty());
        }
        Token lastToken = tokens.previous();
        int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
        throw new ParseException("Expected Identifier", tokensChar);
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        match("FUN");
        if(match(Token.Type.IDENTIFIER)){
            String name = tokens.get(-1).getLiteral();
            List<String> args = new ArrayList<>();
            List<String> argsType = new ArrayList<>();
            Optional<String> returnType = Optional.empty();
            List<Ast.Statement> state = new ArrayList<>();
            if(match("(")){
                while(!peek(")")) {
                    if(match(Token.Type.IDENTIFIER)){
                        String val = tokens.previous().getLiteral();
                        if(!match(":")) {
                            throw new ParseException("Expected :4", tokens.indexError());
                        }
                        if(!match(Token.Type.IDENTIFIER)) {
                            throw new ParseException("Expected indentifier", tokens.indexError());
                        }
                        String valType = tokens.previous().getLiteral();
                        args.add(val);
                        argsType.add(valType);
                        if(peek(",")) {
                            match(",");
                            if (peek(")")){
                                Token lastToken = tokens.previous();
                                int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                                throw new ParseException("Trailing Comma.", tokensChar);
                            }
                        }

                    }
                }
                if(!match(")")){
                    Token lastToken = tokens.previous();
                    int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                    throw new ParseException("Expected ')'", tokensChar);
                }
                if(match(":")) {
                    if(match(Token.Type.IDENTIFIER)) {
                        returnType = Optional.of(tokens.previous().getLiteral());
                    } else {
                        throw new ParseException("Expected indentifier", tokens.indexError());
                    }
                }
                if(match("DO")) {
                    state = parseBlock();
                } else {
                    throw new ParseException("Incorrect Format1", tokens.get(0).getIndex());
                }
//                if(match("DO")) {
//                    state = parseBlock();
//                    if (match("END") && !tokens.has(0)) {
//                        return new Ast.Function(name, args, argsType, returnType, state);
//                    }
//                    else{
//                        throw new ParseException("Incorrect Format1", tokens.get(0).getIndex());
//                    }
//                }
//                else{
//                    throw new ParseException("Incorrect Format2", tokens.get(-1).getIndex());
//                }

            }
            if(!match("END")) {
                throw new ParseException("Expected END", tokens.indexError());
            }
            return new Ast.Function(name, args, argsType, returnType, state);
        }
        Token lastToken = tokens.previous();
        int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
        throw new ParseException("Incorrect Format3", tokensChar);
    }



    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> args = new ArrayList<>();
        while(tokens.has(0) && !peek("CASE") && !peek("END") && !peek("ELSE") && !peek("DEFAULT")){
            args.add(parseStatement());
        }
        return args;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if(peek("LET")) {
            return parseDeclarationStatement();
        } else if(peek("IF")) {
            return parseIfStatement();
        } else if(peek("SWITCH")) {
            return parseSwitchStatement();
        } else if(peek("WHILE")) {
            return parseWhileStatement();
        } else if(peek("RETURN")) {
            return parseReturnStatement();
        } else {
            Ast.Expression expr = parseExpression();
            if(peek("=")) {
                match("=");
                if(peek(";")) {
                    Token lastToken = tokens.previous();
                    int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                    throw new ParseException("Expected expression after '='", tokensChar);
                }
                Ast.Expression val = parseExpression();
                if(!match(";")) {
                    Token lastToken = tokens.previous();
                    int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                    throw new ParseException("Expected ';' 1", tokensChar);
                }
                return new Ast.Statement.Assignment(expr, val);
            }
            else {
                if(!match(";")) {
                    Token lastToken = tokens.previous();
                    int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                    throw new ParseException("Expected ';' 2", tokensChar);
                }
                return new Ast.Statement.Expression(expr);
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        if(!match(Token.Type.IDENTIFIER)){
            throw new ParseException("Expected identifier after LET", tokens.indexError() + 1);
        }
        String name = tokens.previous().getLiteral();
        Optional<Ast.Expression> initialize = Optional.empty();
        Optional<String> type = Optional.empty();
        if(match(":")) {
            if(match(Token.Type.IDENTIFIER)) {
                type = Optional.of(tokens.previous().getLiteral());
            } else {
                throw new ParseException("Expected identifier", tokens.indexError());
            }
        }
        //builds expression
        if(peek("=")) {
            match("=");
            initialize = Optional.of(parseExpression());
        }
        if(!match(";")) {
            throw new ParseException("Expected ';'", tokens.indexError());
        }
        return new Ast.Statement.Declaration(name, type, initialize);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");

        if(peek("IF") || peek("DO") || peek("ELSE") || peek("END")  ) {
            throw new ParseException("Expected expr after IF" + (tokens.indexError() + 1), tokens.indexError() + 1);
        }
        Ast.Expression ifCondition = parseExpression();

        //checks condition of if statement
        if(!match("DO")) {
            throw new ParseException("Expected DO after IF" + tokens.indexError(), tokens.indexError());
        }
        List<Ast.Statement> then = parseBlock();
        List<Ast.Statement> elseCondition = new ArrayList<>();

        //checks for else statement
        if(peek("ELSE")) {
            match("ELSE");
            elseCondition = parseBlock();
        }

        //checks for end
        if(!match("END")) {
            throw new ParseException("Expected END after IF blocks" + tokens.indexError(), tokens.indexError());
        }
        return new Ast.Statement.If(ifCondition, then, elseCondition);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        match("SWITCH");
        if(peek("SWITCH") || peek("CASE") || peek("DEFAULT")) {
            throw new ParseException("Expected expr after SWITCH" + (tokens.indexError()+1), tokens.indexError() + 1);
        }

        Ast.Expression condition = parseExpression();

        List<Ast.Statement.Case> cases = new ArrayList<>();
        while(peek("CASE")) {
            cases.add(parseCaseStatement()); // calls parse case
        }

        if(!peek("DEFAULT")) {
            throw new ParseException("Expected DEFAULT before END", tokens.indexError());
        }

        match("DEFAULT");
        List<Ast.Statement> defaultCondition = parseBlock();
        cases.add(new Ast.Statement.Case(Optional.empty(), defaultCondition));

        if(!match("END")) {
            throw new ParseException("Expected END after SWITCH conditions", tokens.indexError());
        }
        return new Ast.Statement.Switch(condition, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        match("CASE");
        if(peek("SWITCH") || peek("DEFAULT") || peek("END")) {
            throw new ParseException("Expected expr after IF" + (tokens.indexError()+1), tokens.indexError() + 1);
        }
        Ast.Expression caseVal = parseExpression();

        if(!match(":")) {
            throw new ParseException("Expected : after CASE conditions" + tokens.indexError(), tokens.indexError());
        }
        List<Ast.Statement> caseConditions = parseBlock();
        return new Ast.Statement.Case(Optional.of(caseVal), caseConditions);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        if(peek("WHILE") || peek("DO") || peek("END")) {
            throw new ParseException("Expected expr after WHILE" + (tokens.indexError()+1), tokens.indexError() + 1);
        }
        Ast.Expression condition = parseExpression();


        if(!match("DO")) {
            throw new ParseException("Expected DO after WHILE conditions", tokens.indexError());
        }

        List<Ast.Statement> then = parseBlock();

        if(!match("END")) {
            throw new ParseException("Expected END after WHILE conditions", tokens.indexError());
        }
        return new Ast.Statement.While(condition, then);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");

        //checks for expression
        if(!tokens.has(0)) {
            throw new ParseException("Expected expression after RETURN", tokens.indexError());
        }
        Ast.Expression val = parseExpression();

        //checks for ;
        if(!match(";")) {
            throw new ParseException("Expected ; after RETURN expression", tokens.indexError());
        }
        return new Ast.Statement.Return(val);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression expr = parseComparisonExpression();
        while(match("&&") || match("||")) {
            Token tokenOperator = tokens.previous();
            String operator = tokenOperator.getLiteral();
            Ast.Expression right = parseComparisonExpression();
            expr = new Ast.Expression.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression expr = parseAdditiveExpression();
        while(match("<") || match(">") || match("==") || match("!=")) {
            Token tokenOperator = tokens.previous();
            String operator = tokenOperator.getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            expr = new Ast.Expression.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expr = parseMultiplicativeExpression();
        while(match("+") || match("-")) {
            Token tokenOperator = tokens.previous();
            String operator = tokenOperator.getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            expr = new Ast.Expression.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expr = parsePrimaryExpression();
        while(match("*") || match("/") || match("^")) {
            Token tokenOperator = tokens.previous();
            String operator = tokenOperator.getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            expr = new Ast.Expression.Binary(operator, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL")) {
            match("NIL");
            return new Ast.Expression.Literal(null);
        } else if (peek("TRUE")) {
            match("TRUE");
            Boolean x = true;
            return new Ast.Expression.Literal(x);
        } else if (peek("FALSE")) {
            match("FALSE");
            Boolean x = false;
            return new Ast.Expression.Literal(x);
        } else if (peek(Token.Type.INTEGER)) {
            BigInteger value = new BigInteger(tokens.get(0).getLiteral());
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(value);
        } else if (peek(Token.Type.DECIMAL)) {
            BigDecimal value = new BigDecimal(tokens.get(0).getLiteral());
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(value);
        }


        else if (peek(Token.Type.CHARACTER)) {
            String literal = tokens.get(0).getLiteral();
            tokens.advance();
            String removedQuotes = literal.substring(1, literal.length() - 1);
            //replaces escape characters
            removedQuotes = removedQuotes.replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            char value = removedQuotes.charAt(0);
            return new Ast.Expression.Literal(value);
        }

        else if (peek(Token.Type.STRING)) {
            String literal = tokens.get(0).getLiteral();
            tokens.advance();
            String removedQuotes = literal.substring(1, literal.length() - 1);
            removedQuotes = removedQuotes.replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")
                    .replace("\\\"", "\"");
            return new Ast.Expression.Literal(removedQuotes);
        }

        else if (match("(")) {
            Ast.Expression expression = parseExpression();
            if (!match(")")) {
                Token lastToken = tokens.previous();
                int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                throw new ParseException("Expected closing parenthesis `)`." + tokensChar, tokensChar);
            }
            return new Ast.Expression.Group(expression);
        }

        else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if(match("(")) {
                List<Ast.Expression> args = new ArrayList<>();
                while(!peek(")")) {
                    args.add(parseExpression());
                    if(match(",")) {
                        match(",");
                        if (peek(")")){
                            Token lastToken = tokens.previous();
                            int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                            throw new ParseException("Trailing Comma.", tokensChar);
                        }
                    }
                }
                if(match(")")){
                    return new Ast.Expression.Function(name, args);
                }

                else{
                    Token lastToken = tokens.previous();
                    int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                    throw new ParseException("Expected closing parenthesis `)`." + tokensChar, tokensChar);
                }
            }

            else if (match("[")) {
                Ast.Expression index = parseExpression();
                if (!match("]")) {
                    Token lastToken = tokens.previous();
                    int tokensChar = lastToken.getIndex() + lastToken.getLiteral().length();
                    throw new ParseException("Expected closing parenthesis `]`."+ tokensChar, tokensChar);
                }
                return new Ast.Expression.Access(Optional.of(index), name);
            }
            else {
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        }
        throw new ParseException("Expected an Expression. here" +  tokens.indexError(), tokens.indexError());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i++) {
            if(!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if(patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if(patterns[i] instanceof String) {
                if(!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;
        private static final Set<String> keywords = Set.of("LET", "SWITCH", "IF", "WHILE", "RETURN", "DO", "ELSE", "END");

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

        //helper function
        public Token previous() {
            return tokens.get(index - 1);
        }

        //index error
        public int indexError() {
            //if no more tokens, returns index after last token
            if(!has(0)) {
                if(index > 0) {
                    Token lastToken = tokens.get(index - 1);
                    return lastToken.getIndex() + lastToken.getLiteral().length();
                } else {
                    return 0;
                }
            }
            //if additional error tokens, returns index of current token
            else {
                Token nextToken = tokens.get(index);
                return nextToken.getIndex();
            }
        }

        //check valid expression
        public boolean validExpr() {
            if(!has(0)) {
                return false;
            }
            Token curr = tokens.get(0);
            return keywords.contains(curr.getLiteral().toUpperCase());
        }

    }

}