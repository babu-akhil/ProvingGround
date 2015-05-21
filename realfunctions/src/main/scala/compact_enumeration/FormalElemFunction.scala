package compact_enumeration

import PointWise._


/**
 * @author gadgil
 */
sealed trait FormalElemFunction {
  def as[A : FieldOps : ElementaryFunctions : Circ]: A

  def derivative: FormalElemFunction

  def partialDerivative(j: Int) : FormalElemFunction
}

trait OneVar extends FormalElemFunction {
  def partialDerivative(j: Int) : FormalElemFunction = if (j ==0) derivative else Zero
}

import FormalElemFunction._

import FormalElemFunction._


import FieldOpsSyms._

case object Sin extends FormalElemFunction with OneVar{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[ElementaryFunctions[A]].sin
  }

  def derivative: FormalElemFunction = Cos

  override  def toString="sin"
}

case object Cos extends FormalElemFunction with OneVar{
  def as[A : FieldOps : ElementaryFunctions: Circ] = {
    implicitly[ElementaryFunctions[A]].cos
  }

  def derivative: FormalElemFunction = Negate(Sin)

  override  def toString="cos"
}

case object Log extends FormalElemFunction with OneVar{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[ElementaryFunctions[A]].log
  }

  def derivative: FormalElemFunction = Reciprocal(Proj(0))

  override  def toString="log"
}

case class Proj(i: Int) extends FormalElemFunction{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[ElementaryFunctions[A]].proj(i)
  }

  def derivative: FormalElemFunction =
    if (i ==0) One else Zero // Just taken as derivative wrt first coordinate if there are several.

  def partialDerivative(j: Int) : FormalElemFunction =
    if (i ==j) One else Zero

  override  def toString= i match {
      case 0 => "x"
      case 1 => "y"
      case 2 => "z"
      case 3 => "w"
      case _ => s"x_$i"
    }
}


case object Exp extends FormalElemFunction with OneVar{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[ElementaryFunctions[A]].exp
  }

  def derivative: FormalElemFunction = Exp

  override  def toString="sin"
}

case object One extends FormalElemFunction with OneVar{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[FieldOps[A]].one
  }

  def derivative: FormalElemFunction = Zero

  override  def toString="1"
}

case object Zero extends FormalElemFunction with OneVar{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[FieldOps[A]].one
  }

  def derivative: FormalElemFunction = Zero

  override  def toString="0"
}

case object Pi extends FormalElemFunction with OneVar{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[ElementaryFunctions[A]].pi
  }

  def derivative: FormalElemFunction = Zero

  override  def toString="\u220f"
}

case class Plus(x: FormalElemFunction, y: FormalElemFunction) extends FormalElemFunction {
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[FieldOps[A]].plus(x.as[A], y.as[A])
  }

  def derivative: FormalElemFunction = Plus(x.derivative, y.derivative)

  def partialDerivative(j: Int) : FormalElemFunction = Plus(x.partialDerivative(j), y.partialDerivative(j))
  
  override  def toString=s"($x + $y)"
}

case class Times(x: FormalElemFunction, y: FormalElemFunction) extends FormalElemFunction{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[FieldOps[A]].times(x.as[A], y.as[A])
  }

  def derivative: FormalElemFunction = Plus(Times(x.derivative,y), Times(y.derivative, x))
  
  def partialDerivative(j: Int): FormalElemFunction = Plus(Times(x.partialDerivative(j),y), Times(y.partialDerivative(j), x))

  override  def toString=s"($x \u00d7 $y)"
}

case class Reciprocal(x: FormalElemFunction) extends FormalElemFunction{
    def as[A : FieldOps : ElementaryFunctions : Circ] = {
    val f = implicitly[FieldOps[A]]
    f.div(f.one, x.as[A])
  }

    def derivative: FormalElemFunction =
      Times(Negate(x.derivative), Times(Reciprocal(x), Reciprocal(x)))

   def partialDerivative(j: Int): FormalElemFunction =
      Times(Negate(x.partialDerivative(j)), Times(Reciprocal(x), Reciprocal(x)))
      
    override  def toString=s"1/($x)"
}

object Div{
  def apply(x: FormalElemFunction, y: FormalElemFunction) : FormalElemFunction =
    Times(x, Reciprocal(y))

  def unapply(q: FormalElemFunction) : Option[(FormalElemFunction,FormalElemFunction)] = q match{
    case Times(x, Reciprocal(y)) => Some(x, y)
    case Times(Reciprocal(y), x) => Some(x, y)
    case _ => None
  }
}


case class Negate(x: FormalElemFunction) extends FormalElemFunction{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[FieldOps[A]].negate(x.as[A])
  }

  def derivative: FormalElemFunction = Negate(x.derivative)
  
  def partialDerivative(j: Int): FormalElemFunction = Negate(x.partialDerivative(j))

  override  def toString=s"-($x)"
}

case class Compose(x: FormalElemFunction, y: FormalElemFunction) extends FormalElemFunction{
  def as[A : FieldOps : ElementaryFunctions : Circ] = {
    implicitly[Circ[A]].circ(x.as[A], y.as[A])
  }

  def derivative: FormalElemFunction = Times(Compose(x.derivative, y), y.derivative)
  
  def partialDerivative(j: Int): FormalElemFunction = Times(Compose(x.partialDerivative(j), y), y.partialDerivative(j))

  override  def toString=s"$x \u2218 ($y)"
}


object FormalElemFunction{
  implicit val FormalElemFunc = new ElementaryFunctions[FormalElemFunction]{
    val sin : FormalElemFunction = Sin
    val cos : FormalElemFunction = Cos
    val exp : FormalElemFunction = Exp
    val log : FormalElemFunction = Log

    val pi = Pi

    val fieldOps = formalFieldOps

    val proj : Int =>  FormalElemFunction = (i: Int) => Proj(i)
    
    lazy val tan = sin/cos

    lazy val one: FormalElemFunction = One

    def N(n: Int) = natField[FormalElemFunction](n)
    
    lazy val sec = one / cos
    
    lazy val cosec = one / sin

    lazy val x= proj(0)

    lazy val y = proj(1)

    lazy val z = proj(2)
    
    lazy val w = proj(3)
  }

  implicit val formalFieldOps = new FieldOps[FormalElemFunction]{
    def negate(x: FormalElemFunction): FormalElemFunction = Negate(x)

    def zero : FormalElemFunction = Zero

    def one : FormalElemFunction = One

    def plus(x: FormalElemFunction, y: FormalElemFunction): FormalElemFunction = Plus(x, y)

    def times(x: FormalElemFunction, y: FormalElemFunction): FormalElemFunction = Times(x, y)

    def div(x: FormalElemFunction, y: FormalElemFunction) : FormalElemFunction = Div(x, y)
    
    
  }

  implicit val formalCompose = new Circ[FormalElemFunction]{
    def circ(x: FormalElemFunction, y: FormalElemFunction): FormalElemFunction = Compose(x, y)
  }

  import FieldOpsSyms._

  import FormalElemFunc._

  import Circ._

  

}