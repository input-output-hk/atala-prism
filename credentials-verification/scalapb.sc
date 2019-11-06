import $ivy.`com.lihaoyi::mill-contrib-scalapblib:$MILL_VERSION`
import contrib.scalapblib._

import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader

class FixedScalaPBWorker(protocPath: Option[String]) {
  private var scalaPBInstanceCache = Option.empty[(Long, ScalaPBWorkerApi)]

  private def scalaPB(scalaPBClasspath: Agg[os.Path]) = {
    val classloaderSig = scalaPBClasspath.map(p => p.toString().hashCode + os.mtime(p)).sum
    scalaPBInstanceCache match {
      case Some((sig, instance)) if sig == classloaderSig => instance
      case _ =>
        val cl = new URLClassLoader(scalaPBClasspath.map(_.toIO.toURI.toURL).toArray)
        val scalaPBCompilerClass = cl.loadClass("scalapb.ScalaPBC")
        val mainMethod = scalaPBCompilerClass.getMethod("main", classOf[Array[java.lang.String]])

        val instance = new ScalaPBWorkerApi {
          override def compileScalaPB(source: File, scalaPBOptions: String, generatedDirectory: File) {
            val opts = if (scalaPBOptions.isEmpty) "" else scalaPBOptions + ":"
            val args = protocPath.map(p => s"--protoc=$p").toList ++ List(
              "--throw",
              s"--scala_out=${opts}${generatedDirectory.getCanonicalPath}",
              s"--proto_path=${source.getParentFile.getCanonicalPath}",
              source.getCanonicalPath
            )
            mainMethod.invoke(
              null,
              args.toArray
            )
          }
        }
        scalaPBInstanceCache = Some((classloaderSig, instance))
        instance
    }
  }

  def compile(scalaPBClasspath: Agg[os.Path], scalaPBSources: Seq[os.Path], scalaPBOptions: String, dest: os.Path)(
      implicit ctx: mill.api.Ctx
  ): mill.api.Result[PathRef] = {
    val compiler = scalaPB(scalaPBClasspath)

    def compileScalaPBDir(inputDir: os.Path) {
      os.walk(inputDir)
        .filter(_.last.matches(".*.proto"))
        .foreach { proto =>
          compiler.compileScalaPB(proto.toIO, scalaPBOptions, dest.toIO)
        }
    }

    def compileScalaPB(input: os.Path): Unit = {
      // ls throws if the path doesn't exist
      if (input.toIO.exists) {
        if (input.toIO.isDirectory) {
          compileScalaPBDir(input)
        } else {
          compiler.compileScalaPB(input.toIO, scalaPBOptions, dest.toIO)
        }
      }
    }

    scalaPBSources.foreach(compileScalaPB)

    mill.api.Result.Success(PathRef(dest))
  }
}

trait FixedScalaPBModule extends ScalaPBModule {

  def protocPath: T[Option[String]] = T {
    scala.util.Properties.envOrNone("CUSTOM_PROTOC")
  }

  override def compileScalaPB: T[PathRef] =
    T.persistent {
      val protoc = protocPath()
      scalaPBSources().foreach(pathRef => println(pathRef.path))
      new FixedScalaPBWorker(protoc)
        .compile(scalaPBClasspath().map(_.path), scalaPBSources().map(_.path), scalaPBOptions(), T.ctx().dest)
    }

}
