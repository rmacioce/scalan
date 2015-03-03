package scalan.it.lms

import scalan.compilation.GraphVizConfig
import scalan.compilation.lms.cxx.{CoreCXXLmsBackend, LmsCompilerCXX}
import scalan.util.FileUtil
import scalan.{ScalanCommunityDslSeq, ScalanCommunityDslExp, ScalanCtxSeq, ScalanCtxExp}
import scalan.compilation.lms._
import scalan.compilation.lms.scalac.{CommunityLmsCompilerScala, LmsCompilerScala}
import scalan.graphs.{GraphsDslExp, GraphsDslSeq, GraphExamples, MST_example}
import scalan.it.BaseItTests


abstract class LmsMsfItTests extends BaseItTests {

  trait MsfFuncs extends GraphExamples {
    lazy val msfFunAdjBase = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
      val segments = Collection.fromArray(in._3) zip Collection.fromArray(in._4)
      val links = NestedCollection.createNestedCollection(Collection.fromArray(in._1), segments)
      val edge_vals = NestedCollection.createNestedCollection(Collection.fromArray(in._2), segments)

      val vertex_vals = UnitCollection(segments.length)
      val graph = AdjacencyGraph.fromAdjacencyList(vertex_vals, edge_vals, links)
      val startFront = Front.emptyBaseFront(graph.vertexNum)
      val res = MSF_prime(graph, startFront)
      res.arr
    }
    lazy val msfFunIncBase = fun { in: Rep[(Array[Double], Int)] =>
      val incMatrix = Collection.fromArray(in._1)
      val vertex_vals = UnitCollection(in._2)
      val graph = IncidenceGraph.fromAdjacencyMatrix(vertex_vals, incMatrix, in._2)
      val out_in = Collection.replicate(graph.vertexNum, UNVISITED).update(0, NO_PARENT)
      val startFront = Front.emptyBaseFront(graph.vertexNum)
      val res = MSF_prime(graph, startFront)
      res.arr
    }
    lazy val msfFunAdjMap = fun { in: Rep[(Array[Int], (Array[Double], (Array[Int], Array[Int])))] =>
      val segments = Collection.fromArray(in._3) zip Collection.fromArray(in._4)
      val links = NestedCollection.createNestedCollection(Collection.fromArray(in._1), segments)
      val edge_vals = NestedCollection.createNestedCollection(Collection.fromArray(in._2), segments)

      val vertex_vals = UnitCollection(segments.length)
      val graph = AdjacencyGraph.fromAdjacencyList(vertex_vals, edge_vals, links)
      val startFront = Front.emptyMapBasedFront(graph.vertexNum)
      val res = MSF_prime(graph, startFront)
      res.arr
    }
    lazy val msfFunIncMap = fun { in: Rep[(Array[Double], Int)] =>
      val incMatrix = Collection.fromArray(in._1)
      val vertex_vals = UnitCollection(in._2)
      val graph = IncidenceGraph.fromAdjacencyMatrix(vertex_vals, incMatrix, in._2)
      val out_in = Collection.replicate(graph.vertexNum, UNVISITED).update(0, NO_PARENT)
      val startFront = Front.emptyMapBasedFront(graph.vertexNum)
      val res = MSF_prime(graph, startFront)
      res.arr
    }

  }
  class ProgExp extends GraphsDslExp with MsfFuncs with ScalanCommunityDslExp with ScalanCtxExp with CommunityLmsCompilerScala with CommunityBridge { self =>
    val lms = new CommunityLmsBackend
  }
  class ProgExpCXX extends GraphsDslExp with MsfFuncs with ScalanCommunityDslExp with LmsCompilerCXX with CoreBridge { self =>
    val lms = new CoreCXXLmsBackend
  }

  class ProgSeq extends GraphsDslSeq with MsfFuncs with ScalanCommunityDslSeq

  val progSeq = new ProgSeq
  val progStaged1 = new ProgExp
  val progStaged2 = new ProgExp
  val progStaged3 = new ProgExp
  val progStaged4 = new ProgExp
  val progStaged1CXX = new ProgExpCXX
  val progStaged2CXX = new ProgExpCXX
  val progStaged3CXX = new ProgExpCXX
  val progStaged4CXX = new ProgExpCXX
}

