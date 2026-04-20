package pins26.phase;

import java.util.*;

import com.sun.org.apache.xpath.internal.compiler.Token;

import pins26.common.*;

class SynAn implements AutoCloseable {

    /** Leksikalni analizator. */
    private final LexAn lexAn;

    private static final HashSet<Token.Symbol> CONST_SYMBOLS = new HashSet<>(List.of(Token.Symbol.INTCONST, Token.Symbol.STRINGCONST, Token.Symbol.CHARCONST));
    private static final Set<Token.Symbol> DEFINITION_START_SYMBOLS = Set.of(Token.Symbol.FUN, Token.Symbol.VAR);
    private static final Set<Token.Symbol> PREFIX_OPERATORS = Set.of(Token.Symbol.NOT, Token.Symbol.ADD, Token.Symbol.SUB, Token.Symbol.PTR);
    private static final Set<Token.Symbol> MULTIPLICATIVE_OPERATORS = Set.of(Token.Symbol.MUL, Token.Symbol.DIV, Token.Symbol.MOD);
    private static final Set<Token.Symbol> ADDITIVE_OPERATORS = Set.of(Token.Symbol.ADD, Token.Symbol.SUB);
    private static final Set<Token.Symbol> COMPARATOR_OPERATORS = Set.of(Token.Symbol.EQU, Token.Symbol.NEQ, Token.Symbol.LTH, Token.Symbol.GTH, Token.Symbol.LEQ, Token.Symbol.GEQ);

    /**
     * Ustvari nov sintaksni analizator.
     *
     * @param srcFileName Ime izvorne datoteke.
     */
    public SynAn(final String srcFileName) {
        this.lexAn = new LexAn(srcFileName);
    }

    @Override
    public AST.MainDef close() {
        lexAn.close();
    }

