package com.sharethis.textrank

import org.specs2.mutable.Specification
import collection.JavaConversions._

class TextRankSpec extends Specification {

  val textRank = new TextRank("en")

  "TextRank" should {

    "Only return one instance of a given Ngram" in {
      val inputStream = this.getClass.getResourceAsStream("/kill.txt")
      val text = scala.io.Source.fromInputStream(inputStream).mkString
      textRank.run(text)

      val keyphrases = textRank.getKeyphrases
      keyphrases.count(_.getPhrase == "sarah palin") mustEqual 1
    }
  }

}
