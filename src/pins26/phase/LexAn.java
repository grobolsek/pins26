package pins26.phase;

import java.io.*;
import java.util.*;
import pins26.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {
  // At the class level, define operator tables:
  private static final Map<String, Token.Symbol> TWO_CHAR_OPS = Map.of(
    "==", Token.Symbol.EQU,
    "!=", Token.Symbol.NEQ,
    "<=", Token.Symbol.LEQ,
    ">=", Token.Symbol.GEQ,
    "&&", Token.Symbol.AND,
    "||", Token.Symbol.OR
  );

  private static final Map<Character, Token.Symbol> ONE_CHAR_OPS = Map.ofEntries(
    Map.entry('=', Token.Symbol.ASSIGN),
    Map.entry('!', Token.Symbol.NOT),
    Map.entry('<', Token.Symbol.LTH),
    Map.entry('>', Token.Symbol.GTH),
    Map.entry('+', Token.Symbol.ADD),
    Map.entry('-', Token.Symbol.SUB),
    Map.entry('*', Token.Symbol.MUL),
    Map.entry('/', Token.Symbol.DIV),
    Map.entry('%', Token.Symbol.MOD),
    Map.entry('^', Token.Symbol.PTR),
    Map.entry('(', Token.Symbol.LPAREN),
    Map.entry(')', Token.Symbol.RPAREN),
    Map.entry(',', Token.Symbol.COMMA),
    Map.entry(';', Token.Symbol.SEMIC)
  );
    /** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = -2;

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 4 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * 
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private void nextToken() {
    // skip white spaces
    while (Character.isWhitespace(buffChar)) {
      nextChar();
    }

    final Report.Location location = new Report.Location(buffCharLine, buffCharColumn);
    if (buffChar == -1) {
			buffToken = new Token(new Report.Location(0,0), Token.Symbol.EOF, "");
			return;
		}

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append((char) buffChar);

    // comments
    if (buffChar == '/') {
      nextChar();
      if (buffChar == '/') {
        while (buffChar != '\n' && buffChar != -1) {
          stringBuilder.append((char) buffChar);
          nextChar(); 
        }
        nextToken();
        return;
      }
      buffToken = new Token(location, Token.Symbol.DIV, "/");
      return;
    }

    if (Character.toString(buffChar).matches("[a-zA-Z_]")) {
      nextChar();
			while (buffChar != -1 && Character.toString(buffChar).matches("[a-zA-Z0-9_]")) {
				stringBuilder.append((char) buffChar);
				nextChar();
			}

			final String name = stringBuilder.toString();
			final Token.Symbol symbol = switch (name) {
				case "fun" -> Token.Symbol.FUN;
				case "var" -> Token.Symbol.VAR;
				case "if" -> Token.Symbol.IF;
				case "then" -> Token.Symbol.THEN;
				case "else" -> Token.Symbol.ELSE;
				case "while" -> Token.Symbol.WHILE;
				case "do" -> Token.Symbol.DO;
				case "let" -> Token.Symbol.LET;
				case "in" -> Token.Symbol.IN;
				case "end" -> Token.Symbol.END;
				default -> Token.Symbol.IDENTIFIER;
			};
			buffToken = new Token(new Report.Location(location, new Report.Location(buffCharLine, buffCharColumn - 1)), symbol, name);
			return;
		}
    
    // operators
    if (ONE_CHAR_OPS.containsKey((char) buffChar)) {
      char first = (char) buffChar;
      nextChar();

      // try two-char operator first
      String twoChar = "" + first + (char) buffChar;
      if (TWO_CHAR_OPS.containsKey(twoChar)) {
          nextChar(); // consume second char
          buffToken = new Token(
              new Report.Location(location, new Report.Location(buffCharLine, buffCharColumn - 1)),
              TWO_CHAR_OPS.get(twoChar), twoChar
          );
          return;
      }

      // fall back to one-char operator
      buffToken = new Token(location, ONE_CHAR_OPS.get(first), String.valueOf(first));
      return;
    }
  }

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken().symbol() != Token.Symbol.EOF)
					System.out.println(lexAn.takeToken());
				System.out.println(lexAn.takeToken());
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
