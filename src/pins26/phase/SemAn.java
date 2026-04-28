package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Semanticni analizator.
 */
public class SemAn {

  @SuppressWarnings({ "doclint:missing" })
  public SemAn() {
    throw new Report.InternalError();
  }

  /**
   * Abstraktno sintaksno drevo z dodanimi atributi semanticne analize.
   * 
   * Dodani atributi:
   * <ol>
   * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
   * <li>({@link SemAn}) definicija uporabljenega imena;</li>
   * <li>({@link SemAn}) ali je dani izraz levi izraz.</li>
   * </ol>
   */
  public static class AttrAST extends Abstr.AttrAST {

    /** Atribut: definicija uporabljenega imena. */
    public final Map<AST.NameExpr, AST.Def> attrDef;

    /** Atribut: ali je dani izraz levi izraz. */
    public final Map<AST.Expr, Boolean> attrLVal;

    /**
     * Ustvari novo abstraktno sintaksno drevo z dodanim atributi semanticne
     * analize.
     * 
     * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi abstraktne
     *                 sintakse.
     * @param attrDef  Atribut: definicija uporabljenega imena.
     * @param attrLVal Atribut: ali je dani izraz levi izraz.
     */
    public AttrAST(final Abstr.AttrAST attrAST, final Map<AST.NameExpr, AST.Def> attrDef,
        final Map<AST.Expr, Boolean> attrLVal) {
      super(attrAST);
      this.attrDef = attrDef;
      this.attrLVal = attrLVal;
    }

    /**
     * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi semanticne
     * analize.
     * 
     * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi semanticne
     *                analize.
     */
    public AttrAST(final AttrAST attrAST) {
      super(attrAST);
      this.attrDef = attrAST.attrDef;
      this.attrLVal = attrAST.attrLVal;
    }

    @Override
    public String head(final AST.Node node, final boolean highlighted) {
      final StringBuffer head = new StringBuffer();
      head.append(super.head(node, false));
      switch (node) {
        case final AST.NameExpr nameExpr:
          final AST.Def def = attrDef.get(nameExpr);
          if (def == null)
            break;
          final Report.Locatable loc = attrLoc.get(def);
          if (loc == null)
            break;
          head.append((" ") + (highlighted ? "\033[31m" : "") + "def@" + loc.location().toString()
              + (highlighted ? "\033[30m" : ""));
          break;
        default:
          break;
      }
      switch (node) {
        case final AST.Expr expr:
          final Boolean lval = attrLVal.get(expr);
          if (lval == null)
            break;
          if (lval)
            head.append((" ") + (highlighted ? "\033[31m" : "") + "lval" + (highlighted ? "\033[30m" : ""));
          break;
        default:
          break;
      }
      return head.toString();
    }

  }

  /**
   * Opravi semanticno analizo.
   * 
   * @param abstrAttrAST Abstraktno sintaksno drevo z dodanimi atributi abstraktne
   *                     sintakse.
   * @return Abstraktno sintaksno drevo z dodanimi atributi semanticne analize.
   */
  public static AttrAST analyze(Abstr.AttrAST abstrAttrAST) {
    AttrAST attrAST = new AttrAST(abstrAttrAST, new HashMap<AST.NameExpr, AST.Def>(),
        new HashMap<AST.Expr, Boolean>());
    attrAST = new NameResolver(attrAST).resolve();
    attrAST = new TypeResolver(attrAST).resolve();
    attrAST = new LValResolver(attrAST).resolve();
    return attrAST;
  }

  /**
   * Razresevanje imen.
   */
  private static class NameResolver {

    /** Abstraktno sintaksno drevo z dodanimi atributi semanticne analize. */
    private final AttrAST attrAST;

    /**
     * Ustvari nov razresevalnik imen.
     * 
     * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi semanticne
     *                analize.
     */
    public NameResolver(final AttrAST attrAST) {
      this.attrAST = attrAST;
    }

