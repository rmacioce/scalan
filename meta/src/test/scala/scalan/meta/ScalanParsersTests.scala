package scalan.meta

import scalan.BaseNestedTests
import scala.reflect.internal.util.BatchSourceFile
import scalan.meta.ScalanAst._

class ScalanParsersTests extends BaseNestedTests with ScalanParsersEx {
  val ast: this.type = this
  import scalan.meta.ScalanAst.
  {
    STraitCall => TC,
    STraitDef => TD,
    SClassDef => CD,
    SMethodDef => MD,
    SMethodArgs => MAs,
    SMethodArg => MA,
    STpeTuple => T,
    SEntityModuleDef => EMD,
    SImportStat => IS
  }
  import scala.{ List => L }
  import compiler._

  val INT = STpePrimitives("Int")
  val BOOL = STpePrimitives("Boolean")
  val FLOAT = STpePrimitives("Float")

  val config = BoilerplateToolRun.coreTestsConfig

  sealed trait TreeKind
  case object TopLevel extends TreeKind
  case object Type extends TreeKind
  case object Member extends TreeKind
  case object Expr extends TreeKind
  case object Annotation extends TreeKind
  case object AnnotationArg extends TreeKind

  def parseString(kind: TreeKind, prog: String): Tree = {
    // wrap the string into a complete file
    val prog1 = kind match {
      case TopLevel => prog
      case Type => s"object o { val x: $prog }"
      case Member => s"object o { $prog }"
      case Expr => s"object o { val x = $prog }"
      case Annotation => s"object o { @$prog val x = null }"
      case AnnotationArg => s"object o { @OverloadId($prog) val x = null }"
    }
    val fakeSourceFile = new BatchSourceFile("<no file>", prog1.toCharArray)
    // extract the part corresponding to original prog
    (kind, parseFile(fakeSourceFile)) match {
      case (TopLevel, tree) => tree
      case (Member, PackageDef(_, List(ModuleDef(_, _, Template(_, _, List(_, tree)))))) =>
        tree
      case (Type, PackageDef(_, List(ModuleDef(_, _, Template(_, _, List(_, ValDef(_, _, tree, _))))))) =>
        tree
      case (Expr, PackageDef(_, List(ModuleDef(_, _, Template(_, _, List(_, ValDef(_, _, _, tree))))))) =>
        tree
      case (Annotation, PackageDef(_, List(ModuleDef(_, _, Template(_, _, List(_, ValDef(Modifiers(_,_,List(tree)), _, _, _))))))) =>
        tree
      case (AnnotationArg, PackageDef(_, List(ModuleDef(_, _, Template(_, _, List(_, ValDef(Modifiers(_,_,List(ExtractAnnotation(_,List(tree)))), _, _, _))))))) =>
        tree
      case (kind, tree) =>
        ???(tree)
    }
  }

  def test[A](kind: TreeKind, prog: String, expected: A)(f: Tree => A) {
    it(prog) {
      val tree = parseString(kind, prog)
      val res = f(tree)
      assertResult(expected)(res)
    }
  }

  def testModule(prog: String, expected: SEntityModuleDef) {
    test(TopLevel, prog, expected) { case tree: PackageDef => entityModule(tree) }
  }

  def testTrait(prog: String, expected: STraitDef) {
    test(Member, prog, expected) { case tree: ClassDef => traitDef(tree, Some(tree)) }
  }
  def testSClass(prog: String, expected: SClassDef) {
    test(Member, prog, expected) { case tree: ClassDef => classDef(tree, Some(tree)) }
  }

  def testSTpe(prog: String, expected: STpeExpr) {
    test(Type, prog, expected)(tpeExpr)
  }
  def testSMethod(prog: String, expected: SMethodDef) {
    test(Member, prog, expected) { case tree: DefDef => methodDef(tree) }
  }

