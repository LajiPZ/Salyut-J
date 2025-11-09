package frontend.syntax.declaration.object;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.datatype.PointerType;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.GlobalVariable;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IAllocate;
import frontend.llvm.value.instruction.IStore;
import frontend.symbol.VarSymbol;
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

public class VarDef extends ASTNode {
    private Token ident;
    private ArrayList<ConstExp> indexExps;
    private InitVal initVal = null;
    private VarSymbol varSymbol = null;

    public VarDef(Token ident) {
        this.ident = ident;
        this.indexExps = new ArrayList<>();
        this.initVal = null;
    }

    public void addIndexExp(ConstExp indexExp) {
        this.indexExps.add(indexExp);
    }

    public void setInitVal(InitVal initVal) {
        this.initVal = initVal;
    }

    public static VarDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token ident = tokenStream.next(TokenType.Ident);
        VarDef varDef = new VarDef(ident);
        // 多维数组; 不支持的话，while改if即可
        while (tokenStream.checkPoll(TokenType.LeftBracket)) {
            varDef.addIndexExp(ConstExp.parse(tokenStream, errors));
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.getPrevToken().getFileLoc())
                );
            }
        }
        if (tokenStream.checkPoll(TokenType.Assign)) {
            varDef.setInitVal(
                InitVal.parse(tokenStream, errors, false)
            );
        }
        tokenStream.logParse("<VarDef>");
        return varDef;
    }

    public void visit(BType type, boolean isStatic) {
        indexExps.forEach(ConstExp::visit);
        DataType dataType = ArrayType.createDataType(type.toDataType(), indexExps);
        VarSymbol symbol = Tabulator.addVarSymbol(ident.getValue(), isStatic, dataType);
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.NameRedefinition, ident.getFileLoc())
            );
        } else {
            if (initVal != null) initVal.visit();
            // 注：这里只处理全局变量的初值，局部变量的初值在代码生成时处理
            try {
                if (Tabulator.isGlobalDef()) {
                    if (indexExps.isEmpty()) {
                        if (initVal == null) {
                            symbol.setInitType(new ValInitType(0, type.toDataType()));
                        } else {
                            symbol.setInitType(new ValInitType(initVal.getSingleExp().calc(), type.toDataType()));
                        }
                    } else {
                        if (initVal != null) {
                            symbol.setInitType(
                                ArrayInitType.createArrayInitType(
                                    ((ArrayType) dataType).getIndexList(),
                                    initVal.convert(),
                                    type.toDataType()
                                )
                            );
                        } else {
                            symbol.setInitType(
                                new ArrayInitType(
                                    ((ArrayType) dataType).getIndexList(),
                                    type.toDataType()
                                )
                            );
                        }
                    }
                }
            } catch (Exception e) {
                // e.g. getint()
            }
            this.varSymbol = symbol;
        }
    }

    public void build(IrBuilder builder, BType type, boolean isStatic, boolean isGlobal) {
        if (isGlobal) {
            // 全局变量的定义，右侧一定是常量表达式
            varSymbol.setValue(
                new Value("@" + varSymbol.getIdent(), new PointerType(varSymbol.getDataType()))
            );
            builder.addGlobalVariable(
                GlobalVariable.create(varSymbol)
            );
            return;
        }

        varSymbol.setValue(
            builder.insertInst(
                new IAllocate(new PointerType(varSymbol.getDataType()))
            )
        );
        if (initVal == null) {
            return;
        }
        if (initVal.getType() == InitVal.Type.Single) {
            Value pointer = varSymbol.getValue();
            Value val = ValueConverter.toBaseType(
                pointer,
                initVal.getSingleExp().build(builder)
            );
            builder.insertInst(
                new IStore(
                    val,
                    pointer
                )
            );
        } else {
            // 如法炮制
            // TODO： 这里只支持一维数组的初始化！
            List<InitVal> initializers = initVal.getSubInitVals();
            int length = indexExps.get(0).calc();
            for (int i = 0; i < length; i++) {
                Value pointer = builder.callGep(
                    varSymbol,
                    List.of(new IntConstant(i))
                );
                Value val;
                if (i < initializers.size()) {
                    val = initializers.get(i).getSingleExp().build(builder);
                } else {
                    val = new IntConstant(0);
                }

                builder.insertInst(
                    new IStore(
                        ValueConverter.toBaseType(pointer,val),
                        pointer
                    )
                );
            }
        }
    }
}
