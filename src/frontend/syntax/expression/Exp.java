package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;

import java.util.List;

final public class Exp extends ASTNode {
    private final AddExp addExp;

    public Exp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return this.addExp;
    }

    public static Exp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        return new Exp(AddExp.parse(tokenStream, errors));
    }
}
