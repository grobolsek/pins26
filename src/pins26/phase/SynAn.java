package pins26.phase;

import java.util.*;

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
	public void close() {
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

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
    Report.info("Parsing program.");
		parseDefinitions();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			throw new Report.Error(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "' at the end of the program.");
	}

	private void parseDefinitions() {
		while (DEFINITION_START_SYMBOLS.contains(lexAn.peekToken().symbol())) {
			parseDefinition();
		}
	}

	private void parseDefinition() {
    Report.info("Parsing definition.");
		switch (lexAn.peekToken().symbol()) {
			case FUN -> parseFunction();
			case VAR -> parseVariable();
			default -> throw new Report.Error(lexAn.peekToken(),
					"Expected definition (fun/var), got '" + lexAn.peekToken().lexeme() + "'.");
		}
	}

	private void parseVariable() {
    Report.info("Parsing variable");
		lexAn.takeToken(); // VAR
		check(Token.Symbol.IDENTIFIER);
		check(Token.Symbol.ASSIGN);
		parseInitializers();
	}

	private void parseFunction() {
    Report.info("Parsing function");
    lexAn.takeToken(); // FUN
		check(Token.Symbol.IDENTIFIER);
		check(Token.Symbol.LPAREN);
		parseParameters();
		check(Token.Symbol.RPAREN);
		if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
      lexAn.takeToken();
			parseStatements();
		}
	}

	private void parseParameters() {
		if (lexAn.peekToken().symbol() != Token.Symbol.IDENTIFIER)
			return;
		check(Token.Symbol.IDENTIFIER);
    Report.info("Parsing parameter");
		while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
      Report.info("Parsing parameter");
      lexAn.takeToken();
			check(Token.Symbol.IDENTIFIER);
		}
	}

	private void parseInitializers() {
		parseInitializer();
		while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
      lexAn.takeToken();
			parseInitializer();
		}
	}

	private void parseInitializer() {
    Report.info("Parsing initializer");
		switch (lexAn.peekToken().symbol()) {
			case CHARCONST, STRINGCONST -> lexAn.takeToken();
			case INTCONST -> {
				lexAn.takeToken();
				if (lexAn.peekToken().symbol() == Token.Symbol.MUL) {
					lexAn.takeToken();
					if (!CONST_SYMBOLS.contains(lexAn.peekToken().symbol())) {
						throw new Report.Error(lexAn.peekToken(),
								"Syntax error while parsing initializer '" + lexAn.peekToken().lexeme() + "'. Not a const!");
					}
					lexAn.takeToken();
				}
			}
			default -> throw new Report.Error(lexAn.peekToken(),
					"Syntax error while parsing initializer '" + lexAn.peekToken().lexeme() + "'. Value expected.");
		}
	}
	
	private void parseStatements() {
		parseStatement();
		check(Token.Symbol.SEMIC);
		while (isStatementStart(lexAn.peekToken().symbol())) {
			parseStatement();
			check(Token.Symbol.SEMIC);
		}
	}

	private boolean isStatementStart(Token.Symbol sym) {
		return sym == Token.Symbol.IF || sym == Token.Symbol.WHILE
				|| sym == Token.Symbol.LET || isExpressionStart(sym);
	}

	private boolean isExpressionStart(Token.Symbol sym) {
		return CONST_SYMBOLS.contains(sym) || PREFIX_OPERATORS.contains(sym)
				|| sym == Token.Symbol.IDENTIFIER || sym == Token.Symbol.LPAREN;
	}

	private void parseStatement() {
    Report.info("Parsing statement");
		switch (lexAn.peekToken().symbol()) {
			case IF -> {
				lexAn.takeToken();
				parseExpression();
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

	private void parseExpression() {
    Report.info("Parsing expression");
		parseDisjExpr();
	}

	private void parseDisjExpr() {
		parseConjExpr();
		while (lexAn.peekToken().symbol() == Token.Symbol.OR) {
			lexAn.takeToken();
			parseConjExpr();
		}
	}

	private void parseConjExpr() {
		parseCompExpr();
		while (lexAn.peekToken().symbol() == Token.Symbol.AND) {
			lexAn.takeToken();
			parseCompExpr();
		}
	}

	private void parseCompExpr() {
		parseAddExpr();
		if (COMPARATOR_OPERATORS.contains(lexAn.peekToken().symbol())) {
			lexAn.takeToken();
			parseAddExpr();
			if (COMPARATOR_OPERATORS.contains(lexAn.peekToken().symbol()))
				throw new Report.Error(lexAn.peekToken(),
						"Comparison operators are not associative. Cannot chain '" + lexAn.peekToken().lexeme() + "'.");
		}
	}

	private void parseAddExpr() {
		parseMulExpr();
		while (ADDITIVE_OPERATORS.contains(lexAn.peekToken().symbol())) {
			lexAn.takeToken();
			parseMulExpr();
		}
	}

	private void parseMulExpr() {
		parsePrefixExpr();
		while (MULTIPLICATIVE_OPERATORS.contains(lexAn.peekToken().symbol())) {
			lexAn.takeToken();
			parsePrefixExpr();
		}
	}

	private void parsePrefixExpr() {
		if (PREFIX_OPERATORS.contains(lexAn.peekToken().symbol())) {
			lexAn.takeToken();
			parsePrefixExpr();
		} else {
			parsePostfixExpr();
		}
	}

	private void parsePostfixExpr() {
		parseAtomExpr();
		while (lexAn.peekToken().symbol() == Token.Symbol.PTR) {
			lexAn.takeToken();
		}
	}

	private void parseAtomExpr() {
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

	private void parseArguments() {
		if (!isExpressionStart(lexAn.peekToken().symbol()))
			return;
		parseExpression();
		while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
      Report.info("Parsing argument");
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
	public static void main(final String[] cmdLineArgs) {
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
