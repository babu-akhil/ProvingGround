package provingground.library

import provingground._

import learning._

import HoTT._

object Monoid {
  val M = "M" :: Type

  val sym = IdentityTyp.symm(M)

  val trans = IdentityTyp.trans(M)

  val a = "a" :: M
  val b = "b" :: M
  val c = "c" :: M

  val l = "e_l" :: M

  val r = "e_r" :: M

  val op = "_*_" :: M ->: M ->: M

  val leftId  = "leftIdAx" :: (a ~>: (op(l)(a) =:= a))
  val rightId = "rightIdAx" :: (a ~>: (op(a)(r) =:= a))

  val refl = lambda(a)(Refl(M, a))

  val idUnique = b ~>: ((a ~>: (op(b)(a) =:= a)) ->: (l =:= b))

  val assoc =
    "assoc" :: (a ~>: (b ~>: (c ~>: (op(a)(op(b)(c)) =:= op(op(a)(b))(c)))))

  val MVars = Vector[Term](a, b, c) map (Weighted(_, 1.0 / 3.0))

  val X = "X" :: Type

  val Y = "Y" :: Type

  val f = "f" :: X ->: Y

  val g = "g" :: X ->: Y

  val x = "x" :: X

  val extensionality = lambda(X)(lambda(Y)(lambda(f)(IdentityTyp.induced(f))))

  val transfer = (X ~>:
    (Y ~>: (f ~>: (g ~>: (x ~>: ((f =:= g) ->: (f(x) =:= g(x)))))))).Var

  val names = Vector( //refl -> "id.reflexivity",
    //                  sym -> "id.symmetry",
    //                  trans -> "id.transitivity",
    //                   extensionality -> "id.extendsionality",
    transfer -> "id.transfer")

  val elTyps = a :-> (b :-> (a =:= b))

  val vars = Vector(M, a, b, c)

  val dist = FiniteDistribution.unif(M,
                                     a,
                                     b,
                                     c,
                                     l,
                                     r,
                                     op,
                                     leftId,
                                     rightId,
                                     refl,
                                     sym,
                                     trans,
                                     assoc,
                                     extensionality,
                                     transfer)

  val elemDist =
    (FiniteDistribution.unif(a,
                             b,
                             c,
                             l,
                             r,
                             op,
                             leftId,
                             rightId,
                             //  refl,
                             sym,
                             trans,
                             assoc) * 0.5) ++
      (FiniteDistribution.unif[Term](elTyps) * 0.5)

  val smallDist = (elemDist filter (_ != assoc)).normalized()

  val ded = Deducer(vars = MVars, cutoff = 0.01)

  def smooth(fd: FiniteDistribution[Term]) =
    (fd ++ (fd map (sigma)) ++ fd map ((t) => sigma(sigma(t)))).normalized()

  def sigma(t: Term) = {
    val aa = "aa" :: M
    val bb = "bb" :: M
    val cc = "cc" :: M
    t.replace(a, aa)
      .replace(b, bb)
      .replace(c, cc)
      .replace(aa, b)
      .replace(bb, c)
      .replace(cc, a)
  }
}

object SimpleGroup {
  val G = "G" :: Type

  val eqG = "_=_" :: G ->: G ->: Type

  val a = "a" :: G
  val b = "b" :: G
  val c = "c" :: G

  val e = "e" :: G

  val op = "_*_" :: G ->: G ->: G

  val i = "i" :: G ->: G

  import FineDeducer.unif

  val f = "f" :: G ->: G

  val hyp = "hyp" :: eqG(op(a)(a))(a)

  val dist = unif(a, b, c, hyp, f)(e, op, eqG, i)(
    eqG(a)(a),
    eqG(a)(b) ->: eqG(b)(a),
    eqG(a)(b) ->: eqG(b)(c) ->: eqG(a)(c),
    eqG(op(e)(a))(a),
    eqG(op(a)(e))(a),
    eqG(op(a)(i(a)))(a),
    eqG(op(i(a))(a))(a),
    eqG { op(a)(op(b)(c)) } { op(op(a)(b))(c) },
    eqG(a)(b) ->: eqG(f(a))(f(b))
  )
}

