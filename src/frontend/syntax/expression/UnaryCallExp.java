package frontend.syntax.expression;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.FuncSymbol;
import frontend.syntax.misc.FuncRParams;
import frontend.token.Token;

public class UnaryCallExp extends UnaryExp {
    private Token ident;
    private FuncRParams params = null;
    private FuncSymbol funcSymbol = null;

    public UnaryCallExp(Token ident) {
        super(Type.Call);
        this.ident = ident;
    }

    public void setFuncRParams(FuncRParams params) { this.params = params; }

    @Override
    public void visit() {
        FuncSymbol symbol = Tabulator.getFuncSymbol(ident.getValue());
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.UndefinedName, ident.getFileLoc())
            );
        } else {
            funcSymbol = symbol;
            params.visit();
            if (params.getParameterCount() != funcSymbol.getParameterCount()) {
                Tabulator.recordError(
                    new ErrorEntry(ErrorType.MissingArgument, ident.getFileLoc())
                );
            } else {
                // TODO: 类型检查
            }
        }
    }
}
