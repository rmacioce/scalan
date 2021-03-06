package scalan.common

import scalan._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait KindsAbs extends scalan.ScalanDsl with Kinds {
  self: KindsDsl =>

  // single proxy for each type family
  implicit def proxyKind[F[_], A](p: Rep[Kind[F, A]]): Kind[F, A] = {
    proxyOps[Kind[F, A]](p)(scala.reflect.classTag[Kind[F, A]])
  }

  // familyElem
  class KindElem[F[_], A, To <: Kind[F, A]](implicit _cF: Cont[F], _eA: Elem[A])
    extends EntityElem[To] {
    def cF = _cF
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Kind[F, A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Kind[F, A]] => convertKind(x) }
      tryConvert(element[Kind[F, A]], this, x, conv)
    }

    def convertKind(x: Rep[Kind[F, A]]): Rep[To] = {
      x.selfType1.asInstanceOf[Elem[_]] match {
        case _: KindElem[_, _, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have KindElem[_, _, _], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def kindElement[F[_], A](implicit cF: Cont[F], eA: Elem[A]): Elem[Kind[F, A]] =
    cachedElem[KindElem[F, A, Kind[F, A]]](cF, eA)

  implicit case object KindCompanionElem extends CompanionElem[KindCompanionAbs] {
    lazy val tag = weakTypeTag[KindCompanionAbs]
    protected def getDefaultRep = Kind
  }

  abstract class KindCompanionAbs extends CompanionDef[KindCompanionAbs] with KindCompanion {
    def selfType = KindCompanionElem
    override def toString = "Kind"
  }
  def Kind: Rep[KindCompanionAbs]
  implicit def proxyKindCompanionAbs(p: Rep[KindCompanionAbs]): KindCompanionAbs =
    proxyOps[KindCompanionAbs](p)

  abstract class AbsReturn[F[_], A]
      (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends Return[F, A](a) with Def[Return[F, A]] {
    lazy val selfType = element[Return[F, A]]
  }
  // elem for concrete class
  class ReturnElem[F[_], A](val iso: Iso[ReturnData[F, A], Return[F, A]])(implicit override val eA: Elem[A], override val cF: Cont[F])
    extends KindElem[F, A, Return[F, A]]
    with ConcreteElem[ReturnData[F, A], Return[F, A]] {
    override lazy val parent: Option[Elem[_]] = Some(kindElement(container[F], element[A]))
    override lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)

    override def convertKind(x: Rep[Kind[F, A]]) = // Converter is not generated by meta
!!!("Cannot convert from Kind to Return: missing fields List(a)")
    override def getDefaultRep = Return(element[A].defaultRepValue)
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Return[F, A]]
    }
  }

  // state representation type
  type ReturnData[F[_], A] = A

  // 3) Iso for concrete class
  class ReturnIso[F[_], A](implicit eA: Elem[A], cF: Cont[F])
    extends EntityIso[ReturnData[F, A], Return[F, A]] with Def[ReturnIso[F, A]] {
    override def from(p: Rep[Return[F, A]]) =
      p.a
    override def to(p: Rep[A]) = {
      val a = p
      Return(a)
    }
    lazy val eFrom = element[A]
    lazy val eTo = new ReturnElem[F, A](self)
    lazy val selfType = new ReturnIsoElem[F, A](eA, cF)
    def productArity = 2
    def productElement(n: Int) = n match {
      case 0 => eA
      case 1 => cF
    }
  }
  case class ReturnIsoElem[F[_], A](eA: Elem[A], cF: Cont[F]) extends Elem[ReturnIso[F, A]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new ReturnIso[F, A]()(eA, cF))
    lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[ReturnIso[F, A]]
    }
    lazy val typeArgs = TypeArgs("F" -> cF, "A" -> eA)
  }
  // 4) constructor and deconstructor
  class ReturnCompanionAbs extends CompanionDef[ReturnCompanionAbs] with ReturnCompanion {
    def selfType = ReturnCompanionElem
    override def toString = "Return"

    @scalan.OverloadId("fromFields")
    def apply[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]] =
      mkReturn(a)

    def unapply[F[_], A](p: Rep[Kind[F, A]]) = unmkReturn(p)
  }
  lazy val ReturnRep: Rep[ReturnCompanionAbs] = new ReturnCompanionAbs
  lazy val Return: ReturnCompanionAbs = proxyReturnCompanion(ReturnRep)
  implicit def proxyReturnCompanion(p: Rep[ReturnCompanionAbs]): ReturnCompanionAbs = {
    proxyOps[ReturnCompanionAbs](p)
  }

  implicit case object ReturnCompanionElem extends CompanionElem[ReturnCompanionAbs] {
    lazy val tag = weakTypeTag[ReturnCompanionAbs]
    protected def getDefaultRep = Return
  }

  implicit def proxyReturn[F[_], A](p: Rep[Return[F, A]]): Return[F, A] =
    proxyOps[Return[F, A]](p)

  implicit class ExtendedReturn[F[_], A](p: Rep[Return[F, A]])(implicit eA: Elem[A], cF: Cont[F]) {
    def toData: Rep[ReturnData[F, A]] = isoReturn(eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoReturn[F[_], A](implicit eA: Elem[A], cF: Cont[F]): Iso[ReturnData[F, A], Return[F, A]] =
    reifyObject(new ReturnIso[F, A]()(eA, cF))

  // 6) smart constructor and deconstructor
  def mkReturn[F[_], A](a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]]
  def unmkReturn[F[_], A](p: Rep[Kind[F, A]]): Option[(Rep[A])]

  abstract class AbsBind[F[_], S, B]
      (a: Rep[Kind[F, S]], f: Rep[S => Kind[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends Bind[F, S, B](a, f) with Def[Bind[F, S, B]] {
    lazy val selfType = element[Bind[F, S, B]]
  }
  // elem for concrete class
  class BindElem[F[_], S, B](val iso: Iso[BindData[F, S, B], Bind[F, S, B]])(implicit val eS: Elem[S], override val eA: Elem[B], override val cF: Cont[F])
    extends KindElem[F, B, Bind[F, S, B]]
    with ConcreteElem[BindData[F, S, B], Bind[F, S, B]] {
    override lazy val parent: Option[Elem[_]] = Some(kindElement(container[F], element[B]))
    override lazy val typeArgs = TypeArgs("F" -> cF, "S" -> eS, "B" -> eA)

    override def convertKind(x: Rep[Kind[F, B]]) = // Converter is not generated by meta
!!!("Cannot convert from Kind to Bind: missing fields List(a, f)")
    override def getDefaultRep = Bind(element[Kind[F, S]].defaultRepValue, constFun[S, Kind[F, B]](element[Kind[F, B]].defaultRepValue))
    override lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[Bind[F, S, B]]
    }
  }

  // state representation type
  type BindData[F[_], S, B] = (Kind[F, S], S => Kind[F, B])

  // 3) Iso for concrete class
  class BindIso[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends EntityIso[BindData[F, S, B], Bind[F, S, B]] with Def[BindIso[F, S, B]] {
    override def from(p: Rep[Bind[F, S, B]]) =
      (p.a, p.f)
    override def to(p: Rep[(Kind[F, S], S => Kind[F, B])]) = {
      val Pair(a, f) = p
      Bind(a, f)
    }
    lazy val eFrom = pairElement(element[Kind[F, S]], element[S => Kind[F, B]])
    lazy val eTo = new BindElem[F, S, B](self)
    lazy val selfType = new BindIsoElem[F, S, B](eS, eA, cF)
    def productArity = 3
    def productElement(n: Int) = n match {
      case 0 => eS
      case 1 => eA
      case 2 => cF
    }
  }
  case class BindIsoElem[F[_], S, B](eS: Elem[S], eA: Elem[B], cF: Cont[F]) extends Elem[BindIso[F, S, B]] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new BindIso[F, S, B]()(eS, eA, cF))
    lazy val tag = {
      implicit val tagS = eS.tag
      implicit val tagB = eA.tag
      weakTypeTag[BindIso[F, S, B]]
    }
    lazy val typeArgs = TypeArgs("F" -> cF, "S" -> eS, "B" -> eA)
  }
  // 4) constructor and deconstructor
  class BindCompanionAbs extends CompanionDef[BindCompanionAbs] with BindCompanion {
    def selfType = BindCompanionElem
    override def toString = "Bind"
    @scalan.OverloadId("fromData")
    def apply[F[_], S, B](p: Rep[BindData[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
      isoBind(eS, eA, cF).to(p)
    @scalan.OverloadId("fromFields")
    def apply[F[_], S, B](a: Rep[Kind[F, S]], f: Rep[S => Kind[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
      mkBind(a, f)

    def unapply[F[_], S, B](p: Rep[Kind[F, B]]) = unmkBind(p)
  }
  lazy val BindRep: Rep[BindCompanionAbs] = new BindCompanionAbs
  lazy val Bind: BindCompanionAbs = proxyBindCompanion(BindRep)
  implicit def proxyBindCompanion(p: Rep[BindCompanionAbs]): BindCompanionAbs = {
    proxyOps[BindCompanionAbs](p)
  }

  implicit case object BindCompanionElem extends CompanionElem[BindCompanionAbs] {
    lazy val tag = weakTypeTag[BindCompanionAbs]
    protected def getDefaultRep = Bind
  }

  implicit def proxyBind[F[_], S, B](p: Rep[Bind[F, S, B]]): Bind[F, S, B] =
    proxyOps[Bind[F, S, B]](p)

  implicit class ExtendedBind[F[_], S, B](p: Rep[Bind[F, S, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]) {
    def toData: Rep[BindData[F, S, B]] = isoBind(eS, eA, cF).from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoBind[F[_], S, B](implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Iso[BindData[F, S, B], Bind[F, S, B]] =
    reifyObject(new BindIso[F, S, B]()(eS, eA, cF))

  // 6) smart constructor and deconstructor
  def mkBind[F[_], S, B](a: Rep[Kind[F, S]], f: Rep[S => Kind[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]]
  def unmkBind[F[_], S, B](p: Rep[Kind[F, B]]): Option[(Rep[Kind[F, S]], Rep[S => Kind[F, B]])]

  registerModule(Kinds_Module)
}

// Std -----------------------------------
trait KindsStd extends scalan.ScalanDslStd with KindsDsl {
  self: KindsDslStd =>

  lazy val Kind: Rep[KindCompanionAbs] = new KindCompanionAbs {
  }

  case class StdReturn[F[_], A]
      (override val a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsReturn[F, A](a) {
  }

  def mkReturn[F[_], A]
    (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]] =
    new StdReturn[F, A](a)
  def unmkReturn[F[_], A](p: Rep[Kind[F, A]]) = p match {
    case p: Return[F, A] @unchecked =>
      Some((p.a))
    case _ => None
  }

  case class StdBind[F[_], S, B]
      (override val a: Rep[Kind[F, S]], override val f: Rep[S => Kind[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends AbsBind[F, S, B](a, f) {
  }

  def mkBind[F[_], S, B]
    (a: Rep[Kind[F, S]], f: Rep[S => Kind[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
    new StdBind[F, S, B](a, f)
  def unmkBind[F[_], S, B](p: Rep[Kind[F, B]]) = p match {
    case p: Bind[F, S, B] @unchecked =>
      Some((p.a, p.f))
    case _ => None
  }
}

// Exp -----------------------------------
trait KindsExp extends scalan.ScalanDslExp with KindsDsl {
  self: KindsDslExp =>

  lazy val Kind: Rep[KindCompanionAbs] = new KindCompanionAbs {
  }

  case class ExpReturn[F[_], A]
      (override val a: Rep[A])(implicit eA: Elem[A], cF: Cont[F])
    extends AbsReturn[F, A](a)

  object ReturnMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f
  }

  object ReturnCompanionMethods {
  }

  def mkReturn[F[_], A]
    (a: Rep[A])(implicit eA: Elem[A], cF: Cont[F]): Rep[Return[F, A]] =
    new ExpReturn[F, A](a)
  def unmkReturn[F[_], A](p: Rep[Kind[F, A]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: ReturnElem[F, A] @unchecked =>
      Some((p.asRep[Return[F, A]].a))
    case _ =>
      None
  }

  case class ExpBind[F[_], S, B]
      (override val a: Rep[Kind[F, S]], override val f: Rep[S => Kind[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F])
    extends AbsBind[F, S, B](a, f)

  object BindMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f1
  }

  object BindCompanionMethods {
  }

  def mkBind[F[_], S, B]
    (a: Rep[Kind[F, S]], f: Rep[S => Kind[F, B]])(implicit eS: Elem[S], eA: Elem[B], cF: Cont[F]): Rep[Bind[F, S, B]] =
    new ExpBind[F, S, B](a, f)
  def unmkBind[F[_], S, B](p: Rep[Kind[F, B]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: BindElem[F, S, B] @unchecked =>
      Some((p.asRep[Bind[F, S, B]].a, p.asRep[Bind[F, S, B]].f))
    case _ =>
      None
  }

  object KindMethods {
    // WARNING: Cannot generate matcher for method `flatMap`: Method has function arguments f

    object mapBy {
      def unapply(d: Def[_]): Option[(Rep[Kind[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = d match {
        case MethodCall(receiver, method, Seq(f, _*), _) if (receiver.elem.asInstanceOf[Elem[_]] match { case _: KindElem[_, _, _] => true; case _ => false }) && method.getName == "mapBy" =>
          Some((receiver, f)).asInstanceOf[Option[(Rep[Kind[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Kind[F, A]], Rep[A => B]) forSome {type F[_]; type A; type B}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object KindCompanionMethods {
  }
}

object Kinds_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAAL1XS4gcRRiuee281uya+A5xN8tkNT5mgjlEWGSZ3ZmRJJPdZTsSGYNLTXfNpGN3ddtds/R4iOAhB/UkQVDwEFC8hIB6y0E8KIgEQfHmyYOnGJEczEnxr+rH9Mx2z85GtA9FV/ff/+P7/q+q+tptlLEttGjLWMO0rBOGy5K4r9qsJNUpU1n/jKH0NFIjnR+e/soovPXuWhLNttDUBWzXbK2F8u5N3TGDe4kpTZTHVCY2MyybocNNEaEiG5pGZKYatKLqeo/htkYqTdVmS02UbhtK/3V0CSWaaFY2qGwRRqRVDds2sb3nOcIzUoN5Xsz76+YgBq3wKiqhKs5aWGWQPsSYde03iSn1qUH7OkP7vNTWTZ4W2BSJY0INJ3VTE2FSTZRVddOwmB81CxEuGIo/TVMMD9D+5kW8jSsQtVuRmKXSLndmYvk13CVrYMLN01CDTbTO2b5JPOdFmylD8RwTIQSsPCcSKw8wKweYlTlmJYlYKtbUNzB/uWEZTh+5VyKFkGOCi2d2ceF7IHWqlN4+L79yVyrqSf6xw1PJioSmwNFcTIcIegDbbzffs++8ePVEEhVaqKDa1bbNLCyzcBt4cBUxpQYTOQcIYqsLDC7EMSiiVMFmpE3ysqGbmIInD8tpIEpTZZVxY/5s2qMnBvssM4lvmnDMRFDvfEy9opdWsaZt3Hr02SO/1V9OouRwiDy4lEAMlu+UofRplSqeaz7OMJRoDPDl06qY8iHvDMbsmEwCTJ649bvyzTF0Phkg6QWejDxwsf/5D28cIRvXkyjXEr3e0HBX0MihqhFbbqGcsU0s93l2G2v8LpLKrEI6uKcxD+AwMilAhqH5WJmahMO2JNo/4ZdfdDt4zaCk1Ngo/Sl9d+Uab1ALTbtvXN3+rZ746+d9HSZ6F/DEPrIp0PoI9PFYF1yXkqGT+xfuqK9efYcJVBPOsLzX2xdBTkviu8fHAOyvPJ9dvvzgHx9vHRDqyLVVpmOzdGwP2vBb+T/sfRSg4nblw4M5H+YA1ZlNwnoWXQ0Hngt9EcL5sYTPoTBiKEmqPgHpukb0MZzEOJAbgYNVg7JIPYVJZWjKzVc4CMRwKI4rgcZDm80HtNvLXyZR5hTKdKDL7SbKtI0eVXyYYWtixGEr/rPEMMwAK7awHsAqrnk0wIonG0p+OdJiaxSPaLMdsBUTw7hMsPSsxMhhB6VohJFxIpsgrrQjbkyYTkQYC3QXy2OjR+WfTn5wYObQ1i9ifZ5SDB2rohEOAp0WaFjQddBbJAfp/GvcwugtivHoBMq6bwXC3aOupHG6CoN8T8Jc2d3B3oWZ5uWGZRnf4btIhQ+1ybQSIjjaYCehodhH0XBdKVjW99zy0TvPkOBGemUPLXQ6uoX888f/DXd0rVcGaSyCiMsxIq4RWcMWUfihmOhwaHe3xOPvL5879ci5l8SmPK0II/dNcK6I/sU4g80lcSB+csyBGIxKdd2EHx64Of71Cz++efPTT8SBYoA2QxlBM+DtpQ7bsO7t1ryihZiKJG/zhba5dPejtae+/+JXsTQV+DYOhxsa/F0MNhNnZFHNidDwrxCCFtjl+3qoWa7z4fN/AC0LjlTaDQAA"
}
}

trait KindsDsl extends impl.KindsAbs
trait KindsDslStd extends impl.KindsStd
trait KindsDslExp extends impl.KindsExp
