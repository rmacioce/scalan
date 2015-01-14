package scalan.meta

import scalan.meta.ScalanAst.STraitCall

class BoilerplateTool {
  val coreTypeSynonyms = Map(
    "Arr" -> "Array"
  )
  lazy val coreConfig = CodegenConfig(
    srcPath = "../core/src/main/scala",
    entityFiles = List(
    ),
    seqContextTrait = "ScalanSeq",
    stagedContextTrait = "ScalanExp",
    extraImports = List(
      "scala.reflect.runtime.universe._",
      "scalan.common.Default"),
    coreTypeSynonyms
  )

  val liteTypeSynonyms = Map(
    "PA" -> "PArray", "NA" -> "NArray", "Vec" -> "Vector", "Matr" -> "Matrix"
  )
  lazy val liteConfig = CodegenConfig(
    srcPath = "../community-edition/src/main/scala",
    entityFiles = List(
      "scalan/parrays/PArrays.scala"
      , "scalan/linalgebra/Vectors.scala"
      , "scalan/linalgebra/Matrices.scala"
    ),
    seqContextTrait = "ScalanSeq",
    stagedContextTrait = "ScalanExp",
    extraImports = List(
      "scala.reflect.runtime.universe._",
      "scalan.common.Default"),
    coreTypeSynonyms ++ liteTypeSynonyms
  )

  val eeTypeSynonyms = Set(
    "PS" -> "PSet", "PM" -> "PMap", "Dist" -> "Distributed"
  )
  lazy val scalanConfig = CodegenConfig(
    srcPath = "../../scalan/src/main/scala",
    entityFiles = List(
      "scalan/trees/Trees.scala",
      "scalan/math/Matrices.scala",
      "scalan/math/Vectors.scala",
      "scalan/collections/Sets.scala",
      "scalan/dists/Dists.scala",
    // don't regenerate by default because this will break
    // FuncArray. See comments there.
//      "scalan/parrays/PArrays.scala",
      "scalan/iterators/Iterators.scala"
    ),
    seqContextTrait = "ScalanEnterpriseSeq",
    stagedContextTrait = "ScalanEnterpriseExp",
    extraImports = List(
      "scala.reflect.runtime.universe._",
      "scalan.common.Default"),
    coreTypeSynonyms ++ liteTypeSynonyms ++ eeTypeSynonyms
  )
  lazy val scalanFullConfig = scalanConfig.copy(entityFiles = scalanConfig.entityFiles :+ "scalan/parrays/PArrays.scala")

  val effectsTypeSynonims = Map(
    "MonadRep"    -> "Monad",
    "RFree"       -> "Free",
    "RCoproduct"  -> "Coproduct",
    "RepReader" -> "Reader",
    "RepInteract" -> "Interact",
    "RepAuth" -> "Auth"
    // declare your type synonims for User Defined types here (see type PA[A] = Rep[PArray[A]])
  )
  lazy val effectsConfig = CodegenConfig(
    srcPath = "../../scalan-effects/src/main/scala",
    entityFiles = List(
      //"scalan/monads/Monads.scala"
      //, "scalan/monads/Functors.scala"
      "scalan/monads/FreeMs.scala"
      //"scalan/io/Frees.scala"
      //"scalan/monads/Coproducts.scala"
      //"scalan/monads/Interactions.scala"
      //"scalan/monads/Auths.scala"
      //"scalan/monads/Readers.scala"
    ),
    seqContextTrait = "ScalanSeq",
    stagedContextTrait = "ScalanExp",
    extraImports = List(
      "scala.reflect.runtime.universe._",
      "scalan.common.Default"),
    effectsTypeSynonims
  )

  def getConfigs(args: Array[String]): Seq[CodegenConfig] =
    args.flatMap { arg => configsMap.getOrElse(arg,
      sys.error(s"Unknown codegen config $arg. Allowed values: ${configsMap.keySet.mkString(", ")}"))
    }.distinct

  val configsMap = Map(
    "core" -> List(coreConfig),
    "ce" -> List(liteConfig),
    "ee" -> List(scalanConfig),
    "ee-full" -> List(scalanFullConfig),
    "effects" -> List(effectsConfig),
    "all" -> List(coreConfig, liteConfig, scalanConfig)
  )

  def main(args: Array[String]) {
    val configs = getConfigs(args)

    configs.foreach { new EntityManagement(_).generateAll() }
  }
}

object BoilerplateToolRun extends BoilerplateTool {
}
