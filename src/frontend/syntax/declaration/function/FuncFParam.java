package frontend.syntax.declaration.function;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.FuncSymbol;
import frontend.symbol.VarSymbol;
import frontend.symbol.datatype.init.ArrayType;
import frontend.symbol.datatype.init.InitDataType;
import frontend.symbol.datatype.init.IntType;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class FuncFParam extends ASTNode {
    private BType type;
    private Token ident;
    private int depth = 0;
    private VarSymbol paramSymbol = null;

    public FuncFParam(BType type, Token ident) {
        this.type = type;
        this.ident = ident;
    }

    public FuncFParam(BType type, Token ident, int depth) {
        this.type = type;
        this.ident = ident;
        this.depth = depth;
    }

    public static FuncFParam parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        BType type = BType.parse(tokenStream, errors);
        Token ident = tokenStream.next(TokenType.Ident);
        FuncFParam funcFParam;
        if (tokenStream.checkPoll(TokenType.LeftBracket)) {
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.getPrevToken().getFileLoc())
                );
            }
            funcFParam = new FuncFParam(type, ident, 1);
        } else {
            funcFParam =  new FuncFParam(type, ident);
        }
        tokenStream.logParse("<FuncFParam>");
        return funcFParam;
    }

    public void visit(FuncSymbol funcSymbol) {
        InitDataType initDataType = depth == 0 ? new IntType() : new ArrayType(ArrayType.ElementType.Int); // TODO： this is not right
        VarSymbol symbol = Tabulator.addParameter(funcSymbol, ident.getValue(), initDataType);
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.NameRedefinition, ident.getFileLoc())
            );
        } else {
            this.paramSymbol = symbol;
        }
    }
}
