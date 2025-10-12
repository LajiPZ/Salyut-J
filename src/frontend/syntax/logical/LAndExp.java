package frontend.syntax.logical;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ErrorManager;

final public class LAndExp extends ASTNode {
    private final ArrayList<EqExp> eqExps = new ArrayList<>();

    public LAndExp() {}

    public void addEqExp(EqExp EqExp) {
        this.eqExps.add(EqExp);
    }

    public static LAndExp parse(TokenStream ts, List<ErrorEntry> errors) {
        LAndExp exp = new LAndExp();
        exp.addEqExp(EqExp.parse(ts, errors));
        while (ts.check(TokenType.And)) {
            ts.logParse("<LAndExp>");
            ts.poll();
            exp.addEqExp(EqExp.parse(ts, errors));
        }
        ts.logParse("<LAndExp>");
        return exp;
    }
}
