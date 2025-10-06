package frontend.error;

import utils.Pair;

public enum ErrorType {
    IllegalSymbol, // Note: apart from & and |, extended to unknown char
    NameRedefinition,
    UndefinedName,
    MissingArgument,
    ArgumentTypeMismatch,
    ReturnTypeMismatch,
    MissingReturn,
    ConstModification,
    MissingSemicolon,
    MissingRParen,
    MissingRBracket,
    FormatCharMismatch,
    BreakContinueOutsideLoop
}
