package com.octopus.http.core

import java.net.URL
import java.util.{ArrayList, Observable}

import com.octopus.http.core.Octopus._

object Octopus {
  // status names.
  val STATUSES = Array("Downloading", "Paused", "Complete", "Cancelled", "Error")
  val DOWNLOADING: Int = 0
  val PAUSED: Int = 1
  val COMPLETED: Int = 2
  val CANCELLED: Int = 3
  val ERROR: Int = 4

}

abstract class Octopus protected(protected var mURL: URL, protected var mOutputFolder: String,
                                 protected var mNumConnections: Int, protected var rateLimit: Long) extends Observable with Runnable {

  val fileURL: String = mURL.getFile
  protected var tFileName: String = fileURL.substring(fileURL.lastIndexOf('/') + 1)
  protected var tFileSize: Int = -1
  protected var tState: Int = _

  val BLOCK_SIZE = rateLimit.toInt
  val BUFFER_SIZE = rateLimit.toInt

  protected var downloaded: Int = 0

  protected var listDownloadThread: ArrayList[OctopusDownloadThread] =
    new ArrayList[OctopusDownloadThread]()

  def pause(): Unit = {
    this.tState = PAUSED
  }

  def resume(): Unit = {
    this.tState = DOWNLOADING
    download()
  }

  def cancel(): Unit = {
    this.tState = CANCELLED
  }

  def getURL(): String = mURL.toString

  def getFileSize(): Int = tFileSize

  def getProgress(): Float = (downloaded.toFloat / tFileSize) * 100

  def downloadedBytes(): Long = downloaded

  def getState(): Int = tState

  protected def setState(value: Int): Unit = {
    tState = value
    stateChanged()
  }

  protected def download(): Unit = {
    val t: Thread = new Thread(this)
    t.start()
  }

  protected def downloaded(value: Int): Unit = {
    synchronized {
      downloaded += value
      stateChanged()
    }
  }

  protected def stateChanged(): Unit = {
    setChanged()
    notifyObservers()
  }

  protected abstract class OctopusDownloadThread(protected var tThreadID: Int, protected var tURL: URL,
                                                 protected var tOutputFile: String, protected var tStartByte: Int,
                                                 protected var tEndByte: Int) extends Runnable {

    protected var tIsFinished: Boolean = false
    protected var tThread: Thread = _

    download()

    def isFinished(): Boolean = tIsFinished

    def download(): Unit = {
      tThread = new Thread(this)
      tThread.start()
    }

    def waitFinish(): Unit = {
      tThread.join()
    }

  }

}

