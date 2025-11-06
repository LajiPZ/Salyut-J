package frontend.syntax.declaration;

import frontend.error.ErrorEntry;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

final public class BType extends ASTNode {
    enum Type {
        Int
    }

    private Type type;

    public BType(Type type) {
        this.type = type;
    }

    public static BType parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token type = tokenStream.next(TokenType.Int);
        if (type.ofType(TokenType.Int)) {
            // tokenStream.logParse("<BType>");
            return new BType(Type.Int);
        }
        return null;
    }

    public DataType toDataType() {
        switch (type) {
            case Int -> {
                return new IntType();
            }
            default -> {
                return null; // not likely
            }
        }
    }
}