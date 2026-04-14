package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;
  
  private static final HashSet<Token.Symbol> CONST_SYMBOLS = new HashSet<>(List.of(Token.Symbol.INTCONST, Token.Symbol.STRINGCONST, Token.Symbol.CHARCONST));
	private static final HashSet<Token.Symbol> BINARY_OPERATORS = new HashSet<>(List.of(Token.Symbol.MUL, Token.Symbol.DIV, Token.Symbol.MOD, Token.Symbol.ADD,
			Token.Symbol.SUB, Token.Symbol.EQU, Token.Symbol.NEQ, Token.Symbol.LTH, Token.Symbol.GTH, Token.Symbol.LEQ, Token.Symbol.GEQ, Token.Symbol.AND, Token.Symbol.OR));
  private Report.Locatable lastLoc = new Report.Location(0, 0);
  private static final Set<Token.Symbol> DEFINITION_START_SYMBOLS = Set.of(Token.Symbol.FUN, Token.Symbol.VAR);

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
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
    if (DEFINITION_START_SYMBOLS.contains(lexAn.peekToken().symbol())) {
      parseDefinitions();
    }	
    if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
	}

  private void parseDefinitions() {
    while (DEFINITION_START_SYMBOLS.contains(lexAn.peekToken().symbol())) {
      parseDefinition();
    }
  }

  private void parseDefinition() {
    switch (lexAn.peekToken().symbol()) {
      case FUN -> parseFunction();
      case VAR -> parseVariable();
      default -> { return; }
    }
  }

  private void parseVariable() {
    Token varToken = lexAn.takeToken();
    final Token name = check(Token.Symbol.IDENTIFIER);
    final Token assignToken = check(Token.Symbol.ASSIGN);
    parseInitializers();
  }

  private void parseInitializers() {
    while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
      final Token initializer = lexAn.takeToken();
      parseInitializer();
    }
  }

  private void parseInitializer() {
    switch (lexAn.peekToken().symbol()) {
      case CHARCONST, STRINGCONST -> lexAn.takeToken();
      case INTCONST -> {
        lexAn.takeToken();
        if (lexAn.peekToken().symbol() == Token.Symbol.MUL) {
          lexAn.takeToken();
          if (!CONST_SYMBOLS.contains(lexAn.peekToken().symbol())) {
            Report.Error(lexAn.peekToken(), "Syntax error while parsing initializer '" + lexAn.peekToken().lexeme() + "'. Not a const!");
          }
          
        }
      }
      default -> {
        throw new Report.Error(lexAn.peekToken(), "Syntax error while parsing initializer '" + lexAn.peekToken().lexeme() + "'. Value expected.");
			}
    }
  }

  private void parseFunction() {
    
  }

  private void parseParameters() {}

  private void parseStatments() {}

  private void parseStatment() {}

  
  


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
