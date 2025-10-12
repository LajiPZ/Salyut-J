package frontend.syntax.logical;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class LOrExp extends ASTNode {
    private final ArrayList<LAndExp> lAndExps = new ArrayList<>();

    public LOrExp() {}

    public void addLAndExp(LAndExp LAndExp) {
        this.lAndExps.add(LAndExp);
    }

    public static LOrExp parse(TokenStream ts, List<ErrorEntry> errors) {
        LOrExp lOrExp = new LOrExp();
        lOrExp.addLAndExp(LAndExp.parse(ts, errors));
        while (ts.check(TokenType.Or)) {
            ts.logParse("<LOrExp>");
            ts.poll();
            lOrExp.addLAndExp(LAndExp.parse(ts, errors));
        }
        ts.logParse("<LOrExp>");
        return lOrExp;
    }
}
