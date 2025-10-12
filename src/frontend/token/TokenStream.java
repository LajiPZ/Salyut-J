package frontend.token;

import settings.Settings;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class TokenStream {

    private LinkedList<Token> tokens = new LinkedList<>();
    private int index = 0;
    private int size = 0;
    private boolean hasCheckpoint = false;
    private int checkpoint = 0;
    private StringBuilder parseLog = new StringBuilder();
    private Token prevToken = null;

    public void append(Token token) {
        tokens.addLast(token);
        size++;
    }

    public Token peek() {
        return hasCheckpoint ? tokens.get(index - checkpoint) : tokens.peek();
    }

    public Token peek(int offset) {
        int pos = hasCheckpoint ? index - checkpoint + offset: offset;
        if (0 <= pos && pos < tokens.size()) {
            return tokens.get(pos);
        }
        return null;
    }

    public Token poll() {
        Token token = hasCheckpoint ? tokens.get(index - checkpoint) : tokens.poll();
        index++;
        if (Settings.PrintConfig.printParseProcess) {
            assert token != null;
            logParse(token.toString());
        }
        prevToken = token;
        return token;
    }

    public Token next(TokenType... types) {
        if (check(types)) {
            return poll();
        } else {
            throw new IllegalStateException("Expecting " + Arrays.toString(types) + " but got " + tokens.peek());
        }
    }

    public boolean check(TokenType... tokenTypes) {
        if (peek() == null) return false;
        return peek().ofType(tokenTypes);
    }

    public boolean check(int offset,  TokenType... tokenTypes) {
        if (peek() == null) return false;
        return peek(offset).ofType(tokenTypes);
    }

    /**
     * Check if the current token matches the given tokenTypes.
     * If true, poll the current token, returns true, or false otherwise.
     * This is especially useful for parsing optional elements in syntax.
     * @param tokenTypes the specified token types
     * @return result of comparison
     */
    public boolean checkPoll(TokenType... tokenTypes) {
        if (check(tokenTypes)) {
            poll();
            return true;
        } else {
            return false;
        }
    }

    public void setCheckpoint() {
        assert !hasCheckpoint;
        this.hasCheckpoint = true;
        this.checkpoint = this.index;
    }

    public void releaseCheckpoint() {
        assert this.hasCheckpoint;
        this.hasCheckpoint = false;
        this.index = this.checkpoint;
    }

    public boolean isEnd() {
        return index >= size;
    }

    public void logParse(String log) {
        if (!hasCheckpoint) {
            // System.out.println(log);
            parseLog.append(log);
            parseLog.append("\n");
        }
    }

    public Token getPrevToken() {
        return prevToken;
    }

    public void printParseLog(String filePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(parseLog.toString());
        writer.close();
    }

    @Override
    public String toString() {
        return tokens.stream().map(Token::toString).collect(Collectors.joining("\n"));
    }

}
