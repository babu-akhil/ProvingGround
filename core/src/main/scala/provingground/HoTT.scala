package provingground

import scala.util.Try
//import scala.language.existentials
import Math._
import HoTT._
//

/**
  * Core of Homotopy Type Theory (HoTT) implementation.
  * Includes:
  * -   terms : [[Term]],
  * -   types : [[Typ]]
  * -   universes
  * -   functions and dependent functions (see [FuncLike], [Func])
  * -   function types [[FuncTyp]] and pi-types [[PiDefn]],
  * -   lambda definitions [[LambdaLike]],
  * -   pairs [[PairTerm]] and dependent pairs [[DepPair]]
  * -   product types [[ProdTyp]] and sigma types [[SigmaTyp]]
  * -   Coproduct types [[PlusTyp]], the Unit type [[Unit]] and the empty type [[Zero]]
  * -   recursion and induction functions for products, coproducts
  *
  * General inductive types are ``not`` implemented here, but in the [[induction]] package.
  *
  */
object HoTT {

  /**
    * Symbol - may be a name or a formal expression
    */
  trait AnySym {
    def subs(x: Term, y: Term): AnySym
  }

  /**
    * A constant symbol, so a name.
    */
  class AtomicSym extends AnySym {
    def subs(x: Term, y: Term): AtomicSym = this
  }

  /**
    * Strings as symbols
    */
  case class Name(name: String) extends AtomicSym {
    override def toString: String = name.toString
  }

  object Name {
    def nameFromSym(sym: AnySym) : Option[String] = sym match {
      case Name(name) => Some(name)
      case LeftProjSym(name) => nameFromSym(name)
      case RightProjSym(name) => nameFromSym(name)
      case _ => None
    }

    def getName(t: Term): Option[String] = t match {
      case sym: Symbolic =>
        nameFromSym(sym.name)
      case PairTerm(first : Term, _) => getName(first)
      case DepPair(first: Term, second, fibers) => getName(first)
      case _ => None
    }
  }

  object NamedTerm {
    def unapply(arg: Term): Option[String] =
      arg match {
        case sym: Symbolic =>
          sym.name match {
            case Name(name) => Some(name)
            case _          => None
          }
        case _ => None
      }
  }

  trait NameFactory {
    def get: Name
  }

  def getName(n: Int): String =
    if (n < 26) ('a' + n).toChar.toString
    else {
      val last = ('a' + (n % 26)).toChar.toString
      getName(n / 26 - 1) + last
    }

  def nameCard(s: String): Int =
    if (s.length == 1) s.head - 'a'
    else
      (s.head - 'a' + 1) * math.pow(26, (s.length - 1)).toInt + nameCard(s.tail)

  def nextVar(
      typ: Typ[Term],
      vars: Vector[Term],
      prefix: String = "@"
  ): Term = {
    val cards: Vector[Int] = for {
      t <- vars
      if t.typ == typ
      n <- Name.getName(t)
    } yield nameCard(n.tail)
    val name = prefix + getName((cards :+ (-1)).max + 1)
    name :: typ
  }

  /**
    * factory for variable names
    */
  implicit object DefaultNameFactory extends NameFactory {
    var name: String = ""

    def get: Name = {
      val newname = nextName(name)

      name = newname

      Name("$" + newname)
    }
  }

  /**
    * use strings as symbols.
    */
  // implicit def stringSym(name: String) = Name(name)

  import scala.language.existentials

  /**
    * A HoTT term.
    */
  trait Term extends Subs[Term] { self =>

    /**
      * the HoTT-type of the term
      */
    val typ: Typ[U] forSome { type U >: (self.type) <: Term with Subs[U] }

    /**
      * returns whether `this` depends on `that`
      */
    def dependsOn(that: Term): Boolean = Try{
      val newVar = that.newobj
      replace(that, newVar) != this
    }.getOrElse(false)

    /**
      * returns whether `this` is independent of `that`.
      */
    def indepOf(that: Term): Boolean = !dependsOn(that)

    /**
      * returns whether the variable `t` is used as a variable in a lambda
      * definition.
      */
    def usesVar(t: Term): Boolean = false // override in Lambda's
  }

  object Subs {
    var hook: (Term, Term, Term) => Unit = { case (_, _, _) => () }

    var doneHook: (Term, Term, Term, Term) => Unit = { case (_, _, _, _) => () }
  }

  /**
    * specify result of substitution
    * a typical class is closed under substitution.
    */
  trait Subs[+U <: Term] { self =>

    /**
      *  substitute x by y recursively in `this`.
      */
    def subs(x: Term, y: Term): U with Subs[U]

    /**
      * refine substitution so if x and y are both of certain forms such as
      * pairs or formal applications,
      * components are substituted.
      *
      */
    def replace(x: Term, y: Term): U with Subs[U] = {
      Subs.hook(self.asInstanceOf[U], x, y)

      val res =
        if (isWitness(x) || x == y) self.asInstanceOf[U with Subs[U]]
        else if (self == x) Try(y.asInstanceOf[U with Subs[U]]).getOrElse(subs(x, y))
        else
          (x, y) match {
            case (ab: AbsPair[u, v], cd: AbsPair[w, x])
                if (ab.first indepOf ab.second) && (ab.second indepOf ab.first) =>
              replace(ab.first, cd.first) replace (ab.second, cd.second)
            case (ab: DepPair[u, v], cd: AbsPair[w, x]) if ab.indepComponents =>
              replace(ab.first, cd.first) replace (ab.second, cd.second)
            case (FormalAppln(f, a), FormalAppln(g, b)) =>
              replace(f, g) replace (a, b)
            case (MiscAppln(f, a), MiscAppln(g, b)) =>
              replace(f, g) replace (a, b)
            case (xs: Symbolic, _)
                if (x.typ != y.typ) && (y.typ).symbObj(xs.name).typ == y.typ =>
              val typchange = replace(x.typ, y.typ)
//             if (self != typchange) {  pprint.log(self, height = 300)
//              pprint.log(typchange, height = 300)}
              typchange replace ((y.typ).symbObj(xs.name), y)
            case (FuncTyp(a, b), FuncTyp(c, d)) =>
              replace(a, c) replace (b, d)
            case (PiDefn(a: Term, b), PiDefn(c: Term, d)) =>
              replace(a, c) replace (b, d)
            case (PiTyp(fib1), PiTyp(fib2)) =>
              replace(fib1, fib2)
            case _ => subs(x, y)
          }

      Subs.doneHook(self.asInstanceOf[U], x, y, res)
      res
    }

    /**
      * A new object with the same type,
      * to be used in place of a variable to avoid name clashes.
      * Should throw exception when invoked for constants.
      */
    def newobj: U with Subs[U]

  }

  case class AvoidVarFormalException(func: Term, arg: Term, variable: Term)
      extends Exception(
        s"Avoiding variable fails func: $func, arg: $arg, var: $variable"
      )

  /**
    * returns x after modifying to avoid clashes of variables
    */
  def avoidVar[U <: Term with Subs[U]](t: Term, x: U): U = {
    x match {
      case ll: LambdaFixed[u, v] =>
        if (t == ll.variable) {
          val newvar = ll.variable.newobj
          LambdaFixed(
            newvar,
            avoidVar(t, ll.value.replace(ll.variable, newvar))
          ).asInstanceOf[U]
        } else LambdaFixed(ll.variable, avoidVar(t, ll.value)).asInstanceOf[U]
      case ll: LambdaLike[u, v] =>
        if (t == ll.variable) {
          val newvar = ll.variable.newobj
          LambdaTerm(newvar, avoidVar(t, ll.value.replace(ll.variable, newvar)))
            .asInstanceOf[U]
        } else LambdaTerm(ll.variable, avoidVar(t, ll.value)).asInstanceOf[U]
      case FormalAppln(func, arg) =>
//        val res =
        applyFunc(avoidVar(t, func), avoidVar(t, arg)).asInstanceOf[U]
//        if (res == x) res else throw AvoidVarFormalException(func, arg, t)
      case pt: PairTerm[u, v] =>
        PairTerm(avoidVar(t, pt.first), avoidVar(t, pt.second)).asInstanceOf[U]
      case pt: ProdTyp[u, v] =>
        ProdTyp(avoidVar(t, pt.first), avoidVar(t, pt.second)).asInstanceOf[U]
      case pt: FuncTyp[u, v] =>
        FuncTyp(avoidVar(t, pt.dom), avoidVar(t, pt.codom)).asInstanceOf[U]
      case pt: PiDefn[u, v] =>
        PiDefn(avoidVar(t, pt.variable), avoidVar(t, pt.value)).asInstanceOf[U]
      case pt: SigmaTyp[u, v] =>
        SigmaTyp(avoidVar(t, pt.fibers)).asInstanceOf[U]

      case fn: RecFunc[u, v] =>
        val replacements =
          fn.defnData.map { (d) =>
            avoidVar(t, d)
          }
        fn.fromData(replacements)
          .asInstanceOf[U] // .ensuring (_ == fn, s"avoiding var $t failed for recursive function $fn")
      case fn: InducFuncLike[u, v] =>
        val replacements =
          fn.defnData.map { (d) =>
            avoidVar(t, d)
          }
        fn.fromData(replacements)
          .asInstanceOf[U] //.ensuring (_ == fn, s"avoiding var $t failed for inductive function $fn")
      case _ => x
    }
  } ensuring (_ == x, s"avoiding var $t failed for $x")

  /**
    * Objects with simple substitution.
    */
  trait AtomicTerm extends Term with Subs[AtomicTerm] {
    def subs(x: Term, y: Term): AtomicTerm =
      if (x == this) Try(y.asInstanceOf[AtomicTerm]).getOrElse(this) else this

    def newobj: AtomicTerm = typ.obj.asInstanceOf[AtomicTerm]
  }

  /**
    * Term invariant under substitution.
    * Creating a new object from this is not valid.
    */
  trait ConstantTerm extends Term {
    def subs(x: Term, y: Term): ConstantTerm = this

