import frontend.Lexer;
import frontend.token.TokenStream;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        Lexer lexer = new Lexer("testfile.txt");
        lexer.analyze();
        lexer.printTokenStream("output.txt");
    }
}
