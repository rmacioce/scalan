package scalan

import scalan.compilation.GraphVizExport
import scalan.primitives._
import scalan.collections._
import scalan.arrays._
import scalan.seq.BaseSeq
import scalan.staged.{BaseExp, Expressions, Transforming}
import scalan.util.{ExceptionsDslExp, ExceptionsDslSeq, ExceptionsDsl/*, Exceptions*/}

trait Scalan
  extends Base
  with Debugging
  with Elems
  with Containers
  with TypeWrappers
  with Views
  with Entities
  with Proxy
  with Tuples
  with Loops
  with TypeSum
  with UnBinOps
  with LogicalOps
  with OrderingOps
  with NumericOps
  with StringOps
  with Equal
  with MathOps
  with Functions
  with IfThenElse
  with Blocks
  with Monoids
  with PatternMatching
  with Maps
  with ArrayOps
  with ArrayBuffers
  with Exceptions
  with ArrayViews
  with Thunks
  with Effects
  with Metadata
  with ListOps
  with ListViews
  with ConvertersDsl
  with Effectful

trait ScalanDsl
extends Scalan
  with ExceptionsDsl
  with AbstractStringsDsl

trait ScalanSeq
  extends Scalan
  with BaseSeq
  with ElemsSeq
  with ContainersSeq
  with TypeWrappersSeq
  with ViewsSeq
  with ProxySeq
  with TuplesSeq
  with LoopsSeq
  with TypeSumSeq
  with UnBinOpsSeq
  with NumericOpsSeq
  with FunctionsSeq
  with IfThenElseSeq
  with BlocksSeq
  with PatternMatchingSeq
  with MapsSeq
  with MonoidsSeq
  with ArrayOpsSeq
  with ArrayBuffersSeq
  with ExceptionsSeq
  with ArrayViewsSeq
  with StringOpsSeq
  with ThunksSeq
  with EffectsSeq
  with MetadataSeq
  with ListOpsSeq
  with ListViewsSeq
  with ConvertersDslSeq
  with EffectfulSeq

trait ScalanCtxSeq
extends ScalanDsl
  with ScalanSeq
  with ExceptionsDslSeq
  with AbstractStringsDslSeq

trait ScalanExp
  extends Scalan
  with BaseExp
  with ElemsExp
  with ContainersExp
  with TypeWrappersExp
  with ViewsExp
  with ProxyExp
  with TuplesExp
  with LoopsExp
  with TypeSumExp
  with NumericOpsExp
  with UnBinOpsExp
  with LogicalOpsExp
  with EqualExp
  with FunctionsExp
  with IfThenElseExp
  with BlocksExp
  with PatternMatchingExp
  with MapsExp
  with Transforming
  with ArrayOpsExp
  with ArrayBuffersExp
  with ExceptionsExp
  with ArrayViewsExp
  with StringOpsExp
  with ThunksExp
  with EffectsExp
  with MetadataExp
  with ListOpsExp
  with ListViewsExp
  with ConvertersDslExp
  with EffectfulExp
  with RewriteRulesExp
  with GraphVizExport

trait ScalanCtxExp
extends ScalanDsl
  with ScalanExp
  with Expressions
  with ExceptionsDslExp
  with AbstractStringsDslExp
