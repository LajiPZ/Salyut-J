package frontend.syntax.statement;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.syntax.misc.LVal;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

abstract public class Stmt extends ASTNode {
    public enum Type {
        Assign, Exp, Block, If, For, Break, Continue, Return, Printf
    }

    private Type type;

    public Stmt(Type type) {
        this.type = type;
    }

    public static Stmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Stmt retStmt;
        switch (tokenStream.peek().getType()) {
            case LeftBrace: {
                retStmt = BlockStmt.parse(tokenStream, errors);
                break;
            }
            case If: {
                retStmt = IfStmt.parse(tokenStream, errors);
                break;
            }
            case For: {
                retStmt = ForBlockStmt.parse(tokenStream, errors);
                break;
            }
            case Break: {
                retStmt = BreakStmt.parse(tokenStream, errors);
                break;
            }
            case Continue: {
                retStmt = ContinueStmt.parse(tokenStream, errors);
                break;
            }
            case Return: {
                retStmt = ReturnStmt.parse(tokenStream, errors);
                break;
            }
            case Printf: {
                retStmt = PrintfStmt.parse(tokenStream, errors);
                break;
            }
            default: {
                retStmt = parseDefaultStmt(tokenStream, errors);
                break;
            }
        }
        tokenStream.logParse("<Stmt>");
        return retStmt;
    }

    private static Stmt parseDefaultStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        if (tryAssign(tokenStream)) {
            LVal lVal = LVal.parse(tokenStream, errors);
            tokenStream.next(TokenType.Assign);
            Exp exp = Exp.parse(tokenStream, errors);
            if (!tokenStream.checkPoll(TokenType.Semicolon)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
                );
            }
            return new AssignStmt(lVal, exp);
        } else {
            ExpStmt stmt = new ExpStmt();
            if (!tokenStream.check(TokenType.Semicolon)) {
                stmt.setExp(Exp.parse(tokenStream, errors));
            }
            if (!tokenStream.checkPoll(TokenType.Semicolon)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
                );
            }
            return stmt;
        }
    }

    private static boolean tryAssign(TokenStream tokenStream) {
        tokenStream.setCheckpoint();
        try {
            List<ErrorEntry> temp = new ArrayList<>();
            LVal.parse(tokenStream, temp);
            tokenStream.next(TokenType.Assign);
        } catch (Exception e) {
            return false;
        } finally {
            tokenStream.releaseCheckpoint();
        }
        return true;
    }

    public void visit() {
        switch (this.type) {
            case Assign: {
                AssignStmt assignStmt = (AssignStmt) this;
                assignStmt.visit();
                break;
            }
            case Exp: {
                ExpStmt expStmt = (ExpStmt) this;
                expStmt.visit();
                break;
            }
            case Block: {
                BlockStmt blockStmt = (BlockStmt) this;
                Tabulator.intoNewScope();
                blockStmt.visit();
                Tabulator.exitScope();
                break;
            }
            case If: {
                IfStmt ifStmt = (IfStmt) this;
                ifStmt.visit();
                break;
            }
            case For: {
                ForBlockStmt forBlockStmt = (ForBlockStmt) this;
                Tabulator.intoLoop();
                forBlockStmt.visit();
                Tabulator.exitLoop();
                break;
            }
            case Break: {
                BreakStmt breakStmt = (BreakStmt) this;
                breakStmt.visit();
                break;
            }
            case Continue: {
                ContinueStmt continueStmt = (ContinueStmt) this;
                continueStmt.visit();
                break;
            }
            case Return: {
                ReturnStmt returnStmt = (ReturnStmt) this;
                returnStmt.visit();
                break;
            }
            case Printf: {
                PrintfStmt printfStmt = (PrintfStmt) this;
                printfStmt.visit();
                break;
            }
        }
    }

}
