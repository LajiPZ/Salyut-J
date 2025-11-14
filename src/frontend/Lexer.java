package frontend;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;
import utils.FileLoc;
import utils.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

final public class Lexer {

    static class Reader {
        private final BufferedReader reader;
        private int column = 0;
        private int line = 1;

        public Reader(String filePath) throws IOException {
            reader = new BufferedReader(new FileReader(filePath));
        }

        /**
         * Peek one char ahead of current location.
         * @return result
         * @throws IOException
         */
        public int peek() throws IOException {
            reader.mark(1);
            int c = reader.read();
            reader.reset();
            return c;
        }

        /**
         * Read one char from current location.
         * To prevent pushback to stream, you may need to use peek().
         * @return result
         * @throws IOException
         */
        public int read() throws IOException {
            int c = reader.read();
            if (c == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            return c;
        }

        public Pair<Integer, Integer> getLocation() {
            return new Pair<>(line, column);
        }
    }

    static class TokenTypeMaps {
        public static final Map<String, TokenType> keywords = Map.ofEntries(
            entry("main",TokenType.Main), entry("const", TokenType.Const),
            entry("int",TokenType.Int), entry("void", TokenType.Void),
            entry("break", TokenType.Break), entry("continue", TokenType.Continue),
            entry("if", TokenType.If), entry("else", TokenType.Else),
            entry("printf", TokenType.Printf),
            entry("return", TokenType.Return),
            entry("for", TokenType.For), entry("static", TokenType.Static)
        );

        public static final Map<String, TokenType> symbols = Map.ofEntries(
            entry("!", TokenType.Not), entry("&&", TokenType.And), entry("||", TokenType.Or),
            entry("+", TokenType.Plus), entry("-", TokenType.Minus),
            entry("*", TokenType.Mul), entry("/", TokenType.Div), entry("%", TokenType.Mod),
            entry("<", TokenType.LT), entry("<=", TokenType.LE),
            entry(">", TokenType.GT), entry(">=", TokenType.GE),
            entry("==", TokenType.EQ), entry("!=", TokenType.NE),
            entry("=", TokenType.Assign),
            entry(",", TokenType.Comma), entry(";", TokenType.Semicolon),
            entry("(", TokenType.LeftParen), entry(")", TokenType.RightParen),
            entry("[", TokenType.LeftBracket), entry("]", TokenType.RightBracket),
            entry("{", TokenType.LeftBrace), entry("}", TokenType.RightBrace)
        );

        public static final Map<Character, Character> escapeChar = Map.ofEntries(
            entry('n', '\n'), entry('"', '\"')
        );
    }

    private Reader reader;
    private TokenStream tokenStream;
    private ArrayList<ErrorEntry> errors;

    public Lexer(String filePath) throws IOException {
        reader = new Reader(filePath);
        tokenStream = new TokenStream();
        errors = new ArrayList<>();
    }

