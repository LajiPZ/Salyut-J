package frontend.token;

@SuppressWarnings("SpellCheckingInspection")
public enum TokenType {
    Ident("IDENFR"),
    /* CONST */
    IntConst("INTCON"),
    StringConst("STRCON"),
    /* SYMBOLS */
    Not("NOT"),
    And("AND"),
    Or("OR"),
    Plus("PLUS"),
    Minus("MINU"),
    Mul("MULT"),
    Div("DIV"),
    Mod("MOD"),
    LT("LSS"),
    LE("LEQ"),
    GT("GRE"),
    GE("GEQ"),
    EQ("EQL"),
    NE("NEQ"),
    Assign("ASSIGN"),
    Semicolon("SEMICN"),
    Comma("COMMA"),
    LeftParen("LPARENT"),
    RightParen("RPARENT"),
    LeftBracket("LBRACK"),
    RightBracket("RBRACK"),
    LeftBrace("LBRACE"),
    RightBrace("RBRACE"),
    /* KEYWORDS */
    Main("MAINTK"),
    Const("CONSTTK"),
    Int("INTTK"),
    Void("VOIDTK"),
    Break("BREAKTK"),
    Continue("CONTINUETK"),
    If("IFTK"),
    Else("ELSETK"),
    Printf("PRINTFTK"),
    Return("RETURNTK"),
    Static("STATICTK"),
    For("FORTK"),
    Unknown("UNKNOWN");

    private String alias;

    TokenType(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return this.alias;
    }
}
