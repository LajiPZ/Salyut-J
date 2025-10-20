package frontend.syntax.misc;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.ConstSymbol;
import frontend.symbol.ValSymbol;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class LVal extends ASTNode {
    private Token ident;
    private Exp index;
    private ValSymbol val;

    public LVal(Token ident) {
        this.ident = ident;
        this.index = null;
    }

    public LVal(Token ident, Exp index) {
        this.ident = ident;
        this.index = index;
    }

    public static LVal parse(TokenStream ts, List<ErrorEntry> errors) {
        Token ident = ts.next(TokenType.Ident);
        LVal lval;
        if (ts.checkPoll(TokenType.LeftBracket)) {
            Exp exp = Exp.parse(ts, errors);
            if (!ts.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", ts.getPrevToken().getFileLoc())
                );
            }
            lval = new LVal(ident, exp);
        } else {
            lval =  new LVal(ident);
        }
        ts.logParse("<LVal>");
        return lval;
    }

    public void visit(boolean hasAssign) {
        ValSymbol symbol = Tabulator.getValSymbol(ident.getValue());
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.UndefinedName, ident.getFileLoc())
            );
        } else {
            val = symbol;
            index.visit();
            if (hasAssign && symbol instanceof ConstSymbol) {
                Tabulator.recordError(
                    new ErrorEntry(ErrorType.ConstModification, ident.getFileLoc())
                );
            }
        }
    }
}
