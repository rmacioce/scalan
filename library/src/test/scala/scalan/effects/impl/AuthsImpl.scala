package scalan.examples

import scala.reflect.runtime.universe._
import scalan._
import scalan.monads._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait AuthenticationsAbs extends Authentications with scalan.Scalan {
  self: AuthenticationsDsl =>

  // single proxy for each type family
  implicit def proxyAuth[A](p: Rep[Auth[A]]): Auth[A] = {
    proxyOps[Auth[A]](p)(scala.reflect.classTag[Auth[A]])
  }

  // familyElem
  class AuthElem[A, To <: Auth[A]](implicit _eA: Elem[A])
    extends EntityElem[To] {
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("A" -> Left(eA))
    }
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[Auth[A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Auth[A]] => convertAuth(x) }
      tryConvert(element[Auth[A]], this, x, conv)
    }

    def convertAuth(x: Rep[Auth[A]]): Rep[To] = {
      x.selfType1 match {
        case _: AuthElem[_, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have AuthElem[_, _], but got $e")
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def authElement[A](implicit eA: Elem[A]): Elem[Auth[A]] =
    cachedElem[AuthElem[A, Auth[A]]](eA)

  implicit case object AuthCompanionElem extends CompanionElem[AuthCompanionAbs] {
    lazy val tag = weakTypeTag[AuthCompanionAbs]
    protected def getDefaultRep = Auth
  }

  abstract class AuthCompanionAbs extends CompanionDef[AuthCompanionAbs] with AuthCompanion {
    def selfType = AuthCompanionElem
    override def toString = "Auth"
  }
  def Auth: Rep[AuthCompanionAbs]
  implicit def proxyAuthCompanion(p: Rep[AuthCompanion]): AuthCompanion =
    proxyOps[AuthCompanion](p)

  abstract class AbsLogin
      (user: Rep[String], password: Rep[String])
    extends Login(user, password) with Def[Login] {
    lazy val selfType = element[Login]
  }
  // elem for concrete class
  class LoginElem(val iso: Iso[LoginData, Login])
    extends AuthElem[SOption[String], Login]
    with ConcreteElem[LoginData, Login] {
    override lazy val parent: Option[Elem[_]] = Some(authElement(sOptionElement(StringElement)))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }

    override def convertAuth(x: Rep[Auth[SOption[String]]]) = // Converter is not generated by meta
!!!("Cannot convert from Auth to Login: missing fields List(user, password)")
    override def getDefaultRep = Login("", "")
    override lazy val tag = {
      weakTypeTag[Login]
    }
  }

  // state representation type
  type LoginData = (String, String)

  // 3) Iso for concrete class
  class LoginIso
    extends Iso[LoginData, Login]()(pairElement(implicitly[Elem[String]], implicitly[Elem[String]])) {
    override def from(p: Rep[Login]) =
      (p.user, p.password)
    override def to(p: Rep[(String, String)]) = {
      val Pair(user, password) = p
      Login(user, password)
    }
    lazy val eTo = new LoginElem(this)
  }
  // 4) constructor and deconstructor
  class LoginCompanionAbs extends CompanionDef[LoginCompanionAbs] with LoginCompanion {
    def selfType = LoginCompanionElem
    override def toString = "Login"
    def apply(p: Rep[LoginData]): Rep[Login] =
      isoLogin.to(p)
    def apply(user: Rep[String], password: Rep[String]): Rep[Login] =
      mkLogin(user, password)
  }
  object LoginMatcher {
    def unapply(p: Rep[Auth[SOption[String]]]) = unmkLogin(p)
  }
  lazy val Login: Rep[LoginCompanionAbs] = new LoginCompanionAbs
  implicit def proxyLoginCompanion(p: Rep[LoginCompanionAbs]): LoginCompanionAbs = {
    proxyOps[LoginCompanionAbs](p)
  }

  implicit case object LoginCompanionElem extends CompanionElem[LoginCompanionAbs] {
    lazy val tag = weakTypeTag[LoginCompanionAbs]
    protected def getDefaultRep = Login
  }

  implicit def proxyLogin(p: Rep[Login]): Login =
    proxyOps[Login](p)

  implicit class ExtendedLogin(p: Rep[Login]) {
    def toData: Rep[LoginData] = isoLogin.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoLogin: Iso[LoginData, Login] =
    cachedIso[LoginIso]()

  // 6) smart constructor and deconstructor
  def mkLogin(user: Rep[String], password: Rep[String]): Rep[Login]
  def unmkLogin(p: Rep[Auth[SOption[String]]]): Option[(Rep[String], Rep[String])]

  abstract class AbsHasPermission
      (user: Rep[String], password: Rep[String])
    extends HasPermission(user, password) with Def[HasPermission] {
    lazy val selfType = element[HasPermission]
  }
  // elem for concrete class
  class HasPermissionElem(val iso: Iso[HasPermissionData, HasPermission])
    extends AuthElem[Boolean, HasPermission]
    with ConcreteElem[HasPermissionData, HasPermission] {
    override lazy val parent: Option[Elem[_]] = Some(authElement(BooleanElement))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }

    override def convertAuth(x: Rep[Auth[Boolean]]) = // Converter is not generated by meta
!!!("Cannot convert from Auth to HasPermission: missing fields List(user, password)")
    override def getDefaultRep = HasPermission("", "")
    override lazy val tag = {
      weakTypeTag[HasPermission]
    }
  }

  // state representation type
  type HasPermissionData = (String, String)

  // 3) Iso for concrete class
  class HasPermissionIso
    extends Iso[HasPermissionData, HasPermission]()(pairElement(implicitly[Elem[String]], implicitly[Elem[String]])) {
    override def from(p: Rep[HasPermission]) =
      (p.user, p.password)
    override def to(p: Rep[(String, String)]) = {
      val Pair(user, password) = p
      HasPermission(user, password)
    }
    lazy val eTo = new HasPermissionElem(this)
  }
  // 4) constructor and deconstructor
  class HasPermissionCompanionAbs extends CompanionDef[HasPermissionCompanionAbs] with HasPermissionCompanion {
    def selfType = HasPermissionCompanionElem
    override def toString = "HasPermission"
    def apply(p: Rep[HasPermissionData]): Rep[HasPermission] =
      isoHasPermission.to(p)
    def apply(user: Rep[String], password: Rep[String]): Rep[HasPermission] =
      mkHasPermission(user, password)
  }
  object HasPermissionMatcher {
    def unapply(p: Rep[Auth[Boolean]]) = unmkHasPermission(p)
  }
  lazy val HasPermission: Rep[HasPermissionCompanionAbs] = new HasPermissionCompanionAbs
  implicit def proxyHasPermissionCompanion(p: Rep[HasPermissionCompanionAbs]): HasPermissionCompanionAbs = {
    proxyOps[HasPermissionCompanionAbs](p)
  }

  implicit case object HasPermissionCompanionElem extends CompanionElem[HasPermissionCompanionAbs] {
    lazy val tag = weakTypeTag[HasPermissionCompanionAbs]
    protected def getDefaultRep = HasPermission
  }

  implicit def proxyHasPermission(p: Rep[HasPermission]): HasPermission =
    proxyOps[HasPermission](p)

  implicit class ExtendedHasPermission(p: Rep[HasPermission]) {
    def toData: Rep[HasPermissionData] = isoHasPermission.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoHasPermission: Iso[HasPermissionData, HasPermission] =
    cachedIso[HasPermissionIso]()

  // 6) smart constructor and deconstructor
  def mkHasPermission(user: Rep[String], password: Rep[String]): Rep[HasPermission]
  def unmkHasPermission(p: Rep[Auth[Boolean]]): Option[(Rep[String], Rep[String])]

  registerModule(Authentications_Module)
}

// Seq -----------------------------------
trait AuthenticationsSeq extends AuthenticationsDsl with scalan.ScalanSeq {
  self: AuthenticationsDslSeq =>
  lazy val Auth: Rep[AuthCompanionAbs] = new AuthCompanionAbs {
  }

  case class SeqLogin
      (override val user: Rep[String], override val password: Rep[String])
    extends AbsLogin(user, password) {
  }

  def mkLogin
    (user: Rep[String], password: Rep[String]): Rep[Login] =
    new SeqLogin(user, password)
  def unmkLogin(p: Rep[Auth[SOption[String]]]) = p match {
    case p: Login @unchecked =>
      Some((p.user, p.password))
    case _ => None
  }

  case class SeqHasPermission
      (override val user: Rep[String], override val password: Rep[String])
    extends AbsHasPermission(user, password) {
  }

  def mkHasPermission
    (user: Rep[String], password: Rep[String]): Rep[HasPermission] =
    new SeqHasPermission(user, password)
  def unmkHasPermission(p: Rep[Auth[Boolean]]) = p match {
    case p: HasPermission @unchecked =>
      Some((p.user, p.password))
    case _ => None
  }
}

// Exp -----------------------------------
trait AuthenticationsExp extends AuthenticationsDsl with scalan.ScalanExp {
  self: AuthenticationsDslExp =>
  lazy val Auth: Rep[AuthCompanionAbs] = new AuthCompanionAbs {
  }

  case class ExpLogin
      (override val user: Rep[String], override val password: Rep[String])
    extends AbsLogin(user, password)

  object LoginMethods {
    object toOper {
      def unapply(d: Def[_]): Option[Rep[Login]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[LoginElem] && method.getName == "toOper" =>
          Some(receiver).asInstanceOf[Option[Rep[Login]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Login]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object LoginCompanionMethods {
  }

  def mkLogin
    (user: Rep[String], password: Rep[String]): Rep[Login] =
    new ExpLogin(user, password)
  def unmkLogin(p: Rep[Auth[SOption[String]]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: LoginElem @unchecked =>
      Some((p.asRep[Login].user, p.asRep[Login].password))
    case _ =>
      None
  }

  case class ExpHasPermission
      (override val user: Rep[String], override val password: Rep[String])
    extends AbsHasPermission(user, password)

  object HasPermissionMethods {
    object eA {
      def unapply(d: Def[_]): Option[Rep[HasPermission]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[HasPermissionElem] && method.getName == "eA" =>
          Some(receiver).asInstanceOf[Option[Rep[HasPermission]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[HasPermission]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object toOper {
      def unapply(d: Def[_]): Option[Rep[HasPermission]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[HasPermissionElem] && method.getName == "toOper" =>
          Some(receiver).asInstanceOf[Option[Rep[HasPermission]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[HasPermission]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object HasPermissionCompanionMethods {
  }

  def mkHasPermission
    (user: Rep[String], password: Rep[String]): Rep[HasPermission] =
    new ExpHasPermission(user, password)
  def unmkHasPermission(p: Rep[Auth[Boolean]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: HasPermissionElem @unchecked =>
      Some((p.asRep[HasPermission].user, p.asRep[HasPermission].password))
    case _ =>
      None
  }

  object AuthMethods {
    object toOper {
      def unapply(d: Def[_]): Option[Rep[Auth[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[AuthElem[_, _]] && method.getName == "toOper" =>
          Some(receiver).asInstanceOf[Option[Rep[Auth[A]] forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Auth[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object AuthCompanionMethods {
  }
}

object Authentications_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAAL1WzW8bRRQfr+M4ttOmRCjQShWpMSBQsQMS6iGHyk1dPmQcK1sQMhXSeD12pszObGbGYc2hfwDcEFcEPSL1xgkhVUgICXHghACJM6dShCqgJxBvZj/8kTjNBfYwmn375n38fu+9nVt3UU5J9KTyMMO86hONq67d15WuuA2uqR69KnpDRi6T/qPiy0+e+/TM5w5a6aDFXawuK9ZBhWjTCIN075K9Jipg7hGlhVQanWtaDzVPMEY8TQWvUd8fatxlpNakSm820UJX9EZ76AbKNNEpT3BPEk3cLYaVIiqWLxETEU3fC/Z9tB2MffCayaI2kcVViamG8MHHqUh/hwTuiAs+8jU6GYe2HZiwQCdP/UBInbjIg7ld0UteFzgGAVptXsf7uAYuBjVXS8oHcLIUYO9tPCAtUDHqCxCwIqx/dRTY92wTFRXZA4Be9gNmJWGAEAIGnrdBVMf4VFN8qgafikskxYy+i83HthThCEVPJotQGICJ8w8wkVggDd6rvHfNe/O+W/Idczg0oeRthotg6LE51WCpABy/2flA3Xvx5gUHFTuoSFW9q7TEnp6kPEarhDkX2sacAojlANgqz2PLeqmDzkxJFDzhB5iDpRjKZeCJUY9qo2xkyzE7c6DP64AkqpkwyKT5rs/J19bNFmasfef0s0/82njDQc60iwKYdKHwZWJUo4X6UO/Gps26olHejWordfj4PIcBaUvqQ4Hvkxe++uK132+3ctbnao/08ZDp1zEbkqjc4gjG0RjnTrms0eJYoRDOrvkj8k2Rf+rOb72vN9A1J+UrTu94JQImcuqnH0rfP33RQUsd21BXGB50gDLVYMTflluC6w5aEvtERl/y+5iZ3aElk4/Tj4mcZCALDGi0Prf1A2Lo2bRtlkkAKEWd0hKcVK60K3+53354yzSCRMvRl4ivf+iFv38+2de2R4DZoSIy4TQLIyRCwyyPRABbwdnUk1mAj6UAMHlHyN6RZ6cpKkZxuMInD5Xv0bduvq8tGZlwevRsd69Dr2/ac+eO4CUZgX92Npw/Tv/4sYMKAH+Xah8HlY1jNu5/2IwoBWy8lAMznMWA8q1JZ+XxsDpjtxrlrNbMx1JmugtnG3MNzvUxU1A++UtCMIL5QSasl4lDB8j9/4rCrOftWpuH1tpLWLWJ9KlSANaDUDsxpT1Wmgh7MfY3jWQWCulobEGUqU8YO5DNsVM6YSwfkkkyaa0kHUtn5w9WKLa1nebD7O7F2w7KvQLcw7RRTZTriiHvJVUM1w5NQn0pkWWmqxiqFkvsp1Vrn3U0Dmtu3q1pfEFxxYRvriNe1GUgiaMnIYZmIirGR0KCh6flxj0FfNy4/1Hrme8++8X+LoqmO2G48fQSM/mbmOZpdSYMuJxMJAAom8a1wf8L+rFFoiwKAAA="
}
}