    /**
     * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
     * je prave vrste.
     *
     * @param symbol Pricakovana vrsta leksikalnega simbola.
     * @return Prevzeti leksikalni simbol.
     */
    private Token check(Token.Symbol symbol) {
        final Token token = lexAn.takeToken();
        if (token.symbol() != symbol)
            throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "', expected '" + symbol + "'.");
        return token;
    }

    private HashMap<AST.Node, Report.Locatable> attrLoc;

    private Report.Locatable lastLoc = new Report.Location(0, 0);
    /**
     * Opravi sintaksno analizo.
     */
    public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
        this.attrLoc = attrLoc;
        final AST.Nodes<AST.MainDef> defs = parseProgram();
        if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
            Report.warning(lexAn.peekToken(),
                    "Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
        return defs;
    }

    public AST.Nodes<AST.MainDef> parseProgram() {
        AST.Nodes<AST.MainDef> defs = new AST.Nodes<>(parseDefinitions());
        if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
            throw new Report.Error(lexAn.peekToken(),
                    "Unexpected token '" + lexAn.peekToken().lexeme() + "'. Definition expected.");
        return defs;
    }

    private List<AST.MainDef> parseDefinitions() {
        ArrayList<AST.MainDef> defs = new ArrayList<>();
        while (DEFINITION_START_SYMBOLS.contains(lexAn.peekToken().symbol())) {
            defs.add(parseDefinition());
        }
        return defs;
    }

    private AST.MainDef parseDefinition() {
        return (switch (lexAn.peekToken().symbol()) {
            case FUN -> parseFunction();
            case VAR -> parseVariable();
            default -> throw new Report.Error(lexAn.peekToken(),
                    "Expected definition (fun/var), got '" + lexAn.peekToken().lexeme() + "'.");
        });
    }

    private AST.MainDef parseFunction() {
        final Token funToken = lexAn.takeToken(); // FUN
        final Token nameToken = check(Token.Symbol.IDENTIFIER);
        check(Token.Symbol.LPAREN);
        List<AST.ParDef> parameters = parseParameters();
        final Token rParenToken = check(Token.Symbol.RPAREN);
        if (lexAn.peekToken().symbol() != Token.Symbol.ASSIGN) { // → fun IDENTIFIER ( parameters )
            lastLoc = new Report.Location(rParenToken.location().endLine(), rParenToken.location().endColumn());
            final AST.FunDef funDef = new AST.FunDef(nameToken.lexeme(), parameters, List.of());
            attrLoc.put(funDef, lastLoc);
            return funDef;
        }
        // → fun IDENTIFIER ( parameters ) = statements
        lexAn.takeToken();
        List<AST.Stmt> statements = parseStatements();
        final AST.FunDef funDef = new AST.FunDef(nameToken.lexeme(), parameters, statements);
        attrLoc.put(funDef, new Report.Location(funToken, attrLoc.get(statements.getLast()).location().endLocation()));
        return funDef;
    }

    private AST.MainDef parseVariable() {
        lexAn.takeToken(); // VAR
        final Token nameToken = check(Token.Symbol.IDENTIFIER);
        final Token assignToken = check(Token.Symbol.ASSIGN);
        List<AST.Init> initializers = parseInitializers();
        AST.VarDef varDef = new AST.VarDef(nameToken.lexeme(), initializers);
        Report.Location lastLoc = (initializers.isEmpty() ? assignToken.location() : attrLoc.get(initializers.getLast()).location()).endLocation();
        attrLoc.put(varDef, lastLoc);
        return varDef;
    }

    private List<AST.ParDef> parseParameters() {
        if (lexAn.peekToken().symbol() != Token.Symbol.IDENTIFIER)
            return List.of();
        ArrayList<AST.ParDef> parameters = new ArrayList<>();
        parameters.add(parseParameter());
        while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
            lexAn.takeToken(); // ,
            parameters.add(parseParameter());
        }
        return parameters;
    }

    private AST.ParDef parseParameter() {
        final Token nameToken = check(Token.Symbol.IDENTIFIER);
        AST.ParDef parDef = new AST.ParDef(nameToken.lexeme());
        attrLoc.put(parDef, new Report.Location(nameToken, nameToken.location().endLocation()));
        return parDef;
    }

    private List<AST.Init> parseInitializers() {
        ArrayList<AST.Init> inits = new ArrayList<>();
        inits.add(parseInitializer());
        while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
            lexAn.takeToken();
            inits.add(parseInitializer());
        }
        return inits;
    }

    private AST.Init parseInitializer() {
        switch (lexAn.peekToken().symbol()) {
            case CHARCONST, STRINGCONST -> {
                Token token = lexAn.takeToken();
                final AST.AtomExpr value = new AST.AtomExpr(getAtomExprType(token.symbol()), token.lexeme());
                attrLoc.put(value, token);
                final AST.Init init = new AST.Init(
                        new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"),
                        value
                        );
                attrLoc.put(init, new Report.Location(attrLoc.get(value), token));
                return init;
            }
            case INTCONST -> {
                final Token token = lexAn.takeToken();
                final AST.AtomExpr firstNum = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, token.lexeme());
                attrLoc.put(firstNum, token);
                if (lexAn.peekToken().symbol() == Token.Symbol.MUL) {
                    lexAn.takeToken();
                    if (!CONST_SYMBOLS.contains(lexAn.peekToken().symbol())) {
                        throw new Report.Error(lexAn.peekToken(), "Syntax error while parsing initializer '" + lexAn.peekToken().lexeme() + "'. Not a const!");
                    }
                    final AST.AtomExpr value = new AST.AtomExpr(getAtomExprType(lexAn.peekToken().symbol()), lexAn.peekToken().lexeme());
                    attrLoc.put(value, lexAn.peekToken());
                    final AST.Init init = new AST.Init(firstNum, value);
                    attrLoc.put(init, new Report.Location(attrLoc.get(firstNum), lexAn.takeToken().location().endLocation()));
                    return init;
                }
                final AST.Init init = new AST.Init(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), firstNum);
                attrLoc.put(init, token);
                return init;            
            }
            default -> throw new Report.Error(lexAn.peekToken(),
                    "Syntax error while parsing initializer '" + lexAn.peekToken().lexeme() + "'. Value expected.");
        }
    }

    private AST.AtomExpr.Type getAtomExprType(Token.Symbol symbol) {
        return switch (symbol) {
            case INTCONST -> AST.AtomExpr.Type.INTCONST;
            case CHARCONST -> AST.AtomExpr.Type.CHRCONST;
            case STRINGCONST -> AST.AtomExpr.Type.STRCONST;
            default -> null;
        };
    }

    private AST.BinExpr.Oper getBinExprType(Token.Symbol oper) {
        return switch (oper) {
            case GTH -> AST.BinExpr.Oper.GTH;
            case GEQ -> AST.BinExpr.Oper.GEQ;
            case LTH -> AST.BinExpr.Oper.LTH;
            case LEQ -> AST.BinExpr.Oper.LEQ;
            case EQU -> AST.BinExpr.Oper.EQU;
            case NEQ -> AST.BinExpr.Oper.NEQ;
            default -> null;
        };
    }

    private List<AST.Stmt> parseStatements() {
        ArrayList<AST.Stmt> statements = new ArrayList<>();
        statements.add(parseStatement());
        check(Token.Symbol.SEMIC);
        while (isStatementStart(lexAn.peekToken().symbol())) {
            statements.add(parseStatement());
            check(Token.Symbol.SEMIC);
        }
        return statements;
    }

    private boolean isStatementStart(Token.Symbol sym) {
        return sym == Token.Symbol.IF || sym == Token.Symbol.WHILE
            || sym == Token.Symbol.LET || isExpressionStart(sym);
    }

    private boolean isExpressionStart(Token.Symbol sym) {
        return CONST_SYMBOLS.contains(sym) || PREFIX_OPERATORS.contains(sym)
            || sym == Token.Symbol.IDENTIFIER || sym == Token.Symbol.LPAREN;
    }

    private AST.Stmt parseStatement() {
        switch (lexAn.peekToken().symbol()) {
            case IF -> {
                lexAn.takeToken(); // IF
                AST.Expr expr = parseExpression();
                check(Token.Symbol.THEN);
                parseStatements();
                if (lexAn.peekToken().symbol() == Token.Symbol.ELSE) {
                    lexAn.takeToken();
                    parseStatements();
                }
                check(Token.Symbol.END);
            }
            case WHILE -> {
                lexAn.takeToken();
                parseExpression();
                check(Token.Symbol.DO);
                parseStatements();
                check(Token.Symbol.END);
            }
            case LET -> {
                lexAn.takeToken();
                if (!DEFINITION_START_SYMBOLS.contains(lexAn.peekToken().symbol()))
                    throw new Report.Error(lexAn.peekToken(),
                            "Expected at least one definition after 'let', got '" + lexAn.peekToken().lexeme() + "'.");
                parseDefinitions();
                check(Token.Symbol.IN);
                parseStatements();
                check(Token.Symbol.END);
            }
            default -> {
                if (!isExpressionStart(lexAn.peekToken().symbol()))
                    throw new Report.Error(lexAn.peekToken(),
                            "Expected statement, got '" + lexAn.peekToken().lexeme() + "'.");
                parseExpression();
                if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
                    lexAn.takeToken();
                    parseExpression();
                }
            }
        }
    }

    private AST.Expr parseExpression() {
        return parseDisjExpr();
    }
    
    // OR
    private AST.Expr parseDisjExpr() {
        AST.Expr left = parseConjExpr();
        while (lexAn.peekToken().symbol() == Token.Symbol.OR) {
            lexAn.takeToken(); // ||
            AST.Expr right = parseConjExpr();
            AST.BinExpr binExpr = new AST.BinExpr(AST.BinExpr.Oper.OR, left, right);
            attrLoc.put(binExpr, new Report.Location(attrLoc.get(left), attrLoc.get(right)));
            left = binExpr;
        }
        return left;
    }
    
    // AND
    private AST.Expr parseConjExpr() {
        AST.Expr left = parseCompExpr();
        while (lexAn.peekToken().symbol() == Token.Symbol.AND) {
            lexAn.takeToken(); // ||
            AST.Expr right = parseCompExpr();
            AST.BinExpr binExpr = new AST.BinExpr(AST.BinExpr.Oper.AND, left, right);
            attrLoc.put(binExpr, new Report.Location(attrLoc.get(left), attrLoc.get(right)));
            left = binExpr;
        }
        return left;
    }
    
    // CMP
    private AST.Expr parseCompExpr() {
        AST.Expr left = parseAddExpr();
        if (COMPARATOR_OPERATORS.contains(lexAn.peekToken().symbol())) {
            final Token oper = lexAn.takeToken(); // <>!=
            if (COMPARATOR_OPERATORS.contains(lexAn.peekToken().symbol()))
                throw new Report.Error(lexAn.peekToken(),
                        "Comparison operators are not associative. Cannot chain '" + lexAn.peekToken().lexeme() + "'.");

            AST.Expr right = parseAddExpr();
            AST.BinExpr binExpr = new AST.BinExpr(
                    getBinExprType(oper.symbol()),
                    left,
                    right
            );
            attrLoc.put(binExpr, new Report.Location(attrLoc.get(left), attrLoc.get(right)));
            left = binExpr; 
        }
        return left;
    }
    
    // ADD
    private AST.Expr parseAddExpr() {
        parseMulExpr();
        while (ADDITIVE_OPERATORS.contains(lexAn.peekToken().symbol())) {
            lexAn.takeToken();
            parseMulExpr();
        }
    }
    
    // MUL
    private AST.Expr parseMulExpr() {
        parsePrefixExpr();
        while (MULTIPLICATIVE_OPERATORS.contains(lexAn.peekToken().symbol())) {
            lexAn.takeToken();
            parsePrefixExpr();
        }
    }
    
    // PREFIX
    private AST.Expr parsePrefixExpr() {
        if (PREFIX_OPERATORS.contains(lexAn.peekToken().symbol())) {
            lexAn.takeToken();
            parsePrefixExpr();
        } else {
            parsePostfixExpr();
        }
    }

    private AST.Expr parsePostfixExpr() {
        parseAtomExpr();
        while (lexAn.peekToken().symbol() == Token.Symbol.PTR) {
            lexAn.takeToken();
        }
    }

    private AST.Expr parseAtomExpr() {
        if (CONST_SYMBOLS.contains(lexAn.peekToken().symbol())) {
            lexAn.takeToken();
            return;
        }
        switch (lexAn.peekToken().symbol()) {
            case IDENTIFIER -> {
                lexAn.takeToken();
                if (lexAn.peekToken().symbol() == Token.Symbol.LPAREN) {
                    lexAn.takeToken();
                    parseArguments();
                    check(Token.Symbol.RPAREN);
                }
            }
            case LPAREN -> {
                lexAn.takeToken();
                parseExpression();
                check(Token.Symbol.RPAREN);
            }
            default -> throw new Report.Error(lexAn.peekToken(),
                    "Syntax error while parsing expression '" + lexAn.peekToken().lexeme() + "'. Not a valid expression.");
        }
    }

    private AST.MainDef parseArguments() {
        if (!isExpressionStart(lexAn.peekToken().symbol()))
            return;
        parseExpression();
        while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
            lexAn.takeToken();
            parseExpression();
        }
    }

    // --- ZAGON ---

    /**
     * Zagon sintaksnega analizatorja kot samostojnega programa.
     *
     * @param cmdLineArgs Argumenti v ukazni vrstici.
     */
    public static AST.MainDef main(final String[] cmdLineArgs) {
        System.out.println("This is PINS'26 compiler (syntax analysis):");

        try {
            if (cmdLineArgs.length == 0)
                throw new Report.Error("No source file specified in the command line.");
            if (cmdLineArgs.length > 1)
                Report.warning("Unused arguments in the command line.");

            try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
                synAn.parse();
            }

            // Upajmo, da kdaj pridemo to te tocke.
            // A zavedajmo se sledecega:
            // 1. Prevod je zaradi napak v programu lahko napacen :-o
            // 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
            Report.info("Done.");
        } catch (Report.Error error) {
            // Izpis opisa napake.
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

}