    /**
     * Sprozi razresevanje imen.
     * 
     * @return Abstraktno sintaksno drevo z dodanimi atributi semanticne analize
     *         ({@link AttrAST#attrDef} izracunan in nespremenljiv).
     */
    public AttrAST resolve() {
      attrAST.ast.accept(new ResolverVisitor(), null);
      return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrDef), attrAST.attrLVal);
    }

    /** Simbolna tabela, ki se uporablja med razresevanjem imen. */
    private final SymbolTable symbolTable = new SymbolTable();

    /** Simbolna tabela. */
    private class SymbolTable {

      /**
       * Definicija v trenutnem dosega na dani staticni globini.
       * 
       * @param depth Staticna globina definicije.
       * @param def   Definicija.
       */
      private record ScopedDef(int depth, AST.Def def) {
      }

      /**
       * Preslikava imena v seznam definicij tega imena na razlicnih staticnih
       * globinah.
       */
      private final HashMap<String, LinkedList<ScopedDef>> namesToDefs;

      /** Seznami imen definiranih na posameznih staticnih globinah. */
      private final LinkedList<LinkedList<String>> namesToDefsByDepth;

      /** Trenutna staticna globina. */
      private int depth;

      /**
       * Ustvari novo simbolno tabelo.
       */
      public SymbolTable() {
        namesToDefs = new HashMap<String, LinkedList<ScopedDef>>();
        namesToDefsByDepth = new LinkedList<LinkedList<String>>();
        depth = -1;
        newScope();
      }

      /** Pripravi simbolno tabelo za vstavljanje definicij imen v novem dosegu. */
      public void newScope() {
        depth++;
        namesToDefsByDepth.addFirst(new LinkedList<String>());
      }

      /** Razveljavi trenutni doseg. */
      public void oldScope() {
        for (final String name : namesToDefsByDepth.getFirst()) {
          final LinkedList<ScopedDef> defsOfName = namesToDefs.get(name);
          if (defsOfName.size() == 1)
            namesToDefs.remove(name);
          else
            defsOfName.removeFirst();
        }
        namesToDefsByDepth.removeFirst();
        depth--;
      }

      /**
       * Vstavi novo definicijo imena v trenutni doseg.
       * 
       * @param def Definicija imena.
       * @return {@code true}, ce je vstavitev mozna (pred to vstavitvijo v tem dosegu
       *         se ni definicije tega imena), ali {@code false}, ce vstavitev ni
       *         mozna (pred to vstavitvijo je v tem dosegu ze definicija tega imena).
       */
      public boolean ins(final AST.Def def) {
        final LinkedList<ScopedDef> defsOfOldName = namesToDefs.get(def.name);
        if (defsOfOldName == null) {
          final LinkedList<ScopedDef> defsOfNewName = new LinkedList<ScopedDef>();
          defsOfNewName.addFirst(new ScopedDef(depth, def));
          namesToDefs.put(def.name, defsOfNewName);
          namesToDefsByDepth.getFirst().add(def.name);
          return true;
        } else {
          if (defsOfOldName.getFirst().depth == depth)
            return false;
          defsOfOldName.addFirst(new ScopedDef(depth, def));
          namesToDefsByDepth.getFirst().add(def.name);
          return true;
        }
      }

      /**
       * Vrne definicijo imena.
       * 
       * @param name Ime.
       * @return Definicija imena ali {@code null}, ce ime ni definirano v tem in
       *         obsegajocih dosegih.
       */
      public AST.Def fnd(final String name) {
        final LinkedList<ScopedDef> defsOfName = namesToDefs.get(name);
        return (defsOfName == null) ? null : defsOfName.getFirst().def();
      }

    }

    /**
     * Obiskovalec za razresevanje imen.
     */
    private class ResolverVisitor implements AST.FullVisitor<Object, ResolverVisitor.Pass> {

      @SuppressWarnings({ "doclint:missing" })
      public ResolverVisitor() {
      }

      /**
       * Dva preleta abstraktnega sintaksnega drevesa med razresevanjem imen.
       * 
       * Med prvim preletom se obdelajo definicije funkcij in spremenljivk (ne pa tudi
       * telesa funkcij), med drugim preletom se obdela vse ostalo (tudi telesa
       * funkcij). Oba preleta se prepletata in se razcepita le pri obdelavi zaporedja
       * vozlisc.
       */
      private enum Pass {
        /** Prelet definicij funkcij in spremenljivk. */
        Defs,
        /** Prelet vsega razen definicij funkcij in spremenljivk. */
        Rest,
      }

      @Override
      public Object visit(final AST.Nodes<? extends AST.Node> nodes, final Pass pass) {
        for (final AST.Node node : nodes) {
          switch (node) {
            case final AST.FunDef funDef:
              funDef.accept(this, Pass.Defs);
              break;
            case final AST.VarDef varDef:
              varDef.accept(this, Pass.Defs);
              break;
            default:
              break;
          }
        }
        for (final AST.Node node : nodes) {
          switch (node) {
            case final AST.FunDef funDef:
              funDef.accept(this, Pass.Rest);
              break;
            case final AST.VarDef varDef:
              varDef.accept(this, Pass.Rest);
              break;
            default:
              node.accept(this, null);
              break;
          }
        }
        return null;
      }

      @Override
      public Object visit(final AST.FunDef funDef, final Pass pass) {
        switch (pass) {
          case Defs: {
            if (!symbolTable.ins(funDef))
              throw new Report.Error(attrAST.attrLoc.get(funDef),
                  "Illegal definition of function '" + funDef.name + "'.");
            break;
          }
          case Rest: {
            symbolTable.newScope();
            funDef.pars.accept(this, null);
            funDef.stmts.accept(this, null);
            symbolTable.oldScope();
            break;
          }
          default:
            throw new Report.InternalError();
        }
        return null;
      }

      @Override
      public Object visit(final AST.ParDef parDef, final Pass pass) {
        if (!symbolTable.ins(parDef))
          throw new Report.Error(attrAST.attrLoc.get(parDef),
              "Illegal definition of parameter '" + parDef.name + "'.");
        return null;
      }

      @Override
      public Object visit(final AST.VarDef varDef, final Pass pass) {
        switch (pass) {
          case Defs: {
            if (!symbolTable.ins(varDef))
              throw new Report.Error(attrAST.attrLoc.get(varDef),
                  "Illegal definition of variable '" + varDef.name + "'.");
            varDef.inits.accept(this, null);
            break;
          }
          case Rest: {
            break;
          }
          default:
            throw new Report.InternalError();
        }
        return null;
      }

      @Override
      public Object visit(final AST.LetStmt letStmt, final Pass pass) {
        symbolTable.newScope();
        letStmt.defs.accept(this, null);
        letStmt.stmts.accept(this, null);
        symbolTable.oldScope();
        return null;
      }

      @Override
      public Object visit(final AST.VarExpr varExpr, final Pass pass) {
        final AST.Def def = symbolTable.fnd(varExpr.name);
        if (def == null)
          throw new Report.Error(attrAST.attrLoc.get(varExpr), "Undefined name '" + varExpr.name + "'.");
        attrAST.attrDef.put(varExpr, def);
        return null;
      }

      @Override
      public Object visit(final AST.CallExpr callExpr, final Pass pass) {
        final AST.Def def = symbolTable.fnd(callExpr.name);
        if (def == null)
          throw new Report.Error(attrAST.attrLoc.get(callExpr), "Undefined name '" + callExpr.name + "'.");
        attrAST.attrDef.put(callExpr, def);
        callExpr.args.accept(this, null);
        return null;
      }

    }

  }

  /**
   * Preverjanje tipov.
   */
  private static class TypeResolver {

    /** Abstraktno sintaksno drevo z dodanimi atributi semanticne analize. */
    private final AttrAST attrAST;

    /**
     * Ustvari nov razresevalnik imen.
     * 
     * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi semanticne
     *                analize
     */
    public TypeResolver(final AttrAST attrAST) {
      this.attrAST = attrAST;
    }

    /**
     * Sprozi razresevanje imen.
     * 
     * @return Abstraktno sintaksno drevo z dodanimi atributi semanticne analize.
     */
    public AttrAST resolve() {
      attrAST.ast.accept(new ResolverVisitor(), null);
      return new AttrAST(attrAST, attrAST.attrDef, attrAST.attrLVal);
    }

    /**
     * Obiskovalec za preverjanje tipov.
     */
    private class ResolverVisitor implements AST.FullVisitor<Object, ResolverVisitor.Pass> {

      @SuppressWarnings({ "doclint:missing" })
      public ResolverVisitor() {
      }

      /**
       * Dva preleta abstraktnega sintaksnega drevesa med razresevanjem imen.
       * 
       * Med prvim preletom se obdelajo definicije funkcij in spremenljivk (ne pa tudi
       * telesa funkcij), med drugim preletom se obdela vse ostalo (tudi telesa
       * funkcij). Oba preleta se prepletata in se razcepita le pri obdelavi zaporedja
       * vozlisc.
       */
      private enum Pass {
        /** Prelet definicij funkcij in spremenljivk. */
        Defs,
        /** Prelet vsega razen definicij funkcij in spremenljivk. */
        Rest,
      }

      @Override
      public Object visit(final AST.FunDef funDef, final Pass pass) {
        funDef.pars.accept(this, pass);
        funDef.stmts.accept(this, pass);
        if (funDef.stmts.size() != 0) {
          AST.Stmt lastStmt = funDef.stmts.getAll().getLast();
          loop: while (true) {
            switch (lastStmt) {
              case AST.ExprStmt exprStmt:
                break loop;
              case AST.LetStmt letStmt:
                if (letStmt.stmts.size() != 0) {
                  lastStmt = letStmt.stmts.getAll().getLast();
                } else
                  throw new Report.Error(attrAST.attrLoc.get(funDef),
                      "Function '" + funDef.name + "' does not return any value.");
                break;
              default:
                throw new Report.Error(attrAST.attrLoc.get(funDef),
                    "Function '" + funDef.name + "' does not return any value.");
            }
          }
        }
        return null;
      }

      @Override
      public Object visit(final AST.VarExpr varExpr, final Pass pass) {
        switch (attrAST.attrDef.get(varExpr)) {
          case final AST.VarDef varDef:
            break;
          case final AST.ParDef parDef:
            break;
          default:
            throw new Report.Error(attrAST.attrLoc.get(varExpr),
                "'" + varExpr.name + "' is not a variable or a parameter.");
        }
        return null;
      }

      @Override
      public Object visit(final AST.CallExpr callExpr, final Pass pass) {
        switch (attrAST.attrDef.get(callExpr)) {
          case final AST.FunDef funDef: {
            if (funDef.pars.size() != callExpr.args.size())
              throw new Report.Error(attrAST.attrLoc.get(callExpr),
                  "Illegal number of arguments in a call of function '" + callExpr.name + "'.");
            break;
          }
          default:
            throw new Report.Error(attrAST.attrLoc.get(callExpr), "'" + callExpr.name + "' is not a function.");
        }
        callExpr.args.accept(this, null);
        return null;
      }

    }

  }

  /**
   * Preverjanje levih vrednosti.
   */
  private static class LValResolver {

    /** Abstraktno sintaksno drevo z dodanimi atributi semanticne analize. */
    private final AttrAST attrAST;

    /**
     * Ustvari nov razresevalnik levih vrednosti.
     * 
     * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi semanticne
     *                analize.
     */
    public LValResolver(final AttrAST attrAST) {
      this.attrAST = attrAST;
    }

    /**
     * Sprozi preverjanje levih vrednosti.
     *
     * @return Abstraktno sintaksno drevo z dodanimi atributi semanticne analize
     *         ({@link AttrAST#attrLVal} izracunan in nespremenljiv).
     */
    public AttrAST resolve() {
      attrAST.ast.accept(new ResolverVisitor(), null);
      return new AttrAST(attrAST, attrAST.attrDef, Collections.unmodifiableMap(attrAST.attrLVal));
    }

    /**
     * Obiskovalec za preverjanje levih vrednosti.
     */
    private class ResolverVisitor implements AST.FullVisitor<Object, Object> {

      @Override
      public Object visit(AST.AssignStmt assignStmt, Object arg) {
        assignStmt.dstExpr.accept(this, arg);
        assignStmt.srcExpr.accept(this, arg);
        final AST.Expr left = assignStmt.dstExpr;
        if (left instanceof AST.VarExpr
            || (left instanceof AST.UnExpr unExpr && unExpr.oper == AST.UnExpr.Oper.VALUEAT)) {
          attrAST.attrLVal.put(left, true);
          return null;
        }
        throw new Report.Error(attrAST.attrLoc.get(assignStmt.dstExpr), "Not a valid address for assignment.");
      }
    }
  }

  // --- ZAGON ---

  /**
   * Zagon semanticne analize kot samostojnega programa.
   * 
   * @param cmdLineArgs Argumenti v ukazni vrstici.
   */
  public static void main(final String[] cmdLineArgs) {
    System.out.println("This is PINS'26 compiler (semantic analysis):");

    try {
      if (cmdLineArgs.length == 0)
        throw new Report.Error("No source file specified in the command line.");
      if (cmdLineArgs.length > 1)
        Report.warning("Unused arguments in the command line.");

      try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
        // abstraktna sintaksa:
        final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
        // semanticna analiza:
        final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);

        (new AST.Logger(semanAttrAST)).log();
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
