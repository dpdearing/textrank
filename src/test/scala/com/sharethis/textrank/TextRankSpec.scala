package com.sharethis.textrank

import java.util.concurrent.TimeoutException
import org.specs2.mutable.Specification
import collection.JavaConversions._

class TextRankSpec extends Specification {
  isolated
  sequential

  "TextRank in English" should {
    val textRank = new TextRank("en")

    "Only return one instance of a given Ngram" in {
      val keyphrases = getTestKeyPhrases(textRank)
      keyphrases.count(_.getPhrase == "sarah palin") mustEqual 1
    }

    "Not contain any keyphrases that are just punctuation" in {
      val keyphrases = getTestKeyPhrases(textRank)
      keyphrases.count(_.getPhrase == "|") mustEqual 0
    }

    "Timeout if it takes too long" in {
      val impatientTextRank = new TextRank("en", 10)
      impatientTextRank.run(getTestText("/kill.txt")) must throwA[TimeoutException]
    }
  }

  def getTestKeyPhrases(textRank: TextRank): Seq[Keyphrase] = {
    textRank.run(getTestText("/kill.txt")).getKeyphrases
  }

  def getTestText(name: String): String = {
    val inputStream = this.getClass.getResourceAsStream(name)
    scala.io.Source.fromInputStream(inputStream).mkString
  }

  "TextRank in Dutch" should {
    val textRank = new TextRank("nl")
    val keyphrases = textRank.run(getTestText("/good_nl.txt")).getKeyphrases

    "Only return one instance of a given Ngram" in {
      keyphrases.groupBy(_.getPhrase).map(_._2.size) must contain(be_==(1)).forall
    }

    "Not contain any keyphrases that are just punctuation" in {
      keyphrases.count(_.getPhrase == "|") mustEqual 0
    }
  }
}
