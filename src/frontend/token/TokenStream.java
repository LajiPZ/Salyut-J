package frontend.token;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class TokenStream {

    private LinkedList<Token> tokens = new LinkedList<>();
    private int index = 0;

    public void append(Token token) {
        tokens.addLast(token);
        index++;
    }

    public Token peek(int offset) {
        int pos = index + offset;
        if (0 <= pos && pos < tokens.size()) {
            return tokens.get(index + offset);
        }
        return null;
    }

    public Token poll() {
        return tokens.poll();
    }

    @Override
    public String toString() {
        return tokens.stream().map(Token::toString).collect(Collectors.joining("\n"));
    }

}
