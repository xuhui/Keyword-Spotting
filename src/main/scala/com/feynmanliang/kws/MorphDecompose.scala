package com.feynmanliang.kws

import scala.io.Source

class MorphDecompose private (
    qDict: Map[String, List[String]], obDict: Map[String, List[String]]) {
  def decomposeQuery(tokens: Iterable[String]): List[List[String]] = {
    tokens.map(decomposeQuery).toList
  }
  def decomposeQuery(token: String): List[String] = {
    qDict.getOrElse(token, List(token))
  }

  /** Decompose a CTM entry into a list of morphemes
      We distribute startTime/durations and scores uniformly across all morphemes.
   */
  def decomposeEntry(entry: CTMEntry): List[CTMEntry] = {
    obDict.get(entry.token) match {
      case None => List(entry)
      case Some(morphs) => {
        val morphDur = (entry.duration / morphs.size)
        if (morphs.size < 2) {
          List(entry)
        } else {
          morphs.sliding(2).zipWithIndex.flatMap { case (morphPair, i) =>
            if (i == 0) {
              List(
                entry.copy(
                  duration = morphDur,
                  token = morphPair(0),
                  prevToken = entry.prevToken.flatMap(obDict.get).map(_.last)
                ),
                entry.copy(
                  startTime = entry.startTime + (i+1)*morphDur,
                  duration = morphDur,
                  token = morphPair(1),
                  prevToken = Some(morphPair(0)),
                  prevEndTime = entry.startTime + (i+1)*morphDur)
              )
            } else {
              List(
                entry.copy(
                  startTime = entry.startTime + (i+1)*morphDur,
                  duration = morphDur,
                  token = morphPair(1),
                  prevToken = Some(morphPair(0)),
                  prevEndTime = entry.startTime + (i+1)*morphDur)
              )
            }
          }.toList
        }
      }
    }
  }
}

object MorphDecompose {
  private def parseMorphDict(dictPath: String): Map[String, List[String]] = {
    Source.fromFile(dictPath).getLines().map { line =>
      val words = line.split("\\s+")
      words.head -> words.tail.toList
    }.toMap
  }

  def apply(qDictPath: String, obDictPath: String) = {
    val qDict = parseMorphDict(qDictPath)
    val obDict = parseMorphDict(obDictPath)
    new MorphDecompose(qDict, obDict)
  }
}

// vim: set ts=2 sw=2 et sts=2:
