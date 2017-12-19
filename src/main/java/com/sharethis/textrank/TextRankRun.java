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

import net.sf.extjwnl.data.POS;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.Callable;

public class TextRankRun implements Callable<TextRankRun>{

    // logging
    private final static Log LOG =
            LogFactory.getLog(TextRankRun.class.getName());

    /**
     * Public definitions.
     */

    public final static double MIN_NORMALIZED_RANK = 0.05D;
    public final static int MAX_NGRAM_LENGTH = 5;

    /**
     * Protected members.
     */

    protected LanguageModel lang = null;
    protected WordNet wordNet = null;

    protected String text = null;
    protected Graph graph = null;
    protected Graph ngram_subgraph = null;
    protected Map<NGram, MetricVector> metric_space = null;

    protected long start_time = 0L;
    protected long elapsed_time = 0L;


    /**
     * Constructor.
     */

    public TextRankRun(LanguageModel lang, WordNet wordNet, final String text) throws Exception {
        this.lang = lang;
        this.wordNet = wordNet;
        this.text = text;
    }


    /**
     * Run the TextRank algorithm on the given semi-structured text
     * (e.g., results of parsed HTML from crawled web content) to
     * build a graph of weighted key phrases.
     */
    public TextRankRun call() throws Exception {

        graph = new Graph();
        ngram_subgraph = null;
        metric_space = new HashMap<>();

        //////////////////////////////////////////////////
        // PASS 1: construct a graph from PoS tags

        initTime();

        // scan sentences to construct a graph of relevent morphemes

        final ArrayList<Sentence> s_list = new ArrayList<>();

        for (String sent_text : lang.splitParagraph(text)) {
            final Sentence s = new Sentence(sent_text.trim());
            s.mapTokens(lang, graph);
            s_list.add(s);

            if (LOG.isDebugEnabled()) {
                LOG.debug("s: " + s.text);
                LOG.debug(s.md5_hash);
            }
        }

        markTime("construct_graph");

        //////////////////////////////////////////////////
        // PASS 2: run TextRank to determine keywords

        initTime();

        final int max_results =
                (int) Math.round((double) graph.size() * Graph.KEYWORD_REDUCTION_FACTOR);

        graph.runTextRank();
        graph.sortResults(max_results);

        ngram_subgraph = NGram.collectNGrams(lang, s_list, graph.getRankThreshold());

        markTime("basic_textrank");

        if (LOG.isInfoEnabled()) {
            LOG.info("TEXT_BYTES:\t" + text.length());
            LOG.info("GRAPH_SIZE:\t" + graph.size());
        }

        //////////////////////////////////////////////////
        // PASS 3: lemmatize selected keywords and phrases

        initTime();

        Graph synset_subgraph = new Graph();

        // filter for edge cases

        if (usingWordNet()) {
            // test the lexical value of nouns and adjectives in WordNet

            for (Node n : graph.values()) {
                final KeyWord kw = (KeyWord) n.value;

                if (lang.isNoun(kw.pos)) {
                    SynsetLink.addKeyWord(synset_subgraph, n, kw.text, POS.NOUN, wordNet);
                } else if (lang.isAdjective(kw.pos)) {
                    SynsetLink.addKeyWord(synset_subgraph, n, kw.text, POS.ADJECTIVE, wordNet);
                }
            }

            // test the collocations in WordNet

            for (Node n : ngram_subgraph.values()) {
                final NGram gram = (NGram) n.value;

                if (gram.nodes.size() > 1) {
                    SynsetLink.addKeyWord(synset_subgraph, n, gram.getCollocation(), POS.NOUN, wordNet);
                }
            }

            synset_subgraph =
                    SynsetLink.pruneGraph(synset_subgraph, graph);
        }

        // augment the graph with n-grams added as nodes

        for (Node n : ngram_subgraph.values()) {
            final NGram gram = (NGram) n.value;

            if (gram.length < MAX_NGRAM_LENGTH) {
                graph.put(n.key, n);

                for (Node keyword_node : gram.nodes) {
                    n.connect(keyword_node);
                }
            }
        }

        markTime("augment_graph");

        //////////////////////////////////////////////////
        // PASS 4: re-run TextRank on the augmented graph

        initTime();

        graph.runTextRank();
        //graph.sortResults(graph.size() / 2);

        // collect stats for metrics

        final int ngram_max_count =
                NGram.calcStats(ngram_subgraph);

        if (usingWordNet()) {
            SynsetLink.calcStats(synset_subgraph);
        }

        markTime("ngram_textrank");

        if (LOG.isInfoEnabled()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("RANK: " + ngram_subgraph.dist_stats);

                for (Node n : new TreeSet<>(ngram_subgraph.values())) {
                    final NGram gram = (NGram) n.value;
                    LOG.debug(gram.getCount() + " " + n.rank + " " + gram.text /* + " @ " + gram.renderContexts() */);
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("RANK: " + synset_subgraph.dist_stats);

                for (Node n : new TreeSet<>(synset_subgraph.values())) {
                    final SynsetLink s = (SynsetLink) n.value;
                    LOG.info("emit: " + s.synset + " " + n.rank + " " + s.relation);
                }
            }
        }

        //////////////////////////////////////////////////
        // PASS 5: construct a metric space for overall ranking

        initTime();

        final double link_min = ngram_subgraph.dist_stats.getMin();
        final double link_coeff = ngram_subgraph.dist_stats.getMax() - ngram_subgraph.dist_stats.getMin();

        final double count_min = 1;
        final double count_coeff = (double) ngram_max_count - 1;

        final double synset_min = synset_subgraph.dist_stats.getMin();
        final double synset_coeff = synset_subgraph.dist_stats.getMax() - synset_subgraph.dist_stats.getMin();

        for (Node n : ngram_subgraph.values()) {
            final NGram gram = (NGram) n.value;

            if (gram.length < MAX_NGRAM_LENGTH) {
                final double link_rank = (n.rank - link_min) / link_coeff;
                final double count_rank = (gram.getCount() - count_min) / count_coeff;
                final double synset_rank = usingWordNet() ? n.maxNeighbor(synset_min, synset_coeff) : 0.0D;

                final MetricVector mv = new MetricVector(gram, link_rank, count_rank, synset_rank);
                metric_space.put(gram, mv);
            }
        }

        markTime("normalize_ranks");

        return this;
    }

