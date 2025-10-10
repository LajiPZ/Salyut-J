package frontend.syntax.statement;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.syntax.misc.LVal;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class ForStmt extends ASTNode {
    private ArrayList<LVal> lVals = new ArrayList<>();
    private ArrayList<Exp> exps = new ArrayList<>();

    public ForStmt(LVal lVal, Exp exp) {
        this.lVals.add(lVal);
        this.exps.add(exp);
    }

    public void addExtraStmt(LVal lVal, Exp exp) {
        lVals.add(lVal);
        exps.add(exp);
    }

    public static ForStmt parse(TokenStream ts, List<ErrorEntry> errors) {
        LVal lVal = LVal.parse(ts, errors);
        ts.next(TokenType.Assign);
        Exp exp = Exp.parse(ts, errors);
        ForStmt forStmt = new ForStmt(lVal, exp);
        while (ts.checkPoll(TokenType.Comma)) {
            LVal lVal2 = LVal.parse(ts, errors);
            ts.next(TokenType.Assign);
            Exp exp2 = Exp.parse(ts, errors);
            forStmt.addExtraStmt(lVal2, exp2);
        }
        return forStmt;
    }
}