object MonoidSimple {
  val M = "M" :: Type

  val eqM = "eqM" :: M ->: M ->: Type

  // val sym = IdentityTyp.symm(M)
  //
  // val trans = IdentityTyp.trans(M)

  val a = "a" :: M
  val b = "b" :: M
  val c = "c" :: M

  val sym = "sym" :: a ~>: (b ~>: (eqM(a)(b) ->: eqM(b)(a)))

  val trans =
    "trans" :: a ~>:
      (b ~>: (c ~>: ((eqM(a)(b)) ->: (eqM(b)(c)) ->: (eqM(a)(c)))))

  val l = "e_l" :: M

  val r = "e_r" :: M

  val op = "mul" :: M ->: M ->: M

  import FineDeducer.unif

  val leftId  = "leftIdAx" :: (a ~>: (eqM(op(l)(a))(a)))
  val rightId = "rightIdAx" :: (a ~>: (eqM(op(a)(r))(a)))

  lazy val context = Context(
    "M"     -> M,
    "l"     -> l,
    "r"     -> r,
    "op"    -> op,
    "eqM"   -> eqM,
    "sym"   -> sym,
    "trans" -> trans).consider(l, r, op, eqM, sym, trans, leftId, rightId)

  val refl = "refl" :: a ~>: (eqM(a)(a))

  val dist: FiniteDistribution[Term] = unif(a, b, c)(l, r, op, eqM)(
    eqM(a)(a),
    eqM(a)(b) ->: eqM(b)(a),
    eqM(a)(b) ->: eqM(b)(c) ->: eqM(a)(c),
    eqM(op(l)(a))(a),
    eqM(op(a)(r))(a) //, eqM{op(a)(op(b)(c))}{op(op(a)(b))(c)}
  ) * 0.5 ++ (FiniteDistribution.unif(eqM: Term, op) * 0.5)

  val dist1 = dist.filter((t) => !Set(a, b, c).contains(t)).normalized()

  // val dedStep = FineDeducerStep(p = dist, param = FineDeducerStep.Param(vars = Vector(a, b, c)))

  // val idUnique =
  //   b ~>: (
  //       (a ~>: (eqM(op(l)(a))(a))) ~>: (eqM(l)(b))
  //   )

  val assoc =
    "assoc" :: (a ~>: (b ~>: (c ~>: (eqM(op(a)(op(b)(c)))(op(op(a)(b))(c))))))

  val elemDist = FiniteDistribution.unif(M,
                                         a,
                                         b,
                                         c,
                                         l,
                                         r,
                                         op,
                                         leftId,
                                         rightId,
                                         refl,
                                         sym,
                                         trans,
                                         assoc)
}

object Group {
  import Monoid.{M => G, _}

  val i = "inv" :: G ->: G

  val lInv = a ~>: (op(a)(i(a)) =:= a)

  val rInv = a ~>: (op(i(a))(a) =:= a)
}

/* Context to paste in server
val M = "M" :: Type
val l = "e_l" :: M
val r = "e_r" :: M
val a = "a" :: M
val b = "b" :: M
val c = "c" :: M
val op = "mul" :: M ->: M ->: M
val eqM = "eqM" :: M ->: M ->: Type
"sym" :: (a ~>: (b ~>: ((eqM(a)(b)) ->: (eqM(b)(a)))))
"trans" :: (a ~>: (b ~>: (c ~>: ((eqM(a)(b)) ->: (eqM(b)(c)) ->: (eqM(a)(c))))))
"lid" :: (a ~>: (eqM(op(l)(a))(a)))
"rid" :: (a ~>: (eqM(op(a)(r))(a)))
op
eqM
l
r
 */
