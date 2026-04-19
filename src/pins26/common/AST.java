package pins26.common;

import java.util.*;
import java.util.function.*;

/**
 * Abstraktno sintaksno drevo.
 */
public class AST {

	@SuppressWarnings({ "doclint:missing" })
	private AST() {
		throw new Report.InternalError();
	}

	// --- VOZLISCA ABSTRAKTNEGA SINTAKSNEGA DREVESA ---

	// Splosna vozlisca abstraktnega sintaksnega drevesa:

	/**
	 * Vmesnik, ki ga implementirajo vsa vozlisca abstraktnega sintaksnega drevesa.
	 * 
	 * Ta vmesnik je uporabljen pri definiciji atributa abstraktnega sintaksnega
	 * drevesa in tako omogoca, da z definicijo novih vmesnikov atribut pripredimo
	 * razlicnim vozliscem abstraktnega sintaksnega drevesa.
	 */
	public interface AnyNode {
	}

	/**
	 * Vozlisce abstraktnega sintaksnega drevesa.
	 */
	public static abstract class Node implements AnyNode {

		/**
		 * Ustvari novo vozlisce abstraktega sintaksnega drevesa.
		 */
		public Node() {
		}

		/**
		 * Sprejem obiskovalca.
		 * 
		 * @param <Result>   Tip rezultata obhoda z obiskovalcem.
		 * @param <Argument> Tip pomoznega argumenta pri obhodu z obiskovalcem.
		 * @param visitor    Obiskovalec.
		 * @param arg        Pomozni argument obiskovalca.
		 * @return Rezultat obhoda z obiskovalcem.
		 */
		public abstract <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg);

	}

	/**
	 * Zaporedje vozlisc abstraktnega sintaksnega drevesa.
	 * 
	 * @param <ANode> Tip vozlisc abstraktnega sintaksnega drevesa.
	 */
	public static class Nodes<ANode extends Node> extends Node implements Iterable<ANode> {

		/** Zaporedje vozlisc abstraktnega sintaksnega drevesa. */
		private final ANode[] nodes;

		/**
		 * Ustvari prazno zaporedje vozlisc abstraktnega sintaksnega drevesa.
		 */
		public Nodes() {
			this(new Vector<ANode>());
		}

		/**
		 * Ustvari zaporedje vozlisc abstraktnega sintaksnega drevesa.
		 * 
		 * @param nodes Zaporedje vozlisc abstraktnega sintaksnega drevesa.
		 */
		@SuppressWarnings("unchecked")
		public Nodes(final List<ANode> nodes) {
			super();
			this.nodes = (ANode[]) (new Node[nodes.size()]);
			int index = 0;
			for (final ANode n : nodes)
				this.nodes[index++] = n;
		}

		/**
		 * Vrne seznam z vsemi vozlisci abstraktnega sintaksnega drevesa.
		 * 
		 * @return Seznam z vsemi vozlisci abstraktnega sintaksnega drevesa.
		 */
		public List<ANode> getAll() {
			final LinkedList<ANode> list = new LinkedList<ANode>();
			for (int index = 0; index < nodes.length; index++)
				list.add(nodes[index]);
			return list;
		}

		/**
		 * Vrne vozlisce abstraktnega sintaksnega drevesa na podanem mestu v tem
		 * zaporedju vozlisc abstraktnega sintaksnega drevesa.
		 * 
		 * @param index Mesto v tem zaporedju vozlisc abstraktnega sintaksnega drevesa.
		 * @return Vozlisce abstraktnega sintaksnega drevesa na podanem mestu v tem
		 *         zaporedju vozlisc abstraktnega sintaksnega drevesa.
		 */
		public Node get(final int index) {
			return nodes[index];
		}

		/**
		 * Vrne stevilo vozlisc v tem zaporedju vozlisc abstraktnega sintaksnega
		 * drevesa.
		 * 
		 * @return Stevilo vozlisc v tem zaporedju vozlisc abstraktnega sintaksnega
		 *         drevesa.
		 */
		public int size() {
			return nodes.length;
		}

		// Iterable<ANode>

		@Override
		public void forEach(final Consumer<? super ANode> action) throws NullPointerException {
			for (final ANode n : this)
				action.accept(n);
		}

		@Override
		public Iterator<ANode> iterator() {
			return new NodesIterator();
		}

		// Iterator.

		/**
		 * Iterator (brez operacije brisanja) preko tabele vozlisc.
		 * 
		 * Tabela vozlisc se ne sme spreminjati.
		 */
		private final class NodesIterator implements Iterator<ANode> {

			/** Ustvari nov iterator. */
			private NodesIterator() {
			}

			/** Indeks vozlisca, ki bo vrnjen pb naslednjem klicu. */
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < nodes.length;
			}

			@Override
			public ANode next() throws NoSuchElementException {
				if (index < nodes.length)
					return nodes[index++];
				else
					throw new NoSuchElementException("");
			}

			@Override
			public void remove() {
				throw new Report.InternalError();
			}

			@Override
			public void forEachRemaining(final Consumer<? super ANode> action) {
				while (hasNext())
					action.accept(next());
			}

		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// Definicije:

	/**
	 * Definicija.
	 */
	public static abstract class Def extends Node {

		/** Definirano ime. */
		public final String name;

		/**
		 * Ustvari novo definicijo.
		 * 
		 * @param name Definirano ime.
		 */
		public Def(final String name) {
			super();
			this.name = name;
		}

	}

	/**
	 * Samostojna definicija.
	 */
	public static abstract class MainDef extends Def {

		/**
		 * Ustvari novo samostojno definicijo.
		 * 
		 * @param name Definirano ime.
		 */
		public MainDef(final String name) {
			super(name);
		}

	}

	/**
	 * Definicija funkcije.
	 */
	public static class FunDef extends MainDef {

		/** Parametri funkcije. */
		public final Nodes<ParDef> pars;

		/** Stavki telesa funkcije. */
		public final Nodes<Stmt> stmts;

		/**
		 * Ustvari novo definicijo funkcije.
		 * 
		 * @param name  Ime funkcije.
		 * @param pars  Parametri funkcije.
		 * @param stmts Stavki telesa funkcije.
		 */
		public FunDef(final String name, final List<ParDef> pars, final List<Stmt> stmts) {
			super(name);
			this.pars = new Nodes<ParDef>(pars);
			this.stmts = new Nodes<Stmt>(stmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Definicija parametra.
	 */
	public static class ParDef extends Def {

		/**
		 * Ustvari novo definicijo parametra.
		 * 
		 * @param name Ime parametra.
		 */
		public ParDef(final String name) {
			super(name);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Definicija spremenljivke.
	 */
	public static class VarDef extends MainDef {

		/** Zacetne vrednosti. */
		public final Nodes<Init> inits;

		/**
		 * Ustvari novo definicijo spremenljivke.
		 * 
		 * @param name  Ime spremenljivke.
		 * @param inits Zacetne vrednosti.
		 */
		public VarDef(final String name, final List<Init> inits) {
			super(name);
			this.inits = new Nodes<Init>(inits);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Zacetna vrednost spremenljivke.
	 */
	public static class Init extends Node {

		/** Stevilo ponovitev. */
		public final AtomExpr num;

		/** Vrednost. */
		public final AtomExpr value;

		/**
		 * Ustvari novo zaceetno vrednost spremenljivke.
		 * 
		 * @param num   Stevilo ponovitev.
		 * @param value Vrednost.
		 */
		public Init(final AtomExpr num, final AtomExpr value) {
			super();
			this.num = num;
			this.value = value;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// Stavki:

	/**
	 * Stavek.
	 */
	public static abstract class Stmt extends Node {

		/**
		 * Ustvari nov stavek.
		 */
		public Stmt() {
			super();
		}

	}

	/**
	 * Izraz kot samostojen stavek.
	 */
	public static class ExprStmt extends Stmt {

		/** Izraz. */
		public final Expr expr;

		/**
		 * Ustvari nov stavek, ki je samo izraz.
		 * 
		 * @param expr Izraz.
		 */
		public ExprStmt(final Expr expr) {
			super();
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Prireditveni stavek.
	 */
	public static class AssignStmt extends Stmt {

		/** Izraz, ki doloca naslov, kamor se prireja vrednost. */
		public final Expr dstExpr;

		/** Izraz, ki doloca vrednost, ki se prireja. */
		public final Expr srcExpr;

		/**
		 * Ustvari nov pripreditveni stavek.
		 * 
		 * @param dstExpr Izraz, ki doloca naslov, kamor se prireja vrednost.
		 * @param srcExpr Izraz, ki doloca vrednost, ki se prireja.
		 */
		public AssignStmt(final Expr dstExpr, final Expr srcExpr) {
			super();
			this.dstExpr = dstExpr;
			this.srcExpr = srcExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Pogojni stavek.
	 */
	public static class IfStmt extends Stmt {

		/** Pogoj. */
		public final Expr cond;

		/** Stavki v pozitivni veji. */
		public final Nodes<Stmt> thenStmts;

		/** Stavki v negativni veji. */
		public final Nodes<Stmt> elseStmts;

		/**
		 * Ustvari nov pogojni stavek.
		 * 
		 * @param cond      Pogoj.
		 * @param thenStmts Stavki v pozitivni veji.
		 * @param elseStmts Stavki v negativni veji.
		 */
		public IfStmt(final Expr cond, final List<Stmt> thenStmts, final List<Stmt> elseStmts) {
			super();
			this.cond = cond;
			this.thenStmts = new Nodes<Stmt>(thenStmts);
			this.elseStmts = new Nodes<Stmt>(elseStmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Zanka.
	 */
	public static class WhileStmt extends Stmt {

		/** Pogoj. */
		public final Expr cond;

		/** Stavki telesa zanke. */
		public final Nodes<Stmt> stmts;

		/**
		 * Ustvari novo zanko.
		 * 
		 * @param cond  Pogoj.
		 * @param stmts Stavki telesa zanke.
		 */
		public WhileStmt(final Expr cond, final List<Stmt> stmts) {
			super();
			this.cond = cond;
			this.stmts = new Nodes<Stmt>(stmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Sestavljeni stavek.
	 */
	public static class LetStmt extends Stmt {

		/** Lokalne definicije. */
		public final Nodes<MainDef> defs;

		/** Stavki telesa sestavljenega stavka. */
		public final Nodes<Stmt> stmts;

		/**
		 * Ustvari nov sestavljeni stavek.
		 * 
		 * @param defs  Lokalne definicije.
		 * @param stmts Stavki telesa sestavljenega stavka.
		 */
		public LetStmt(final List<MainDef> defs, final List<Stmt> stmts) {
			super();
			this.defs = new Nodes<MainDef>(defs);
			this.stmts = new Nodes<Stmt>(stmts);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// Izrazi:

	/**
	 * Izraz.
	 */
	public static abstract class Expr extends Node {

		/**
		 * Ustvari nov izraz.
		 */
		public Expr() {
			super();
		}

	}

	/**
	 * Atomarni izraz.
	 */
	public static class AtomExpr extends Expr {

		/**
		 * Tipi atomarnega izraza.
		 */
		public enum Type {
			/** Celostevilska konstanta. */
			INTCONST,
			/** Znakovna konstanta. */
			CHRCONST,
			/** Niz. */
			STRCONST,
		}

		/** Tip atomarnega izraza. */
		public final Type type;

		/** Vrednost. */
		public final String value;

		/**
		 * Ustvari nov atomarni izraz.
		 * 
		 * @param type  Tip atomarnega izraza.
		 * @param value Vrednost.
		 */
		public AtomExpr(final Type type, final String value) {
			super();
			this.type = type;
			this.value = value;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Enomestni izraz.
	 */
	public static class UnExpr extends Expr {

		/** Enomestni operatorji. */
		public enum Oper {
			/** Negacija. */
			NOT,
			/** Pozitivni predznak. */
			ADD,
			/** Negativni predznak. */
			SUB,
			/** Referenciranje (prefiksni ^). */
			MEMADDR,
			/** Dereferenciranje (postfiksni ^). */
			VALUEAT,
		}

		/** Enomestni operator. */
		public final Oper oper;

		/** Podizraz. */
		public final Expr expr;

		/**
		 * Ustvari nov enomestni izraz.
		 * 
		 * @param oper Enomestni operator.
		 * @param expr Podizraz.
		 */
		public UnExpr(final Oper oper, final Expr expr) {
			super();
			this.oper = oper;
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Dvomestni izraz.
	 */
	public static class BinExpr extends Expr {

		/** Dvomestni operatorji. */
		public enum Oper {
			/** Disjunkcija. */
			OR,
			/** Konjunkcija. */
			AND,
			/** Enakonst. */
			EQU,
			/** Neenakost. */
			NEQ,
			/** Vecji kot. */
			GTH,
			/** Manjsi kot. */
			LTH,
			/** Vecji ali enak. */
			GEQ,
			/** Manjsi ali enak. */
			LEQ,
			/** Sestevanje. */
			ADD,
			/** Odstevanje. */
			SUB,
			/** Mnozenje. */
			MUL,
			/** Deljenje. */
			DIV,
			/** Modulo. */
			MOD,
		}

		/** Dvomestni operator. */
		public final Oper oper;

		/** Prvi (levi) podizraz. */
		public final Expr fstExpr;

		/** Drugi (desni) podizraz. */
		public final Expr sndExpr;

		/**
		 * Ustvari nov dvomestni izraz.
		 * 
		 * @param oper    Dvomestni operator.
		 * @param fstExpr Prvi (levi) podizraz.
		 * @param sndExpr Drugi (desni) podizraz.
		 */
		public BinExpr(final Oper oper, final Expr fstExpr, final Expr sndExpr) {
			super();
			this.oper = oper;
			this.fstExpr = fstExpr;
			this.sndExpr = sndExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Uporaba imena v izrazu.
	 */
	public static abstract class NameExpr extends Expr {

		/** Ime. */
		public final String name;

		/**
		 * Ustvari novo ime v izrazu.
		 * 
		 * @param name Ime.
		 */
		public NameExpr(final String name) {
			super();
			this.name = name;
		}

	}

	/**
	 * Dostop do spremenljivke ali parametra.
	 */
	public static class VarExpr extends NameExpr {

		/**
		 * Ustvari nov dostop do spremenljivke ali parametra.
		 * 
		 * @param name Ime spremenljivke ali parametra.
		 */
		public VarExpr(final String name) {
			super(name);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Funkcijski klic.
	 */
	public static class CallExpr extends NameExpr {

		/** Argumenti klica. */
		public final Nodes<Expr> args;

		/**
		 * Ustvari nov funkcijski klic.
		 * 
		 * @param name Ime funkcije.
		 * @param args Argumenti klica.
		 */
		public CallExpr(final String name, final List<Expr> args) {
			super(name);
			this.args = new Nodes<Expr>(args);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// --- ABSTRAKTNO SINTAKSNO DREVO OPREMLJENO Z ATRIBUTI ---

	/**
	 * Abstraktno sintaksno drevo opremljeno z atributi.
	 * 
	 * V tem razredu atributov ni, predvideno je, da so dodani v vsakem podrazredu
	 * tega razreda. Ti podrazredi so definirani v posameznih fazah prevajalnika.
	 */
	public static class AttrAST {

		/** Abstraktno sintaksno drevo. */
		public final AST.Node ast;

		/**
		 * Ustvari novo abstraktno sintaksno drevo opremljeno z atributi.
		 * 
		 * @param ast Abstraktno sintaksno drevo.
		 */
		public AttrAST(AST.Node ast) {
			this.ast = ast;
		}

		/**
		 * Kratek izpis atributov.
		 * 
		 * @param node        Vozlisce abstraktnega sintaksnega drevesa.
		 * @param highlighted Ali mora biti izpis poudarjen.
		 * @return Vrne kratek izpis atributov.
		 */
		public String head(final AST.Node node, final boolean highlighted) {
			return "";
		}

		/**
		 * Dolg izpis atributov.
		 * 
		 * @param indent      Zamik (sorazmeren z globino vozlisca).
		 * @param highlighted Ali mora biti izpis poudarjen.
		 * @param node        Vozlisce abstraktnega sintaksnega drevesa.
		 */
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			return;
		}

	}

	// --- OBISKOVALCI ---

	/**
	 * Obiskovalec abstraktnega sintaksnega drevesa.
	 * 
	 * @param <Result>   Tip rezultata.
	 * @param <Argument> Tip pomoznega argumenta.
	 */
	public static interface Visitor<Result, Argument> {

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final Nodes<? extends Node> nodes, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final FunDef funDef, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final ParDef parDef, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final VarDef varDef, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final Init init, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final ExprStmt exprStmt, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final AssignStmt assignStmt, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final IfStmt ifStmt, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final WhileStmt whileStmt, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final LetStmt letStmt, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final AtomExpr atomExpr, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final UnExpr unExpr, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final BinExpr binExpr, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final VarExpr varExpr, final Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(final CallExpr callExpr, final Argument arg) {
			throw new Report.InternalError();
		}

	}

	/**
	 * Obiskovalec, ki ne naredi obhoda.
	 * 
	 * @param <Result>   Tip rezultata.
	 * @param <Argument> Tip pomoznega argmenta.
	 */
	public static interface NullVisitor<Result, Argument> extends Visitor<Result, Argument> {

		@Override
		public default Result visit(final Nodes<? extends Node> nodes, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final FunDef funDef, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final ParDef parDef, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final VarDef varDef, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final Init init, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final ExprStmt exprStmt, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final AssignStmt assignStmt, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final IfStmt ifStmt, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final WhileStmt whileStmt, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final LetStmt letStmt, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final AtomExpr atomExpr, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final UnExpr unExpr, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final BinExpr binExpr, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final VarExpr varExpr, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final CallExpr callExpr, final Argument arg) {
			return null;
		}

	}

	/**
	 * Obiskovalec, ki naredi poln obhod in obisce vsa vozlisca.
	 * 
	 * @param <Result>   Tip rezultata.
	 * @param <Argument> Tip pomoznega argumenta.
	 */
	public static interface FullVisitor<Result, Argument> extends Visitor<Result, Argument> {

		@Override
		public default Result visit(final Nodes<? extends Node> nodes, final Argument arg) {
			for (final Node node : nodes)
				node.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final FunDef funDef, final Argument arg) {
			funDef.pars.accept(this, arg);
			funDef.stmts.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final ParDef parDef, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final VarDef varDef, final Argument arg) {
			varDef.inits.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final Init init, final Argument arg) {
			init.num.accept(this, arg);
			init.value.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final ExprStmt exprStmt, final Argument arg) {
			exprStmt.expr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final AssignStmt assignStmt, final Argument arg) {
			assignStmt.dstExpr.accept(this, arg);
			assignStmt.srcExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final IfStmt ifStmt, final Argument arg) {
			ifStmt.cond.accept(this, arg);
			ifStmt.thenStmts.accept(this, arg);
			ifStmt.elseStmts.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final WhileStmt whileStmt, final Argument arg) {
			whileStmt.cond.accept(this, arg);
			whileStmt.stmts.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final LetStmt letStmt, final Argument arg) {
			letStmt.defs.accept(this, arg);
			letStmt.stmts.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final AtomExpr atomExpr, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final UnExpr unExpr, final Argument arg) {
			unExpr.expr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final BinExpr binExpr, final Argument arg) {
			binExpr.fstExpr.accept(this, arg);
			binExpr.sndExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(final VarExpr varExpr, final Argument arg) {
			return null;
		}

		@Override
		public default Result visit(final CallExpr callExpr, final Argument arg) {
			callExpr.args.accept(this, arg);
			return null;
		}

	}

	/**
	 * Izpis abstraktnega sintaksnega drevesa.
	 */
	public static class Logger {

		/** Abstraktno sintaksno drevo opremljeno z atributi. */
		private final AttrAST attrAST;

		/**
		 * Ustvari nov objekt za izpis abstraktnega sintaksnega drevesa.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo opremljeno z atributi.
		 */
		public Logger(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi izpis abstraktnega sintaksnega drevesa opremljenega z atributi.
		 */
		public void log() {
			attrAST.ast.accept(new LoggerVisitor(), new LoggerVisitor.Log(0, "Program:"));
		}

		/**
		 * Izpise opis vozlisca abstraktnega sintaksnega drevesa in njegove atribute.
		 * 
		 * @param indent Zamik (sorazmeren z globino vozlisca).
		 * @param node   Vozlisce.
		 * @param text   Opis strukture vozlisca.
		 */
		private void print(final int indent, final AST.Node node, final String text) {
			if (indent > 0)
				System.out.printf("%" + indent + "c", ' ');
			System.out.printf("%s%s\n", text, attrAST.head(node, true));
			attrAST.desc(indent + 4, node, true);
		}

		/**
		 * Obiskovalec za izpis abstraktnega sintaksnega drevesa.
		 */
		private class LoggerVisitor implements AST.FullVisitor<Object, LoggerVisitor.Log> {

			@SuppressWarnings({ "doclint:missing" })
			public LoggerVisitor() {
			}

			/**
			 * Nacin izpisa abstraktnega sintaksnega drevesa.
			 * 
			 * @param indent    Zamik (sorazmeren z globino vozlisca).
			 * @param groupName Ime zaporedja vozlisc.
			 */
			private record Log(int indent, String groupName) {

				/**
				 * Povecanje zamika izpisa izpisa abstraktnega sintaksnega drevesa.
				 * 
				 * @return Zamaknjen nacin izpisa abstraktnega sintaksnega drevesa.
				 */
				public Log advance() {
					return advance("");
				}

				/**
				 * Povecanje zamika izpisa izpisa abstraktnega sintaksnega drevesa.
				 * 
				 * @param groupName Ime zaporedja vozlisc.
				 * @return Zamaknjen nacin izpisa abstraktnega sintaksnega drevesa.
				 */
				public Log advance(final String groupName) {
					return new Log(indent + 2, groupName);
				}

			}

			@Override
			public Object visit(final AST.Nodes<? extends AST.Node> nodes, final Log log) {
				print(log.indent, nodes, "\033[1m" + log.groupName + "\033[0m");
				{
					for (final AST.Node node : nodes)
						node.accept(this, log.advance());
				}
				return null;
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Log log) {
				print(log.indent, funDef, "\033[1m" + "FunDef " + funDef.name + "\033[0m");
				{
					funDef.pars.accept(this, log.advance("Pars:"));
					funDef.stmts.accept(this, log.advance("Stmts:"));
				}
				return null;
			}

			@Override
			public Object visit(final AST.ParDef parDef, final Log log) {
				print(log.indent, parDef, "\033[1m" + "ParDef " + parDef.name + "\033[0m");
				{
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Log log) {
				print(log.indent, varDef, "\033[1m" + "VarDef " + varDef.name + "\033[0m");
				{
					varDef.inits.accept(this, log.advance("Inits:"));
				}
				return null;
			}

			@Override
			public Object visit(final AST.Init init, final Log log) {
				print(log.indent, init, "\033[1m" + "Init " + init.num.value + "*" + "\033[0m");
				{
					init.value.accept(this, log.advance());
				}
				return null;
			}

			@Override
			public Object visit(final AST.ExprStmt exprStmt, final Log log) {
				print(log.indent, exprStmt, "\033[1m" + "ExprStmt" + "\033[0m");
				{
					exprStmt.expr.accept(this, log.advance());
				}
				return null;
			}

			@Override
			public Object visit(final AST.AssignStmt assignStmt, final Log log) {
				print(log.indent, assignStmt, "\033[1m" + "AssignStmt" + "\033[0m");
				{
					assignStmt.dstExpr.accept(this, log.advance());
					assignStmt.srcExpr.accept(this, log.advance());
				}
				return null;
			}

			@Override
			public Object visit(final AST.IfStmt ifStmt, final Log log) {
				print(log.indent, ifStmt, "\033[1m" + "IfStmt" + "\033[0m");
				{
					ifStmt.cond.accept(this, log.advance());
					ifStmt.thenStmts.accept(this, log.advance("IfThenStmts:"));
					ifStmt.elseStmts.accept(this, log.advance("IfElseStmts:"));
				}
				return null;
			}

			@Override
			public Object visit(final AST.WhileStmt whileStmt, final Log log) {
				print(log.indent, whileStmt, "\033[1m" + "WhileStmt" + "\033[0m");
				{
					whileStmt.cond.accept(this, log.advance());
					whileStmt.stmts.accept(this, log.advance("WhileStmts:"));
				}
				return null;
			}

			@Override
			public Object visit(final AST.LetStmt letStmt, final Log log) {
				print(log.indent, letStmt, "\033[1m" + "LetStmt" + "\033[0m");
				{
					letStmt.defs.accept(this, log.advance("LetDefs:"));
					letStmt.stmts.accept(this, log.advance("LetStmts:"));
				}
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Log log) {
				print(log.indent, atomExpr,
						"\033[1m" + "AtomExpr " + atomExpr.type + "(" + atomExpr.value + ")" + "\033[0m");
				{
				}
				return null;
			}

			@Override
			public Object visit(final AST.UnExpr unExpr, final Log log) {
				print(log.indent, unExpr, "\033[1m" + "UnExpr " + unExpr.oper + "\033[0m");
				{
					unExpr.expr.accept(this, log.advance());
				}
				return null;
			}

			@Override
			public Object visit(final AST.BinExpr binExpr, final Log log) {
				print(log.indent, binExpr, "\033[1m" + "BinExpr " + binExpr.oper + "\033[0m");
				{
					binExpr.fstExpr.accept(this, log.advance());
					binExpr.sndExpr.accept(this, log.advance());
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarExpr varExpr, final Log log) {
				print(log.indent, varExpr, "\033[1m" + "NameExpr " + varExpr.name + "\033[0m");
				{
				}
				return null;
			}

			@Override
			public Object visit(final AST.CallExpr callExpr, final Log log) {
				print(log.indent, callExpr, "\033[1m" + "CallExpr " + callExpr.name + "\033[0m");
				{
					callExpr.args.accept(this, log.advance("Args:"));
				}
				return null;
			}

		}

	}

}
