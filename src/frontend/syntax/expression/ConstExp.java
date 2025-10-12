package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;

import java.util.List;

public class ConstExp extends ASTNode {
    // TODO: 使用的 Ident 必须是常量

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
}
