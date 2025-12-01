package frontend.llvm;

public interface Pass {

    public void run(IrModule module);
}
