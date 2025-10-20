package frontend.syntax.declaration.object;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.ConstSymbol;
import frontend.symbol.ValSymbol;
import frontend.symbol.datatype.ArrayType;
import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.init.ArrayInitType;
import frontend.symbol.datatype.init.ValInitType;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.syntax.expression.ConstExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ConstDef extends ASTNode {
    private Token ident;
    private ArrayList<ConstExp> indexExps;
    private ConstInitVal initVal = null;
    private ConstSymbol constSymbol = null;

    public ConstDef(Token ident) {
        this.ident = ident;
        this.indexExps = new ArrayList<>();
    }

    public void setInitVal(ConstInitVal initVal) {
        this.initVal = initVal;
    }

    public void addIndexExp(ConstExp indexExp) {
        this.indexExps.add(indexExp);
    }

    public static ConstDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token ident = tokenStream.next(TokenType.Ident);
        ConstDef def = new ConstDef(ident);
        if (tokenStream.checkPoll(TokenType.LeftBracket)) {
            def.addIndexExp(ConstExp.parse(tokenStream, errors));
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.getPrevToken().getFileLoc())
                );
            }
        }
        tokenStream.next(TokenType.Assign);
        def.setInitVal(ConstInitVal.parse(tokenStream, errors));
        tokenStream.logParse("<ConstDef>");
        return def;
    }

    public void visit(BType type) {
        indexExps.forEach(ConstExp::visit);
        DataType dataType = ArrayType.createDataType(type.toDataType(), indexExps);
        ConstSymbol symbol = Tabulator.addConstSymbol(ident.getValue(), dataType);
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.NameRedefinition, ident.getFileLoc())
            );
        } else {
            initVal.visit();
            if (indexExps.isEmpty()) {
                symbol.setInitType(new ValInitType(initVal.singleCalc(), dataType));
            } else {
                symbol.setInitType(ArrayInitType.createArrayInitType(
                    ((ArrayType)dataType).getIndexList(),
                    initVal,
                    type.toDataType() // TODO: 应该没有必要, 再从dataType找下去
                ));
            }
            this.constSymbol = symbol;
        }
    }
}
