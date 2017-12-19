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

import com.sharethis.common.IOUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Java implementation of the TextRank algorithm by Rada Mihalcea, et al.
 * http://lit.csci.unt.edu/index.php/Graph-based_NLP
 *
 * @author paco@sharethis.com
 */

public class TextRank {

    protected LanguageModel lang = null;
    protected WordNet wordNet = null;

    private ExecutorService ex = Executors.newSingleThreadExecutor();
    private static long DEFAULT_TIMEOUT_MILLIS = 10000;
    private long timeoutMillis;

    public TextRank(final String lang_code) throws Exception{
        this(lang_code, DEFAULT_TIMEOUT_MILLIS);
    }

    public TextRank(final String lang_code, final long timeoutMillis) throws Exception {
        lang = LanguageModel.buildLanguage(lang_code);
        boolean use_wordnet = ("en".equals(lang_code));
        if (use_wordnet) {
            wordNet = new WordNet();
        }
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Run the TextRank algorithm on the given semi-structured text
     * (e.g., results of parsed HTML from crawled web content) to
     * build a graph of weighted key phrases.
     */
    public TextRankRun run(final String text) throws Exception {
        return ex.submit(new TextRankRun(lang, wordNet, text))
              .get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public TextRankRun run(final File textFile) throws Exception {
        final String text = IOUtils.readFile(textFile.getAbsolutePath());
        return run(text);
    }

    public void shutdown() {
        ex.shutdown();
    }

}
