package com.sharethis.textrank

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
  }

  def getTestKeyPhrases: Seq[Keyphrase] = {
    val inputStream = this.getClass.getResourceAsStream("/kill.txt")
    val text = scala.io.Source.fromInputStream(inputStream).mkString
    textRank.run(text).getKeyphrases
  }
}
