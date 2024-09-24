package plc.project;

import java.util.List;
import java.util.ArrayList;
/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    //lex
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Lexer(String input) {

        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    //loop through input string, skips whitespace
    public List<Token> lex() {

        //iterate each char until end of input
        while(chars.has(0)) {
            if (peek("[\b\n\r\t]") || peek(" ")) {
                chars.advance();
                chars.skip();
            }
            else {
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if(peek("@") || peek("[A-Za-z]")){
            return lexIdentifier();
        }
        else if (peek("[0-9]")){
            return lexNumber();
        }
        else if(peek("-")){
            if(chars.has(1)){
                chars.advance();
                if(peek("[0-9]")){
                    if(chars.has(1)) {
                        chars.advance();
                        if(peek(".") || peek("[1-9]")) {
                            return lexNumber();
                        }

                    }
                    else if(peek("[1-9]")){
                        return lexNumber();
                    }
                    throw new ParseException("incorrect token type", chars.index);
                }
                else{
                    return lexOperator();
                }
            }
            else{
                return lexOperator();
            }

        } else if (peek("'")) {
            return lexCharacter();
        } else if(peek("\"")){
            return lexString();
        } else if (peek("[!=]=?|&&|\\|\\||.")) {
            return lexOperator();
        }
        else {
            throw new ParseException("incorrect token type", chars.index);
        }
    }

    public Token lexIdentifier() {
        //identifier ::= ( '@' | [A-Za-z] ) [A-Za-z0-9_-]*
        if(match("@") || match("[A-Za-z]")) {
            while(peek("[A-Za-z0-9_-]*")) {
                chars.advance();
            }
            return chars.emit(Token.Type.IDENTIFIER);
        }
        throw new ParseException("incorrect token type", chars.index);    }


    public Token lexNumber() {
        if(peek("[0-9]")) {
            if(peek("[1-9]")) {
                match("[1-9]");
                while (peek("[0-9]")) {
                    match("[0-9]");
                }
            }

            else {
                match("[0-9]");

            }
        }

        if (peek("[.]","[\\d]")) {
            match(".");
            while (peek("[0-9]")) {
                match("[0-9]");
            }
            return chars.emit(Token.Type.DECIMAL);
        }

        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        //character ::= ['] ([^'\n\r\\] | escape) [']

        //single char
        if(match("'")) {
            if(match("[^'\\n\\r\\\\]")) {
                if (match("'")) {
                    return chars.emit(Token.Type.CHARACTER);
                }
            }
            //escape char
            else if (match("\\\\")) {
                if (match("[bnrt'\"\\\\]")) {
                    if (match("'")) {
                        return chars.emit(Token.Type.CHARACTER);
                    }
                }
            }


        }
        throw new ParseException("incorrect token type", chars.index);
    }

    public Token lexString() {
        //string ::= '"' ([^"\n\r\\] | escape)* '"'
        //Case 1: string
        if (match("\"")) {
            //Case 2: escape between chars
            while (!peek("\"")) {
                if (peek("[^\"\\n\\r\\\\]")) {
                    match("[^\"\\n\\r\\\\]");
                } else if (peek("\\\\")) {
                    match("\\\\");

                    //invalid escape
                    if (!match("[bnrt'\"\\\\]")) {
                        throw new ParseException("incorrect token type", chars.index);
                    }
                } else {
                    throw new ParseException("incorrect token type", chars.index);
                }
            }
            match("\"");
            return chars.emit(Token.Type.STRING);
        }

        //escape char
        else if (match("\\\\")) {
            if (match("[bnrt'\"\\\\]")) {
                if (match("\"")) {
                    return chars.emit(Token.Type.STRING);
                }
            }
        }
        return null;
    }

    public void lexEscape() {
        if (match("\\\\")) {
            if (match("[bnrt'\"\\\\]")) {
                if (match("\"")) {
                    throw new ParseException("incorrect token type", chars.index);
                }
            }
        }
    }

    public Token lexOperator() {
        if (match("!")) {
            match("=");
        }
        else if (match("=")) {
            match("=");
        }
        else if (match("&")) {
            match("&");
        }
        else if (match("\\|")) {
            match("\\|");
        }
        else if(peek(" ")){
            return chars.emit(Token.Type.OPERATOR);
        }
        else {
            chars.advance();

        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }
    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }


    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}