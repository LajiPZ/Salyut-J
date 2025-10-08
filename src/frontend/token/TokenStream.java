package frontend.token;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class TokenStream {

    private LinkedList<Token> tokens = new LinkedList<>();
    private int index = 0;

    public void append(Token token) {
        tokens.addLast(token);
    }

    public Token peek() {
        return tokens.peek();
    }

    public Token peek(int offset) {
        int pos = index + offset;
        if (0 <= pos && pos < tokens.size()) {
            return tokens.get(index + offset);
        }
        return null;
    }

    public Token poll() {
        index++;
        return tokens.poll();
    }

    public boolean isEnd() {
        return index >= tokens.size();
    }

    @Override
    public String toString() {
        return tokens.stream().map(Token::toString).collect(Collectors.joining("\n"));
    }

}