class LmsMsfPrimeItTests extends LmsMsfItTests {
  import progSeq._
  val graph = Array(
    Array(1, 8),
    Array(0, 2, 8),
    Array(1, 3, 5, 7),
    Array(2, 4, 5),
    Array(3, 5),
    Array(2, 3, 4, 6),
    Array(5, 7, 8),
    Array(2, 6, 8),
    Array(0, 1, 6, 7),
    Array(10,11),
    Array(9,11),
    Array(9,10)
  )
  val graphValues = Array(
    Array(4.0, 8.0),
    Array(4.0, 8.0, 11.0),
    Array(8.0, 7.0, 4.0, 2.0),
    Array(7.0, 9.0, 14.0),
    Array(9.0, 10.0),
    Array(4.0, 14.0, 10.0, 2.0),
    Array(2.0, 6.0, 1.0),
    Array(2.0, 6.0, 7.0),
    Array(8.0, 11.0, 1.0, 7.0),
    Array(1.0, 2.0),
    Array(1.0, 0.5),
    Array(2.0, 0.5)
  )

  // Commented
  test("MSF_adjList") {
    val links = graph.flatMap( i=> i)
    val edgeVals = graphValues.flatMap(i => i)
    val lens = graph.map(i => i.length)
    val offs = Array(0,2,5,9,12,14,18,21,24,28,30,32) //(Array(0) :+ lens.scan.slice(lens.length-1)
    val input = (links, (edgeVals, (offs, lens)))
    val resSeq = progSeq.msfFunAdjBase(input)
    println("Seq: " + resSeq.mkString(" , "))
    val resStaged = getStagedOutputConfig(progStaged1)(progStaged1.msfFunAdjBase, "MSF_adjList", input, progStaged1.defaultCompilerConfig)
    println("Staged: " + resStaged.mkString(","))

    val dir = FileUtil.file(prefix, "MSF_adjList")
    progStaged1CXX.buildExecutable(dir,dir,"MSF_adjList", progStaged1CXX.msfFunAdjBase, GraphVizConfig.default)(progStaged1CXX.defaultCompilerConfig)
  }
  test("MSF_adjMatrix") {
    val vertexNum = graph.length
    val incMatrix = (graph zip graphValues).flatMap({ in =>
      val row = in._1
      val vals = in._2
      val zero = scala.Array.fill(vertexNum)(0.0)
      for (i <- 0 to row.length - 1) {
        zero(row(i)) = vals(i)
      }
      zero
    })
    val input = (incMatrix, vertexNum)
    val resSeq = progSeq.msfFunIncBase(input)
    println("Seq: " + resSeq.mkString(" , "))
    val resStaged = getStagedOutputConfig(progStaged2)(progStaged2.msfFunIncBase, "MSF_adjMatrix", input, progStaged2.defaultCompilerConfig)
    println("Staged: " + resStaged.mkString(","))

    val dir = FileUtil.file(prefix, "MSF_adjMatrix")
    progStaged2CXX.buildExecutable(dir,dir,"MSF_adjMatrix", progStaged2CXX.msfFunIncBase, GraphVizConfig.default)(progStaged2CXX.defaultCompilerConfig)
  }

  test("MSF_adjListMap") {
    val links = graph.flatMap( i=> i)
    val edgeVals = graphValues.flatMap(i => i)
    val lens = graph.map(i => i.length)
    val offs = Array(0,2,5,9,12,14,18,21,24,28,30,32) //(Array(0) :+ lens.scan.slice(lens.length-1)
    val input = (links, (edgeVals, (offs, lens)))
    val resSeq = progSeq.msfFunAdjMap(input)
    println("Seq: " + resSeq.mkString(" , "))
    val resStaged = getStagedOutputConfig(progStaged3)(progStaged3.msfFunAdjMap, "MSF_adjListMap", input, progStaged3.defaultCompilerConfig)
    println("Staged: " + resStaged.mkString(","))

    val dir = FileUtil.file(prefix, "MSF_adjListMap")
    progStaged3CXX.buildExecutable(dir,dir,"MSF_adjListMap", progStaged3CXX.msfFunAdjMap, GraphVizConfig.default)(progStaged3CXX.defaultCompilerConfig)
  }

  test("MSF_adjMatrixMap") {
    val vertexNum = graph.length
    val incMatrix = (graph zip graphValues).flatMap({ in =>
      val row = in._1
      val vals = in._2
      val zero = scala.Array.fill(vertexNum)(0.0)
      for (i <- 0 to row.length - 1) {
        zero(row(i)) = vals(i)
      }
      zero
    })
    val input = (incMatrix, vertexNum)
    val resSeq = progSeq.msfFunIncMap(input)
    println("Seq: " + resSeq.mkString(" , "))
    val resStaged = getStagedOutputConfig(progStaged4)(progStaged4.msfFunIncMap, "MSF_adjMatrixMap", input, progStaged4.defaultCompilerConfig)
    println("Staged: " + resStaged.mkString(","))

    val dir = FileUtil.file(prefix, "MSF_adjMatrixMap")
    progStaged4CXX.buildExecutable(dir,dir,"MSF_adjMatrixMap", progStaged4CXX.msfFunIncMap, GraphVizConfig.default)(progStaged4CXX.defaultCompilerConfig)
  }
}
