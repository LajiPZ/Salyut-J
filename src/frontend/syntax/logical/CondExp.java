package frontend.syntax.logical;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;

import java.util.List;

final public class CondExp extends ASTNode {
    private final LOrExp lOrExp;

    public CondExp(LOrExp lOrExp) {
        this.lOrExp = lOrExp;
    }

    public LOrExp getLOrExp() {
        return lOrExp;
    }

    public static CondExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        return new CondExp(LOrExp.parse(tokenStream, errors));
    }
}