  describe("STpeExpr") {
    testSTpe("Int", INT)
    testSTpe("(Int,Boolean)", STpeTuple(L(INT, BOOL)))
    testSTpe("Int=>Boolean", STpeFunc(INT, BOOL))
    testSTpe("Int=>Boolean=>Float", STpeFunc(INT, STpeFunc(BOOL, FLOAT)))
    testSTpe("(Int,Boolean=>Float)", STpeTuple(L(INT, STpeFunc(BOOL, FLOAT))))
    testSTpe("(Int,(Boolean=>Float))", STpeTuple(L(INT, STpeFunc(BOOL, FLOAT))))
    testSTpe("(Int,Boolean)=>Float", STpeFunc(STpeTuple(L(INT, BOOL)), FLOAT))
    testSTpe("Edge", TC("Edge", Nil))
    testSTpe("Edge[V,E]", TC("Edge", L(TC("V", Nil), TC("E", Nil))))
    testSTpe("Rep[A=>B]", TC("Rep", L(STpeFunc(TC("A", Nil), TC("B", Nil)))))
  }

  describe("SMethodDef") {
    testSMethod("def f: Int", MD("f", Nil, Nil, Some(INT), false, false, None, Nil, None))
    testSMethod("@OverloadId(\"a\") implicit def f: Int", MD("f", Nil, Nil, Some(INT), true, false, Some("a"), L(SMethodAnnotation("OverloadId",List(SConst("a")))), None))
    testSMethod(
      "def f(x: Int): Int",
      MD("f", Nil, L(MAs(List(MA(false, false, "x", INT, None)))), Some(INT), false, false, None, Nil, None))
    testSMethod(
      "def f[A <: T](x: A): Int",
      MD("f", L(STpeArg("A", Some(TC("T", Nil)), Nil)), L(MAs(L(MA(false, false, "x", TC("A", Nil), None)))), Some(INT), false, false, None, Nil, None))
    testSMethod(
      "def f[A : Numeric]: Int",
      MD("f", L(STpeArg("A", None, L("Numeric"))), Nil, Some(INT), false, false, None, Nil, None))
    testSMethod(
      "def f[A <: Int : Numeric : Fractional](x: A)(implicit y: A): Int",
      MD(
        "f",
        L(STpeArg("A", Some(INT), L("Numeric", "Fractional"))),
        L(MAs(L(MA(false, false, "x", TC("A", Nil), None))), MAs(L(MA(true, false, "y", TC("A", Nil), None)))),
        Some(INT), false, false, None, Nil, None))
  }

  describe("TraitDef") {
    val traitA = TD("A", Nil, Nil, Nil, None, None)
    val traitEdgeVE = TD("Edge", L(STpeArg("V", None, Nil), STpeArg("E", None, Nil)), Nil, Nil, None, None)

    testTrait("trait A", traitA)
    testTrait("trait A extends B",
      traitA.copy(ancestors = L(TC("B", Nil))))
    testTrait("trait A extends B with C",
      traitA.copy(ancestors = L(TC("B", Nil), TC("C", Nil))))
    testTrait("trait Edge[V,E]", traitEdgeVE)
    testTrait("trait Edge[V,E]{}", traitEdgeVE)
    testTrait("trait Edge[V,E]{ def f[A <: T](x: A, y: (A,T)): Int }",
      traitEdgeVE.copy(
        body = L(MD("f", L(STpeArg("A", Some(TC("T", Nil)), Nil)),
          L(MAs(L(MA(false, false, "x", TC("A", Nil), None), MA(false, false, "y", T(L(TC("A", Nil), TC("T", Nil))), None)))),
          Some(INT), false, false, None, Nil, None))))
    testTrait(
      """trait A {
        |  import scalan._
        |  type Rep[A] = A
        |  def f: (Int,A)
        |  @OverloadId("b")
        |  def g(x: Boolean): A
        |}""".stripMargin,
      TD("A", Nil, Nil, L(
        IS("scalan._"),
        STpeDef("Rep", L(STpeArg("A", None, Nil)), TC("A", Nil)),
        MD("f", Nil, Nil, Some(T(L(INT, TC("A", Nil)))), false, false, None, Nil, None),
        MD("g", Nil, L(MAs(L(MA(false, false, "x", BOOL, None)))), Some(TC("A", Nil)), false, false, Some("b"), L(SMethodAnnotation("OverloadId",List(SConst("b")))), None)), None, None))

  }

