package scalan.monads

import scalan._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait CoproductsAbs extends scalan.ScalanDsl with Coproducts {
  self: MonadsDsl =>

  // single proxy for each type family
  implicit def proxyCoproduct[F[_], G[_], A](p: Rep[Coproduct[F, G, A]]): Coproduct[F, G, A] = {
    proxyOps[Coproduct[F, G, A]](p)(scala.reflect.classTag[Coproduct[F, G, A]])
  }

  // familyElem
  class CoproductElem[F[_], G[_], A, To <: Coproduct[F, G, A]](implicit _cF: Cont[F], _cG: Cont[G], _eA: Elem[A])
    extends EntityElem[To] {
    def cF = _cF
    def cG = _cG
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs("F" -> cF, "G" -> cG, "A" -> eA)
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Coproduct[F, G, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Coproduct[F, G, A]] => convertCoproduct(x) }
      tryConvert(element[Coproduct[F, G, A]], this, x, conv)
    }

    def convertCoproduct(x: Rep[Coproduct[F, G, A]]): Rep[To] = {
      x.selfType1.asInstanceOf[Elem[_]] match {
        case _: CoproductElem[_, _, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have CoproductElem[_, _, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def coproductElement[F[_], G[_], A](implicit cF: Cont[F], cG: Cont[G], eA: Elem[A]): Elem[Coproduct[F, G, A]] =
    cachedElem[CoproductElem[F, G, A, Coproduct[F, G, A]]](cF, cG, eA)

  implicit case object CoproductCompanionElem extends CompanionElem[CoproductCompanionAbs] {
    lazy val tag = weakTypeTag[CoproductCompanionAbs]
    protected def getDefaultRep = Coproduct
  }

  abstract class CoproductCompanionAbs extends CompanionDef[CoproductCompanionAbs] with CoproductCompanion {
    def selfType = CoproductCompanionElem
    override def toString = "Coproduct"
  }
  def Coproduct: Rep[CoproductCompanionAbs]
  implicit def proxyCoproductCompanionAbs(p: Rep[CoproductCompanionAbs]): CoproductCompanionAbs =
    proxyOps[CoproductCompanionAbs](p)

  abstract class AbsCoproductImpl[F[_], G[_], A]
      (run: Rep[Either[F[A], G[A]]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A])
    extends CoproductImpl[F, G, A](run) with Def[CoproductImpl[F, G, A]] {
    lazy val selfType = element[CoproductImpl[F, G, A]]
  }
  // elem for concrete class
  class CoproductImplElem[F[_], G[_], A](val iso: Iso[CoproductImplData[F, G, A], CoproductImpl[F, G, A]])(implicit override val cF: Cont[F], override val cG: Cont[G], override val eA: Elem[A])
    extends CoproductElem[F, G, A, CoproductImpl[F, G, A]]
    with ConcreteElem[CoproductImplData[F, G, A], CoproductImpl[F, G, A]] {
    override lazy val parent: Option[Elem[_]] = Some(coproductElement(container[F], container[G], element[A]))
    override lazy val typeArgs = TypeArgs("F" -> cF, "G" -> cG, "A" -> eA)

    override def convertCoproduct(x: Rep[Coproduct[F, G, A]]) = CoproductImpl(x.run)
    override def getDefaultRep = CoproductImpl(element[Either[F[A], G[A]]].defaultRepValue)
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[CoproductImpl[F, G, A]]
    }
  }

  // state representation type
  type CoproductImplData[F[_], G[_], A] = Either[F[A], G[A]]

  // 3) Iso for concrete class
  class CoproductImplIso[F[_], G[_], A](implicit cF: Cont[F], cG: Cont[G], eA: Elem[A])
    extends EntityIso[CoproductImplData[F, G, A], CoproductImpl[F, G, A]] with Def[CoproductImplIso[F, G, A]] {
    override def from(p: Rep[CoproductImpl[F, G, A]]) =
      p.run
    override def to(p: Rep[Either[F[A], G[A]]]) = {
      val run = p
      CoproductImpl(run)
    }
    lazy val eFrom = element[Either[F[A], G[A]]]
    lazy val eTo = new CoproductImplElem[F, G, A](self)
    lazy val selfType = new CoproductImplIsoElem[F, G, A](cF, cG, eA)
    def productArity = 3
    def productElement(n: Int) = n match {
      case 0 => cF
      case 1 => cG
      case 2 => eA
    }
  }
  case class CoproductImplIsoElem[F[_], G[_], A](cF: Cont[F], cG: Cont[G], eA: Elem[A]) extends Elem[CoproductImplIso[F, G, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new CoproductImplIso[F, G, A]()(cF, cG, eA))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[CoproductImplIso[F, G, A]]
    }
    lazy val typeArgs = TypeArgs("F" -> cF, "G" -> cG, "A" -> eA)
  }
  // 4) constructor and deconstructor
  class CoproductImplCompanionAbs extends CompanionDef[CoproductImplCompanionAbs] with CoproductImplCompanion {
    def selfType = CoproductImplCompanionElem
    override def toString = "CoproductImpl"

    @scalan.OverloadId("fromFields")
    def apply[F[_], G[_], A](run: Rep[Either[F[A], G[A]]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A]): Rep[CoproductImpl[F, G, A]] =
      mkCoproductImpl(run)

    def unapply[F[_], G[_], A](p: Rep[Coproduct[F, G, A]]) = unmkCoproductImpl(p)
  }
  lazy val CoproductImplRep: Rep[CoproductImplCompanionAbs] = new CoproductImplCompanionAbs
  lazy val CoproductImpl: CoproductImplCompanionAbs = proxyCoproductImplCompanion(CoproductImplRep)
  implicit def proxyCoproductImplCompanion(p: Rep[CoproductImplCompanionAbs]): CoproductImplCompanionAbs = {
    proxyOps[CoproductImplCompanionAbs](p)
  }

  implicit case object CoproductImplCompanionElem extends CompanionElem[CoproductImplCompanionAbs] {
    lazy val tag = weakTypeTag[CoproductImplCompanionAbs]
    protected def getDefaultRep = CoproductImpl
  }

  implicit def proxyCoproductImpl[F[_], G[_], A](p: Rep[CoproductImpl[F, G, A]]): CoproductImpl[F, G, A] =
    proxyOps[CoproductImpl[F, G, A]](p)

  implicit class ExtendedCoproductImpl[F[_], G[_], A](p: Rep[CoproductImpl[F, G, A]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A]) {
    def toData: Rep[CoproductImplData[F, G, A]] = isoCoproductImpl(cF, cG, eA).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoCoproductImpl[F[_], G[_], A](implicit cF: Cont[F], cG: Cont[G], eA: Elem[A]): Iso[CoproductImplData[F, G, A], CoproductImpl[F, G, A]] =
    reifyObject(new CoproductImplIso[F, G, A]()(cF, cG, eA))

  // 6) smart constructor and deconstructor
  def mkCoproductImpl[F[_], G[_], A](run: Rep[Either[F[A], G[A]]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A]): Rep[CoproductImpl[F, G, A]]
  def unmkCoproductImpl[F[_], G[_], A](p: Rep[Coproduct[F, G, A]]): Option[(Rep[Either[F[A], G[A]]])]

  registerModule(Coproducts_Module)
}

// Std -----------------------------------
trait CoproductsStd extends scalan.ScalanDslStd with CoproductsDsl {
  self: MonadsDslStd =>

  lazy val Coproduct: Rep[CoproductCompanionAbs] = new CoproductCompanionAbs {
  }

  case class StdCoproductImpl[F[_], G[_], A]
      (override val run: Rep[Either[F[A], G[A]]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A])
    extends AbsCoproductImpl[F, G, A](run) {
  }

  def mkCoproductImpl[F[_], G[_], A]
    (run: Rep[Either[F[A], G[A]]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A]): Rep[CoproductImpl[F, G, A]] =
    new StdCoproductImpl[F, G, A](run)
  def unmkCoproductImpl[F[_], G[_], A](p: Rep[Coproduct[F, G, A]]) = p match {
    case p: CoproductImpl[F, G, A] @unchecked =>
      Some((p.run))
    case _ => None
  }
}

// Exp -----------------------------------
trait CoproductsExp extends scalan.ScalanDslExp with CoproductsDsl {
  self: MonadsDslExp =>

  lazy val Coproduct: Rep[CoproductCompanionAbs] = new CoproductCompanionAbs {
  }

  case class ExpCoproductImpl[F[_], G[_], A]
      (override val run: Rep[Either[F[A], G[A]]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A])
    extends AbsCoproductImpl[F, G, A](run)

  object CoproductImplMethods {
  }

  object CoproductImplCompanionMethods {
  }

  def mkCoproductImpl[F[_], G[_], A]
    (run: Rep[Either[F[A], G[A]]])(implicit cF: Cont[F], cG: Cont[G], eA: Elem[A]): Rep[CoproductImpl[F, G, A]] =
    new ExpCoproductImpl[F, G, A](run)
  def unmkCoproductImpl[F[_], G[_], A](p: Rep[Coproduct[F, G, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: CoproductImplElem[F, G, A] @unchecked =>
      Some((p.asRep[CoproductImpl[F, G, A]].run))
    case _ =>
      None
  }

  object CoproductMethods {
    object run {
      def unapply(d: Def[_]): Option[Rep[Coproduct[F, G, A]] forSome {type F[_]; type G[_]; type A}] = d match {
        case MethodCall(receiver, method, _, _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: CoproductElem[_, _, _, _] => true; case _ => false }) && method.getName == "run" =>
          Some(receiver).asInstanceOf[Option[Rep[Coproduct[F, G, A]] forSome {type F[_]; type G[_]; type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Coproduct[F, G, A]] forSome {type F[_]; type G[_]; type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object CoproductCompanionMethods {
  }
}

object Coproducts_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAALVWTYgcRRR+M7O7szOzxnXVYEBxd5mo+DMTXDDCImGcnR0Msz9sRxLGYKzprpnt2F3VdtcuPR6iiOSgnkQ8CB4CipcgiDcP4kFBRAS9evYUI5KDOSm+qv6dcX4SMH0o+lW/fj/f996runodZj0XHvF0YhFWsakgFU291zxR1hpMmKK/xY0Di27Q7s9PfMOLb723nYXFNsztE2/Ds9pQCF4avhO/a8JoQYEwnXqCu56AlZbyUNW5ZVFdmJxVTds+EKRj0WrL9MR6C2Y63Oi/Bpcg04JFnTPdpYJqdYt4HvXC/XkqIzJjuaDk/o6T+GBVmUU1lcUZl5gCw0cfi4H+HnW0PuOsbws4Eoa248iwUKdEfQdzeMF2LOUm14K8aTvcFZHXPHrY50YkzjCCG7DUukgOSRW99qqacE3Wk8Ycor9KenQbVaT6DObgUat7pu/Q0HjJE8aAP98BAGTlaRVYJcGsEmNWkZiVNeqaxDJfJ/Ljrsv9PgRPJgfgO2jiySkmIgu0wYzyO+f1l25qJTsrf/ZlKHkV0BwaenhMhSh6ENvv9973bjSvnMxCsQ1F06t1POESXaTLIISrRBjjQsUcI0jcHjK4Oo5B5aWGOkNlUtC57RCGlkIsF5Aoy9RNIZXl3kJIzxjs88KhkWrGdzJxvstj8lW1VCeWtXvt2FPHf2+cy0J20EUBTWrYDG5kVEChzh0Xm0gXoX253i0gs5mALMXmoFhTolwKfrLmJ0QX4/TotT+M707A+WyMbhjMrRGKJpae/eir43T38yzMt1X9b1qkp6iV8G1QT2/DPD+kbrCfPySWfBtJb96gXXJgiRD0NFo5REvA8tjWdaiEcl21RCZKvxRU9TZntLy5W/5L++GDq7JoXVgIvgS9/I958u9fj3SFqmcBOfeARdjmcAIMcjHXMMU+dYcJGpLTlCS0TVAavcokikGoGrfpPas3zJevvCsUWxl/cJTsdC5i666r/5YnEBdNuS8uX77/z08u3Ks6cb5jCps45RO30YdR29zBPoMYoaDajyWyXFaQraNxz8jBWE/7X0n9mIL+wUxUIkpJQFbfjDiZqXM2uvlSXI4w0JxkoDndAK3FBhoWtacWioC7BvJWduKefWgc9Qrco3ut+6zrp77OwuxpmO1iM3otmO3wA2ZErOGpKqgvno/2MoOsIUvEJXbMknqWIcF8qCO0kRoXhmEZrdacbEgu527N0n9hTJl+BgZBz2GLDO78r9O5kDo3U1Wt5LUwoOnFvxSHNKLwBw6UdIGMR2gKabeB9Z1kTa6vJAmtYcVXxlT8BtUt4lJD9gi18VIXjLG1D0+dPf3A2RfVIF0wlFLwJT5jRl9Bt4izri5Mj024MKFSuWE7eCHGl7Vvn/vljR8/+1QdLgmDAopJOWE3R/FzRgwvTmt1TFpaODWxRi/d/Hj78Z++/E3dL4py/uJpx+IraNK2/tBBVthSvvBGmQIY54+cyKkyfFMub/8LIEjcpwAMAAA="
}
}

