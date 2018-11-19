package com.octopus.http.core

import java.io.FileWriter
import java.net.URL
import scala.io.Source

object OctopusMain {
  def fail(msg: String): Nothing = {
    System.err.println(msg)
    sys.exit(1)
  }

  def usage: String =
    s"""
       | Usage: -n=numberOfThreads -l=rateLimit -i=inputFile -o=outputPath
     """.stripMargin

  case class ArgsCls(threads: Int = 1, limit: Long = 10, file: String = "/home/", output: String = "/home/")

  object ArgsCls {
    private def parseInner(options: ArgsCls, args: List[String]): ArgsCls = {
      args match {
        case Nil => options
        case "--help" :: _ =>
          System.err.println(usage)
          sys.exit(0)
        case flag :: Nil => fail(s"flag $flag has no value\n$usage")
        case flag :: value :: tail =>
          val newOptions: ArgsCls = flag.toLowerCase match {
            case "-n" => options.copy(threads = value.toInt)
            case "-l" =>
              value match {
                case value if(value.endsWith("k")) =>
                  options.copy(limit = value.split("k")(0).toLong * 1024)
                case value if(value.endsWith("m")) =>
                  options.copy(limit = value.split("m")(0).toLong * 1024 * 1024)
                case _ =>
                  fail(s"unknown argument given $flag")
            }
            case "-i" => options.copy(file = value)
            case "-o" => options.copy(output = value)
            case _ => fail(s"unknown argument given $flag")
          }
          parseInner(newOptions, tail)
      }
    }

    def parse(args: Array[String]): ArgsCls = {
      parseInner(ArgsCls(), args.flatMap(_.split('=')).toList)
    }
  }

  def main(args: Array[String]): Unit = {
    var downloadedSize: Long = 0
    var timeSpent: Long = 0
    val arguments = ArgsCls.parse(args)
    for (line <- Source.fromFile(arguments.file).getLines) {
      val fileName = line.split(" ")
      timeSpent = timeSpent + time {
        val httpDownloader = new OctopusDownloader(new URL(fileName(0)), arguments.output + "/" + fileName(1),
          arguments.threads, arguments.limit)
        httpDownloader.run()
        downloadedSize = httpDownloader.downloadedBytes()
      }
    }
    writeToFile(arguments.output + "/output.txt", s"Running time, seconds: ${timeSpent}")
    writeToFile(arguments.output + "/output.txt", s"Bytes downloaded: ${downloadedSize}")
    println(s"Running time, seconds: ${timeSpent}")
    println(s"Bytes downloaded: ${downloadedSize}")
  }

  def time[R](block: => R): Long = {
    val t0 = System.currentTimeMillis()
    val result = block // call-by-name
    val t1 = System.currentTimeMillis()
    (t1 - t0) / 1000
  }

  def writeToFile(fileName: String, content: String): Unit ={
    val pw = new FileWriter(fileName, true)
    pw.append(content +"\n")
    pw.flush()
    pw.close()
  }
}
