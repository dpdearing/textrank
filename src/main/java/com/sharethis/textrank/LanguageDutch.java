package com.sharethis.textrank;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Sequence;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tartarus.snowball.ext.DutchStemmer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;


/**
 * Implementation of Dutch-specific tools for natural language processing.
 * Copied from https://github.com/emres/textrank/blob/master/src/com/sharethis/textrank/LanguageDutch.java
 *
 * @author Emre Sevinc emre.sevinc@gmail.com
 * @author Robert Gibbon
 */
public class LanguageDutch extends LanguageModel {
    private final static Log LOG = LogFactory.getLog(LanguageDutch.class.getName());

    /**
     * Public definitions.
     */
    public static SentenceModel splitter_nl = null;
    public static TokenizerModel tokenizer_nl = null;
    public static POSModel tagger_nl = null;
    public static DutchStemmer stemmer_nl = null;


    /**
     * Constructor. Not quite a Singleton pattern but close enough
     * given the resources required to be loaded ONCE.
     */
    public LanguageDutch() throws Exception {
        if (splitter_nl == null) {
            loadResources();
        }
    }


    /**
     * Load libraries for OpenNLP for this specific language.
     */
    public void loadResources() throws Exception {
        splitter_nl = new SentenceModel(getResourceAsStream("/nl-sent.bin"));
        tokenizer_nl = new TokenizerModel(getResourceAsStream("/nl-token.bin"));
        tagger_nl = new POSModel(getResourceAsStream("/nl-pos-maxent.bin"));
        stemmer_nl = new DutchStemmer();
    }

    private InputStream getResourceAsStream(String name) throws FileNotFoundException {
        return this.getClass().getResourceAsStream("/nl/opennlp" + name);
    }


    /**
     * Split sentences within the paragraph text.
     */
    public String[] splitParagraph (final String text) {
        return new SentenceDetectorME(splitter_nl).sentDetect(text);
    }


    /**
     * Tokenize the sentence text into an array of tokens.
     */
    public String[] tokenizeSentence (final String text) {
        final String[] token_list = new TokenizerME(tokenizer_nl).tokenize(text);

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
        final Sequence[] sequences = new POSTaggerME(tagger_nl).topKSequences(token_list);
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
        return pos.substring(0, 1) + stemToken(scrubToken(text)).toLowerCase();
    }


    /**
     * Determine whether the given PoS tag is a noun.
     */
    public boolean isNoun (final String pos) {
        return (pos.startsWith("N") && !pos.startsWith("Num"));
    }


    /**
     * Determine whether the given PoS tag is an adjective.
     */
    public boolean isAdjective (final String pos) {
        return pos.startsWith("Adj");
    }


    /**
     * Perform stemming on the given token.
     */
    public String stemToken (final String token) {
        stemmer_nl.setCurrent(token);
        stemmer_nl.stem();

        return stemmer_nl.getCurrent();
    }
}