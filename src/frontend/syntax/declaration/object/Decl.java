package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class Decl extends ASTNode {
    public enum Type {
        ConstDecl, VarDecl
    }

    private Type type;
    private Object value;

    public Decl(ConstDecl decl) {
        this.type = Type.ConstDecl;
        this.value = decl;
    }

    public Decl(VarDecl decl) {
        this.type = Type.VarDecl;
        this.value = decl;
    }

    public static Decl parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Decl decl;
        if (tokenStream.check(TokenType.Const)) {
            decl = new Decl(ConstDecl.parse(tokenStream, errors));
        } else {
            decl = new Decl(VarDecl.parse(tokenStream, errors));
        }
        tokenStream.logParse("<Decl>");
        return decl;
    }
}
