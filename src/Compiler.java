import frontend.Lexer;
import frontend.token.TokenStream;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        Executor.execute();
    }
}
