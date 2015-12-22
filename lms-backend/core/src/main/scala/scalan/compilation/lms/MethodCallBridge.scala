package scalan.compilation.lms

import java.lang.reflect.Method

import scalan.compilation.language.{CoreMethodMappingDSL, LanguageId}

trait MethodCallBridge extends LmsBridge with CoreMethodMappingDSL {
  import scalan._

  def mappedFunc(method: Method): Option[MappingTags#Fun] =
    methodReplaceConf.map(_.get(method.getDeclaringClass.getName, method.getName)).reverse.find(_.isDefined).getOrElse(None)

  def mappedClassName(c: Class[_]): Option[String] = {
    var fun: Option[String] = None
    methodReplaceConf.foreach(languageBackend => {
      val f = languageBackend.classMap.get(c)
      f match {
        case Some(func) => fun = Some(func.name)
        case _ =>
      }
    })
    fun
  }

  val languageId: LanguageId

  def methodReplaceConf = mappingDSLs(languageId)

  def transformMethodCall[T](m: LmsMirror, receiver: Exp[_], method: Method, args: List[AnyRef], returnType: Elem[T]): lms.Exp[_] =
    !!!(s"Don't know how to transform method call: $method")

  def newObj[A: Manifest](aClass: Class[_], args: Seq[Any], newKeyWord: Boolean): lms.Exp[A] = {
    val name = mappedClassName(aClass).getOrElse(aClass.getName)
    lms.newObj[A](name, args, newKeyWord)
  }

  override protected def transformDef[T](m: LmsMirror, g: AstGraph, sym: Exp[T], d: Def[T]) = d match {
    case mc@MethodCall(receiver, method, args, _) =>
      val exp = (isWrapperElem(receiver.elem) && isWrappedValueAccessor(method)) match {
        case true  => m.symMirror[T](receiver)
        case false => transformMethodCall[T](m, receiver, method, args, mc.selfType.asInstanceOf[Elem[T]])
      }

      m.addSym(sym, exp)

    case lr@NewObject(aClass, args, _) =>
      Manifest.classType(aClass) match { //TODO backend: better manifest construction
        case (mA: Manifest[a]) =>
          // TODO handle case when some of params are functions
          val lmsArgs = args.map(mapParam(m, _, false))
          val exp = newObj[a](aClass, lmsArgs, true)(mA)
          m.addSym(sym, exp)
      }

    case _ => super.transformDef(m, g, sym, d)
  }
}
