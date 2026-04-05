package pins26.common;

/**
 * Leksikalni simbol.
 * 
 * @param location Lokacija simbola v izvornem programu.
 * @param symbol   Vrsta simbola.
 * @param lexeme   Znakovna predstavitev simbola.
 */
public record Token(Report.Location location, Symbol symbol, String lexeme) implements Report.Locatable {

	/**
	 * Vrste leksikalnih simbolov.
	 */
	public enum Symbol {
		/** Konec datoteke. */
		EOF,
		/** Stevilo. */
		INTCONST,
		/** Znak. */
		CHARCONST,
		/** Niz znakov. */
		STRINGCONST,
		/** Ime. */
		IDENTIFIER,
		/** Kljucna beseda {@code fun}. */
		FUN,
		/** Kljucna beseda {@code var}. */
		VAR,
		/** Kljucna beseda {@code if}. */
		IF,
		/** Kljucna beseda {@code then}. */
		THEN,
		/** Kljucna beseda {@code else}. */
		ELSE,
		/** Kljucna beseda {@code while}. */
		WHILE,
		/** Kljucna beseda {@code do}. */
		DO,
		/** Kljucna beseda {@code let}. */
		LET,
		/** Kljucna beseda {@code in}. */
		IN,
		/** Kljucna beseda {@code end}. */
		END,
		/** Simbol {@code =}. */
		ASSIGN,
		/** Simbol {@code ,}. */
		COMMA,
		/** Simbol {@code &&}. */
		AND,
		/** Simbol {@code ||}. */
		OR,
		/** Simbol {@code !}. */
		NOT,
		/** Simbol {@code ==}. */
		EQU,
		/** Simbol {@code !=}. */
		NEQ,
		/** Simbol {@code >}. */
		GTH,
		/** Simbol {@code <}. */
		LTH,
		/** Simbol {@code >=}. */
		GEQ,
		/** Simbol {@code <=}. */
		LEQ,
		/** Simbol {@code +}. */
		ADD,
		/** Simbol {@code -}. */
		SUB,
		/** Simbol {@code *}. */
		MUL,
		/** Simbol {@code /}. */
		DIV,
		/** Simbol {@code %}. */
		MOD,
		/** Simbol {@code ^}. */
		PTR,
		/** Simbol {@code (}. */
		LPAREN,
		/** Simbol {@code )}. */
		RPAREN,
	}

	@Override
	public String toString() {
		String lexeme = switch (symbol) {
		case INTCONST -> "(" + this.lexeme + ")";
		case CHARCONST -> "(" + this.lexeme + ")";
		case STRINGCONST -> "(" + this.lexeme + ")";
		case IDENTIFIER -> "(" + this.lexeme + ")";
		default -> "";
		};
		return location + " " + symbol + lexeme;
	}

}