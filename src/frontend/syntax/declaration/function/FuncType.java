package frontend.syntax.declaration.function;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class FuncType extends ASTNode {
    public enum Type {
        Int, Void
    }

    private Type type;

    public FuncType(Type type) {
        this.type = type;
    }

    public static FuncType parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token type = tokenStream.next(TokenType.Void, TokenType.Int);
        FuncType funcType = null;
        switch (type.getType()) {
            case Int:
                funcType = new FuncType(Type.Int);
                break;
            case Void:
                funcType =  new FuncType(Type.Void);
                break;
            default: {}
        }
        tokenStream.logParse("<FuncType>");
        return funcType;
    }

    public Type getType() {
        return type;
    }
}