    def newobj =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )
  }

  /**
    * Term invariant under substitution.
    * Creating a new object from this is not valid.
    */
  trait ConstantTyp extends Typ[Term] {
    type Obj = Term

    def subs(x: Term, y: Term): ConstantTyp = this

    def newobj =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    def variable(name: AnySym) = SymbObj(name, this)

    val typ: Universe = Type
  }

  /**
    * HoTT Type;
    *  The compiler knows that objects have scala-type extending U.
    *  In particular, we can specify that the objects are types, functions, dependent functions etc.
    *
    *  @tparam U bound on scala type of objects with `this` as type.
    * in practice, it is symbolic objects of the type whose scala type is bounded.
    */
  trait Typ[+U <: Term with Subs[U]] extends Term with Subs[Typ[U]] { self =>

    /** scala type of objects with this HoTT-type (refining `U`) */
    type Obj <: U with Subs[Obj]

    /**
      * factory for producing objects of the given type.
      * can use {{innervar}} if one wants name unchanged.
      */
    def obj: U = {
      object newname extends AtomicSym
      symbObj(newname)
    }

    /**
      * Pattern for element of the given type.
      */
    object Elem {
      def apply(u: Term): Term = u !: self

      def unapply(term: Term): Option[U] =
        if (term.typ == self) Try(Some(term.asInstanceOf[U])).getOrElse(None)
        else None
    }

    /**
      * checks term is of this type and returns it; useful for documentation.
      */
    def !:(term: Term): U = {
      require(
        term.typ == this,
        " Expected " + toString + "but found " + term.typ.toString
      )
      term.asInstanceOf[U]
    }

    /**
      * type of a type is a universe.
      */
    val typ: Univ

    lazy val typlevel: Int = univlevel(typ)

    /** A symbolic object with this HoTT type, and with scala-type Obj*/
    def variable(name: AnySym): Obj with Subs[Obj]

    /** A symbolic object with this HoTT type, and with scala-type Obj*/
    def symbObj(name: AnySym): U with Subs[U] = variable(name)

    /** symbolic object with given name*/
    def ::(name: String): U = symbObj(Name(name))

    /** shortcut for symbolic object */
    def sym(implicit name: sourcecode.Name): U = ::(name.value)

    /**
      * new variable from a factory.
      */
    def Var(implicit factory: NameFactory): U = getVar[U](this)

    /**
      * function type:  `this -> that`
      */
    def ->:[W <: Term with Subs[W], UU >: U <: Term with Subs[UU]](
        that: Typ[W]
    ): FuncTyp[W, UU] = FuncTyp[W, UU](that, this)

    /**
      * dependent function type (Pi-Type) define by a lambda:
      *  `this` depends on the `variable`, which hence gives a type family;
      * note that a new variable is created and substituted in `this`
      * to avoid name clashes.
      */
    def ~>:[UU >: U <: Term with Subs[UU], V <: Term with Subs[V]](
        variable: V
    ): GenFuncTyp[V, UU] = {
      piDefn(variable)(this: Typ[UU])
    }

    /**
      * returns product type, mainly to use for "and" for structures
      */
    def &&[UU >: U <: Term with Subs[UU], V <: Term with Subs[V]](
        that: Typ[V]
    ): ProdTyp[UU, V] = ProdTyp[UU, V](this, that)

    /**
      * returns coproduct type, mainly to use for "or".
      */
    def ||[UU >: U <: Term with Subs[UU], V <: Term with Subs[V]](
        that: Typ[V]
    ): PlusTyp[UU, V] = PlusTyp(this: Typ[UU], that)

    /**
      * returns Sigma-Type, mainly to use as "such that",
      * for example a group type is this with product etc. dependent on this.
      */
    def ++[UU >: Typ[U] <: Typ[Term] with Subs[UU], VV <: Term with Subs[VV], V <: Typ[
      VV
    ] with Subs[V]](those: V): SigmaTyp[UU, VV] =
      SigmaTyp[UU, VV](LambdaFixed[UU, V](this, those))

    def &:[UU >: U <: Term with Subs[UU], V <: Term with Subs[V]](
        variable: V
    ): SigmaTyp[V, UU] = {
      SigmaTyp(variable :-> this)
    }
  }

  /**
    * Symbols for printing, allowing choice between e.g. unicode and plain text
    */
  trait TermSyms {
    val Arrow: String
    val MapsTo: String
    val Pi: String
    val Sigma: String
    val UnivSym: String
    val Prod: String
  }

  /**
    * plain text symbols for maps etc.
    */
  object SimpleSyms extends TermSyms {
    val Arrow   = "->"
    val MapsTo  = ":->"
    val Pi      = "Pi"
    val Sigma   = "Sigma"
    val UnivSym = "_"
    val Prod    = "x"
  }

  /**
    * unicode symbols for maps etc.
    */
  object UnicodeSyms extends TermSyms {
    val Arrow: String = '\u2192'.toString
    val MapsTo        = "\u21A6"
    val Pi            = "\u220f"
    val Sigma         = "\u2211"
    val UnivSym       = "\uD835\uDCB0 "
    val Prod          = "\u00D7"
  }

  //  import SimpleSyms._
  import UnicodeSyms._

  /**
    * terms that are given (and determined) by name;
    *  does not include, for instance, pairs each of whose instance is given by a name;
    *  most useful for pattern matching, where the name contains information about formal function applications etc.
    */
  trait Symbolic extends Term {
    val name: AnySym
    override def toString: String = name.toString

    override lazy val hashCode: Int = name.hashCode + 41 * (typ.hashCode)

    override def equals(that: Any): Boolean = that match {
      case sym: Symbolic => sym.typ == typ && sym.name == name
      case _             => false
    }
  }

  trait Variable[U <: Term with Subs[U]] {
    def typ(t: U): Typ[U]
  }

  object Variable {
    implicit object TermVar extends Variable[Term] {
      def typ(t: Term): Typ[Term] = t.typ
    }

    implicit object TypVar extends Variable[Typ[Term]] {
      def typ(t: Typ[Term]): Typ[Typ[Term]] = t.typ
    }

    implicit def funcVar[U <: Term with Subs[U], V <: Term with Subs[V]]
        : Variable[Func[U, V]] {} =
      new Variable[Func[U, V]] {
        def typ(t: Func[U, V]): Typ[Func[U, V]] = t.typ
      }

    implicit def funcLikeVar[U <: Term with Subs[U], V <: Term with Subs[V]]
        : Variable[FuncLike[U, V]] {} =
      new Variable[FuncLike[U, V]] {
        def typ(t: FuncLike[U, V]): Typ[FuncLike[U, V]] = t.typ
      }
  }

  def vartyp[U <: Term with Subs[U]: Variable](t: U): Typ[U] =
    implicitly[Variable[U]].typ(t)

  /**
    * checks if symbolic object has given name.
    */
  def hasName(sym: AnySym): Term => Boolean = {
    case obj: Symbolic =>
      obj.name == sym
    case _ => false
  }

  /**
    *  symbolic objects that are Terms but no more refined
    *  ie, not pairs, formal functions etc.
    */
  case class SymbObj[+U <: Term with Subs[U]](name: AnySym, typ: Typ[U])
      extends Term
      with Symbolic {
    // override def toString = name.toString + " : (" + typ.toString + ")"

    def newobj = SymbObj(InnerSym[Term](this), typ)

    def subs(x: Term, y: Term): Term =
      if (x == this) y
      else {
        def symbobj(sym: AnySym) = typ.replace(x, y).symbObj(sym)
        symSubs(symbobj)(x, y)(name)
        // typ.replace(x, y).symbObj(name.subs(x, y))
      }
  }

  def evalSym(
      symbobj: AnySym => Term
  )(x: Term, y: Term): AnySym => Option[Term] = {
    case LeftProjSym(s) =>
      evalSym(symbobj)(x, y)(s) flatMap {
        case pair: AbsPair[u, v] =>
          Some(pair.first)
        case a =>
          println(s"inner evaluation gives $a")
          None
      }
    case RightProjSym(s) =>
      evalSym(symbobj)(x, y)(s) flatMap {
        case pair: AbsPair[u, v] =>
          Some(pair.second)
        case a =>
          println(s"inner evaluation gives $a")
          None
      }
    case fx: ApplnSym[w, u] =>
      val fn = fx.func.replace(x, y)
      val z = fx.arg.replace(x, y).asInstanceOf[w] 
      // pprint.log(x)
      // pprint.log(y)
      // pprint.log(x == y)
      // pprint.log(fn)
      // pprint.log(z)
      Try(fn(z)).toOption
    case _ => None
  }

  /**
    * substitute symbols, with the only non-trivial substitution for formal applications.
    */
  def symSubs[U <: Term](symbobj: AnySym => U)(x: Term, y: Term): AnySym => U =
    (sym) => {
      // case fx: ApplnSym[w, u] =>
      //   Try((fx.func
      //         .replace(x, y))(fx.arg.replace(x, y).asInstanceOf[w])
      //         .asInstanceOf[U]) getOrElse symbobj(fx)
      // case sym => symbobj(sym.subs(x, y))
      evalSym(symbobj)(x, y)(sym)
        .flatMap((t) => Try(t.asInstanceOf[U]).toOption)
        .getOrElse(symbobj(sym.subs(x, y)))
    }

  /**
    * Symbolic types, which the compiler knows are types.
    *
    */
  case class SymbTyp(name: AnySym, level: Int) extends Typ[Term] with Symbolic {
    lazy val typ = Universe(level)

    def newobj: SymbTyp = SymbTyp(InnerSym[Typ[Term]](this), level)

    type Obj = Term

    def variable(name: AnySym): SymbObj[Term] =
//      if (level == 0)
      SymbObj(name, this)
//      else SymbTyp(name, level - 1)

    // override def toString = s"""${name.toString} : ${UnivSym}_$level"""

    def elem: SymbTyp = this

    //     val applptntypu = ApplnPattern[Term, Typ[Term]]()

    def subs(x: Term, y: Term): Typ[Term] = (x, y) match {
      case (u: Typ[_], v: Typ[_]) if (u == this) => v
      case _ =>
        def symbobj(name: AnySym) = SymbTyp(name, 0)

        symSubs(symbobj)(x, y)(name)
    }
  }

  /**
    * Symbolic propositions. All symbolic objects of this type are witnesses, hence equal.
    *
    */
  case class SymbProp(name: AnySym) extends Typ[Term] with Symbolic {
    lazy val typ: Prop.type = Prop

    def newobj: SymbProp = SymbProp(InnerSym[Typ[Term]](this))

    type Obj = Term

    def variable(name: AnySym): SymbObj[Term] =
      SymbObj(Name("_"), this)

    def elem: SymbProp = this

    def subs(x: Term, y: Term): Typ[Term] = (x, y) match {
      case (u: Typ[_], v: Typ[_]) if (u == this) => v
      case _ =>
        def symbobj(name: AnySym) = SymbProp(name)

        symSubs(symbobj)(x, y)(name)
    }
  }

  def isWitness(t: Term): Boolean = t match {
    case sym: Symbolic => Name("_") == sym.name
    case _             => false
  }

  def witVar[U <: Term with Subs[U]](t: U): U =
    "_" :: t.typ.asInstanceOf[Typ[U]]

  /**
    * Types with symbolic objects not refined.
    */
  trait SmallTyp extends Typ[Term] {
    type Obj = Term

    val typ = Universe(0)

    def variable(name: AnySym): Term = SymbObj(name, this)

    def newobj: Typ[Term] = typ.obj

    def subs(x: Term, y: Term): Typ[Term] = (x, y) match {
      case (xt: Typ[_], yt: Typ[_]) if (xt == this) => yt
      case _                                        => this
    }
  }

  /**
    * Empty type
    */
  case object Zero extends SmallTyp {
    case class RecFn[U <: Term with Subs[U]](codom: Typ[U])
        extends RecFunc[Term, U] {
      val defnData: Vector[Term] = Vector()

      def fromData(data: Vector[Term]) = this

      def act(arg: provingground.HoTT.Term): U = codom.symbObj(vacuousSym)

      val dom: provingground.HoTT.Typ[provingground.HoTT.Term] = Zero

      def subs(
          x: provingground.HoTT.Term,
          y: provingground.HoTT.Term
      ): provingground.HoTT.Func[provingground.HoTT.Term, U] =
        RecFn(codom.replace(x, y))

      val typ: provingground.HoTT.Typ[
        provingground.HoTT.Func[provingground.HoTT.Term, U]
      ] = Zero ->: codom

      def newobj: provingground.HoTT.Func[provingground.HoTT.Term, U]
        with provingground.HoTT.Subs[
          provingground.HoTT.Func[provingground.HoTT.Term, U]
        ] = throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    }

    case class InducFn[U <: Term with Subs[U]](depcodom: Func[Term, Typ[U]])
        extends InducFuncLike[Term, U] {
      val defnData: Vector[Term] = Vector()

      def fromData(
          data: Vector[provingground.HoTT.Term]
      ): provingground.HoTT.InducFuncLike[provingground.HoTT.Term, U] = this
      def act(arg: provingground.HoTT.Term): U =
        PiDefn(depcodom).symbObj(vacuousSym)(arg)
      val dom: provingground.HoTT.Typ[provingground.HoTT.Term] = Zero
      def subs(
          x: provingground.HoTT.Term,
          y: provingground.HoTT.Term
      ): provingground.HoTT.FuncLike[provingground.HoTT.Term, U] =
        InducFn(depcodom.replace(x, y))
      val typ: provingground.HoTT.Typ[
        provingground.HoTT.FuncLike[provingground.HoTT.Term, U]
      ] = PiDefn(depcodom)

      def newobj: provingground.HoTT.FuncLike[provingground.HoTT.Term, U]
        with provingground.HoTT.Subs[
          provingground.HoTT.FuncLike[provingground.HoTT.Term, U]
        ] =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

    }

    def rec[U <: Term with Subs[U]](codom: Typ[U]): RecFunc[Term, U] =
      RecFn(codom)

    def induc[U <: Term with Subs[U]](
        depcodom: Func[Term, Typ[U]]
    ): InducFuncLike[Term, U] =
      InducFn(depcodom)
      // PiDefn(depcodom).symbObj(vacuousSym)
  }

  /**
    * Negation with some simplification
    */
  def negate: Typ[Term] => Typ[Term] = {
    case FuncTyp(tp: Typ[u], Zero) => tp
    case pd: ProdTyp[u, v]         => PlusTyp(negate(pd.first), negate(pd.second))
    case pt: PlusTyp[u, v]         => ProdTyp(negate(pt.first), negate(pt.second))
    case pt: PiDefn[u, v]          => SigmaTyp(lmbda(pt.variable)(negate(pt.value) ))
    case st: SigmaTyp[u, v]        => 
      val x = st.fibers.dom.Var
      val y = st.fibers(x.asInstanceOf[u])
      PiDefn(x, negate(y))
    case Unit                      => Zero
    case Zero                      => Unit
    case tp                        => tp ->: Zero
  }

  /**
    * Unit type.
    */
  case object Unit extends SmallTyp {
    case class RecFn[U <: Term with Subs[U]](codom: Typ[U], data: U)
        extends RecFunc[Term, U] { self =>
      val dom: Unit.type = Unit

      val defnData: Vector[U] = Vector(data)

      def fromData(data: Vector[Term]) = RecFn(codom, data.head.asInstanceOf[U])

      val typ: FuncTyp[Term, U] = dom ->: codom

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      def subs(x: Term, y: Term) =
        RecFn(codom.replace(x, y), data.replace(x, y))

      def act(t: Term): U = t match {
        case Star => data
        case _    => codom.symbObj(ApplnSym(self, t))
      }
    }

    def rec[U <: Term with Subs[U]](codom: Typ[U]): Func[U, Func[Term, U]] = {
      val x = codom.Var
      x :-> (RecFn(codom, x): Func[Term, U])
    }

    case class InducFn[U <: Term with Subs[U]](
        depcodom: Func[Term, Typ[U]],
        data: U
    ) extends InducFuncLike[Term, U] { self =>
      val dom: Unit.type = Unit

      val defnData: Vector[U] = Vector(data)

      def fromData(data: Vector[Term]) =
        InducFn(depcodom, data.head.asInstanceOf[U])

      val typ = PiDefn(depcodom)

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      def subs(x: Term, y: Term) =
        InducFn(depcodom.replace(x, y), data.replace(x, y))

      def act(t: Term): U = t match {
        case Star => data
        case _    => depcodom(t).symbObj(ApplnSym(self, t))
      }
    }

    def induc[U <: Term with Subs[U]](
        depcodom: Func[Term, Typ[U]]
    ): FuncLike[U, FuncLike[Term, U]] = {
      val x = depcodom(Star).Var
      x :~> (InducFn(depcodom, x): FuncLike[Term, U])
    }
  }

  /**
    *  the object in the Unit type
    */
  case object Star extends AtomicTerm {
    val typ: Unit.type = Unit
  }

  val One: Unit.type = Unit

  /**
    *  Symbol for map 0 -> A
    */
  case object vacuousSym extends AtomicSym

  /**
    * Map from 0 to A.
    */
  def vacuous[U <: Term with Subs[U]](codom: Typ[U]): Func[Term, U] =
    (Zero ->: codom).symbObj(vacuousSym)

  /**
    * Notation for universes.
    */
  type Univ = Typ[Typ[Term]]

  /**
    * A universe that is equal (in scala) to `Type`
    * but may have refined scala type.
    */
  trait BaseUniv {
    override lazy val hashCode: Int = Type.hashCode

    override def equals(a: Any): Boolean = a match {
      case _: BaseUniv                 => true
      case Type                        => true
      case Universe(_) if ignoreLevels => true
      case _                           => false
    }
  }

  var ignoreLevels = false

  /** The (usual) universes */
  case class Universe(level: Int) extends Univ with Subs[Universe] {
    require(level >= 0)

    type Obj = Typ[Term]

    lazy val typ = Universe(level + 1)

    def variable(name: AnySym) = SymbTyp(name, level)

    override def hashCode = 37

    def newobj =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    def subs(x: Term, y: Term): Universe = this

    override def toString: String = UnivSym + "_" + level

    override def equals(that: Any): Boolean = that match {
      case Universe(k) if (ignoreLevels || k == level) =>
        // if (k != level) pprint.log(s"mismatched universe levels $level and $k")
        true
      case _: BaseUniv if (ignoreLevels || level == 0) =>
        // if (level != 0) pprint.log(s"mismatched universe levels $level and Base")
        true
      case _ => false
    }
  }

  case object Prop extends BaseUniv with Univ {
    type Obj = Typ[Term]

    val typ: Universe = Type.typ

    def variable(name: AnySym) = SymbProp(name)

    def newobj =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    def subs(x: Term, y: Term): Prop.type = this

  }

  def isProp(x: Typ[Term]): Boolean = (x.typ) match {
    case _: Prop.type => true
    case _            => false
  }

  def isPropFmly(t: Typ[Term]): Boolean = t match {
    case PiDefn(_, value: Typ[u])  => isPropFmly(value)
    case FuncTyp(_, codom: Typ[u]) => isPropFmly(codom)
    case x                         => isProp(x)
  }

  def univlevel: Typ[Typ[Term]] => Int = {
    case Universe(l) => l
    case _           => 0
  }

  /**
    * The first universe
    */
  val Type: Universe = Universe(0)

  /**
    * The product type `A  times B`
    *
    * @param first the first component
    * @param second the second component
    */
  case class ProdTyp[U <: Term with Subs[U], V <: Term with Subs[V]](
      first: Typ[U],
      second: Typ[V]
  ) extends Typ[PairTerm[U, V]]
      with AbsPair[Typ[U], Typ[V]]
      with Subs[ProdTyp[U, V]] { prod =>

    type Obj = PairTerm[U, V]

    lazy val typ = Universe(Math.max(first.typlevel, second.typlevel))

    def newobj: ProdTyp[U, V] = {
      val newfirst = first.newobj
      ProdTyp(newfirst, second replace (first, newfirst))
    }

    /**
      * Introduction rule for the product type
      */
    lazy val paircons: Func[U, Func[V, PairTerm[U, V]]] = {
      val a = first.Var
      val b = second.Var
      lmbda(a)(lmbda(b)(PairTerm(a, b)))
    }

    /**
      * Projection from product
      */
    lazy val (proj1, proj2) = {
      val x = this.Var
      (x :-> x.first, x :-> x.second)
    }

    def subs(x: Term, y: Term): ProdTyp[U, V] =
      if (x == this)
        Try(y.asInstanceOf[ProdTyp[U, V]]).getOrElse({
          println(y); println(x); println(y.typ)
          ProdTyp(first.replace(x, y), second.replace(x, y))
        })
      else ProdTyp(first.replace(x, y), second.replace(x, y))

    // The name is lost as `name', but can be recovered using pattern matching.
    def variable(name: AnySym): Obj =
      PairTerm(
        first.symbObj(LeftProjSym(name)),
        second.symbObj(RightProjSym(name))
      )

    /**
      * Recursion function from product type
      *
      * @param codom the codomain
      * @param data the definition data
      */
    case class RecFn[W <: Term with Subs[W]](
        codom: Typ[W],
        data: Func[U, Func[V, W]]
    ) extends RecFunc[PairTerm[U, V], W] { self =>
      lazy val dom: ProdTyp[U, V] = prod

      val defnData: Vector[Func[U, Func[V, W]]] = Vector(data)

      def fromData(data: Vector[Term]) =
        RecFn(codom, data.head.asInstanceOf[Func[U, Func[V, W]]])

      lazy val typ: FuncTyp[PairTerm[U, V], W] = dom ->: codom

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      def subs(x: Term, y: Term) =
        ProdTyp(first.replace(x, y), second.replace(x, y))
          .RecFn(codom.replace(x, y), data.replace(x, y))

      def act(w: PairTerm[U, V]): W = w match {
        case PairTerm(a, b) if a.typ == first && b.typ == second => data(a)(b)
        case _                                                   => codom.symbObj(ApplnSym(self, w))
      }
    }

    /**
      * recursive definition
      *
      * @param target the codomain
      */
    def rec[W <: Term with Subs[W]](
        target: Typ[W]
    ): Func[Func[U, Func[V, W]], Func[PairTerm[U, V], W]] = {
      val d = (first ->: second ->: target).Var
      d :-> (RecFn(target, d): Func[PairTerm[U, V], W])
    }

    /**
      * Inductive definition
      *
      * @param targetFmly type family for the Pi-type dependent codomain
      * @param data definition data
      */
    case class InducFn[W <: Term with Subs[W]](
        targetFmly: Func[U, Func[V, Typ[W]]],
        data: FuncLike[U, FuncLike[V, W]]
    ) extends InducFuncLike[PairTerm[U, V], W] { self =>
      lazy val dom: ProdTyp[U, V] = prod

      val defnData: Vector[FuncLike[U, FuncLike[V, W]]] = Vector(data)

      def fromData(data: Vector[Term]): InducFn[W] =
        InducFn(targetFmly, data.head.asInstanceOf[FuncLike[U, FuncLike[V, W]]])

      val xy: PairTerm[U, V] = prod.Var

      lazy val typ: GenFuncTyp[PairTerm[U, V], W] = xy ~>: (targetFmly(
        xy.first
      )(xy.second))

      lazy val depcodom: Func[PairTerm[U, V], Typ[W]] = {
        val p = prod.Var
        p :-> targetFmly(p.first)(p.second)
      }
//        (p: PairTerm[U, V]) => targetFmly(p.first)(p.second)

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      def subs(x: Term, y: Term) =
        ProdTyp(first.replace(x, y), second.replace(x, y))
          .InducFn(targetFmly.replace(x, y), data.replace(x, y))

      def act(w: PairTerm[U, V]): W = w match {
        case PairTerm(a, b) if a.typ == first && b.typ == second => data(a)(b)
        case _                                                   => targetFmly(w.first)(w.second).symbObj(ApplnSym(self, w))
      }
    }

    /**
      * Inductively defined function to targetFmly
      */
    def induc[W <: Term with Subs[W]](
        targetFmly: Func[U, Func[V, Typ[W]]]
    ) = {
      val xy     = prod.Var
      val (x, y) = (xy.first, xy.second)
      val d      = (x ~>: (y ~>: targetFmly(x)(y))).Var
      if ((targetFmly(x)(y).dependsOn(x)==false) && (targetFmly(x)(y).dependsOn(y)==false)) 
      {val dd = (first ->: second ->: targetFmly(x)(y)).Var
        dd :-> (RecFn(targetFmly(x)(y), dd): Func[PairTerm[U,V],W])
      }
      else {d :-> (InducFn(targetFmly, d): FuncLike[PairTerm[U, V], W])}
    }
  }

  /**
    * Term (a, b) in  A times B
    */
  case class PairTerm[U <: Term with Subs[U], V <: Term with Subs[V]](
      first: U,
      second: V
  ) extends AbsPair[U, V]
      with Subs[PairTerm[U, V]] {
    lazy val typ: ProdTyp[U, V] =
      ProdTyp(first.typ.asInstanceOf[Typ[U]], second.typ.asInstanceOf[Typ[V]])

    def newobj: PairTerm[U, V] = {
      val newfirst = first.newobj
      PairTerm(newfirst, second replace (first, newfirst))
    }

    def subs(x: Term, y: Term): PairTerm[U, V] =
      if (x == this) y.asInstanceOf[PairTerm[U, V]]
      else PairTerm[U, V](first.replace(x, y), second.replace(x, y))
  }

  /**
    * Abstract pair, parametrized by scala types of components,
    * generally a (dependent) pair object or a product type.
    */
  trait AbsPair[+U <: Term with Subs[U], +V <: Term with Subs[V]]
      extends Term
      with Subs[AbsPair[U, V]] {
    val first: U
    val second: V

    override def toString = s"""(($first) , ($second))"""
  }

  object AbsPair {
    def apply(x: Term, y: Term): AbsPair[Term, Term] = mkPair(x, y)

    def unapply(x: Term): Option[(Term, Term)] = x match {
      case ab: AbsPair[_, _] => Some((ab.first, ab.second))
      case _                 => None
    }
  }

  object Tuple {
    def apply(xs: Term*): Term = xs.toList match {
      case List()  => Star
      case List(x) => x
      case x :: ys => AbsPair(x, apply(ys: _*))
    }

    def asTuple(x: Term): List[Term] = x match {
      case Star          => List()
      case AbsPair(a, y) => a :: asTuple(y)
      case _             => List(x)
    }

    def unapplySeq(x: Term): Option[Seq[Term]] =
      if (x == Star) None else Some(asTuple(x))
  }

  /**
    * overloaded method returning a pair object or a pair type.
    */
  def pair[U <: Term with Subs[U], V <: Term with Subs[V]](
      first: U,
      second: V
  ) =
    PairTerm(first, second)

  /**
    * overloaded method returning a pair object or a pair type.
    */
  def pair[U <: Term with Subs[U], V <: Term with Subs[V]](
      first: Typ[U] with Subs[Typ[U]],
      second: Typ[V] with Subs[Typ[V]]
  ) = ProdTyp(first, second)

  /**
    * makes a pair with the appropriate runtime type
    */
  lazy val mkPair: (Term, Term) => AbsPair[Term, Term] = {
    case (a: Typ[u], b: Typ[v]) => ProdTyp[Term, Term](a, b)
    case (a, b) if b.typ.dependsOn(a) =>
      val fiber = lmbda(a)(b.typ)
      DepPair(a, b, fiber)
    case (a, b) => PairTerm(a, b)
  }

  case object HashSym extends AtomicSym

  /**
    * Possibly dependent function type.
    * Meant to override equality so that an object with scala type a Pi-Type
    * but with fiber a constant is equal to the corresponding function type.
    *
    * @param domain the domain
    * @param fib fiber of the codomain
    */
  abstract class GenFuncTyp[W <: Term with Subs[W], U <: Term with Subs[U]](
      val domain: Typ[W],
      val fib: W => Typ[U]
  ) extends Typ[FuncLike[W, U]]
      with Subs[GenFuncTyp[W, U]] {
    override lazy val hashCode: Int =
      fib(domain.symbObj(HashSym)).hashCode() * 41 + 7

    override def equals(that: Any): Boolean = that match {
      case g: GenFuncTyp[u, v] =>
        g.domain == domain && {
          val x = domain.Var
          g.fib(x.asInstanceOf[u]) == fib(x)
        }
      case _ => false
    }
  }

  /**
    * Function type - pure, not dependent.
    */
  case class FuncTyp[W <: Term with Subs[W], U <: Term with Subs[U]](
      dom: Typ[W],
      codom: Typ[U]
  ) extends GenFuncTyp[W, U](dom, (w: W) => codom)
      with Typ[Func[W, U]]
      with Subs[FuncTyp[W, U]] {
    type Obj = Func[W, U]

    def asPi: PiDefn[W, U] = piDefn("###" :: dom)(codom)

    lazy val typ: Typ[Typ[Term]] = Universe(max(dom.typlevel, codom.typlevel))

    def variable(name: AnySym): Func[W, U] =
      SymbolicFunc[W, U](name, dom, codom)

    override def toString = s"(${dom.toString}) $Arrow (${codom.toString})"

    def newobj: FuncTyp[W, U] = {
      val newdom = dom.newobj
      FuncTyp(newdom, codom replace (dom, newdom))
    }

    def subs(x: Term, y: Term): FuncTyp[W, U] =
      FuncTyp[W, U](dom.replace(x, y), codom.replace(x, y))
  }

  /**
    * Symbol for domain of a symbolic function
    */
  case class DomSym(func: AnySym) extends AnySym {
    def subs(x: Term, y: Term) = DomSym(func.subs(x, y))
  }

  /**
    * Symbol for co-domain of a symbolic function
    */
  case class CodomSym(func: AnySym) extends AnySym {
    def subs(x: Term, y: Term) = CodomSym(func.subs(x, y))
  }

  /**
    * common trait for induction functions,
    * for serialization and pretty printing.
    */
  trait InducFuncLike[W <: Term with Subs[W], +U <: Term with Subs[U]]
      extends FuncLike[W, U] {

    /**
      * the definition data for all the introduction rules
      */
    val defnData: Vector[Term]

    def fromData(data: Vector[Term]): InducFuncLike[W, U]

    override def usesVar(t: Term): Boolean = defnData.exists(_.usesVar(t))

    lazy val fullData: (Typ[W], W => Typ[U], Vector[Term]) =
      (dom, depcodom, defnData)

    override def hashCode: Int = fullData.hashCode

    override def equals(that: Any): Boolean = that match {
      case r: InducFuncLike[_, _] => fullData == r.fullData
      case _                      => false
    }

    override def toString(): String =
      defnData.foldLeft(s"ind($dom)($depcodom)") {
        case (str, t) => s"$str($t)"
      }
  }

  /**
    * common trait for indexed induction functions,
    * for serialization and pretty printing.
    */
  trait IndInducFuncLike[W <: Term with Subs[W], +U <: Term with Subs[U], F <: Term with Subs[
    F
  ], IDFT <: Term with Subs[IDFT]]
      extends InducFuncLike[W, U] {

    /**
      * the domain family, e.g. `Vec`
      */
    val domW: F

    /**
      * the dependent codomain on the family.
      */
    val codXs: IDFT

    /**
      * indices of the introduction rules.
      */
    val index: Vector[Term]

    lazy val fullIndData: (F, Vector[Term], IDFT, Vector[Term]) =
      (domW, index, codXs, defnData)

    override def hashCode: Int = fullIndData.hashCode

    override def equals(that: Any): Boolean = that match {
      case r: IndInducFuncLike[_, _, _, _] =>
        fullIndData == r.fullIndData
      case _ => false
    }

    override def toString(): String =
      defnData.foldLeft(s"ind{$domW${index.mkString("(", ")(", ")")}}{$codXs}") {
        case (str, t) => s"$str($t)"
      }

  }

  class ApplnFailException(val func: Term, val arg: Term)
      extends IllegalArgumentException(
        s"function $func with domain(optional) ${ApplnFailException
          .domOpt(func)} cannot act on given term $arg with type ${arg.typ}"
      ) {
    def domOpt: Option[Typ[Term]] = func match {
      case fn: FuncLike[u, v] => Some(fn.dom)
      case _                  => None
    }

    def argType: Typ[Term] = arg.typ
  }

  object ApplnFailException {
    def domOpt(func: Term): Option[Typ[Term]] = func match {
      case fn: FuncLike[u, v] => Some(fn.dom)
      case _                  => None
    }

  }

  case class NotTypeException(tp: Term)
      extends IllegalArgumentException("Expected type but got term")

  /**
    * record a function as an application, especially for serialization
    */
  trait MiscAppln extends Term {
    val func: Term
    val arg: Term
  }

  object MiscAppln {
    def unapply(t: Term): Option[(Term, Term)] = t match {
      case ma: MiscAppln          => Some(ma.func -> ma.arg)
      case FormalAppln(func, arg) => Some(func -> arg)
      case _                      => None
    }
  }

  def applyFunc(func: Term, arg: Term): Term = func match {
    // case fn: Func[u, v] if isWitness(arg) => "_" :: fn.codom
    case fn: FuncLike[u, v] if fn.canApply(arg.asInstanceOf[u]) =>
      fn.applyUnchecked(arg.asInstanceOf[u])
    case _ => throw new ApplnFailException(func, arg)
  }

  def applyFuncOpt(func: Term, arg: Term): Option[Term] = func match {
    case fn: Func[u, v] if isWitness(arg) => Some("_" :: fn.codom)
    case fn: FuncLike[u, v] if fn.canApply(arg.asInstanceOf[u]) =>
      Some(fn.applyUnchecked(arg.asInstanceOf[u]))
    case _ => None
  }

  def toTyp(t: Term): Typ[U] forSome { type U <: Term with Subs[U] } =
    t match {
      case tp: Typ[u] => tp
      case _          => throw NotTypeException(t)
    }

  def toTypOpt(
      t: Term
  ): Option[Typ[U] forSome { type U <: Term with Subs[U] }] =
    t match {
      case tp: Typ[u] => Some(tp)
      case _          => None
    }

  def typOpt(x: Term): Option[Typ[Term]] = x match {
    case typ: Typ[_] => Some(typ: Typ[Term])
    case _           => None
  }

  trait ExstFunc {
    type U <: Term with Subs[U]

    type V <: Term with Subs[V]

    val func: FuncLike[U, V]

    lazy val term: Term = func

    lazy val dom: Typ[Term] = func.dom

    override def hashCode(): Int = term.hashCode()

    override def equals(obj: Any): Boolean = obj match {
      case that: ExstFunc => term == that.term
      case _              => false
    }

    def apply(arg: Term): Option[Term] =
      if (arg.typ == func.dom) Some(func(arg.asInstanceOf[U])) else None
  }

  object ExstFunc {
    case class Wrap[X <: Term with Subs[X], Y <: Term with Subs[Y]](
        func: FuncLike[X, Y]
    ) extends ExstFunc {
      type U = X

      type V = Y
    }

    case object GetFunc extends (ExstFunc => Term){
      def apply(f: ExstFunc) = f.func

      override def toString(): String = "GetFunc"
    }

    def apply[X <: Term with Subs[X], Y <: Term with Subs[Y]](
        fn: FuncLike[X, Y]
    ): ExstFunc = Wrap(fn)
    //       new ExstFunc {
    //   type U = X
    //   type V = Y
    //
    //   val func: FuncLike[X, Y] = fn
    // }

    def opt(t: Term): Option[ExstFunc] = t match {
      case fn: FuncLike[u, v] => Some(ExstFunc(fn))
      case _                  => None
    }
  }

  def skipVars(t: Term, n: Int): Option[Term] =
    if (n <= 0) Some(t)
    else
      t match {
        case l: LambdaLike[u, v] => skipVars(l.value, n - 1)
        case _                   => None
      }

  def skipApply(f: Term, x: Term, n: Int): Option[Term] =
    f match {
      case l: LambdaLike[u, v] =>
        val canSkip =
          skipVars(l.value, n - 1).exists(_.indepOf(l.variable))
        if (canSkip) Some(l.value) else None
      case _ => None
    }

  /**
    * Terms that are functions or dependent functions,
    * is a scala function, but the apply is not directly defined -
    * instead the method [[FuncLike#act]] is defined.
    */
  trait FuncLike[W <: Term with Subs[W], +U <: Term with Subs[U]]
      extends Term
      with (W => U)
      with Subs[FuncLike[W, U]] {
    type Obj <: FuncLike[W, U]

    //    // val domobjtpe : Type

    //    // val codomobjtpe: Type

    val typ: Typ[FuncLike[W, U]]

    val dom: Typ[W]

    val depcodom: W => Typ[U]

    /**
      * the action of the function to define:
      * define this method, but use apply.
      */
    def act(arg: W): U

    /**
      * checks if application is valid; can override to allow for example resizing universes
      *
      * @param arg the argument
      * @return whether the argument has the correct type.
      */
    def canApply(arg: W): Boolean = arg.typ == dom

    /**
      * application of the function: use this but define the [[act]] method;
      * checks HoTT-type of argument is in the domain and throws exception if it fails.
      */
    def apply(arg: W): U =
      if (!canApply(arg)) throw new ApplnFailException(this, arg)
      else applyUnchecked(arg)

    def applyUnchecked(arg: W): U = {
      arg match {
        case t: Cnst => Try(apply(t.term.asInstanceOf[W])).getOrElse(act(arg))
        case _       => act(arg)
      }

      act(arg)
    }

    def subs(x: Term, y: Term): FuncLike[W, U]
  }

  /**
    * A symbol representing a formal application
    */
  case class ApplnSym[W <: Term with Subs[W], U <: Term with Subs[U]](
      func: FuncLike[W, U],
      arg: W
  ) extends AnySym {
    override def toString = s"""(${func.toString}) (${arg.toString})"""

    def subs(x: Term, y: Term) =
      ApplnSym(func.replace(x, y), arg.replace(x, y))
  }

  /**
    * Pattern matching for a formal application.
    */
  object FormalAppln {
    def unapply(term: Term): Option[(Term, Term)] = term match {
      case sym: Symbolic =>
        sym.name match {
          case sm: ApplnSym[_, _] =>
            Some((sm.func, sm.arg))
          case _ => None
        }
      case _ => None
    }

    def apply[U <: Term with Subs[U], V <: Term with Subs[V]](
        fn: Func[U, V],
        x: U
    ): V =
      fn.codom.symbObj(ApplnSym(fn, x))

    def apply[U <: Term with Subs[U], V <: Term with Subs[V]](
        fn: FuncLike[U, V],
        x: U
    ): V =
      fn.depcodom(x).symbObj(ApplnSym(fn, x))
  }

  /**
    * common trait for recursion functions,
    * for serialization and pretty printing.
    */
  trait RecFunc[W <: Term with Subs[W], +U <: Term with Subs[U]]
      extends Func[W, U] {

    /**
      * definition data for all introduction  rules.
      */
    val defnData: Vector[Term]

    def fromData(data: Vector[Term]): RecFunc[W, U]

    override def usesVar(t: Term): Boolean = defnData.exists(_.usesVar(t))

    lazy val fullData: (Typ[W], Typ[U], Vector[Term]) = (dom, codom, defnData)

    override def hashCode: Int = fullData.hashCode

    override def equals(that: Any): Boolean = that match {
      case r: RecFunc[_, _] =>
        fullData == r.fullData
      case _ => false
    }

    override def toString(): String =
      defnData.foldLeft(s"rec($dom)($codom)") {
        case (str, t) => s"$str($t)"
      }
  }

  /**
    * common trait for indexed recurion functions,
    * for serialization and pretty printing.
    */
  trait IndRecFunc[W <: Term with Subs[W], +U <: Term with Subs[U], F <: Term with Subs[
    F
  ]] extends RecFunc[W, U] {

    /**
      * the dependent codomain on the family.
      */
    val domW: F

    /**
      * indices of the introduction rules.
      */
    val index: Vector[Term]

    lazy val fullIndData: (F, Vector[Term], Typ[U], Vector[Term]) =
      (domW, index, codom, defnData)

    override def hashCode: Int = fullIndData.hashCode

    override def equals(that: Any): Boolean = that match {
      case r: IndRecFunc[_, _, _] =>
        fullIndData == r.fullIndData
      case _ => false
    }

    override def toString(): String =
      defnData.foldLeft(
        s"rec{$domW${index.mkString("(", ")(", ")")}}{$codom}"
      ) {
        case (str, t) => s"$str($t)"
      }

  }

  /**
    *  a function (pure, not dependent), i.e.,
    * a term in a function type, has a codomain and a fixed type for the domain.
    *
    */
  trait Func[W <: Term with Subs[W], +U <: Term with Subs[U]]
      extends FuncLike[W, U]
      with Subs[Func[W, U]] {

    /** domain*/
    val dom: Typ[W]

    /** codomain */
    val codom: Typ[U]

    val typ: Typ[Func[W, U]]

    val depcodom: W => Typ[U] = _ => codom

    def act(arg: W): U

    def subs(x: Term, y: Term): Func[W, U]
  }

  /**
    * wrapped function adding name
    */
  case class NamedFunc[W <: Term with Subs[W], +U <: Term with Subs[U]](
      name: AnySym,
      func: Func[W, U]
  ) extends Func[W, U] {
    lazy val dom: Typ[W] = func.dom

    lazy val codom: Typ[U] = func.codom

    lazy val typ: Typ[Func[W, U]] = func.typ

    def newobj = NamedFunc(name, func.newobj)

    def act(arg: W): U = func.act(arg)

    def subs(x: Term, y: Term): NamedFunc[W, U] =
      NamedFunc(name, func.subs(x, y))
  }

  /**
    * wrapped dependent function adding name
    */
  case class NamedDepFunc[W <: Term with Subs[W], +U <: Term with Subs[U]](
      name: AnySym,
      func: FuncLike[W, U]
  ) extends FuncLike[W, U] {
    lazy val dom: Typ[W] = func.dom

    lazy val depcodom: W => Typ[U] = func.depcodom

    lazy val typ: Typ[FuncLike[W, U]] = func.typ

    def newobj = NamedDepFunc(name, func.newobj)

    def act(arg: W): U = func.act(arg)

    def subs(x: Term, y: Term) = NamedDepFunc(name, func.subs(x, y))
  }

  /**
    * A symbolic function, acts formally.
    */
  case class SymbolicFunc[W <: Term with Subs[W], U <: Term with Subs[U]](
      name: AnySym,
      dom: Typ[W],
      codom: Typ[U]
  ) extends Func[W, U]
      with Subs[Func[W, U]]
      with Symbolic {

    //      // val domobjtpe = typeOf[W]

    //	  // val codomobjtpe = typeOf[U]

    lazy val typ: FuncTyp[W, U] = FuncTyp[W, U](dom, codom)

    override def canApply(arg: W): Boolean =
      (dom == arg.typ) || (isUniv(dom) && isUniv(arg.typ))

    def act(arg: W): U =
      if (dom == arg.typ) codom.symbObj(ApplnSym(this, arg))
      else
        codom
          .replace(dom, arg.typ)
          .symbObj(ApplnSym(replace(dom, arg.typ), arg))

    def newobj = SymbolicFunc(InnerSym[Func[W, U]](this), dom, codom)

    def subs(x: Term, y: Term): Func[W, U] = (x, y) match {
      //        case (u: Typ[_], v: Typ[_]) => SymbolicFunc(name, dom.replace(u, v), codom.replace(u, v))
      case (u, v: Func[W, U]) if (u == this) => v
      case (u, v: Symbolic) if (u == this)   => typ.variable(v.name)
      case _ =>
        def symbobj(sym: AnySym) =
          SymbolicFunc(sym, dom.replace(x, y), codom.replace(x, y))

        symSubs(symbobj)(x, y)(name)
    }

    // override def toString = s"""${name.toString} : (${typ.toString})"""
  }

  /**
    * A function defined by a scala function
    */
  class FuncDefn[W <: Term with Subs[W], U <: Term with Subs[U]](
      func: => (W => U),
      val dom: Typ[W],
      val codom: Typ[U]
  ) extends Func[W, U] {
    //	  // val domobjtpe = typeOf[W]

    //	  // val codomobjtpe = typeOf[U]

    type D = W

    type Cod = U

    lazy val typ: FuncTyp[W, U] = FuncTyp[W, U](dom, codom)

    def act(arg: W): U = func(arg)

    def newobj: Func[W, U] = typ.obj

    def subs(x: Term, y: Term) =
      new FuncDefn(
        (w) => func(w).replace(x, y),
        dom.replace(x, y),
        codom.replace(x, y)
      )
  }

  def replaceVar[U <: Term with Subs[U]](
      variable: U
  )(x: Term, y: Term): U with Subs[U] =
    variable match {
      case sym: Symbolic =>
        val newtyp = variable.typ.asInstanceOf[Typ[U]]
        newtyp.variable(sym.name)
      case t =>
        println(s"Encountered non-symbolic variable $t of type ${t.typ}")
        t.replace(x, y)
    }

  val resizedEqual: (Typ[Term], Typ[Term]) => Boolean = {
    case (x, y) if (x == y)         => true
    case (_: Universe, _: Universe) => true
    case (ft1: FuncTyp[x1, y1], ft2: FuncTyp[x2, y2]) =>
      (ft1.dom == ft2.dom) && resizedEqual(ft1.codom, ft2.codom)
    case (gt1: GenFuncTyp[x1, y1], gt2: GenFuncTyp[x2, y2])
        if gt1.domain == gt2.domain =>
      val x = gt1.domain.Var
      resizedEqual(gt1.fib(x.asInstanceOf[x1]), gt2.fib(x.asInstanceOf[x2]))
    case _ => false
  }

  /**
    *  A lambda-expression;
    *  variable is mapped to value.
    *  This may or may not be a dependent function.
    *  If it is required at compile time that it is not dependent,
    * and hence has scala type Func, then use the class [[LambdaFixed]]
    *
    * @note Equality is overriden here, so that pure functions disguised as dependent ones
    * can be equal to undisguised pure functions.
    *
    */
  sealed trait LambdaLike[X <: Term with Subs[X], Y <: Term with Subs[Y]]
      extends FuncLike[X, Y] {
    //	  // val domobjtpe = typeOf[X]

    //	  // val codomobjtpe = typeOf[Y]

    /**
      * the variable `x` in the lambda definition `x \mapsto y`
      */
    val variable: X

    /**
      * the value `y` in the lambda definition `x mapsto y`
      */
    val value: Y

    type D = X

    type Cod = Y

    lazy val dom: Typ[X] = variable.typ.asInstanceOf[Typ[X]]

    override def usesVar(t: Term): Boolean = {
      // pprint.log(t)
      // pprint.log(variable)
      // pprint.log(value)
      (t == variable) || value.usesVar(t)
    }

    override def toString =
      s"""(${variable.toString} :  ${variable.typ.toString}) $MapsTo (${value.toString})"""

    val dep: Boolean

    lazy val typ: Typ[FuncLike[X, Y]] =
      if (dep) {
        PiDefn(variable, value.typ.asInstanceOf[Typ[Y]])
      } else
        FuncTyp(
          variable.typ.asInstanceOf[Typ[X]],
          value.typ.asInstanceOf[Typ[Y]]
        )

    override def canApply(arg: X): Boolean =
      resizedEqual(dom, arg.typ)

    def act(arg: X): Y =
      if (usesVar(arg)) {
        val newvar = variable.newobj
        val result =
          avoidVar(arg, value).replace(variable, newvar).replace(newvar, arg)
        // if (result != value.replace(variable, arg)) println("Escaping needed")
        result
      } else avoidVar(arg, value).replace(variable, arg)

    override lazy val hashCode: Int = {
      val newvar = variable.typ.symbObj(Name("hash"))
      val newval = value.replace(variable, newvar)
      41 * (variable.typ.hashCode + 41) + newval.hashCode
    }

    override def equals(that: Any): Boolean = that match {
      case l: LambdaLike[u, v] if l.variable.typ == variable.typ =>
        l.value.replace(l.variable, variable) == value &&
          value.replace(variable, l.variable) == l.value
      case _ => false
    }

    def subs(x: Term, y: Term): LambdaLike[X, Y] = {
      val xx = avoidVar(variable, x)
      val yy = avoidVar(variable, y)
      if (variable.replace(x, y) == variable)
        LambdaTerm(variable, value.replace(xx, yy))
      else {
        val newvar = variable.replace(x, y)
        LambdaTerm(newvar, value.replace(variable, newvar).replace(xx, yy))
      }
    }

    //    private lazy val myv = variable.newobj

    def andthen[Z <: Term with Subs[Z]](f: Y => Z): LambdaTerm[X, Z] =
      LambdaTerm(variable, f(value))
  }

  /**
    * functions given by lambda, which may be dependent;
    * even if it is pure, the scala type does not show this;
    * one should usually not use the constructor of this case class,
    * instead use the function [[lmbda]] or the syntactic sugar for this,
    * e.g. {{{ val f = x :~> y }}}
    * Note the `:~>`, not `:->`
    * These create a new variable to avoid name collisions.
    */
  case class LambdaTerm[X <: Term with Subs[X], Y <: Term with Subs[Y]](
      variable: X,
      value: Y
  ) extends LambdaLike[X, Y] {

    val depcodom: X => Typ[Y] = (t: X) =>
      value.typ.replace(variable, t).asInstanceOf[Typ[Y]]

    val dep = true //value.typ dependsOn variable

    def newobj: LambdaTerm[X, Y] = {
      val newvar = variable.newobj
      LambdaTerm(newvar, value.replace(variable, newvar))
    }

  }

  /**
    * lambda which is known to be pure, i.e., have fixed codomain;
    * this is reflected in its scala class;
    * one should usually not use the constructor of this case class,
    * instead use the function [[lambda]] or the syntactic sugar for this,
    * e.g. {{{ val f = x :-> y }}}
    * this creates a new variable to avoid name collisions.
    *
    * @note that this construction gives a runtime error if the type of `y` depends on `x`.
    */
  case class LambdaFixed[X <: Term with Subs[X], Y <: Term with Subs[Y]](
      variable: X,
      value: Y
  ) extends LambdaLike[X, Y]
      with Func[X, Y]
      with Subs[LambdaFixed[X, Y]] {
    override lazy val dom: Typ[X] = variable.typ.asInstanceOf[Typ[X]]

    val codom: Typ[Y] = value.typ.asInstanceOf[Typ[Y]]

    override lazy val typ: FuncTyp[X, Y] = dom ->: codom

    val dep = false

    // override def equals(that: Any) = that match {
    //   case LambdaFixed(x: Term, y: Term) if (x.typ == variable.typ) =>
    //     y.replace(x, variable) == value
    //   case LambdaTerm(x: Term, y: Term) if x.typ == variable.typ =>
    //       y.replace(x, variable) == value
    //   case _ => false
    // }

    def newobj: LambdaFixed[X, Y] = {
      val newvar = variable.newobj
      LambdaFixed(newvar, value.replace(variable, newvar))
    }

    override def subs(x: Term, y: Term): LambdaFixed[X, Y] = {
      val xx = avoidVar(variable, x)
      val yy = avoidVar(variable, y)
      if (variable.replace(x, y) == variable)
        LambdaFixed(variable, value.replace(xx, yy))
      else {
        val newvar = variable.newobj
        LambdaFixed(
          newvar.replace(xx, yy),
          value.replace(variable, newvar).replace(xx, yy)
        )
      }
    }
  }

  /**
    * term as a symbol
    */
  case class TermSymbol(term: Term) extends AnySym {
    def subs(x: Term, y: Term) = TermSymbol(term.replace(x, y))
  }

  def termSymbol(term: Term): AnySym = TermSymbol(term)

  /**
    * returns symbolic object with new type.
    */
  //  def changeTyp[U <: Term with Subs[U]](term: U, newtyp: Typ[U]): U = term match {
  //    case sym: Symbolic => newtyp.symbObj(sym.name)
  //    case _ => newtyp.symbObj(term)
  //  }

  /**
    * instantiates variable values in a term if it is made from lambdas, to give a term (if possible) of a required HoTT type.
    *
    * @param target required type after instantiation
    * @param substitutions substitutions to make.
    */
  def instantiate(
      substitutions: Term => Option[Term],
      target: Typ[Term]
  ): Term => Option[Term] = {
    case t: Term if t.typ == target => Some(t)
    case LambdaTerm(variable: Term, value: Term) =>
      substitutions(variable) flatMap
        ((cnst) => {
          val reduced = (value.replace(variable, cnst))
          instantiate(substitutions, target)(reduced)
        })
    case _ => None
  }

  /**
    * A symbol to be used to generate new variables of a type,
    * without changing [[toString]].
    */
  class InnerSym[U <: Term with Subs[U]](variable: U with Symbolic)
      extends AnySym {
    val outer: U with Symbolic = variable

    lazy val outerSym: AnySym = outer.name

    override def toString: String = "`" + variable.name.toString
    // match {
    //  case sym: Symbolic => sym.name.toString
    //  case x             => x.toString
    // }

    def subs(x: Term, y: Term): InnerSym[U] = {
      // val newvar = outer.replace(x, y) match {
      //   case sym: Symbolic => sym.asInstanceOf[U with Symbolic]
      //   case _             => variable
      // }
      //      outer = newvar
      this
    }
  }

  object InnerSym {
    def apply[U <: Term with Subs[U]](variable: U with Symbolic): AnySym =
      if (isWitness(variable)) variable.name else new InnerSym[U](variable)
  }

  /**
    * Unwraps symbols if they are wrapped - wrapping typical happens in lambdas.
    */
  def outerSym(sym: Symbolic): Symbolic = sym.name match {
    case inn: InnerSym[_] => outerSym(inn.outer)
    case _                => sym
  }

  /**
    * variable of given type with string as in given variable.
    */
  /*  private def innervar[U <: Term with Subs[U]](variable: U): U = {
    val typ = variable.typ.asInstanceOf[Typ[U]]
    val newvar = InnerSym(variable)
    variable match {
      case PairTerm(a: Term, b: Term) => PairTerm(
        a.typ.symbObj(newvar),
        b.typ.symbObj(newvar)
      ).asInstanceOf[U]
      case ProdTyp(a: Term, b: Term) => ProdTyp(
        a.typ.symbObj(newvar),
        b.typ.symbObj(newvar)
      ).asInstanceOf[U]
      case DepPair(a: Term, b: Term, fibre) => DepPair[Term, Term](
        a.typ.symbObj(newvar),
        b.typ.symbObj(newvar), fibre.asInstanceOf[TypFamily[Term, Term]]
      ).asInstanceOf[U]
      case _ => typ.symbObj(newvar)
    }

  }*/

  /**
    * constructor for an (in general) dependent lambda;
    * creates a new variable and substitutes this in the value,
    * to avoid name collisions;
    * it is better style to use the syntactic sugar
    * e.g. {{{ val f = x :~> y }}}
    * Note the `:~>`, not `:->`
    *
    */
  def lambda[U <: Term with Subs[U], V <: Term with Subs[V]](
      variable: U
  )(value: V): FuncLike[U, V] = {
    // if (isVar(variable)) LambdaTerm(variable, value)
    // else {
    val newvar   = variable.newobj
    val newValue = value.replace(variable, newvar)
    if (newValue == value) LambdaFixed(witVar(variable), value)
    else if (value.typ != newValue.typ)
      LambdaTerm(newvar, newValue)
    else LambdaFixed(newvar, newValue)
    // }
  }

  /**
    * constructor for pi-Types;
    * creates a new variable and substitutes this in the value,
    * to avoid name collisions;
    * it is better style to use the syntactic sugar
    * e.g. {{{ val P = x ~>: A }}}
    * Note the `~>:`, not `->:`
    *
    */
  def piDefn[U <: Term with Subs[U], V <: Term with Subs[V]](
      variable: U
  )(value: Typ[V]): PiDefn[U, V] = {
    val newvar = variable.newobj
    PiDefn(variable.replace(variable, newvar), value.replace(variable, newvar))
  }

  /**
    * returns term as a (lambda) function of the variables in `vars` on which
    * it depends.
    */
  def lambdaClosure(vars: Vector[Term])(t: Term): Term =
    vars.foldRight(t) { case (v, a) => if (a.dependsOn(v)) v :~> a else a }

  /**
    * returns all partial lambda closures
    */
  def partialLambdaClosures(vars: Vector[Term])(t: Term): Vector[Term] =
    vars match {
      case Vector() => Vector(t)
      case x +: ys =>
        val tail = partialLambdaClosures(ys)(t)
        tail ++ (tail.collect {
          case z if z.dependsOn(x) => x :~> z
        })
    }

  def maxLambda(t: Term): (Vector[Term], Term) = t match {
    case lt: LambdaLike[u, v] =>
      val (xs, y) = maxLambda(lt.value)
      (lt.variable +: xs, y)
    case _ => (Vector(), t)
  }

  def getVars(t: Term, n: Int): (Vector[Term], Term) =
    if (n < 1) (Vector(), t)
    else
      t match {
        case lt: LambdaLike[u, v] =>
          val (xs, y) = getVars(lt.value, n - 1)
          (lt.variable +: xs, y)
        case ft: FuncLike[u, v] =>
          val x       = ft.dom.Var
          val (xs, y) = getVars(ft(x.asInstanceOf[u]), n - 1)
          (x +: xs, y)
        case _ =>
          throw new Exception(s"expected lambda but got $t with type ${t.typ}")
      }

  /**
    * returns type as a Pi-Type of the variables in `vars` on which
    * it depends.
    */
  def piClosure(vars: Vector[Term])(t: Typ[Term]): Typ[Term] =
    vars.foldRight(t) { case (v, a) => if (a.dependsOn(v)) v ~>: a else a }

  /**
    * constructor for a pure lambda, i.e., not a dependent function;
    * this gives a runtime error if the type of the variable depends on the value.
    * creates a new variable and substitutes this in the value,
    * to avoid name collisions;
    * it is better style to use the syntactic sugar
    * e.g. {{{ val f = x :-> y }}}
    *
    *
    */
  def lmbda[U <: Term with Subs[U], V <: Term with Subs[V]](
      variable: U
  )(value: V): Func[U, V] = {
    require(
      value.typ.indepOf(variable),
      s"lmbda returns function type but value $value has type ${value.typ} depending on variable $variable; you may wish to use lambda instead"
    )
    // if (isVar(variable)) LambdaFixed(variable, value)
    // else {
    val newvar   = variable.newobj
    val newValue = value.replace(variable, newvar)
    // assert(newvar != variable, s"new variable of type ${newvar.typ} not new")
    // assert(newvar.typ == variable.typ, s"variable $variable changed type")
    //    LambdaTypedFixed(newvar.typed, value.replace(variable, newvar).typed)
    if (newValue == value) LambdaFixed(witVar(variable), value)
    else LambdaFixed(newvar, newValue)
    // }
  }

  /**
    * the identity function defined as a lambda.
    */
  def id[U <: Term with Subs[U]](typ: Typ[U]): Func[U, U] = {
    val x = typ.Var
    lmbda(x)(x)
  }

  /**
    * lambda-like syntax for [[piDefn]]
    */
  def pi[U <: Term with Subs[U], V <: Term with Subs[V]](
      variable: U
  )(value: Typ[V]): Typ[FuncLike[U, V]] =
    if (value dependsOn variable) piDefn(variable)(value)
    else (variable.typ.asInstanceOf[Typ[U]] ->: value)

  /**
    * [[lambda]] if necessary, otherwise constant.
    */
  def optlambda(variable: Term): Term => Term =
    value => if (value dependsOn variable) lambda(variable)(value) else value

  def lambdaPair[U <: Term with Subs[U], V <: Term with Subs[V]](
      variable: U
  )(value: V): AbsPair[U, V] = {
    val fibre = lmbda(variable)(value.typ.asInstanceOf[Typ[V]])
    DepPair(variable, value, fibre)
  }

  /**
    * composition of functions, defined as a lambda
    */
  def composition[U <: Term with Subs[U], V <: Term with Subs[V], W <: Term with Subs[
    W
  ]](f: Func[V, W], g: Func[U, V]): Composition[U, V, W] = {
    // val x = g.dom.Var
    // LambdaFixed(x, f(g(x)))
    Composition(f, g)
  }

  case class Composition[U <: Term with Subs[U], V <: Term with Subs[V], W <: Term with Subs[
    W
  ]](f: Func[V, W], g: Func[U, V])
      extends Func[U, W]
      with Subs[Composition[U, V, W]] {
    val dom: Typ[U]        = g.dom
    val codom: Typ[W]      = f.codom
    val typ: FuncTyp[U, W] = dom ->: codom

    def newobj =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )
    def subs(x: Term, y: Term) = Composition(f.replace(x, y), g.replace(x, y))

    def act(a: U): W = f(g(a))
  }

  /**
    * Sigma type defined using [[lmbda]], so with new variable created.
    *
    */
  def sigma[U <: Term with Subs[U], V <: Term with Subs[V]](
      variable: U
  )(value: Typ[V]): Typ[AbsPair[U, V]] =
    if (value.dependsOn(variable)) {
      val fibre = lmbda(variable)(value)
      SigmaTyp(fibre)
    } else ProdTyp(variable.typ.asInstanceOf[Typ[U]], value)

  /**
    * type family
    */
  type TypFamily[W <: Term with Subs[W], +U <: Term with Subs[U]] =
    Func[W, Typ[U]]

  object PiDefn {
    def apply[W <: Term with Subs[W], U <: Term with Subs[U]](
        fibre: Func[W, Typ[U]]
    ): PiDefn[W, U] = fibre match {
      case LambdaFixed(variable, value) =>
        PiDefn(variable, value)
      case _ =>
        val x = fibre.dom.Var
        piDefn(x)(fibre(x))
    }
  }

  /**
    * Pi-Type, defined in terms of a variable and value, i.e.,
    * `\Pi_{x: X}Q` (latex) with `x` the `variable` and `Q` the `value`
    */
  case class PiDefn[W <: Term with Subs[W], U <: Term with Subs[U]](
      variable: W,
      value: Typ[U]
  ) extends GenFuncTyp(
        variable.typ.asInstanceOf[Typ[W]],
        (w: W) => value.replace(variable, w)
      )
      with Typ[FuncLike[W, U]]
      with Subs[PiDefn[W, U]] {
    //type Obj = DepFunc[W, U]
    type Obj = FuncLike[W, U]

    lazy val typ: Typ[Typ[Term]] = Universe(univlevel(value.typ))

    lazy val fibers = LambdaFixed(variable, value)

    override def variable(name: AnySym): FuncLike[W, U] =
      // DepSymbolicFunc(name, fibers)
      PiSymbolicFunc(name, variable, value)

    def newobj: PiDefn[W, U] = {
      val newvar = variable.newobj
      PiDefn(
        variable.replace(variable, newvar),
        value.replace(variable, newvar)
      )
    }

    def subs(x: Term, y: Term) =
      PiDefn(variable.replace(x, y), value.replace(x, y))

    override def toString = s"($variable : ${variable.typ} ) ~> ($value)"
  }

  /**
    *  For all/Product for a type family. This is the type of dependent functions.
    * deprecated in favour of [[PiDefn]]
    */
  @deprecated("Use PiDefn", "14/12/2016")
  case class PiTyp[W <: Term with Subs[W], U <: Term with Subs[U]](
      fibers: TypFamily[W, U]
  ) extends GenFuncTyp(fibers.dom, fibers)
      with Typ[FuncLike[W, U]]
      with Subs[PiTyp[W, U]] {
    //type Obj = DepFunc[W, U]
    type Obj = FuncLike[W, U]

    // severe deprecation
    throw new IllegalArgumentException(
      s"Error: PiTyp with fibers $fibers created"
    )

    lazy val typ: Typ[Typ[Term]] = Universe(
      max(univlevel(fibers.codom), univlevel(fibers.dom.typ))
    )

    override def variable(name: AnySym): FuncLike[W, U] =
      DepSymbolicFunc[W, U](name, fibers)

    def newobj = PiTyp(fibers.newobj)

    def subs(x: Term, y: Term): PiTyp[W, U] = PiTyp[W, U](fibers.replace(x, y))

    override def toString: String = Pi + "(" + fibers.toString + ")"
  }

  /**
    *  a dependent function - rarely used trait as not closed under substitution.
    */
  trait DepFunc[W <: Term with Subs[W], U <: Term with Subs[U]]
      extends FuncLike[W, U] {
    val fibers: TypFamily[W, U]

    type D = W

    type Cod = U

    def act(arg: W): U
  }

  /**
    * slightly different [[DepSymbolicFunc]] is used instead as this causes
    * some tests to fail
    */
  case class PiSymbolicFunc[W <: Term with Subs[W], U <: Term with Subs[U]](
      name: AnySym,
      variable: W,
      value: Typ[U]
  ) extends FuncLike[W, U]
      with Symbolic {

    val dom: Typ[W] = variable.typ.asInstanceOf[Typ[W]]

    val depcodom: W => Typ[U] = (arg: W) => value.replace(variable, arg)

    lazy val typ = PiDefn(variable, value)

    def act(arg: W): U = depcodom(arg).symbObj(ApplnSym(this, arg))

    def newobj: PiSymbolicFunc[W, U] = {
      // val newvar = variable.newobj
      PiSymbolicFunc(
        InnerSym[FuncLike[W, U]](this),
        variable, // newvar,
        value     //value.replace(variable, newvar)
      )
      // val fibers = LambdaFixed(variable, value)
      // DepSymbolicFunc(InnerSym[FuncLike[W, U]](this), fibers.newobj)
    }

    def subs(x: Term, y: Term): FuncLike[W, U] = (x, y) match {
      case (u, v: FuncLike[W, U]) if (u == this) => v
      case _ =>
        def symbobj(sym: AnySym) =
          PiSymbolicFunc(sym, variable.replace(x, y), value.replace(x, y))

        symSubs(symbobj)(x, y)(name)
    }

    // override def toString = s"""${name.toString} : (${typ.toString})"""
  }

  /**
    * Symbolic dependent function, acts formally.
    */
  case class DepSymbolicFunc[W <: Term with Subs[W], U <: Term with Subs[U]](
      name: AnySym,
      fibers: TypFamily[W, U]
  ) extends FuncLike[W, U]
      with Symbolic {

    val dom: Typ[W] = fibers.dom

    val depcodom: W => Typ[U] = (arg: W) => fibers(arg)

    lazy val typ = PiDefn(fibers)

    def act(arg: W): U = fibers(arg).symbObj(ApplnSym(this, arg))

    def newobj =
      DepSymbolicFunc(InnerSym[FuncLike[W, U]](this), fibers)

    def subs(x: Term, y: Term): FuncLike[W, U] = (x, y) match {
      //        case (u: Typ[_], v: Typ[_]) => SymbolicFunc(name, dom.replace(u, v), codom.replace(u, v))
      case (u, v: FuncLike[W, U]) if (u == this) => v
      case _ =>
        def symbobj(sym: AnySym) = DepSymbolicFunc(sym, fibers.replace(x, y))

        symSubs(symbobj)(x, y)(name)
    }

    // override def toString = s"""${name.toString} : (${typ.toString})"""
  }

  /** A dependent function defined by a scala funcion */
  class DepFuncDefn[W <: Term with Subs[W], U <: Term with Subs[U]](
      func: W => U,
      val dom: Typ[W],
      val fibers: TypFamily[W, U]
  ) extends DepFunc[W, U] {
    //	  // val domobjtpe = typeOf[W]

    //	  // val codomobjtpe = typeOf[U]

    val depcodom: W => Typ[U] = (arg: W) => fibers(arg)

    lazy val typ: PiDefn[W, U] = PiDefn[W, U](fibers)

    //	  def act(arg: W) = if (arg.typ == dom) Some(func(arg)) else None

    def act(arg: W): U = func(arg)

    def newobj: FuncLike[W, U] = typ.obj

    def subs(x: Term, y: Term) =
      new DepFuncDefn(
        (w: W) => func(w).replace(x, y),
        dom.replace(x, y),
        fibers.replace(x, y)
      )
  }

  case class OptDepFuncDefn[W <: Term with Subs[W]](
      func: W => Option[Term],
      dom: Typ[W]
  ) extends DepFunc[W, Term]
      with Subs[OptDepFuncDefn[W]] {

    lazy val depcodom: W => Typ[Term] = (arg: W) =>
      (func(arg) map (_.typ)).getOrElse(Unit)

    lazy val fibers: Func[W, Typ[Term]] = {
      val x = getVar(dom)
      lmbda(x)(depcodom(x))
    }

    lazy val typ: Typ[FuncLike[W, Term]] = PiDefn(fibers)

    def act(arg: W): Term = func(arg).getOrElse(Star)

    def newobj: OptDepFuncDefn[W] =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    def subs(x: Term, y: Term) =
      OptDepFuncDefn((w: W) => func(w) map (_.replace(x, y)), dom.replace(x, y))
  }

  /**
    *  symbol for left component in pair from a given symbol
    */
  case class LeftProjSym(name: AnySym) extends AnySym {
    def subs(x: Term, y: Term) = LeftProjSym(name.subs(x, y))

    override def toString: String = name.toString + "_1"
  }

  /**
    *  symbol for right component in pair from a given symbol
    */
  case class RightProjSym(name: AnySym) extends AnySym {
    def subs(x: Term, y: Term) = RightProjSym(name.subs(x, y))

    override def toString: String = name.toString + "_2"
  }

  /**
    *  Sigma Type, i.e., dependent pair type.
    */
  case class SigmaTyp[W <: Term with Subs[W], U <: Term with Subs[U]](
      fibers: TypFamily[W, U]
  ) extends Typ[DepPair[W, U]]
      with Subs[SigmaTyp[W, U]] { prod =>
    lazy val typ = Universe(
      max(univlevel(fibers.codom), univlevel(fibers.dom.typ))
    )

    type Obj = DepPair[W, U]

    def variable(name: AnySym): DepPair[W, U] = {
      val a = fibers.dom.symbObj(LeftProjSym(name))
      val b = fibers(a).symbObj(RightProjSym(name))
      DepPair(a, b, fibers)
    }

    /**
      * introduction rule for the Sigma type
      */
    lazy val paircons: FuncLike[W, Func[U, DepPair[W, U]]] = {
      val a = fibers.dom.Var
      val b = (fibers(a)).Var
      lambda(a)(lmbda(b)(DepPair(a, b, fibers)))
    }

    /**
      * projections from the Sigma-Type
      */
    lazy val (proj1, proj2) = {
      val x = this.Var
      (x :-> x.first, x :~> x.second)
    }

    def newobj: SigmaTyp[W, U] = SigmaTyp(fibers.newobj)

    def subs(x: Term, y: Term): SigmaTyp[W, U] =
      SigmaTyp[W, U](fibers.replace(x, y))

    override def toString: String = Sigma + "(" + fibers.toString + ")"

    /**
      * recursively defined function on `this`
      */
    case class RecFn[V <: Term with Subs[V]](
        codom: Typ[V],
        data: FuncLike[W, Func[U, V]]
    ) extends RecFunc[AbsPair[W, U], V] { self =>
      lazy val dom: Typ[AbsPair[W, U]] = prod: Typ[AbsPair[W, U]]

      lazy val typ: FuncTyp[AbsPair[W, U], V] = dom ->: codom

      val defnData: Vector[FuncLike[W, Func[U, V]]] = Vector(data)

      def fromData(data: Vector[Term]) =
        RecFn(codom, data.head.asInstanceOf[FuncLike[W, Func[U, V]]])

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      def subs(x: Term, y: Term): SigmaTyp[W, U]#RecFn[V] =
        SigmaTyp(fibers.replace(x, y))
          .RecFn(codom.replace(x, y), data.replace(x, y))

      def act(w: AbsPair[W, U]): V = w match {
        case DepPair(a, b, f) if f == fibers => data(a)(b)
        case _                               => codom.symbObj(ApplnSym(self, w))
      }
    }

    /**
      * recursive definition
      *
      * @param target the codomain
      */
    def rec[V <: Term with Subs[V]](
        target: Typ[V]
    ): Func[FuncLike[W, Func[U, V]], FuncLike[AbsPair[W, U], V]] = {
      val a = fibers.dom.Var
      val d = (a ~>: (fibers(a) ->: target)).Var
      d :-> (RecFn(target, d): FuncLike[AbsPair[W, U], V])
    }

    /**
      * inductive definition on the Sigma-type
      */
    case class InducFn[V <: Term with Subs[V]](
        targetFmly: FuncLike[W, Func[U, Typ[V]]],
        data: FuncLike[W, FuncLike[U, V]]
    ) extends InducFuncLike[AbsPair[W, U], V] { self =>
      lazy val dom: SigmaTyp[W, U] = prod

      val defnData: Vector[FuncLike[W, FuncLike[U, V]]] = Vector(data)

      def fromData(data: Vector[Term]) =
        InducFn(targetFmly, data.head.asInstanceOf[FuncLike[W, FuncLike[U, V]]])

      val xy: AbsPair[W, U] = prod.Var

      lazy val typ: GenFuncTyp[AbsPair[W, U], V] = xy ~>: (targetFmly(xy.first)(
        xy.second
      ))

      lazy val depcodom: Func[AbsPair[W, U], Typ[V]] = {
        val p: AbsPair[W, U] = prod.Var
        p :-> targetFmly(p.first)(p.second)
      }
//        (p: AbsPair[W, U]) => targetFmly(p.first)(p.second)

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      def subs(x: Term, y: Term) =
        SigmaTyp(fibers.replace(x, y))
          .InducFn(targetFmly.replace(x, y), data.replace(x, y))

      def act(w: AbsPair[W, U]): V = w match {
        case DepPair(a, b, f) if f == fibers => data(a)(b)
        case _                               => targetFmly(w.first)(w.second).symbObj(ApplnSym(self, w))
      }
    }

    /**
      * inductive definition
      *
      * @param targetFmly the codomain
      */
    def induc[V <: Term with Subs[V]](
        targetFmly: FuncLike[W, Func[U, Typ[V]]]
    ): Func[FuncLike[W, FuncLike[U, V]], FuncLike[AbsPair[W, U], V]] = {
      val xy     = prod.Var
      val (x, y) = (xy.first, xy.second)
      val d      = (x ~>: (y ~>: targetFmly(x)(y))).Var
      d :-> (InducFn(targetFmly, d): FuncLike[AbsPair[W, U], V])
    }
  }

  object Sgma {
    def apply[W <: Term with Subs[W], U <: Term with Subs[U]](
        variable: W,
        typ: Typ[U]
    ): SigmaTyp[W, U] = {
      val fibers = lmbda(variable)(typ)
      SigmaTyp(fibers)
    }
  }

  /**
    * Dependent pair (a: A, b : B(a)) - element of a Sigma type.
    *
    */
  case class DepPair[W <: Term with Subs[W], U <: Term with Subs[U]](
      first: W,
      second: U,
      fibers: TypFamily[W, U]
  ) extends Term
      with Subs[DepPair[W, U]]
      with AbsPair[W, U] {
    lazy val typ = SigmaTyp(fibers)

    def newobj: DepPair[W, U] = {
      val newfirst = first.newobj
      DepPair(
        newfirst,
        second.replace(first, newfirst).newobj,
        fibers.replace(first, newfirst)
      )
    }

    def indepComponents: Boolean =
      Try {
        val newFirst = first.newobj
        second.replace(first, newFirst) == second.replace(
          fibers(first),
          fibers(newFirst)
        )
      }.getOrElse(true)

    def subs(x: Term, y: Term): DepPair[W, U] =
      if (x == this) y.asInstanceOf[DepPair[W, U]]
      else
        DepPair(first.replace(x, y), second.replace(x, y), fibers.replace(x, y))
  }

  case class SymbEquality[U <: Term with Subs[U]](
      name: AnySym,
      typ: IdentityTyp[U]
  ) extends Equality[U]
      with Symbolic {
    override def toString: String = name.toString + " : (" + typ.toString + ")"

    def newobj = SymbEquality(InnerSym[Term](this), typ)

    def subs(x: Term, y: Term): Equality[U] =
      if (x == this) y.asInstanceOf[Equality[U]]
      else {
        def symbobj(sym: AnySym): Equality[U] =
          (typ.replace(x, y): IdentityTyp[U]).symbObj(sym)
        symSubs(symbobj)(x, y)(name)
        // typ.replace(x, y).symbObj(name.subs(x, y))
      }
  }

  trait Equality[U <: Term with Subs[U]] extends Term with Subs[Equality[U]] {
    self =>
    val typ: IdentityTyp[U]

    import IdentityTyp._

    val lhs: U = typ.lhs

    val rhs: U = typ.rhs

    lazy val sym: Equality[U] = symm(typ.dom)(lhs)(rhs)(self)

    def &&(that: Equality[U]): Equality[U] =
      trans(typ.dom)(lhs)(rhs)(that.rhs)(this)(that)

    def *:[V <: Term with Subs[V]](f: Func[U, V]): Equality[V] =
      induced(f)(lhs)(rhs)(self)

    def lift[V <: Term with Subs[V]](f: Func[U, Typ[V]]): Func[V, V] =
      transport(f)(lhs)(rhs)(self)
  }

  /**
    * The identity type.
    *  This is the type `lhs = rhs`
    */
  case class IdentityTyp[U <: Term with Subs[U]](dom: Typ[U], lhs: U, rhs: U)
      extends Typ[Equality[U]]
      with Subs[IdentityTyp[U]] {
    type Obj = Equality[U]

    lazy val typ = Universe(max(univlevel(lhs.typ.typ), univlevel(rhs.typ.typ)))

    def newobj: IdentityTyp[U] = {
      val newlhs = lhs.newobj
      IdentityTyp(dom replace (lhs, newlhs), newlhs, rhs replace (lhs, newlhs))
    }

    def subs(x: Term, y: Term) =
      IdentityTyp(dom.replace(x, y), lhs.replace(x, y), rhs.replace(x, y))

    def variable(name: AnySym) = SymbEquality(name, this)

    override def toString = s"$lhs = $rhs"

    /**
      * recursive definition on the identity type family
      */
    @deprecated("misleading since identity is an idexed family", "forever")
    def rec[UU >: U <: Term with Subs[UU], V <: Term with Subs[V]](
        codom: Typ[V]
    ): Func[Func[UU, V], FuncLike[UU, FuncLike[UU, Func[Equality[UU], V]]]] =
      IdentityTyp.rec(dom: Typ[UU], codom)

    /**
      * inductive definition on the identity type family
      */
    @deprecated("misleading since identity is an indexed family", "forever")
    def induc[UU >: U <: Term with Subs[UU], V <: Term with Subs[V]](
        targetFmly: FuncLike[UU, FuncLike[UU, FuncLike[Equality[UU], Typ[V]]]]
    ): Func[FuncLike[UU, V], FuncLike[
      UU,
      FuncLike[UU, Func[Equality[UU], V]]
    ]] =
      IdentityTyp.induc(dom: Typ[UU], targetFmly)
  }

  /**
    * the `reflexivity` term with type an equality `value = value`
    */
  case class Refl[U <: Term with Subs[U]](dom: Typ[U], value: U)
      extends Equality[U]
      with Subs[Refl[U]] {
    lazy val typ = IdentityTyp(dom, value, value)

    def subs(x: Term, y: Term) = Refl(dom.replace(x, y), value.replace(x, y))

    def newobj: Refl[U] = {
      val newvalue = value.newobj
      Refl(newvalue.typ.asInstanceOf[Typ[U]], newvalue)
    }
  }

  /**
    * Operations on terms
    */
  implicit class RichTerm[U <: Term with Subs[U]](term: U) {

    /**
      * equality type 'term = rhs'
      */
    def =:=(rhs: U): IdentityTyp[U] = {
      require(
        term.typ == rhs.typ,
        "mismatched types for equality " + term.typ + " and " + rhs.typ
      )
      IdentityTyp(term.typ.asInstanceOf[Typ[U]], term, rhs)
    }

    /**
      * constructor for (pure) lambda functions, see [[lmbda]]
      */
    def :->[V <: Term with Subs[V]](that: V): Func[U, V] = lmbda(term)(that)

    /**
      * constructor for (in general dependent) lambda functions, see [[lambda]]
      */
    def :~>[V <: Term with Subs[V]](that: V): FuncLike[U, V] =
      lambda(term)(that)

    /**
      * reflexivity term `refl : term = term`
      */
    def refl: Refl[U] = Refl(term.typ.asInstanceOf[Typ[U]], term)
  }

  object IdentityTyp {
    def get[U <: Term with Subs[U]](lhs: U, rhs: U): IdentityTyp[U] = {
      require(
        lhs.typ == rhs.typ,
        "mismatched types for equality " + lhs.typ + " and " + rhs.typ
      )
      IdentityTyp(lhs.typ.asInstanceOf[Typ[U]], lhs, rhs)
    }

    /**
      * recursive definition on identity type families
      */
    case class RecFn[U <: Term with Subs[U], V <: Term with Subs[V]](
        domain: Typ[U],
        target: Typ[V],
        data: Func[U, V],
        start: U,
        end: U
    ) extends IndRecFunc[Term, V, Func[U, Func[U, Typ[Term]]]] { self =>
      lazy val dom: IdentityTyp[U] = (start =:= end)

      lazy val codom: Typ[V] = target

      lazy val domW: Func[U, Func[U, IdentityTyp[U]]] = {
        val x = domain.Var
        val y = domain.Var
        x :-> (y :-> (x =:= y))
      }

      val index: Vector[U] = Vector(start, end)

      val defnData: Vector[Func[U, V]] = Vector(data)

      def fromData(data: Vector[Term]) =
        RecFn(domain, target, data.head.asInstanceOf[Func[U, V]], start, end)

      lazy val typ: FuncTyp[Term, V] = dom ->: codom

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      def subs(x: Term, y: Term) =
        RecFn(
          domain.replace(x, y),
          target.replace(x, y),
          data.replace(x, y),
          start.replace(x, y),
          end.replace(x, y)
        )

      def act(t: Term): V =
        if (start == end && t == Refl(dom, start)) data(start)
        else target.symbObj(ApplnSym(self, t))
    }

    /**
      * recursive definition for identity type family
      */
    def rec[U <: Term with Subs[U], V <: Term with Subs[V]](
        dom: Typ[U],
        target: Typ[V]
    ): Func[Func[U, V], FuncLike[U, FuncLike[U, Func[Equality[U], V]]]] = {
      val dataVar = (dom ->: target).Var
      val x       = dom.Var
      val y       = dom.Var
      val p       = (x =:= y).Var
      dataVar :-> (x :~> (y :~> (p :-> RecFn(dom, target, dataVar, x, y)(p))))
    }

    /**
      * inductive definition for identity type family.
      */
    case class InducFn[U <: Term with Subs[U], V <: Term with Subs[V]](
        domain: Typ[U],
        targetFmly: FuncLike[U, FuncLike[U, FuncLike[Equality[U], Typ[V]]]],
        data: FuncLike[U, V],
        start: U,
        end: U
    ) extends IndInducFuncLike[Equality[U], V, Func[U, Func[U, Typ[Term]]], FuncLike[
          U,
          FuncLike[U, FuncLike[Equality[U], Typ[V]]]
        ]] { self =>
      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )

      val defnData: Vector[FuncLike[U, V]] = Vector(data)

      def fromData(data: Vector[Term]) =
        InducFn(
          domain,
          targetFmly,
          data.head.asInstanceOf[FuncLike[U, V]],
          start,
          end
        )

      lazy val domW: Func[U, Func[U, IdentityTyp[U]]] = {
        val x = domain.Var
        val y = domain.Var
        x :-> (y :-> (x =:= y))
      }

      val codXs: FuncLike[U, FuncLike[U, FuncLike[Equality[U], Typ[V]]]] =
        targetFmly

      val index: Vector[U] = Vector(start, end)

      def subs(x: Term, y: Term) =
        InducFn(
          domain.replace(x, y),
          targetFmly.replace(x, y),
          data.replace(x, y),
          start.replace(x, y),
          end.replace(x, y)
        )

      //      val a = domain.Var

      //      val dom = a ~>:(targetFmly(a)(a)(Refl(domain, a)))

      lazy val dom: IdentityTyp[U] = start =:= end

      lazy val p: Equality[U] = dom.Var

      lazy val typ: GenFuncTyp[Equality[U], V] = p ~>: (targetFmly(start)(end)(
        p
      ))

      lazy val depcodom: Func[Equality[U], Typ[V]] = p :-> targetFmly(start)(
        end
      )(p)

      def act(t: Equality[U]): V =
        if (start == end && t == Refl(domain, start)) data(start)
        else targetFmly(start)(end)(t).symbObj(ApplnSym(self, t))
    }

    /**
      * inductive definition for identity type family.
      */
    def induc[U <: Term with Subs[U], V <: Term with Subs[V]](
        domain: Typ[U],
        targetFmly: FuncLike[U, FuncLike[U, FuncLike[Equality[U], Typ[V]]]]
    ): Func[FuncLike[U, V], FuncLike[U, FuncLike[U, Func[Equality[U], V]]]] = {

      val a = domain.Var

      val dataVar = (a ~>: (targetFmly(a)(a)(Refl(domain, a)))).Var

      val x = domain.Var

      val y = domain.Var

      val p = (x =:= y).Var

      dataVar :->
        (x :~> (y :~> (p :-> InducFn(domain, targetFmly, dataVar, x, y)(p))))
    }

    /**
      * symmetry term of type `x = y -> y = x` as a function of `x` and `y`
      */
    def symm[U <: Term with Subs[U]](
        dom: Typ[U]
    ): FuncLike[U, FuncLike[U, Func[Equality[U], Equality[U]]]] = {
      val x         = dom.Var
      val y         = dom.Var
      val p         = IdentityTyp(dom, x, y).Var
      val typFamily = lambda(x)(lambda(y)(lmbda(p)(y =:= x)))
      val inducFn   = induc(dom, typFamily)
      val baseCase  = lambda(x)(Refl(dom, x))
      inducFn(baseCase)
    }

    def preTrans[U <: Term with Subs[U]](dom: Typ[U]): FuncLike[U, FuncLike[
      U,
      FuncLike[U, Func[Equality[U], Func[Equality[U], Equality[U]]]]
    ]] = {
      val x         = dom.Var
      val y         = dom.Var
      val z         = dom.Var
      val p         = IdentityTyp(dom, x, y).Var
      val typFamily = lambda(x)(lambda(y)(lmbda(p)((y =:= z) ->: (x =:= z))))
      val inducFn   = induc(dom, typFamily)
//      val q         = (x =:= x).Var
      val baseCase = lambda(x)(id(x =:= z))
      lambda(z)(inducFn(baseCase))
    }

    /**
      * transitivity term of type `x = y -> y = z -> x = z` as a function of `x`, `y` and `z`
      */
    def trans[U <: Term with Subs[U]](dom: Typ[U]): FuncLike[U, FuncLike[
      U,
      FuncLike[U, Func[Equality[U], Func[Equality[U], Equality[U]]]]
    ]] = {
      val x = dom.Var
      val y = dom.Var
      val z = dom.Var
      x :~> (y :~> (z :~> (IdentityTyp.preTrans(dom)(z)(x)(y))))
    }

    /**
      * equality induced by a (pure) function
      * term with type `x =y -> f(x) = f(y)` as function of `x` and `y`
      */
    def induced[U <: Term with Subs[U], V <: Term with Subs[V]](
        f: Func[U, V]
    ): FuncLike[U, FuncLike[U, Func[Equality[U], Equality[V]]]] = {
      val x         = f.dom.Var
      val y         = f.dom.Var
      val p         = IdentityTyp(f.dom, x, y).Var
      val typFamily = lambda(x)(lambda(y)(lmbda(p)((f(x) =:= f(y)))))
      val inducFn   = induc(f.dom, typFamily)
      val image     = Refl(f.codom, f(x)): Equality[V]
      val baseCase  = lambda(x)(image)
      inducFn(baseCase)
    }

    /**
      * transport: term with type `x = y -> f(x) -> f(y)` as function of `x` and `y`
      */
    def transport[U <: Term with Subs[U], V <: Term with Subs[V]](
        f: Func[U, Typ[V]]
    ): FuncLike[U, FuncLike[U, Func[Equality[U], Func[V, V]]]] = {
      val x         = f.dom.Var
      val y         = f.dom.Var
      val p         = IdentityTyp(f.dom, x, y).Var
      val typFamily = lambda(x)(lambda(y)(lmbda(p)((f(x) ->: f(y)))))
      val inducFn   = induc(f.dom, typFamily)
      val baseCase  = x :~> (id(f(x)))
      inducFn(baseCase)
    }

    def apf[U <: Term with Subs[U], V <: Term with Subs[V]](
        f: Func[U, V]
    ): FuncLike[U, FuncLike[U, Func[Equality[U], Equality[V]]]] = {
      val x         = f.dom.Var
      val y         = f.dom.Var
      val p         = IdentityTyp(f.dom, x, y).Var
      val typFamily = lambda(x)(lambda(y)(lmbda(p)((f(x) =:= f(y)))))
      val inducFn   = induc(f.dom, typFamily)
      val baseCase  = x :~> (f(x).refl)
      inducFn(baseCase)
    }

    lazy val A: Typ[Term]        = Type.Var
    lazy val x: Term             = A.Var
    lazy val B: Typ[Term]        = Type.Var
    lazy val f: Func[Term, Term] = (A ->: B).Var

    lazy val symmTerm: FuncLike[Typ[Term], FuncLike[
      Term,
      FuncLike[Term, Func[Equality[Term], Equality[Term]]]
    ]] = lambda(A)(symm(A))
    lazy val transTerm: FuncLike[Typ[Term], FuncLike[Term, FuncLike[
      Term,
      FuncLike[Term, Func[Equality[Term], Func[Equality[Term], Equality[Term]]]]
    ]]] = lambda(A)(trans(A))
    lazy val reflTerm: FuncLike[Typ[Term], FuncLike[Term, Refl[Term]]] =
      lambda(A)(lambda(x)(Refl(A, x)))
    lazy val apfTerm: FuncLike[Typ[Term], FuncLike[Typ[Term], FuncLike[
      Func[Term, Term],
      FuncLike[Term, FuncLike[Term, Func[Equality[Term], Equality[Term]]]]
    ]]] = lambda(A)(lambda(B)(lambda(f)(apf(f))))

  }

  //	implicit def richTerm(term: Term with Subs[Term]) = RichTerm(term)

  //	implicit def richTyp(typ: Typ[Term] with Subs[Typ[Term]]) = RichTerm(typ)
  //	implicit def richTyp(typ: Typ[Term] with Subs[Typ[Term]]) = RichTerm(typ)

  object PlusTyp {

    /**
      * A -> A + B
      */
    case class FirstIncl[U <: Term with Subs[U], V <: Term with Subs[V]](
        typ: PlusTyp[U, V],
        value: U
    ) extends Term
        with Subs[FirstIncl[U, V]] {

      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        ) //FirstIncl(typ, value.newobj)

      def subs(x: Term, y: Term) =
        FirstIncl(typ.replace(x, y), value.replace(x, y))
    }

    /**
      * B -> A + B
      */
    case class ScndIncl[U <: Term with Subs[U], V <: Term with Subs[V]](
        typ: PlusTyp[U, V],
        value: V
    ) extends Term
        with Subs[ScndIncl[U, V]] {
      def newobj =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        ) //ScndIncl(typ, value.newobj)

      def subs(x: Term, y: Term) =
        ScndIncl(typ.replace(x, y), value.replace(x, y))
    }

    /**
      * recursive definition for co-product
      */
    case class RecFn[U <: Term with Subs[U], V <: Term with Subs[V], W <: Term with Subs[
      W
    ]](
        first: Typ[U],
        second: Typ[V],
        codom: Typ[W],
        firstCase: Func[U, W],
        secondCase: Func[V, W]
    ) extends RecFunc[Term, W] {
      val defnData: Vector[Term] = Vector(firstCase, secondCase)

      def fromData(data: Vector[Term]) =
        RecFn(
          first,
          second,
          codom,
          data(0).asInstanceOf[Func[U, W]],
          data(1).asInstanceOf[Func[V, W]]
        )

      def act(x: Term): W = x match {
        case PlusTyp.FirstIncl(tp, y) if tp == (first || second) =>
          firstCase(y.asInstanceOf[U])
        case PlusTyp.ScndIncl(tp, y) if tp == (first || second) =>
          secondCase(y.asInstanceOf[V])
        case _ =>
          codom.symbObj(ApplnSym(this, x))
      }

      lazy val typ: FuncTyp[Term, W] = dom ->: codom

      def subs(x: Term, y: Term) =
        RecFn(
          first.replace(x, y),
          second.replace(x, y),
          codom.replace(x, y),
          firstCase.replace(x, y),
          secondCase.replace(x, y)
        )

      val dom: provingground.HoTT.Typ[provingground.HoTT.Term] =
        PlusTyp(first, second)

      def newobj: Func[Term, W] =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )
    }
  }

  /**
    * coproduct type A + B
    */
  case class PlusTyp[U <: Term with Subs[U], V <: Term with Subs[V]](
      first: Typ[U],
      second: Typ[V]
  ) extends Typ[Term]
      with Subs[PlusTyp[U, V]] { plustyp =>
    def i(value: U) = PlusTyp.FirstIncl(this, value)

    def j(value: V) = PlusTyp.ScndIncl(this, value)

    lazy val incl1: Func[U, PlusTyp.FirstIncl[U, V]] = {
      val a = first.Var
      lmbda(a)(i(a))
    }

    lazy val incl2: Func[V, PlusTyp.ScndIncl[U, V]] = {
      val a = second.Var
      lmbda(a)(j(a))
    }

    /**
      * recursive definition on the co-product
      */
    def rec[W <: Term with Subs[W]](
        codom: Typ[W]
    ): Func[Func[U, W], Func[Func[V, W], Func[Term, W]]] = {
      val firstData  = (first ->: codom).Var
      val secondData = (second ->: codom).Var
      firstData :->
        (secondData :->
          (PlusTyp.RecFn(first, second, codom, firstData, secondData): Func[
            Term,
            W
          ]))
    }

    /**
      * inductive definition on the co-product
      */
    case class InducFn[W <: Term with Subs[W]](
        depcodom: Func[Term, Typ[W]],
        firstCase: FuncLike[U, W],
        secondCase: FuncLike[V, W]
    ) extends InducFuncLike[Term, W] {
      val defnData: Vector[Term] = Vector(firstCase, secondCase)

      def fromData(data: Vector[Term]) =
        InducFn(
          depcodom,
          firstCase.asInstanceOf[FuncLike[U, W]],
          secondCase.asInstanceOf[FuncLike[V, W]]
        )

      def act(x: Term): W = x match {
        case PlusTyp.FirstIncl(tp, y) if tp == plustyp =>
          firstCase(y.asInstanceOf[U])
        case PlusTyp.ScndIncl(tp, y) if tp == plustyp =>
          secondCase(y.asInstanceOf[V])
        case _ =>
          depcodom(x).symbObj(ApplnSym(this, x))
      }

      lazy val typ: PiDefn[Term, W] = PiDefn(depcodom)

      def subs(x: Term, y: Term): PlusTyp[U, V]#InducFn[W] =
        PlusTyp(first.replace(x, y), second.replace(x, y)).InducFn(
          depcodom.replace(x, y),
          firstCase.replace(x, y),
          secondCase.replace(x, y)
        )

      lazy val dom: provingground.HoTT.Typ[provingground.HoTT.Term] = plustyp

      def newobj: FuncLike[Term, W] =
        throw new IllegalArgumentException(
          s"trying to use the constant $this as a variable (or a component of one)"
        )
    }

    /**
      * inductive definition on the coproduct
      */
    def induc[W <: Term with Subs[W]](
        depcodom: Func[Term, Typ[W]]
    ): FuncLike[FuncLike[U, W], FuncLike[FuncLike[V, W], FuncLike[Term, W]]] = {
      val (a, b)     = (first.Var, second.Var)
      val firstData  = (a ~>: depcodom(incl1(a))).Var
      val secondData = (b ~>: depcodom(incl2(b))).Var
      firstData :~>
        (secondData :~>
          (InducFn(depcodom, firstData, secondData): FuncLike[Term, W]))
    }

    def subs(x: Term, y: Term) =
      PlusTyp(first.replace(x, y), second.replace(x, y))

    def newobj: PlusTyp[U, V] =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    type Obj = Term

    lazy val typ: Universe = Type

    def variable(name: AnySym) = SymbObj(name, this)
  }

  /**
    * folds in as many terms of the list as possible,
    * applying terms as long as the result is a function and the list is non-empty.
    */
  def foldterms: (Term, List[Term]) => Term = {
    case (f: FuncLike[u, _], x :: ys) if f.canApply(x.asInstanceOf[u]) =>
      foldterms(f.applyUnchecked(x.asInstanceOf[u]), ys)
    case (t, _) => t
  }

  /**
    * fold using function application after applying functions;
    * used mainly when type information is lost (at runtime).
    */
  def fold(fn: Term)(args: Term*): Term = (fn, args.toList) match {
    case (t, List()) => t
    case (f: FuncLike[u, _], x :: ys) if f.canApply(x.asInstanceOf[u]) =>
      fold(f.applyUnchecked(x.asInstanceOf[u]))(ys: _*)
    case (f: FuncLike[u, _], x :: ys) if isWitness(x) =>
      fold(f)(ys: _*)
    case (t, x :: ys) if isWitness(x) =>
      fold(t)(ys: _*)
    case (f: FuncLike[u, _], x :: ys) =>
      throw new ApplnFailException(f, x)
    case (t, x :: ys) =>
      throw new IllegalArgumentException(
        s"attempting to apply $t, which is not a function, to $x (and then to $ys)"
      )
  }

  def foldOpt(fn: Term)(args: Term*): Option[Term] = (fn, args.toList) match {
    case (t, List()) => Some(t)
    case (f: FuncLike[u, _], x :: ys) if f.dom == x.typ =>
      foldOpt(f.applyUnchecked(x.asInstanceOf[u]))(ys: _*)
    case (f: FuncLike[u, _], x :: ys) if isWitness(x) =>
      foldOpt(f)(ys: _*)
    case (t, x :: ys) if isWitness(x) =>
      foldOpt(t)(ys: _*)
    case (f: FuncLike[u, _], x :: ys) =>
      None
    case (t, x :: ys) =>
      None
  }

  /**
    * convenience methods to apply methods of special scala types such as functions
    * without the compiler knowing that the object has the special type.
    */
  object Fold {
    implicit class Folder[U <: Term with Subs[U]](fn: U) {
      def apply(args: Term*): Term = fold(fn)(args: _*)
    }

    def domain: Term => Typ[Term] = {
      case fn: FuncLike[u, v] => fn.dom
    }

    def variable: Term => Term = {
      case l: LambdaLike[_, _] => l.variable
    }

    def value: Term => Term = {
      case l: LambdaLike[_, _] => l.value
    }

    def defnData: Term => Vector[Term] = {
      case rf: RecFunc[u, v]       => rf.defnData
      case rf: InducFuncLike[u, v] => rf.defnData
    }
  }

  /**
    * folds in as many terms with names given by the list as possible,
    * applying terms as long as the result is a function and the list is non-empty.
    */
  def foldnames: (Term, List[AnySym]) => Term = {
    case (f: FuncLike[u, _], x :: ys) =>
      val newvar = f.dom.symbObj(x)
      foldnames(f(newvar.asInstanceOf[u]), ys)
    case (t, _) => t
  }

  /**
    * repeatedly apply lambda
    */
  def polyLambda(variables: List[Term], value: Term): Term = variables match {
    case List()  => value
    case x :: ys => lambda(x)(polyLambda(ys, value))
  }

  /**
    * Symbol factory
    */
  def nextChar(s: Traversable[Char]): Char =
    if (s.isEmpty) 'a' else (s.max + 1).toChar

  /**
    * Helper for symbol factory
    */
  def usedChars(s: Traversable[Term]): Traversable[Char] = {
    def charOpt(obj: Term): Option[Char] = obj match {
      case sym: Symbolic => Try(sym.name.asInstanceOf[Char]).toOption
      case _             => None
    }

    s collect (Function.unlift(charOpt))
  }

  /**
    * get variable from symbol factory
    */
  def nextVar(s: Traversable[Term])(typ: Typ[Term]): Term = {
    typ.symbObj(Name(nextChar(usedChars(s)).toString))
  }

  //  def nextName(name: String): String = {
  //    if (name == "") "a"
  //    else if (name.takeRight(1) == "z") nextName(name.dropRight(1)) + "a"
  //    else (name.dropRight(1)) + (name.toCharArray.last + 1).toChar.toString
  //  }

  def nextName(name: String): String =
    if (name == "") "a"
    else {
      if (name.endsWith("z")) nextName(name.dropRight(1)) + "a"
      else name.dropRight(1) + (name.toCharArray.last + 1).toChar.toString
    }

  def prefixedNextName(fullname: String): String = {
    val (prefix, name) = fullname.splitAt(1)
    prefix + nextName(name)
  }

  /**
    * return variable with name from factory.
    */
  def getVar[U <: Term with Subs[U]](
      typ: Typ[U]
  )(implicit factory: NameFactory): U =
    typ.symbObj(factory.get)

  /**
    * returns whether term is a variable
    */
  def isVar(t: Term): Boolean = t match {
    case sym: Symbolic if sym.name.toString.startsWith("$") => true
    case _                                                  => false
  }

  /**
    * returns whether term is a function
    */
  def isFunc: Term => Boolean = {
    case _: FuncLike[_, _] => true
    case _                 => false
  }

  /**
    * returns whether term is a type
    */
  def isTyp: Term => Boolean = {
    case _: Typ[_] => true
    case _         => false
  }

  /**
    * returns whether term is a type family
    */
  def isTypFamily: Term => Boolean = {
    // case f: Func[u, v]     => isTyp(f.codom) || isTypFamily(f.codom)
    case f: FuncLike[u, v] =>
      val y = f(f.dom.Var)
      isTyp(y) || isTypFamily(y)
    case _ => false
  }

  def typFamilyDepth: Term => Option[Int] = {
    case tp: Typ[_]        => Some(0)
    case f: Func[u, v]     => typFamilyDepth(f.codom).map(_ + 1)
    case f: FuncLike[u, v] => typFamilyDepth(f.depcodom(f.dom.Var)).map(_ + 1)
    case _                 => None
  }

  def typFamilyTarget: Term => Option[Typ[Term]] = {
    case tp: Typ[_] => Some(tp ->: Type)
    case f: FuncLike[u, v] =>
      val x = f.dom.Var
      typFamilyTarget(f(x.asInstanceOf[u])).map(tp => x ~>: tp)
    case _ => None
  }

  def getTypFamily(dom: Term, target: Typ[Term]): Option[Term] =
    (dom, target) match {
      case (typ: Typ[u], gf: GenFuncTyp[v, w]) if gf.domain == typ =>
        val x = typ.Var
        Some(x :-> gf.fib(x.asInstanceOf[v]))
      case (fn: FuncLike[x, y], gf: GenFuncTyp[v, w]) if gf.domain == fn.dom =>
        val x = fn.dom.Var
        getTypFamily(fn(x.asInstanceOf[x]), gf.fib(x.asInstanceOf[v])).map {
          y =>
            x :~> y
        }
      case _ => None
    }

  /**
    * returns whether term is a universe
    */
  def isUniv(x: Term): Boolean = x match {
    case tp: Typ[u] => isTyp(tp.obj)
    case _          => false
  }

  /**
    * converts a general function `f` to a lambda `x :-> f(x)`;
    * if f is already a lambda, returns `f` without boxing
    *
    */
  def funcToLambda[U <: Term with Subs[U], V <: Term with Subs[V]](
      fn: FuncLike[U, V]
  ): LambdaLike[U, V] = fn match {
    case l: LambdaLike[U, V] => l
    case f: Func[U, V] =>
      val x = f.dom.Var
      LambdaFixed(x, f(x))
    case f: FuncLike[U, V] =>
      val x = f.dom.Var
      LambdaTerm(x, f(x))
  }

  def asLambdas[U <: Term with Subs[U]](term: U): Option[U] = term match {
    case LambdaFixed(x: Term, y: Term) =>
      for (z <- asLambdas(y); w <- Try(lmbda(x)(z).asInstanceOf[U]).toOption)
        yield w
    case LambdaTerm(x: Term, y: Term) =>
      for (z <- asLambdas(y); w <- Try(lambda(x)(z).asInstanceOf[U]).toOption)
        yield w
    case fn: Func[u, v] =>
      val x = fn.dom.Var
      val y = fn(x)
      Try(lmbda(x)(y).asInstanceOf[U]).toOption flatMap (asLambdas)
    case fn: FuncLike[u, v] =>
      val x = fn.dom.Var
      val y = fn(x)
      Try(lambda(x)(y).asInstanceOf[U]).toOption flatMap (asLambdas)
    case _ => None
  }

  /**
    * returns `Some(x)` if the term is  of the form `f(x)` with `f` given,
    * otherwise None
    * @param func function f to match for f(x)
    **/
  def getArg[D <: Term with Subs[D], U <: Term with Subs[U]](
      func: FuncLike[D, U]
  ): Term => Option[D] = {
    case sym: Symbolic =>
      sym.name match {
        case fx: ApplnSym[u, w] =>
          val x = func.dom.Var
          if (fx.func.dom == func.dom &&
              fx.func(x.asInstanceOf[u]) == func(x) && fx.arg.typ == func.dom)
            Try(Some(fx.arg.asInstanceOf[D])).getOrElse(None)
          else getArg(func)(fx.func)
        case _ =>
          if (isProp(func.dom) && isProp(sym.typ))
            Some(("_" :: func.dom))
          else None
      }
    case res =>
      if (isProp(func.dom) && isProp(res.typ))
        Some(("_" :: func.dom))
      else None
  }

  /**
    * returns variab in the term, converting functions to lambdas if needed.
    */
  def getVariables(n: Int)(t: Term): List[Term] =
    if (n == 0) List()
    else
      t match {
        case fn: FuncLike[u, v] =>
          val l = funcToLambda(fn)
          l.variable :: getVariables(n - 1)(l.value)
      }

  def getTypVariables(n: Int)(t: Term): List[Typ[Term]] =
    getVariables(n)(t) map { case t: Typ[u] => t }

  /**
    * Just a wrapper to allow singleton objects
    */
  class Cnst(val term: Term) extends Term {
    //    val value = term

    val typ: Typ[Term] = term.typ

    def subs(x: Term, y: Term): Cnst = this

    def newobj: Cnst =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )
  }

  class CnstFunc[U <: Term with Subs[U], V <: Term with Subs[V]](
      override val term: Func[U, V]
  ) extends Cnst(term)
      with Func[U, V] {
    //    override val value : Func[U, V] = term

    def act(x: U): V = term(x)

    val dom: Typ[U] = term.dom

    val codom: Typ[V] = term.codom

    override val typ: FuncTyp[U, V] = dom ->: codom

    override def newobj: CnstFunc[U, V] =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    override def subs(x: Term, y: Term): CnstFunc[U, V] = this
  }

  class CnstFuncLike[U <: Term with Subs[U], V <: Term with Subs[V]](
      override val term: FuncLike[U, V]
  ) extends Cnst(term)
      with FuncLike[U, V] {
    //    override val value:

    def act(x: U): V = term(x)

    val dom: Typ[U] = term.dom

    override val typ: Typ[FuncLike[U, V]] = term.typ

    val depcodom: U => Typ[V] = term.depcodom

    override def newobj: CnstFuncLike[U, V] =
      throw new IllegalArgumentException(
        s"trying to use the constant $this as a variable (or a component of one)"
      )

    override def subs(x: Term, y: Term): CnstFuncLike[U, V] = this
  }

}
