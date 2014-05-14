package tests.scalan.linalgebra

import scalan.linalgebra.MatricesDsl

trait LinearAlgebraExamples extends MatricesDsl {
  lazy val ddmvm = fun { p: Rep[(Array[Array[Double]], Array[Double])] => 
    val Pair(m, v) = p
    val matrix = RowMajorMatrix(PArray(m.map(fun { r: Arr[Double] => DenseVector(PArray(r)) })))
    val vector = DenseVector(PArray(v))
    (matrix * vector).coords.arr
  }
  
  lazy val dsmvm = fun { p: Rep[(Array[Array[Double]], (Array[Int], (Array[Double], Int)))] => 
    val Tuple(m, vIs, vVs, vL) = p
    val matrix = RowMajorMatrix(PArray(m.map(fun { r: Arr[Double] => DenseVector(PArray(r)) })))
    val vector = SparseVector(vIs, PArray(vVs), vL)
    (matrix * vector).coords.arr
  }
  
  lazy val sdmvm = fun { p: Rep[(Array[(Array[Int], (Array[Double], Int))], Array[Double])] => 
    val Pair(m, v) = p
    val matrix = RowMajorSparseMatrix(PArray(m.map(fun { r: Rep[(Array[Int], (Array[Double], Int))] => SparseVector(r._1, PArray(r._2), r._3) })))
    val vector = DenseVector(PArray(v))
    (matrix * vector).coords.arr
  }
  
  lazy val ssmvm = fun { p: Rep[(Array[(Array[Int], (Array[Double], Int))], (Array[Int], (Array[Double], Int)))] => 
    val Tuple(m, vIs, vVs, vL) = p
    val matrix = RowMajorSparseMatrix(PArray(m.map(fun { r: Rep[(Array[Int], (Array[Double], Int))] => SparseVector(r._1, PArray(r._2), r._3) })))
    val vector = SparseVector(vIs, PArray(vVs), vL)
    (matrix * vector).coords.arr
  }
  
  lazy val fdmvm = fun { p: Rep[((Array[Double], Int), Array[Double])] => 
    val Pair(m, v) = p
    val matrix = RowMajorFlatMatrix(PArray(m._1), m._2)
    val vector = DenseVector(PArray(v))
    (matrix * vector).coords.arr
  }
  
  lazy val fsmvm = fun { p: Rep[((Array[Double], Int), (Array[Int], (Array[Double], Int)))] => 
    val Tuple(m, vIs, vVs, vL) = p
    val matrix = RowMajorFlatMatrix(PArray(m._1), m._2)
    val vector = SparseVector(vIs, PArray(vVs), vL)
    (matrix * vector).coords.arr
  }
}