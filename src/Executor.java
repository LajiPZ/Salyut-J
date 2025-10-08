import frontend.Lexer;
import frontend.Parser;
import frontend.token.TokenStream;

import java.io.IOException;

public class Executor {
    public static void execute() throws IOException {
        // 1. Lexical analysis
        Lexer lexer = new Lexer(Config.FilePath.src);
        TokenStream tokenStream = null;
        if (lexer.analyze()) {
            tokenStream = lexer.getTokenStream();
            if (Config.PrintConfig.printTokenStream) {
                lexer.printTokenStream(Config.FilePath.lexerOut);
            }
        } else {
            if (Config.PrintConfig.printError) {
                lexer.printErrors(Config.FilePath.errOut);
            }
            Executor.exit(1);
        }

        // 2. Syntactic analysis
        Parser parser = new Parser(tokenStream);



        // 3. Intermediate code generation




        // 4. Optimization




        // 5. Target code generation



        Executor.exit(0);
    }

    private static void exit(int exitCode) {
        System.exit(exitCode);
    }
}