  val reactiveTrait =
    """trait Reactive extends ScalanDsl {
      |  type Obs[A] = Rep[Observable[A]]
      |  trait Observable[A] {
      |    implicit def eA: Elem[A]
      |  }
      |  class ObservableImpl[A](implicit val eA: Elem[A]) extends Observable[A] {
      |  }
      |}
    """.stripMargin

  describe("SClassDef") {
    val classA =
      CD("A", Nil, SClassArgs(Nil), SClassArgs(Nil), Nil, Nil, None, None, false)
    val classEdgeVE =
      CD("Edge", L(STpeArg("V", None, Nil), STpeArg("E", None, Nil)), SClassArgs(Nil), SClassArgs(Nil), Nil, Nil, None, None, false)
    testSClass("class A", classA)
    testSClass("class A extends B",
      classA.copy(ancestors = L(TC("B", Nil))))
    testSClass("class A extends B with C",
      classA.copy(ancestors = L(TC("B", Nil), TC("C", Nil))))
    testSClass("class Edge[V,E]", classEdgeVE)
    testSClass("class Edge[V,E](val x: V){ def f[A <: T](x: A, y: (A,T)): Int }",
      classEdgeVE.copy(
        args = SClassArgs(L(SClassArg(false, false, true, "x", TC("V", Nil), None))),
        body = L(MD("f", L(STpeArg("A", Some(TC("T", Nil)), Nil)),
          L(MAs(L(MA(false, false, "x", TC("A", Nil), None), MA(false, false, "y", T(L(TC("A", Nil), TC("T", Nil))), None)))),
          Some(INT), false, false, None, Nil, None))))
  }

  describe("SEntityModuleDef") {
    val reactiveModule =
      """package scalan.rx
      |import scalan._
      |trait Reactive extends ScalanDsl {
      |  type Obs[A] = Rep[Observable[A]]
      |  trait Observable[A] {
      |    implicit def eA: Elem[A]
      |  }
      |  class ObservableImpl1[A](implicit val eA: Elem[A]) extends Observable[A] {
      |  }
      |  class ObservableImpl2[A](implicit val eA: Elem[A]) extends Observable[A] {
      |  }
      |}
    """.stripMargin

    val tpeArgA = L(STpeArg("A", None, Nil))
    val ancObsA = L(TC("Observable", L(TC("A", Nil))))
    val argEA = L(SClassArg(true, false, true, "eA", TC("Elem", L(TC("A", Nil))), None))
    val entity = TD("Observable", tpeArgA, Nil, L(SMethodDef("eA",List(),List(),Some(TC("Elem",L(TC("A",Nil)))),true,false, None, Nil, None, true)), None, None)
    val obsImpl1 = CD("ObservableImpl1", tpeArgA, SClassArgs(Nil), SClassArgs(argEA), ancObsA, Nil, None, None, false)
    val obsImpl2 = obsImpl1.copy(name = "ObservableImpl2")

    testModule(
      reactiveModule,
      EMD("scalan.rx", L(SImportStat("scalan._")), "Reactive",
        Some(STpeDef("Obs", L(STpeArg("A",None,Nil)) , TC("Rep", ancObsA))),
        entity,
        List(entity),
        L(obsImpl1, obsImpl2),
        Nil,
        None,
        stdDslImpls = Some(SDeclaredImplementations(Map())),
        expDslImpls = Some(SDeclaredImplementations(Map())),
        ancestors = L(STraitCall("ScalanDsl", Nil))))
  }

  val testModule =
    """package scalan.test
      |trait Module extends ScalanDsl {
      |  type Obs[A] = Rep[Observable[A]]
      |  @ContainerType
      |  trait Cont[A] extends Def[Cont[A]] {
      |    implicit def eA: Elem[A]
      |    @External
      |    def map[B:Elem](
      |  }
      |  trait PairCont[A,B] extends Cont[(A,B)] {
      |
      |  }
      |  class ContImpl1[A](implicit val eA: Elem[A]) extends Observable[A] {
      |  }
      |  class ContImpl2[A](implicit val eA: Elem[A]) extends Observable[A] {
      |  }
      |}
    """.stripMargin
  describe("annotations")  {
  }
}