    public boolean usingWordNet() {
        return wordNet != null;
    }


    //////////////////////////////////////////////////////////////////////
    // access and utility methods
    //////////////////////////////////////////////////////////////////////

    /**
     * Re-initialize the timer.
     */

    public void initTime() {
        start_time = System.currentTimeMillis();
    }


    /**
     * Report the elapsed time with a label.
     */

    public void markTime(final String label) {
        elapsed_time = System.currentTimeMillis() - start_time;

        if (LOG.isInfoEnabled()) {
            LOG.info("ELAPSED_TIME:\t" + elapsed_time + "\t" + label);
        }
    }


    /**
     * Accessor for the graph.
     */

    public Graph getGraph() {
        return graph;
    }


    /**
     * Serialize results to a string.
     */

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (Keyphrase keyphrase: getKeyphrases()) {
            sb.append(keyphrase.getMetric()).append("\t").append(keyphrase.getPhrase()).append("\n");
        }
        return sb.toString();
    }

    public List<Keyphrase> getKeyphrases() {
        final TreeSet<MetricVector> key_phrase_list = new TreeSet<>(metric_space.values());
        final List<Keyphrase> keyphraseList = new ArrayList<>();

        for (MetricVector mv : key_phrase_list) {
            if (mv.metric >= MIN_NORMALIZED_RANK) {
                keyphraseList.add(new Keyphrase(mv.value.text, mv.metric));
            }
        }
        return keyphraseList;
    }

}
