package scalan.common

import scala.reflect.runtime.universe._
import scalan._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait SegmentsAbs extends scalan.ScalanDsl with Segments {
  self: SegmentsDsl =>

  // single proxy for each type family
  implicit def proxySegment(p: Rep[Segment]): Segment = {
    proxyOps[Segment](p)(scala.reflect.classTag[Segment])
  }

  // familyElem
  class SegmentElem[To <: Segment]
    extends EntityElem[To] {
    lazy val parent: Option[Elem[_]] = None
    lazy val typeArgs = TypeArgs()
    override def isEntityType = true
    override lazy val tag = {
      weakTypeTag[Segment].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[Segment] => convertSegment(x) }
      tryConvert(element[Segment], this, x, conv)
    }

    def convertSegment(x: Rep[Segment]): Rep[To] = {
      x.selfType1 match {
        case _: SegmentElem[_] => x.asRep[To]
        case e => !!!(s"Expected $x to have SegmentElem[_], but got $e", x)
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def segmentElement: Elem[Segment] =
    cachedElem[SegmentElem[Segment]]()

  implicit case object SegmentCompanionElem extends CompanionElem[SegmentCompanionAbs] {
    lazy val tag = weakTypeTag[SegmentCompanionAbs]
    protected def getDefaultRep = Segment
  }

  abstract class SegmentCompanionAbs extends CompanionDef[SegmentCompanionAbs] with SegmentCompanion {
    def selfType = SegmentCompanionElem
    override def toString = "Segment"
  }
  def Segment: Rep[SegmentCompanionAbs]
  implicit def proxySegmentCompanionAbs(p: Rep[SegmentCompanionAbs]): SegmentCompanionAbs =
    proxyOps[SegmentCompanionAbs](p)

  abstract class AbsInterval
      (start: Rep[Int], end: Rep[Int])
    extends Interval(start, end) with Def[Interval] {
    lazy val selfType = element[Interval]
  }
  // elem for concrete class
  class IntervalElem(val iso: Iso[IntervalData, Interval])
    extends SegmentElem[Interval]
    with ConcreteElem[IntervalData, Interval] {
    override lazy val parent: Option[Elem[_]] = Some(segmentElement)
    override lazy val typeArgs = TypeArgs()

    override def convertSegment(x: Rep[Segment]) = Interval(x.start, x.end)
    override def getDefaultRep = Interval(0, 0)
    override lazy val tag = {
      weakTypeTag[Interval]
    }
  }

  // state representation type
  type IntervalData = (Int, Int)

  // 3) Iso for concrete class
  class IntervalIso
    extends EntityIso[IntervalData, Interval] with Def[IntervalIso] {
    override def from(p: Rep[Interval]) =
      (p.start, p.end)
    override def to(p: Rep[(Int, Int)]) = {
      val Pair(start, end) = p
      Interval(start, end)
    }
    lazy val eFrom = pairElement(element[Int], element[Int])
    lazy val eTo = new IntervalElem(self)
    lazy val selfType = new IntervalIsoElem
    def productArity = 0
    def productElement(n: Int) = ???
  }
  case class IntervalIsoElem() extends Elem[IntervalIso] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new IntervalIso())
    lazy val tag = {
      weakTypeTag[IntervalIso]
    }
    lazy val typeArgs = TypeArgs()
  }
  // 4) constructor and deconstructor
  class IntervalCompanionAbs extends CompanionDef[IntervalCompanionAbs] with IntervalCompanion {
    def selfType = IntervalCompanionElem
    override def toString = "Interval"
    @scalan.OverloadId("fromData")
    def apply(p: Rep[IntervalData]): Rep[Interval] =
      isoInterval.to(p)
    @scalan.OverloadId("fromFields")
    def apply(start: Rep[Int], end: Rep[Int]): Rep[Interval] =
      mkInterval(start, end)

    def unapply(p: Rep[Segment]) = unmkInterval(p)
  }
  lazy val IntervalRep: Rep[IntervalCompanionAbs] = new IntervalCompanionAbs
  lazy val Interval: IntervalCompanionAbs = proxyIntervalCompanion(IntervalRep)
  implicit def proxyIntervalCompanion(p: Rep[IntervalCompanionAbs]): IntervalCompanionAbs = {
    proxyOps[IntervalCompanionAbs](p)
  }

  implicit case object IntervalCompanionElem extends CompanionElem[IntervalCompanionAbs] {
    lazy val tag = weakTypeTag[IntervalCompanionAbs]
    protected def getDefaultRep = Interval
  }

  implicit def proxyInterval(p: Rep[Interval]): Interval =
    proxyOps[Interval](p)

  implicit class ExtendedInterval(p: Rep[Interval]) {
    def toData: Rep[IntervalData] = isoInterval.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoInterval: Iso[IntervalData, Interval] =
    reifyObject(new IntervalIso())

  // 6) smart constructor and deconstructor
  def mkInterval(start: Rep[Int], end: Rep[Int]): Rep[Interval]
  def unmkInterval(p: Rep[Segment]): Option[(Rep[Int], Rep[Int])]

  abstract class AbsSlice
      (start: Rep[Int], length: Rep[Int])
    extends Slice(start, length) with Def[Slice] {
    lazy val selfType = element[Slice]
  }
  // elem for concrete class
  class SliceElem(val iso: Iso[SliceData, Slice])
    extends SegmentElem[Slice]
    with ConcreteElem[SliceData, Slice] {
    override lazy val parent: Option[Elem[_]] = Some(segmentElement)
    override lazy val typeArgs = TypeArgs()

    override def convertSegment(x: Rep[Segment]) = Slice(x.start, x.length)
    override def getDefaultRep = Slice(0, 0)
    override lazy val tag = {
      weakTypeTag[Slice]
    }
  }

  // state representation type
  type SliceData = (Int, Int)

  // 3) Iso for concrete class
  class SliceIso
    extends EntityIso[SliceData, Slice] with Def[SliceIso] {
    override def from(p: Rep[Slice]) =
      (p.start, p.length)
    override def to(p: Rep[(Int, Int)]) = {
      val Pair(start, length) = p
      Slice(start, length)
    }
    lazy val eFrom = pairElement(element[Int], element[Int])
    lazy val eTo = new SliceElem(self)
    lazy val selfType = new SliceIsoElem
    def productArity = 0
    def productElement(n: Int) = ???
  }
  case class SliceIsoElem() extends Elem[SliceIso] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new SliceIso())
    lazy val tag = {
      weakTypeTag[SliceIso]
    }
    lazy val typeArgs = TypeArgs()
  }
  // 4) constructor and deconstructor
  class SliceCompanionAbs extends CompanionDef[SliceCompanionAbs] with SliceCompanion {
    def selfType = SliceCompanionElem
    override def toString = "Slice"
    @scalan.OverloadId("fromData")
    def apply(p: Rep[SliceData]): Rep[Slice] =
      isoSlice.to(p)
    @scalan.OverloadId("fromFields")
    def apply(start: Rep[Int], length: Rep[Int]): Rep[Slice] =
      mkSlice(start, length)

    def unapply(p: Rep[Segment]) = unmkSlice(p)
  }
  lazy val SliceRep: Rep[SliceCompanionAbs] = new SliceCompanionAbs
  lazy val Slice: SliceCompanionAbs = proxySliceCompanion(SliceRep)
  implicit def proxySliceCompanion(p: Rep[SliceCompanionAbs]): SliceCompanionAbs = {
    proxyOps[SliceCompanionAbs](p)
  }

  implicit case object SliceCompanionElem extends CompanionElem[SliceCompanionAbs] {
    lazy val tag = weakTypeTag[SliceCompanionAbs]
    protected def getDefaultRep = Slice
  }

  implicit def proxySlice(p: Rep[Slice]): Slice =
    proxyOps[Slice](p)

  implicit class ExtendedSlice(p: Rep[Slice]) {
    def toData: Rep[SliceData] = isoSlice.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoSlice: Iso[SliceData, Slice] =
    reifyObject(new SliceIso())

  // 6) smart constructor and deconstructor
  def mkSlice(start: Rep[Int], length: Rep[Int]): Rep[Slice]
  def unmkSlice(p: Rep[Segment]): Option[(Rep[Int], Rep[Int])]

  abstract class AbsCentered
      (center: Rep[Int], radius: Rep[Int])
    extends Centered(center, radius) with Def[Centered] {
    lazy val selfType = element[Centered]
  }
  // elem for concrete class
  class CenteredElem(val iso: Iso[CenteredData, Centered])
    extends SegmentElem[Centered]
    with ConcreteElem[CenteredData, Centered] {
    override lazy val parent: Option[Elem[_]] = Some(segmentElement)
    override lazy val typeArgs = TypeArgs()

    override def convertSegment(x: Rep[Segment]) = // Converter is not generated by meta
!!!("Cannot convert from Segment to Centered: missing fields List(center, radius)")
    override def getDefaultRep = Centered(0, 0)
    override lazy val tag = {
      weakTypeTag[Centered]
    }
  }

  // state representation type
  type CenteredData = (Int, Int)

  // 3) Iso for concrete class
  class CenteredIso
    extends EntityIso[CenteredData, Centered] with Def[CenteredIso] {
    override def from(p: Rep[Centered]) =
      (p.center, p.radius)
    override def to(p: Rep[(Int, Int)]) = {
      val Pair(center, radius) = p
      Centered(center, radius)
    }
    lazy val eFrom = pairElement(element[Int], element[Int])
    lazy val eTo = new CenteredElem(self)
    lazy val selfType = new CenteredIsoElem
    def productArity = 0
    def productElement(n: Int) = ???
  }
  case class CenteredIsoElem() extends Elem[CenteredIso] {
    def isEntityType = true
    def getDefaultRep = reifyObject(new CenteredIso())
    lazy val tag = {
      weakTypeTag[CenteredIso]
    }
    lazy val typeArgs = TypeArgs()
  }
  // 4) constructor and deconstructor
  class CenteredCompanionAbs extends CompanionDef[CenteredCompanionAbs] with CenteredCompanion {
    def selfType = CenteredCompanionElem
    override def toString = "Centered"
    @scalan.OverloadId("fromData")
    def apply(p: Rep[CenteredData]): Rep[Centered] =
      isoCentered.to(p)
    @scalan.OverloadId("fromFields")
    def apply(center: Rep[Int], radius: Rep[Int]): Rep[Centered] =
      mkCentered(center, radius)

    def unapply(p: Rep[Segment]) = unmkCentered(p)
  }
  lazy val CenteredRep: Rep[CenteredCompanionAbs] = new CenteredCompanionAbs
  lazy val Centered: CenteredCompanionAbs = proxyCenteredCompanion(CenteredRep)
  implicit def proxyCenteredCompanion(p: Rep[CenteredCompanionAbs]): CenteredCompanionAbs = {
    proxyOps[CenteredCompanionAbs](p)
  }

  implicit case object CenteredCompanionElem extends CompanionElem[CenteredCompanionAbs] {
    lazy val tag = weakTypeTag[CenteredCompanionAbs]
    protected def getDefaultRep = Centered
  }

  implicit def proxyCentered(p: Rep[Centered]): Centered =
    proxyOps[Centered](p)

  implicit class ExtendedCentered(p: Rep[Centered]) {
    def toData: Rep[CenteredData] = isoCentered.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoCentered: Iso[CenteredData, Centered] =
    reifyObject(new CenteredIso())

  // 6) smart constructor and deconstructor
  def mkCentered(center: Rep[Int], radius: Rep[Int]): Rep[Centered]
  def unmkCentered(p: Rep[Segment]): Option[(Rep[Int], Rep[Int])]

  registerModule(Segments_Module)
}

// Std -----------------------------------
trait SegmentsStd extends scalan.ScalanDslStd with SegmentsDsl {
  self: SegmentsDslStd =>

  lazy val Segment: Rep[SegmentCompanionAbs] = new SegmentCompanionAbs {
  }

  case class StdInterval
      (override val start: Rep[Int], override val end: Rep[Int])
    extends AbsInterval(start, end) {
  }

  def mkInterval
    (start: Rep[Int], end: Rep[Int]): Rep[Interval] =
    new StdInterval(start, end)
  def unmkInterval(p: Rep[Segment]) = p match {
    case p: Interval @unchecked =>
      Some((p.start, p.end))
    case _ => None
  }

  case class StdSlice
      (override val start: Rep[Int], override val length: Rep[Int])
    extends AbsSlice(start, length) {
  }

  def mkSlice
    (start: Rep[Int], length: Rep[Int]): Rep[Slice] =
    new StdSlice(start, length)
  def unmkSlice(p: Rep[Segment]) = p match {
    case p: Slice @unchecked =>
      Some((p.start, p.length))
    case _ => None
  }

  case class StdCentered
      (override val center: Rep[Int], override val radius: Rep[Int])
    extends AbsCentered(center, radius) {
  }

  def mkCentered
    (center: Rep[Int], radius: Rep[Int]): Rep[Centered] =
    new StdCentered(center, radius)
  def unmkCentered(p: Rep[Segment]) = p match {
    case p: Centered @unchecked =>
      Some((p.center, p.radius))
    case _ => None
  }
}

// Exp -----------------------------------
trait SegmentsExp extends scalan.ScalanDslExp with SegmentsDsl {
  self: SegmentsDslExp =>

  lazy val Segment: Rep[SegmentCompanionAbs] = new SegmentCompanionAbs {
  }

  case class ExpInterval
      (override val start: Rep[Int], override val end: Rep[Int])
    extends AbsInterval(start, end)

  object IntervalMethods {
    object length {
      def unapply(d: Def[_]): Option[Rep[Interval]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[IntervalElem] && method.getName == "length" =>
          Some(receiver).asInstanceOf[Option[Rep[Interval]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Interval]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object shift {
      def unapply(d: Def[_]): Option[(Rep[Interval], Rep[Int])] = d match {
        case MethodCall(receiver, method, Seq(ofs, _*), _) if receiver.elem.isInstanceOf[IntervalElem] && method.getName == "shift" =>
          Some((receiver, ofs)).asInstanceOf[Option[(Rep[Interval], Rep[Int])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Interval], Rep[Int])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object attach {
      def unapply(d: Def[_]): Option[(Rep[Interval], Rep[Segment])] = d match {
        case MethodCall(receiver, method, Seq(seg, _*), _) if receiver.elem.isInstanceOf[IntervalElem] && method.getName == "attach" =>
          Some((receiver, seg)).asInstanceOf[Option[(Rep[Interval], Rep[Segment])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Interval], Rep[Segment])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object IntervalCompanionMethods {
  }

  def mkInterval
    (start: Rep[Int], end: Rep[Int]): Rep[Interval] =
    new ExpInterval(start, end)
  def unmkInterval(p: Rep[Segment]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: IntervalElem @unchecked =>
      Some((p.asRep[Interval].start, p.asRep[Interval].end))
    case _ =>
      None
  }

  case class ExpSlice
      (override val start: Rep[Int], override val length: Rep[Int])
    extends AbsSlice(start, length)

  object SliceMethods {
    object end {
      def unapply(d: Def[_]): Option[Rep[Slice]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[SliceElem] && method.getName == "end" =>
          Some(receiver).asInstanceOf[Option[Rep[Slice]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Slice]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object shift {
      def unapply(d: Def[_]): Option[(Rep[Slice], Rep[Int])] = d match {
        case MethodCall(receiver, method, Seq(ofs, _*), _) if receiver.elem.isInstanceOf[SliceElem] && method.getName == "shift" =>
          Some((receiver, ofs)).asInstanceOf[Option[(Rep[Slice], Rep[Int])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Slice], Rep[Int])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object attach {
      def unapply(d: Def[_]): Option[(Rep[Slice], Rep[Segment])] = d match {
        case MethodCall(receiver, method, Seq(seg, _*), _) if receiver.elem.isInstanceOf[SliceElem] && method.getName == "attach" =>
          Some((receiver, seg)).asInstanceOf[Option[(Rep[Slice], Rep[Segment])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Slice], Rep[Segment])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object SliceCompanionMethods {
  }

  def mkSlice
    (start: Rep[Int], length: Rep[Int]): Rep[Slice] =
    new ExpSlice(start, length)
  def unmkSlice(p: Rep[Segment]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: SliceElem @unchecked =>
      Some((p.asRep[Slice].start, p.asRep[Slice].length))
    case _ =>
      None
  }

  case class ExpCentered
      (override val center: Rep[Int], override val radius: Rep[Int])
    extends AbsCentered(center, radius)

  object CenteredMethods {
    object start {
      def unapply(d: Def[_]): Option[Rep[Centered]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[CenteredElem] && method.getName == "start" =>
          Some(receiver).asInstanceOf[Option[Rep[Centered]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Centered]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object end {
      def unapply(d: Def[_]): Option[Rep[Centered]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[CenteredElem] && method.getName == "end" =>
          Some(receiver).asInstanceOf[Option[Rep[Centered]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Centered]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object length {
      def unapply(d: Def[_]): Option[Rep[Centered]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[CenteredElem] && method.getName == "length" =>
          Some(receiver).asInstanceOf[Option[Rep[Centered]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Centered]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object shift {
      def unapply(d: Def[_]): Option[(Rep[Centered], Rep[Int])] = d match {
        case MethodCall(receiver, method, Seq(ofs, _*), _) if receiver.elem.isInstanceOf[CenteredElem] && method.getName == "shift" =>
          Some((receiver, ofs)).asInstanceOf[Option[(Rep[Centered], Rep[Int])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Centered], Rep[Int])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object attach {
      def unapply(d: Def[_]): Option[(Rep[Centered], Rep[Segment])] = d match {
        case MethodCall(receiver, method, Seq(seg, _*), _) if receiver.elem.isInstanceOf[CenteredElem] && method.getName == "attach" =>
          Some((receiver, seg)).asInstanceOf[Option[(Rep[Centered], Rep[Segment])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Centered], Rep[Segment])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object CenteredCompanionMethods {
  }

  def mkCentered
    (center: Rep[Int], radius: Rep[Int]): Rep[Centered] =
    new ExpCentered(center, radius)
  def unmkCentered(p: Rep[Segment]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: CenteredElem @unchecked =>
      Some((p.asRep[Centered].center, p.asRep[Centered].radius))
    case _ =>
      None
  }

  object SegmentMethods {
    object start {
      def unapply(d: Def[_]): Option[Rep[Segment]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[SegmentElem[_]] && method.getName == "start" =>
          Some(receiver).asInstanceOf[Option[Rep[Segment]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Segment]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object length {
      def unapply(d: Def[_]): Option[Rep[Segment]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[SegmentElem[_]] && method.getName == "length" =>
          Some(receiver).asInstanceOf[Option[Rep[Segment]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Segment]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object end {
      def unapply(d: Def[_]): Option[Rep[Segment]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[SegmentElem[_]] && method.getName == "end" =>
          Some(receiver).asInstanceOf[Option[Rep[Segment]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[Segment]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object shift {
      def unapply(d: Def[_]): Option[(Rep[Segment], Rep[Int])] = d match {
        case MethodCall(receiver, method, Seq(ofs, _*), _) if receiver.elem.isInstanceOf[SegmentElem[_]] && method.getName == "shift" =>
          Some((receiver, ofs)).asInstanceOf[Option[(Rep[Segment], Rep[Int])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Segment], Rep[Int])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }

    object attach {
      def unapply(d: Def[_]): Option[(Rep[Segment], Rep[Segment])] = d match {
        case MethodCall(receiver, method, Seq(seg, _*), _) if receiver.elem.isInstanceOf[SegmentElem[_]] && method.getName == "attach" =>
          Some((receiver, seg)).asInstanceOf[Option[(Rep[Segment], Rep[Segment])]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[(Rep[Segment], Rep[Segment])] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object SegmentCompanionMethods {
  }
}

object Segments_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAALVWTYgcRRSumdmd33XdXQmYvbiuEw1GZxaDRFhElt2JRCazQzoxMoZITffb2Y7V3WV3zTLjIYKHHNSTiAfBQ0DxEgTxpiCCCiIi6NWzp6iEHMxJ8VV1dU/PZDs7CjuHorvq9Xvv+7733tSNP8hs4JNHA5My6tYcELRmqOeNQFSNhitsMTzrWX0GW7Dz84mvvfKb77SyZKFD8rs02ApYh5TCh8aAx8+GsJqkRF0TAuH5gSAPN1WEuukxBqawPbduO05f0C6DetMOxHqTzHQ9a/gauUoyTbJgeq7pgwBjk9EggEDvF0FmZMfvJfU+3OajGG5doqgnUJz3qS0wfYyxENqfA24MXc8dOoLM69S2uUwLbSow4IjhjMOZCpNrkoLtcM8XUdQCRtj1rOh1xqW4QZaaV+gerWPUXt0Qvu32pDNOzVdpD1poIs1nEEMAbOf8kIN2XgmENRZvwAkhqMpTKrHaiLNazFlNclY1wLcps1+n8rDte4MhCX+ZHCEDji6eOMBF5AEarlV965L58h2j4mTlxwOZSkEllEdHD6VUiJIHuf3+3LvB7eevn8qScoeU7WCjGwifmiJZBpquCnVdT6icYwap30MFV9MUVFE20GaiTEqm53DqoifN5RwKxWzTFtJY7s1peVK4LwgOkWlmwDMx3pUUvKqWNilj7ZtHnzz2e+OlLMmOhyihSwObwY+cClIwoOdg5SlK5VLS7KbHiRE/dvNP67s1cikb86TdTicNulh65oMvjkH70ywpdlQln2a0p0SSRGxBYHZI0dsDP9wv7FEmn/YVqmDBDu0zoelL4s4hbkFWUpuQgyRlXRV3JoJfCeuz5blQPd2u/mX88N4NWX4+mQtPwq78xz7196/zO0JVpsCRJajuxvsFyWE3x2Q8kqYbh7ZvOzg79uDpb768cOur1qySbkkjepGyPoRtqwGNwMmYmTWMdCbUsDRQ8Y7ESOSyjOfgWndnJZeVkeoj7cshRMNzYHH1tn35+ttCqZwZjA+T7e4VbN519d3RewgezbnPrl07cuujVx5QvVjs2sKhvLr2HzoxapxD7DQS8xbOmKXRuyITGV9EtsHHatxMhl6e/EaQYmQ4cV6J2llLktqGylnC9i5h0wouIe3+H+YZuD2xO3VRyPW4Wk9MwdC8gQrAQfTMKqtD4yZvgqT//5DjU8vuB4dEzuKmygusA8snMhydJ7LO64DjdOWwzaYicIzGiaSnx7Kgne8DZeLvZXki/mW1qcP5pJYyOrbAZBQpkJcQkL7CoXDy/ecuvvDgxQtqLM1Zyig8iSf9/le6s5SvqwvI8XtcQNCo2nA4XjDx4eS3z/7yxo+ffKxG/IgBlEejw5D36exxFjl6ZElQqymgDD2BUKurdz5sPf7T57+pkV+Wswz/cdz4Qpcc9eOyVqLoeENLUCvIjJxvCRnVKf8XE3IpGVALAAA="
}
}

trait SegmentsDsl extends impl.SegmentsAbs
trait SegmentsDslStd extends impl.SegmentsStd
trait SegmentsDslExp extends impl.SegmentsExp
