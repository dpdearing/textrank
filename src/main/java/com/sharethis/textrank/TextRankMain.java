package com.sharethis.textrank;


import com.sharethis.common.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextRankMain {

    // logging
    private final static Log LOG = LogFactory.getLog(TextRankMain.class.getName());

    //////////////////////////////////////////////////////////////////////
    // command line interface
    //////////////////////////////////////////////////////////////////////

    /**
     * Main entry point.
     */

    public static void main(final String[] args)
            throws Exception {

        final String log4j_conf = args[0];
        final String lang_code = args[1];
        final String data_file = args[2];

        // set up logging for debugging and instrumentation
        PropertyConfigurator.configure(log4j_conf);

        List<File> texts;
        File data = new File(data_file);
        if (data.isDirectory()) {
            texts = listFilesForFolder(data);
        }
        else {
            texts = Collections.singletonList(data);
        }

        // main entry point for the algorithm
        final TextRank tr = new TextRank(lang_code);
        for (File textFile : texts) {
            final TextRankRun run = tr.run(textFile);
            LOG.info("\n\n=======:: "+textFile.getAbsolutePath());
            LOG.info("\n" + run);
        }
        tr.shutdown();
    }

    static public List<File> listFilesForFolder(final File folder) {
        ArrayList<File> files = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                files.addAll(listFilesForFolder(fileEntry));
            } else {
                files.add(fileEntry);
            }
        }
        return files;
    }

}
