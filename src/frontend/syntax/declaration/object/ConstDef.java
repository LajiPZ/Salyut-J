package frontend.syntax.declaration.object;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.datatype.PointerType;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IAllocate;
import frontend.llvm.value.instruction.IStore;
import frontend.symbol.ConstSymbol;
import frontend.datatype.ArrayType;
import frontend.datatype.DataType;
import frontend.datatype.init.ArrayInitType;
import frontend.datatype.init.ValInitType;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.syntax.expression.ConstExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ConstDef extends ASTNode {
    private Token ident;
    private ArrayList<ConstExp> indexExps;
    private ConstInitVal initVal = null;
    private ConstSymbol constSymbol = null;

    public ConstDef(Token ident) {
        this.ident = ident;
        this.indexExps = new ArrayList<>();
    }

    public void setInitVal(ConstInitVal initVal) {
        this.initVal = initVal;
    }

    public void addIndexExp(ConstExp indexExp) {
        this.indexExps.add(indexExp);
    }

    public static ConstDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token ident = tokenStream.next(TokenType.Ident);
        ConstDef def = new ConstDef(ident);
        // 多维数组; 不支持的话，while改if即可
        while (tokenStream.checkPoll(TokenType.LeftBracket)) {
            def.addIndexExp(ConstExp.parse(tokenStream, errors));
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.getPrevToken().getFileLoc())
                );
            }
        }
        tokenStream.next(TokenType.Assign);
        def.setInitVal(ConstInitVal.parse(tokenStream, errors, false));
        tokenStream.logParse("<ConstDef>");
        return def;
    }

    public void visit(BType type) {
        indexExps.forEach(ConstExp::visit);
        DataType dataType = ArrayType.createDataType(type.toDataType(), indexExps);
        ConstSymbol symbol = Tabulator.addConstSymbol(ident.getValue(), dataType);
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.NameRedefinition, ident.getFileLoc())
            );
        } else {
            initVal.visit();
            try {
                if (indexExps.isEmpty()) {
                    symbol.setInitType(new ValInitType(initVal.singleCalc(), dataType));
                } else {
                    symbol.setInitType(ArrayInitType.createArrayInitType(
                        ((ArrayType)dataType).getIndexList(),
                        initVal,
                        type.toDataType() // TODO: 应该没有必要, 再从dataType找下去
                    ));
                }
            } catch (Exception e) {
                // 为什么？考虑getInt()
            }
            this.constSymbol = symbol;
        }
    }

    public void build(IrBuilder builder, BType type) {
        // 和普通变量逻辑基本一致，不过不用考虑左值计算和static
        constSymbol.setValue(
            builder.insertInst(
                new IAllocate(new PointerType(constSymbol.getDataType()))
            )
        );
        if (initVal.getType() == ConstInitVal.Type.Single) {
            Value pointer = constSymbol.getValue();
            Value val = ValueConverter.toBaseType(
                pointer,
                ((ValInitType)constSymbol.getInitType()).toValue()
            );
            builder.insertInst(
                new IStore(val, pointer)
            );
        } else {
            // That is, multiple
            // TODO： 这里只支持一维数组的初始化！
            List<ConstInitVal> initializers = initVal.getSubInitVals();
            int length = indexExps.get(0).calc();
            for (int i = 0; i < length; i++) {
                Value pointer = builder.callGep(
                    constSymbol,
                    List.of(new IntConstant(i))
                );
                Value val = new IntConstant(
                    (i < initializers.size() ? initializers.get(i).getSingleConstExp().calc() : 0),
                    type.toDataType()
                );
                builder.insertInst(
                    new IStore(val, pointer)
                );
            }
        }
    }
}
