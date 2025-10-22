package frontend.syntax.declaration.function;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.FuncSymbol;
import frontend.symbol.datatype.IntType;
import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class MainFuncDef extends ASTNode {
    private Token ident;
    private Block block = null;
    private FuncSymbol symbol = null;

    public MainFuncDef(Token ident) {
        this.ident = ident;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public Token getEndToken() {
        return block.getEndToken();
    }

    public static MainFuncDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        tokenStream.next(TokenType.Int);
        MainFuncDef mainFuncDef = new MainFuncDef(tokenStream.next(TokenType.Main));
        tokenStream.next(TokenType.LeftParen);
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        mainFuncDef.setBlock(Block.parse(tokenStream, errors));
        tokenStream.logParse("<MainFuncDef>");
        return mainFuncDef;
    }

    public void visit() {
        // Basically identical to FuncDef
        FuncSymbol funcSymbol = Tabulator.addFuncSymbol(
            ident.getValue(),
            new IntType()
        );
        if (funcSymbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.NameRedefinition,  ident.getFileLoc())
            );
        } else {
            this.symbol = funcSymbol;
            Tabulator.setExpectedReturnType(
                Tabulator.FuncReturnType.Int
            );
            Tabulator.intoNewScope();
            block.visit();
            if (!Tabulator.hasReturn()) {
                Tabulator.recordError(
                    new ErrorEntry(ErrorType.MissingReturn, this.getEndToken().getFileLoc())
                );
            }
            Tabulator.exitScope();
        }
    }
}
