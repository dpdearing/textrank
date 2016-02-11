package com.sharethis.textrank;


import com.sharethis.common.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

public class TextRankMain {

    // logging
    private final static Log LOG = LogFactory.getLog(TextRankMain.class.getName());

    //////////////////////////////////////////////////////////////////////
    // command line interface
    //////////////////////////////////////////////////////////////////////

    /**
     * Main entry point.
     */

    public static void
    main(final String[] args)
            throws Exception {

        final String log4j_conf = args[0];
        final String lang_code = args[1];
        final String data_file = args[2];

        // set up logging for debugging and instrumentation

        PropertyConfigurator.configure(log4j_conf);

        // load the sample text from a file

        final String text = IOUtils.readFile(data_file);

        // main entry point for the algorithm
        final TextRank tr = new TextRank(lang_code);
        final TextRankRun run = tr.run(text);
        tr.shutdown();

        LOG.info("\n" + run);
    }
}
