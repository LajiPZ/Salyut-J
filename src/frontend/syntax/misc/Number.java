package frontend.syntax.misc;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class Number extends ASTNode {
    private int value;

    public Number(int value) {
        this.value = value;
    }

    public static Number parse(TokenStream ts, List<ErrorEntry> errors) {
        Token t = ts.next(TokenType.IntConst);
        ts.logParse("<Number>");
        return new Number(Integer.parseInt(t.getValue()));
    }
}
