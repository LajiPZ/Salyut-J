package frontend.syntax.statement;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.syntax.expression.Exp;
import frontend.syntax.logical.CondExp;
import frontend.syntax.misc.LVal;
import frontend.token.Token;
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
                retStmt = parseBlockStmt(tokenStream, errors);
                break;
            }
            case If: {
                retStmt = parseIfStmt(tokenStream, errors);
                break;
            }
            case For: {
                retStmt = parseForBlockStmt(tokenStream, errors);
                break;
            }
            case Break: {
                retStmt = parseBreakStmt(tokenStream, errors);
                break;
            }
            case Continue: {
                retStmt = parseContinueStmt(tokenStream, errors);
                break;
            }
            case Return: {
                retStmt = parseReturnStmt(tokenStream, errors);
                break;
            }
            case Printf: {
                retStmt = parsePrintfStmt(tokenStream, errors);
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

    private static BlockStmt parseBlockStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        return new BlockStmt(Block.parse(tokenStream, errors));
    }

    private static ContinueStmt parseContinueStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        return new ContinueStmt(token);
    }

    private static IfStmt parseIfStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        tokenStream.next(TokenType.LeftParen);
        CondExp cond = CondExp.parse(tokenStream, errors);
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        Stmt stmt = Stmt.parse(tokenStream, errors);
        if (tokenStream.checkPoll(TokenType.Else)) {
            Stmt elseStmt = Stmt.parse(tokenStream, errors);
            return new IfStmt(token, cond, stmt, elseStmt);
        }
        return new IfStmt(token, cond, stmt);
    }

    private static ForBlockStmt parseForBlockStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        tokenStream.next(TokenType.LeftParen);
        ForStmt init = tokenStream.check(TokenType.Semicolon) ? null : ForStmt.parse(tokenStream, errors);
        tokenStream.next(TokenType.Semicolon);
        CondExp cond = tokenStream.check(TokenType.Semicolon) ? null : CondExp.parse(tokenStream, errors);
        tokenStream.next(TokenType.Semicolon);
        ForStmt step = tokenStream.check(TokenType.RightParen) ? null : ForStmt.parse(tokenStream, errors);
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        Stmt stmt = Stmt.parse(tokenStream, errors);
        return new ForBlockStmt(init, cond, step, stmt);
    }

    private static BreakStmt parseBreakStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        return new BreakStmt(token);
    }

    private static PrintfStmt parsePrintfStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        tokenStream.next(TokenType.LeftParen);
        Token str = tokenStream.next(TokenType.StringConst);
        PrintfStmt retStmt = new PrintfStmt(token, str.getValue());
        while (tokenStream.checkPoll(TokenType.Comma)) {
            retStmt.addArgument(Exp.parse(tokenStream, errors));
        }
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        return retStmt;
    }

    private static ReturnStmt parseReturnStmt(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        if (!tokenStream.check(TokenType.Semicolon)) {
            Exp expr = Exp.parse(tokenStream, errors);
            if (!tokenStream.checkPoll(TokenType.Semicolon)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
                );
            }
            return new ReturnStmt(token,expr);
        }
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        tokenStream.next(TokenType.Semicolon);
        return new ReturnStmt(token);
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

}
