package com.sharethis.textrank

import java.util.concurrent.TimeoutException

import org.specs2.mutable.Specification
import collection.JavaConversions._

class TextRankSpec extends Specification {
  isolated
  sequential

  val textRank = new TextRank("en")

  "TextRank" should {

    "Only return one instance of a given Ngram" in {
      val keyphrases = getTestKeyPhrases
      keyphrases.count(_.getPhrase == "sarah palin") mustEqual 1
    }

    "Not contain any keyphrases that are just punctuation" in {
      val keyphrases = getTestKeyPhrases
      keyphrases.count(_.getPhrase == "|") mustEqual 0
    }

    "Timeout if it takes too long" in {
      val impatientTextRank = new TextRank("en", 10)
      impatientTextRank.run(getTestText) must throwA[TimeoutException]
    }
  }

  def getTestKeyPhrases: Seq[Keyphrase] = {
    textRank.run(getTestText).getKeyphrases
  }

  def getTestText: String = {
    val inputStream = this.getClass.getResourceAsStream("/kill.txt")
    scala.io.Source.fromInputStream(inputStream).mkString
  }
}
