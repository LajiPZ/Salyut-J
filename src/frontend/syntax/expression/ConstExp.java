package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;

import java.util.List;

public class ConstExp extends ASTNode {

    private final AddExp addExp;

    public ConstExp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return addExp;
    }

    public static ConstExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
       ConstExp exp =  new ConstExp(AddExp.parse(tokenStream, errors));
       tokenStream.logParse("<ConstExp>");
       return exp;
    }

    public void visit() {
        addExp.visit();
    }
}
