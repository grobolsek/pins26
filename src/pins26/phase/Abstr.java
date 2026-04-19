package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Abstraktna sintaksa.
 */
public class Abstr {

	@SuppressWarnings({ "doclint:missing" })
	public Abstr(SynAn synAn) {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi abstraktne sintakse.
	 * 
	 * Dodani atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu.</li>
	 * </ol>
	 */
	public static class AttrAST extends AST.AttrAST {

		/** Atribut: lokacija kode, ki priprada posameznemu vozliscu. */
		public final Map<AST.Node, Report.Locatable> attrLoc;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi abstraktne
		 * sintakse.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo.
		 * @param attrLoc Atribut: lokacija kode, ki priprada posameznemu vozliscu.
		 */
		public AttrAST(final AST.AttrAST attrAST, final Map<AST.Node, Report.Locatable> attrLoc) {
			super(attrAST.ast);
			this.attrLoc = attrLoc;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi abstraktne
		 * sintakse.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi abstraktne
		 *                sintakse.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST.ast);
			this.attrLoc = attrAST.attrLoc;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			switch (node) {
			case AST.Nodes<?> nodes:
				return "";
			default:
				final Report.Locatable loc = attrLoc.get(node);
				return (" ") + (loc == null ? "???" : loc.location().toString());
			}
		}

	}

	/**
	 * S klicem sintaksnega analizatorja zgradi abstraktno sintaksno drevo.
	 * 
	 * @param synAn Sintaksni analizator.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi abstraktne sintakse.
	 */
	public static AttrAST constructAST(SynAn synAn) {
		final HashMap<AST.Node, Report.Locatable> attrLoc = new HashMap<AST.Node, Report.Locatable>();
		final AST.Node ast = synAn.parse(attrLoc);
		return new AttrAST(new AST.AttrAST(ast), Collections.unmodifiableMap(attrLoc));
	}

	// --- ZAGON ---

	/**
	 * Zagon gradnje abstraktnega sintaksnega drevesa kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (abstract syntax):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (final SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);

				(new AST.Logger(abstrAttrAST)).log();
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
