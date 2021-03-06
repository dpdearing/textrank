/*
Copyright (c) 2009, ShareThis, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of the ShareThis, Inc., nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.sharethis.textrank;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Sequence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.tartarus.snowball.ext.EnglishStemmer;


/**
 * Implementation of English-specific tools for natural language
 * processing.
 *
 * @author paco@sharethis.com
 */

public class LanguageEnglish extends LanguageModel {

	private final static Log LOG = LogFactory.getLog(LanguageEnglish.class.getName());

	public static SentenceModel splitter_en = null;
	public static TokenizerModel tokenizer_en = null;
	public static POSModel tagger_en = null;
	public static EnglishStemmer stemmer_en = null;


	/**
	 * Constructor. Not quite a Singleton pattern but close enough
	 * given the resources required to be loaded ONCE.
	 */
	public LanguageEnglish() throws Exception {
		if (splitter_en == null) {
			loadResources();
		}
	}


	/**
	 * Load libraries for OpenNLP for this specific language.
	 */
	public void loadResources () throws Exception {
		splitter_en = new SentenceModel(getResourceAsStream("/opennlp/en-sent.bin"));
		tokenizer_en = new TokenizerModel(getResourceAsStream("/opennlp/en-token.bin"));
		tagger_en = new POSModel(getResourceAsStream("/opennlp/en-pos-maxent.bin"));
		stemmer_en = new EnglishStemmer();
	}

	/**
	 * Split sentences within the paragraph text.
	 */
	public String[] splitParagraph (final String text) {

		return new SentenceDetectorME(splitter_en).sentDetect(text);
	}

	/**
	 * Tokenize the sentence text into an array of tokens.
	 */
	public String[] tokenizeSentence (final String text) {
		final String[] token_list = new TokenizerME(tokenizer_en).tokenize(text);

		ArrayList<String> cleanedTokens = new ArrayList<>(token_list.length);

		for (String token : token_list) {
			String clean = token.replace("\"", "").toLowerCase().trim();
			if (clean.matches("[a-zA-Z0-9].*")) {
				cleanedTokens.add(clean);
			}
		}

		return cleanedTokens.toArray(new String[cleanedTokens.size()]);
	}

	/**
	 * Run a part-of-speech tagger on the sentence token list.
	 */
	public String[] tagTokens (final String[] token_list) {
		final Sequence[] sequences = new POSTaggerME(tagger_en).topKSequences(token_list);
		final String[] tag_list = new String[token_list.length];

		int i = 0;
		for (Object obj : sequences[0].getOutcomes()) {
			tag_list[i] = (String) obj;
			i++;
		}

		return tag_list;
	}


	/**
	 * Prepare a stable key for a graph node (stemmed, lemmatized)
	 * from a token.
	 */
	public String getNodeKey (final String text, final String pos) throws Exception {
		return pos.substring(0, 2) + stemToken(scrubToken(text)).toLowerCase();
	}


	/**
	 * Determine whether the given PoS tag is a noun.
	 */
	public boolean isNoun (final String pos) {
		return pos.startsWith("NN");
	}

	/**
	 * Determine whether the given PoS tag is an adjective.
	 */

	public boolean isAdjective (final String pos) {
		return pos.startsWith("JJ");
	}


	/**
	 * Perform stemming on the given token.
	 */
	public String stemToken (final String token) {
		stemmer_en.setCurrent(token);
		stemmer_en.stem();

		return stemmer_en.getCurrent();
	}

	private InputStream getResourceAsStream(String name) throws FileNotFoundException {
		return this.getClass().getResourceAsStream("/en" + name);
	}
}