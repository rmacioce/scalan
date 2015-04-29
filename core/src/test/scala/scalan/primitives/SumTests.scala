package scalan.primitives

import scala.language.reflectiveCalls
import scalan.{TestContext, BaseTests}
import scalan.common.{CommonExamples, SegmentsDslExp, ViewExamples}

class SumTests extends BaseTests { suite =>

  test("IsSumMapLambda") {
    val ctx = new TestContext(this, "IsSumMapLambda") with SegmentsDslExp {
      lazy val t1 = fun { x: Rep[Int|Unit] => x.mapSum(l => l + 1, r => r) }
    }
    import ctx._
    val IsSumMapLambda(m) = t1
  }

  test("constant propagation from If to SumFold") {
    val ctx = new TestContext(this, "fromIfToSumFold") with SegmentsDslExp {
      lazy val t1 = fun { x: Rep[Int] =>
        val s = IF (x > 0) THEN { (x + 1).asLeft[Int] } ELSE { (x + 2).asRight[Int] }
        s.fold(l => l + 1, r => r - 2)
      }
      lazy val t2 = fun { x: Rep[Int] =>
        val s = IF (x > 0) THEN { (x + 1).asRight[Int] } ELSE { (x + 2).asLeft[Int] }
        s.fold(l => l + 1, r => r - 2)
      }
    }
    import ctx._
    emit("t1", t1)
    emit("t2", t2)
  }

}