    public boolean analyze() throws IOException {
        int chr;

        while ((chr = reader.peek()) != -1) {
            if (Character.isWhitespace(chr)) {
                reader.read();
                continue;
            }
            if (Character.isDigit(chr)) { // Number
                scanNumber();
            } else if (Character.isLetter(chr) || chr == '_') { // Identifier
                scanIdentifier();
            } else if (chr == '"') { // String
                scanString();
            } else if (chr == '/') { // Comments or DIV
                scanSlash();
            } else {
                scanSymbol();
            }
        }

        if (errors.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public TokenStream getTokenStream() {
        return tokenStream;
    }

    public void printTokenStream() {
        System.out.println(tokenStream.toString());
    }

    public void printTokenStream(String filePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(tokenStream.toString());
        writer.close();
    }

    public List<ErrorEntry> getErrors() {
        return errors;
    }

    private void scanNumber() throws IOException {
        StringBuilder number = new StringBuilder();
        number.append((char)reader.read());
        Pair<Integer, Integer> startLoc = reader.getLocation();
        while (Character.isDigit(reader.peek())) {
            number.append((char)reader.read());
        }
        addToken(TokenType.IntConst, number.toString(), startLoc);
    }

    private void scanIdentifier() throws IOException {
        StringBuilder ident = new StringBuilder();
        ident.append((char)reader.read());
        Pair<Integer, Integer> startLoc = reader.getLocation();
        int chr = reader.peek();
        while (Character.isLetterOrDigit(chr) || chr == '_') {
            ident.append((char)reader.read());
            chr = reader.peek();
        }
        addToken(TokenTypeMaps.keywords.getOrDefault(ident.toString(), TokenType.Ident), ident.toString(), startLoc);
    }


    private void scanSlash() throws IOException {
        reader.read();
        Pair<Integer, Integer> startLoc = reader.getLocation();
        int nextChar = reader.peek();
        if (nextChar == '/') { // Inline comment
            while (nextChar != -1) {
                nextChar = reader.read();
                if (nextChar == '\n') {
                    break;
                }
            }
        } else if (nextChar == '*') { // Multi-line comment
            reader.read();
            while ((nextChar = reader.read()) != -1) {
                if (nextChar == '*' && reader.peek() == '/') {
                    reader.read();
                    break;
                }
            }
        } else { // DIV
            addToken(TokenType.Div, "/", startLoc);
        }
    }

    private void scanString() throws IOException {
        StringBuilder str = new StringBuilder();
        str.append((char) reader.read());
        Pair<Integer, Integer> startLoc = reader.getLocation();
        int nextChar = reader.peek();
        while (nextChar != -1) {
            str.append((char)reader.read());
            if (nextChar == '\\') {
                // Handle escape characters
                int nextChar2 = reader.peek();
                if (TokenTypeMaps.escapeChar.containsKey((char)nextChar2)) {
                    str.deleteCharAt(str.length() - 1); // remove '\'
                    str.append(TokenTypeMaps.escapeChar.get((char)reader.read()));
                } else {
                    throw new RuntimeException("Unexpected escape char");
                }
            } else if (nextChar == '"') {
                break;
            }
            nextChar = reader.peek();
        }
        // 去掉""
        addToken(TokenType.StringConst, str.toString().substring(1, str.length() - 1), startLoc);
    }

    private void scanSymbol() throws IOException {
        char chr = (char) reader.read();
        Pair<Integer, Integer> startLoc = reader.getLocation();
        int nextChar = reader.peek();
        if (chr == '!') {
            if (nextChar == '=') {
                reader.read();
                addToken(TokenType.NE, "!=", startLoc);
            } else {
                addToken(TokenType.Not, "!", startLoc);
            }
        } else if (chr == '=') {
            if (nextChar == '=') {
                reader.read();
                addToken(TokenType.EQ, "==", startLoc);
            } else {
                addToken(TokenType.Assign, "=", startLoc);
            }
        } else if (chr == '<') {
            if (nextChar == '=') {
                reader.read();
                addToken(TokenType.LE, "<=", startLoc);
            } else {
                addToken(TokenType.LT, "<", startLoc);
            }
        } else if (chr == '>') {
            if (nextChar == '=') {
                reader.read();
                addToken(TokenType.GE, ">=", startLoc);
            } else {
                addToken(TokenType.GT, ">", startLoc);
            }
        } else if (chr == '&') {
            if (nextChar == '&') {
                reader.read();
                addToken(TokenType.And, "&&", startLoc);
            } else {
                addError(ErrorType.IllegalSymbol, "&", startLoc);
                addToken(TokenType.And, "&", startLoc);
            }
        } else if (chr == '|') {
            if (nextChar == '|') {
                reader.read();
                addToken(TokenType.Or, "||", startLoc);
            } else {
                addError(ErrorType.IllegalSymbol, "|", startLoc);
                addToken(TokenType.Or, "|", startLoc);
            }
        } else {
            if (TokenTypeMaps.symbols.containsKey(String.valueOf(chr))) {
                addToken(
                        TokenTypeMaps.symbols.get(String.valueOf(chr)),
                        String.valueOf(chr),
                        startLoc
                );
            } else {
                addToken(
                        TokenType.Unknown,
                        String.valueOf(chr),
                        startLoc
                );
                addError(ErrorType.IllegalSymbol, String.valueOf(chr), startLoc);
            }
        }
    }

    private void addToken(TokenType type, String content, Pair<Integer, Integer> startLoc) {
        Token token = new Token(type, content, new FileLoc(startLoc, reader.getLocation()));
        tokenStream.append(token);
    }

    private void addError(ErrorType type, String content, Pair<Integer, Integer> startLoc) {
        ErrorEntry err = new ErrorEntry(type, content, new FileLoc(startLoc, reader.getLocation()));
        errors.add(err);
    }
}
