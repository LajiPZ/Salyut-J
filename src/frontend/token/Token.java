package frontend.token;

import utils.FileLoc;

import java.util.Arrays;

public class Token {
    private TokenType type;
    private String value;
    private FileLoc loc;
    public Token(TokenType type, String value, FileLoc loc) {
        this.type = type;
        this.value = value;
        this.loc = loc;
    }

    public FileLoc getFileLoc() {
        return loc;
    }

    @Override
    public String toString() {
        // TODO: Some special scenarios may need to be tackled
        // i.e. "\\", StrConst;
        // Maybe CharConst
        if (type == TokenType.StringConst) {
            StringBuilder builder = new StringBuilder();
            builder.append('\"');
            for (char c : value.toCharArray()) {
                switch (c) {
                    case '\n': builder.append("\\n"); break;
                    case '\"': builder.append("\\\""); break;
                    default: builder.append(c);
                }
            }
            builder.append('\"');
            return builder.toString();
        } else {
            return type.toString() + " " + value;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Token) {
            return ((Token) obj).type.equals(this.type) && ((Token) obj).value.equals(this.value);
        }
        return false;
    }

    public String getValue() {
        return value;
    }

    /**
     * Check the specified token is of the same type as the current token.
     * @param type TokenType for comparison
     * @return result
     */
    public boolean ofType(TokenType type) {
        return this.type.equals(type);
    }

    public boolean ofType(TokenType... types) {
        return Arrays.stream(types).anyMatch(this::ofType);
    }

    public TokenType getType() {
        return type;
    }
}
