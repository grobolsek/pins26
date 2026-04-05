package pins26.common;

/**
 * Izpis obvestil, opozoril in napak.
 */
public class Report {

	@SuppressWarnings({ "doclint:missing" })
	private Report() {
		throw new InternalError();
	}

	/**
	 * Opis lokacije v izvorni datoteki.
	 * 
	 * @param begLine   Zacetna vrstica.
	 * @param begColumn Zacetni stolpec.
	 * @param endLine   Koncna vrstica.
	 * @param endColumn Koncni stolpec.
	 */
	public record Location(int begLine, int begColumn, int endLine, int endColumn) implements Locatable {

		/**
		 * Ustvari novo lokacijo, ki opisuje en sam znak izvorne datoteke.
		 * 
		 * @param line   Vrstica znaka.
		 * @param column Stolpec znaka.
		 */
		public Location(int line, int column) {
			this(line, column, line, column);
		}

		/**
		 * Ustvari novo lokacijo, ki se razteza od ene do druge lokacije.
		 * 
		 * @param beg Prva lokacija.
		 * @param end Druga lokacija.
		 */
		public Location(Locatable beg, Locatable end) {
			this(beg.location().begLine, beg.location().begColumn, end.location().endLine, end.location().endColumn);
		}

		@Override
		public String toString() {
			return "[" + (begLine + "." + begColumn) + ":" + (endLine + "." + endColumn) + "]";
		}

		@Override
		public Location location() {
			return this;
		}

	}

	/**
	 * Vmesnik, ki naj ga implementirajo razredi, katerih objekti predstavljajo dele
	 * izvorne datoteke.
	 */
	public interface Locatable {

		/**
		 * Vrne lokacijo dela izvorne datoteke, ki ga opisuje objekt.
		 * 
		 * @return Opis lokacije v izvorni datoteki.
		 */
		public Location location();

	}

	/**
	 * Izpis splosnega obvestila.
	 * 
	 * @param message Obvestilo.
	 */
	public static void info(final String message) {
		System.out.println(":-) " + message);
	}

	/**
	 * Izpis obvestila, ki je vezano na del izvorne datoteke.
	 * 
	 * @param location Opis lokacije v izvorni datoteki.
	 * @param message  Obvestilo.
	 */
	public static void info(final Locatable location, final String message) {
		System.out.println(":-) " + location + " " + message);
	}

	/**
	 * Izpis splosnega opozorila.
	 * 
	 * @param message Opozorilo.
	 */
	public static void warning(final String message) {
		System.out.println(":-o " + message);
	}

	/**
	 * Izpis opozorila, ki je vezano na del izvorne datoteke.
	 * 
	 * @param location Opis lokacije v izvorni datoteki.
	 * @param message  Opozorilo.
	 */
	public static void warning(final Locatable location, final String message) {
		System.out.println(":-o " + location + " " + message);
	}

	/**
	 * Napaka.
	 * 
	 * Objekt tega razreda se vrze v primeru, ko je program odkril napako v izvorni
	 * datoteki, zaradi katere ni vec mozno nadaljevati z izvajanjem.
	 */
	@SuppressWarnings("serial")
	public static class Error extends java.lang.Error {

		/**
		 * Ustvari novo napako.
		 * 
		 * @param message Opis napake.
		 */
		public Error(final String message) {
			super(":-( " + message);
		}

		/**
		 * Ustvari novo napako, ki je veznana na del izvorne datoteke.
		 * 
		 * @param location Opis lokacije v izvorni datoteki.
		 * @param message  Opis napake.
		 */
		public Error(final Locatable location, final String message) {
			super(":-( " + "[" + location.location() + "] " + message);
		}

	}

	/**
	 * Notranja napaka.
	 * 
	 * Objekt tega razreda se vze v primeru, ko program zazna notranjo napako.
	 */
	@SuppressWarnings("serial")
	public static class InternalError extends Error {

		/**
		 * Ustvari novo notranjo napako.
		 */
		public InternalError() {
			super("Internal error.");
			this.printStackTrace();
		}

	}

}