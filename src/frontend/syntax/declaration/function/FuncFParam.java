package frontend.syntax.declaration.function;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.FuncSymbol;
import frontend.symbol.VarSymbol;
import frontend.symbol.datatype.ArrayType;
import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.init.ArrayInitType;
import frontend.symbol.datatype.init.InitType;
import frontend.symbol.datatype.init.ValInitType;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.syntax.expression.ConstExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.LinkedList;
import java.util.List;

public class FuncFParam extends ASTNode {
    private BType type;
    private Token ident;
    private List<ConstExp> indexList;
    private VarSymbol paramSymbol = null;

    public FuncFParam(BType type, Token ident, boolean isArray) {
        this.type = type;
        this.ident = ident;
        if (isArray) {
            this.indexList = new LinkedList<>();
        } else {
            this.indexList = null;
        }
    }

    public List<ConstExp> getIndexExps() {
        if (indexList == null) {
            return List.of();
        } else {
            return indexList;
        }
    }

    public void addIndexExp(ConstExp exp) {
        indexList.add(exp);
    }

    public static FuncFParam parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        BType type = BType.parse(tokenStream, errors);
        Token ident = tokenStream.next(TokenType.Ident);
        FuncFParam funcFParam;
        if (tokenStream.checkPoll(TokenType.LeftBracket)) {
            funcFParam = new FuncFParam(type, ident, true);
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.getPrevToken().getFileLoc())
                );
            }
            funcFParam.addIndexExp(null);
            /* 多维数组，暂时不会发生
            while (tokenStream.checkPoll(TokenType.LeftBracket)) {
                funcFParam.addIndexExp(ConstExp.parse(tokenStream, errors));
                if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                    errors.add(
                        new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.getPrevToken().getFileLoc())
                    );
                }
            }
            */
        } else {
            funcFParam =  new FuncFParam(type, ident, false);
        }
        tokenStream.logParse("<FuncFParam>");
        return funcFParam;
    }

    public void visit(FuncSymbol funcSymbol) {
        DataType dataType = ArrayType.createDataType(type.toDataType(), this.getIndexExps());
        VarSymbol symbol = Tabulator.addParameter(funcSymbol, ident.getValue(), dataType);
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.NameRedefinition, ident.getFileLoc())
            );
        } else {
            this.paramSymbol = symbol;
        }
    }
}
