package com.octopus.http.core

import java.io.{BufferedInputStream, IOException, RandomAccessFile}
import java.net.{HttpURLConnection, URL}

class OctopusDownloader(url: URL, outputFolder: String, numConnections: Int, rateLimit: Long)
  extends Octopus(url, outputFolder, numConnections, rateLimit) {

  download()

  private def error(): Unit = {
    println(s"ERROR")
    this.tState = Octopus.ERROR
  }

  override def run(): Unit = {
    var conn: HttpURLConnection = null
    try {
      System.setProperty("http.agent", "Chrome")
      conn = mURL.openConnection().asInstanceOf[HttpURLConnection]
      conn.setConnectTimeout(10000)
      conn.connect()
      if (conn.getResponseCode / 100 != 2) {
        error()
      }
      val contentLength: Int = conn.getContentLength
      if (contentLength < 1) {
        error()
      }

      if (tFileSize == -1) {
        tFileSize = contentLength
        stateChanged()
        println("File size: " + tFileSize)
      }

      // if the state is DOWNLOADING (no error) -> start downloading
      if (tState == Octopus.DOWNLOADING) {
        // check whether we have list of download threads or not, if not -> init download
        if (listDownloadThread.size == 0) {
          if (tFileSize > 0 && tFileSize > BLOCK_SIZE) {
            // downloading size for each thread
            val partSize: Int = Math.round(
              (tFileSize.toFloat / mNumConnections) / BLOCK_SIZE) * BLOCK_SIZE
            println("Part size: " + partSize)
            // start/end Byte for each thread
            var startByte: Int = 0
            var endByte: Int = partSize - 1

            var aThread: OctopusHttpDownloadThread = new OctopusHttpDownloadThread(
              1,
              mURL,
              mOutputFolder + tFileName,
              startByte,
              endByte)

            listDownloadThread.add(aThread)
            var i: Int = 2
            while (endByte < tFileSize) {
              startByte = endByte + 1
              endByte += partSize
              aThread = new OctopusHttpDownloadThread(i,
                mURL,
                mOutputFolder + tFileName,
                startByte,
                endByte)
              listDownloadThread.add(aThread)
            }
          } else {
            val aThread: OctopusHttpDownloadThread = new OctopusHttpDownloadThread(1, mURL, mOutputFolder + tFileName, 0, tFileSize)
            listDownloadThread.add(aThread)
          }
        } else {
          for (i <- 0 until listDownloadThread.size if !listDownloadThread.get(i).isFinished) {
            listDownloadThread.get(i).download()
          }
        }
        for (i <- 0 until listDownloadThread.size)
          listDownloadThread.get(i).waitFinish()

        // check the current state again
        if (tState == Octopus.DOWNLOADING) {
          this.tState = Octopus.COMPLETED
        }
      }
    } catch {
      case e: Exception =>
        error()

    } finally {
      if (conn != null) conn.disconnect()
    }
  }


  private class OctopusHttpDownloadThread(threadID: Int, url: URL, outputFile: String, startByte: Int, endByte: Int)
    extends OctopusDownloadThread(threadID, url, outputFile, startByte, endByte) {

    override def run(): Unit = {

      var in: BufferedInputStream = null
      var raf: RandomAccessFile = null

      try {
        System.setProperty("http.agent", "Chrome")
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
        val conn: HttpURLConnection = tURL.openConnection().asInstanceOf[HttpURLConnection]
        val byteRange: String = tStartByte + "-" + tEndByte
        conn.setRequestProperty("Range", "bytes=" + byteRange)
        conn.setRequestProperty("content-type", "binary/data")
        // connect to server
        conn.connect()
        // Make sure the response code is in the 200 range.
        if (conn.getResponseCode / 100 != 2) {
          error()
        }
        // get the input stream
        in = new BufferedInputStream(conn.getInputStream)
        // open the output file and seek to the start location
        raf = new RandomAccessFile(tOutputFile, "rw")
        raf.seek(tStartByte)
        val data: Array[Byte] = Array.ofDim[Byte](BUFFER_SIZE)
        var numRead: Int = 0
        while ((tState == Octopus.DOWNLOADING) && ((numRead =
          in.read(data, 0, BUFFER_SIZE)) != -1) && numRead > 0) {
          // write to buffer
          raf.write(data, 0, numRead)
          // increase the startByte for resume later
          tStartByte += numRead
          // increase the downloaded size
          downloaded(numRead)
        }
        if (tState == Octopus.DOWNLOADING) {
          tIsFinished = true
        }
      } catch {
        case e: IOException =>
          error()
      } finally {
        if (raf != null) {
          try raf.close()
          catch {
            case e: IOException => {}

          }
        }
        if (in != null) {
          try in.close()
          catch {
            case e: IOException => {}

          }
        }
      }
      println("End thread " + tThreadID)
    }

  }

}

