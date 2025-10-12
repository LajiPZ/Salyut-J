import frontend.Lexer;
import frontend.Parser;
import frontend.syntax.CompileUnit;
import frontend.token.TokenStream;
import settings.Settings;

import java.io.IOException;

public class Executor {
    public static void execute() throws IOException {
        // 1. Lexical analysis
        Lexer lexer = new Lexer(Settings.FilePath.src);
        TokenStream tokenStream = null;
        if (lexer.analyze()) {
            tokenStream = lexer.getTokenStream();
            if (Settings.PrintConfig.printTokenStream) {
                lexer.printTokenStream(Settings.FilePath.lexerOut);
            }
        } else {
            if (Settings.PrintConfig.printError) {
                lexer.printErrors(Settings.FilePath.errOut);
            }
            Executor.exit(1);
        }

        // 2. Syntactic analysis
        Parser parser = new Parser(tokenStream);
        CompileUnit compileUnit = null;
        if (parser.parse()) {
            compileUnit = parser.getCompileUnit();
            if (Settings.PrintConfig.printParseProcess) {
                tokenStream.printParseLog(Settings.FilePath.parserOut);
            }
        } else {
            if (Settings.PrintConfig.printError) {
                parser.printErrors(Settings.FilePath.errOut);
            }
            Executor.exit(1);
        }



        // 3. Intermediate code generation




        // 4. Optimization




        // 5. Target code generation



        Executor.exit(0);
    }

    private static void exit(int exitCode) {
        System.exit(exitCode);
    }
}
