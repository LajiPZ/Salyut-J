package frontend.syntax.declaration.function;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.value.Function;
import frontend.llvm.value.instruction.IReturn;
import frontend.llvm.value.instruction.ITerminator;
import frontend.symbol.FuncSymbol;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.datatype.VoidType;
import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class FuncDef extends ASTNode {
    private FuncType type;
    private Token ident;
    private FuncFParams fParams = null;
    private Block block = null;
    private FuncSymbol symbol = null;

    public FuncDef(FuncType type, Token ident) {
        this.type = type;
        this.ident = ident;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public void setfParams(FuncFParams fParams) {
        this.fParams = fParams;
    }

    public Token getEndToken() {
        return block.getEndToken();
    }

    public static FuncDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        FuncType type = FuncType.parse(tokenStream, errors);
        Token ident = tokenStream.next(TokenType.Ident);
        FuncDef funcDef = new FuncDef(type, ident);
        tokenStream.next(TokenType.LeftParen);
        if (!tokenStream.check(TokenType.RightParen)) {
            // 可能出现无可选项，但缺右括号的情况
            // 此时parse必定出例外，故套个catch，交给下面括号匹配检查即可
            try {
                funcDef.setfParams(FuncFParams.parse(tokenStream, errors));
            } catch (Exception e) {}
        }
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        funcDef.setBlock(Block.parse(tokenStream, errors));
        tokenStream.logParse("<FuncDef>");
        return funcDef;
    }


    public void visit() {
        FuncSymbol funcSymbol = Tabulator.addFuncSymbol(
            ident.getValue(),
            type.getType().equals(FuncType.Type.Void) ? new VoidType() : new IntType()
        );
        if (funcSymbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.NameRedefinition,  ident.getFileLoc())
            );
        } else {
            this.symbol = funcSymbol;
        }

        Tabulator.setExpectedReturnType(
            type.getType().equals(FuncType.Type.Void) ? Tabulator.FuncReturnType.Void : Tabulator.FuncReturnType.Int
        );
        Tabulator.intoNewScope();
        if (fParams != null) fParams.visit(funcSymbol);
        block.visit();
        if (!type.getType().equals(FuncType.Type.Void) && !Tabulator.hasReturn()) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.MissingReturn, this.getEndToken().getFileLoc())
            );
        }
        Tabulator.exitScope();
    }

    public void build(IrBuilder builder) {
        Function func = builder.registerFunction(
            this.getName(),
            this.getDataType(),
            this.getParams()
        );
        builder.newBBlock(true);
        block.build(builder);
        if (
            this.type.getType() == FuncType.Type.Void &&
            !(builder.getInsertPoint().getLastInstruction() instanceof ITerminator)
        ) {
            builder.insertInst(
                new IReturn()
            );
        }
        // TODO: function valCounter
    }

    public String getName() {
        return ident.getValue();
    }

    public DataType getDataType() {
        return symbol.getType();
    }

    public List<FuncFParam> getParams() {
        return fParams.getParams();
    }
}
