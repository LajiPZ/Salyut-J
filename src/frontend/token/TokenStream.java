package frontend.token;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class TokenStream {

    private LinkedList<Token> tokens = new LinkedList<>();
    private int index = 0;
    private boolean hasCheckpoint = false;
    private int checkpoint = 0;

    public void append(Token token) {
        tokens.addLast(token);
    }

    public Token peek() {
        return hasCheckpoint ? tokens.get(index) : tokens.peek();
    }

    public Token peek(int offset) {
        int pos = index + offset;
        if (0 <= pos && pos < tokens.size()) {
            return tokens.get(pos);
        }
        return null;
    }

    public Token poll() {
        index++;
        return hasCheckpoint ? tokens.get(index) : tokens.poll();
    }

    public Token next(TokenType... types) {
        if (check(types)) {
            return hasCheckpoint ? tokens.get(index) : tokens.poll();
        } else {
            throw new IllegalStateException("Expecting " + Arrays.toString(types) + "but got " + tokens.peek());
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

    public Token next(TokenType... types) {
        if (check(types)) {
            return tokens.poll();
        } else {
            throw new IllegalStateException("Expecting " + Arrays.toString(types) + "but got " + tokens.peek());
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

    public boolean isEnd() {
        return index >= tokens.size();
    }

    @Override
    public String toString() {
        return tokens.stream().map(Token::toString).collect(Collectors.joining("\n"));
    }

}
