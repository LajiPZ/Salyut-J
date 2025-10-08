package frontend;

import frontend.syntax.CompileUnit;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;

final public class Parser {
    private TokenStream tokenStream;
    private ArrayList<Error> errors;
    private CompileUnit compileUnit = null; // Parser内记录分析完成后的顶层单元

    public Parser(TokenStream tokenStream) {
        this.tokenStream = tokenStream;
        this.errors = new ArrayList<>();
    }

    public boolean parse() {

        return true;
    }
}
