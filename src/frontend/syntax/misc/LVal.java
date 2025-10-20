package frontend.syntax.misc;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.ConstSymbol;
import frontend.symbol.ValSymbol;
import frontend.symbol.datatype.init.ArrayInitType;
import frontend.symbol.datatype.init.InitType;
import frontend.symbol.datatype.init.ValInitType;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class LVal extends ASTNode {
    private Token ident;
    private ArrayList<Exp> indexList;
    private ValSymbol valSymbol = null;

    public LVal(Token ident) {
        this.ident = ident;
        this.indexList = new ArrayList<>();
    }

    public void addIndexExp(Exp exp) {
        indexList.add(exp);
    }

    public static LVal parse(TokenStream ts, List<ErrorEntry> errors) {
        Token ident = ts.next(TokenType.Ident);
        LVal lval = new LVal(ident);
        if (ts.checkPoll(TokenType.LeftBracket)) {
            Exp exp = Exp.parse(ts, errors);
            if (!ts.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", ts.getPrevToken().getFileLoc())
                );
            }
            lval.addIndexExp(exp);
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
            valSymbol = symbol;
            indexList.forEach(Exp::visit);
            if (hasAssign && symbol instanceof ConstSymbol) {
                Tabulator.recordError(
                    new ErrorEntry(ErrorType.ConstModification, ident.getFileLoc())
                );
            }
        }
    }

    public int calc() {
        if (valSymbol == null) {
            throw new RuntimeException("Symbol of LVal not linked");
        }
        if (!valSymbol.hasInitType()) {
            throw new RuntimeException("Symbol of LVal has no initType");
        }
        InitType init = valSymbol.getInitType();
        if (init instanceof ValInitType) {
            return ((ValInitType)init).getValue();
        } else {
            return ((ArrayInitType)init).get(indexList);
        }
    }
}
