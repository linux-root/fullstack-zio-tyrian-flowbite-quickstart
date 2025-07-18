import sbt.io.Path.relativeTo
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

lazy val scala3Version = "3.7.1"
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := scala3Version

lazy val root = (project in file("."))

/**
 * Frontend subproject
 */
lazy val viteDevServer = taskKey[Unit]("Start the dev server. It should be opened in a separate terminal")
lazy val publishDist      = taskKey[Unit]("Build a static web artifact")
lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin, DockerPlugin)
  .settings(
    name    := "frontend",
    version := "0.1.0",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSUseMainModuleInitializer := true,
    // Source maps seem to be broken with bundler
    Compile / fastOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    Compile / fullOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    libraryDependencies ++= Seq(
      "io.indigoengine"            %%% "tyrian-zio"                % Dependencies.Tyrian,
      "dev.zio"                    %%% "zio-interop-cats"          % Dependencies.ZioInteropCats,
      "com.softwaremill.quicklens" %%% "quicklens"                 % Dependencies.Quicklens,
      ("org.scala-js"              %%% "scalajs-java-securerandom" % Dependencies.JavaSecureRandom).cross(CrossVersion.for3Use2_13)
    )
  )
  .settings(
    dockerBaseImage       := "nginx:stable-perl",
    Docker / publish      := (Docker / publish).dependsOn(Compile / fullLinkJS).value,
    Docker / publishLocal := (Docker / publishLocal).dependsOn(Compile / fullLinkJS).value,
    dockerExposedPorts    := Seq(80),
    dockerRepository      := Some(DockerSettings.repository),
    dockerAlias           := DockerAlias.apply(dockerRepository.value, None, "tyrian-flowbite-quickstart", dockerAlias.value.tag.map(t => "frontend-" + t)),
    DockerSettings.x86ArchSetting,
    Docker / defaultLinuxInstallLocation := "/usr/share/nginx/html",
    dockerCommands := dockerCommands.value.filter {
      case ExecCmd(cmd, _) => cmd != "ENTRYPOINT" && cmd != "CMD" && cmd != "USER"
      case Cmd(cmd, _)     => cmd != "USER" && cmd != "RUN"
      case _               => true

    } ++ Seq(Cmd("COPY", "nginx.conf", "/etc/nginx/nginx.conf"), Cmd("CMD", """["nginx", "-g", "daemon off;"]""")),
    Docker / mappings ++= {
      publishDist.value
      val frontendDist    = baseDirectory.value / "dist"
      val nginxConfigFile = baseDirectory.value / "nginx.conf"

      (frontendDist ** "*").get.map { file =>
        file -> s"/usr/share/nginx/html/${frontendDist.relativize(file).get.getPath}"
      } :+ (nginxConfigFile -> "/nginx.conf")

    }
  )
  .settings(
    viteDevServer := {
      CLIUtils.startFrontendDevServer(scalaVersion.value, Environtments.backendBaseUrl)
    },
    publishDist := {
      (Compile / fullLinkJS).value
      CLIUtils.buildFrontend(Environtments.backendBaseUrl)
    }
  )
  .dependsOn(common.js)

/**
 * Backend subproject
 */
lazy val backend = (project in file("backend"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name    := "backend",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "ch.qos.logback"       % "logback-classic"    % Dependencies.LogbackClassic,
      "org.fusesource.jansi" % "jansi"              % Dependencies.Jansi,
      "dev.zio"             %% "zio-logging"        % Dependencies.ZioLogging,
      "dev.zio"             %% "zio-logging-slf4j2" % Dependencies.ZioLogging,
      "org.scalatest"       %% "scalatest"          % Dependencies.Scalatest % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .settings(
    dockerBaseImage    := "openjdk:17-slim",
    dockerExposedPorts := Seq(8080),
    dockerRepository   := Some(DockerSettings.repository),
    dockerAlias        := DockerAlias.apply(dockerRepository.value, None, "tyrian-flowbite-quickstart", dockerAlias.value.tag.map(t => "backend-" + t)),
    DockerSettings.x86ArchSetting
  )
  .dependsOn(common.jvm)

/**
 * Shared project between frontend and backend
 */
lazy val common = (crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure) in file("common"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    scalaVersion := scala3Version,
    scalacOptions ++= Seq("-Xmax-inlines", "64"), // see [[https://docs.scala-lang.org/scala3/guides/migration/options-new.html]]
    libraryDependencies ++= Seq(
      "dev.zio"              %%% "zio-json"        % Dependencies.ZioJson,
      "dev.zio"              %%% "zio-http"        % Dependencies.ZioHttp,
      "com.github.jwt-scala" %%% "jwt-json-common" % Dependencies.JwtScala,
      "org.scalatest"         %% "scalatest"       % Dependencies.Scalatest % Test
    ),
    buildInfoKeys := Seq[BuildInfoKey](
      BuildInfoKey.action("backendBaseUrl") {
        Environtments.backendBaseUrl()
      }
    ),
    buildInfoPackage := "com.example.tyrianflowbitequickstart.common"
  )
