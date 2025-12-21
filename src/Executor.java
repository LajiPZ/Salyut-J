import backend.mips.MipsModule;
import frontend.IrBuilder;
import frontend.Lexer;
import frontend.Parser;
import frontend.Tabulator;
import frontend.error.ErrorComparator;
import frontend.error.ErrorEntry;
import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.analysis.ControlFlowAnalysis;
import frontend.llvm.analysis.DominatorAnalysis;
import frontend.llvm.analysis.LoopAnalysis;
import frontend.llvm.optimization.*;
import frontend.syntax.CompileUnit;
import frontend.token.TokenStream;
import settings.Settings;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Executor {
    private static ArrayList<ErrorEntry> errors = new ArrayList<>();

    public static void execute() throws IOException {
        // 1. Lexical analysis
        Lexer lexer = new Lexer(Settings.FilePath.src);
        if (lexer.analyze()) {
            if (Settings.PrintConfig.printTokenStream) {
                lexer.printTokenStream(Settings.FilePath.lexerOut);
            }
        } else {
            collectErrors(lexer.getErrors());
        }
        TokenStream tokenStream = lexer.getTokenStream();

        // 2. Syntactic analysis
        Parser parser = new Parser(tokenStream);
        CompileUnit compileUnit = null;
        if (parser.parse()) {
            if (Settings.PrintConfig.printParseProcess) {
                tokenStream.printParseLog(Settings.FilePath.parserOut);
            }
        } else {
            collectErrors(parser.getErrors());
        }

        compileUnit = parser.getCompileUnit();

        // 2.5 Symbol table
        Tabulator tabulator = new Tabulator(compileUnit);
        if (tabulator.tabulate()) {
            if (Settings.PrintConfig.printTabulation) {
                tabulator.printTabulationLog(Settings.FilePath.tabulatorOut);
            }
        } else {
            collectErrors(tabulator.getErrors());
        }

        // 3. Intermediate code generation
        IrBuilder irBuilder = new IrBuilder(compileUnit);
        IrModule irModule = irBuilder.build();

        // 4. Optimization
        if (Settings.OptimizeConfig.enableOptimization) {
            List<Pass> passes = List.of(
                new ControlFlowAnalysis(),
                new RemoveUnreachableBBlocks(),
                new DominatorAnalysis(),

                new Mem2Reg()/*,
                new EliminateReadOnlyGlobal(),

                new EliminateDeadCode(),

                new LoopAnalysis(),
                new LoopHoist(),
                new ControlFlowAnalysis(),
                new ConstantFolding(),
                new SimplifyMultiplication(),
                new EliminateDeadCode(),

                new SimplifyControlFlow(),
                new LocalVariableNumbering(),
                new ControlFlowAnalysis(),
                new DominatorAnalysis(),

                // new EliminateDeadCode(),

                new SimplifyControlFlow(),
                new ControlFlowAnalysis(),
                new DominatorAnalysis(),
                // new EliminateDeadCode(),

                new InlineFunction(),
                new ControlFlowAnalysis(),
                new DominatorAnalysis(),

                new SimplifyControlFlow(),
                new ControlFlowAnalysis(),
                new DominatorAnalysis(),
                new EliminateDeadCode(),

                new RenameValues()*/
            );
            for (int i = 0; i < passes.size(); i++) {
                Pass pass = passes.get(i);
                pass.run(irModule);
            }
            // for (Pass pass : passes) pass.run(irModule);
        }

        if (Settings.PrintConfig.printIR) {
            irModule.printIR(Settings.FilePath.IROut);
        }

        // 5. Target code generation
        MipsModule mipsModule = new MipsModule();
        mipsModule.buildFromIR(irModule);
        mipsModule.runPostBuildProcessing();
        if (Settings.PrintConfig.printMIPS) {
            mipsModule.printMIPS(Settings.FilePath.MIPSOut);
        }


        if (Settings.PrintConfig.printError) {
            printErrors(Settings.FilePath.errOut);
            Executor.exit(1);
        }
        Executor.exit(0);
    }

    public static void collectErrors(List<ErrorEntry> subErrors) {
        errors.addAll(subErrors);
    }

    public static void printErrors(String filePath) throws IOException {
        errors.sort(new ErrorComparator());
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        for (ErrorEntry error : errors) {
            writer.write(error.toString());
        }
        writer.close();
    }

    private static void exit(int exitCode) {
        System.exit(exitCode);
    }
}
