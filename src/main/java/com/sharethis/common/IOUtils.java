package com.sharethis.common;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IOUtils {
    private static final Log LOG = LogFactory.getLog(IOUtils.class);

    public IOUtils() {
    }

    public static BufferedReader getReader(String file_name) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file_name)));
    }

    public static String getFileText(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[256];

        int len;
        while((len = in.read(buf)) > -1) {
            sb.append(buf, 0, len);
        }

        return sb.toString();
    }

    public static String readFile(String file_name) throws IOException {
        BufferedReader r = getReader(file_name);

        String var2;
        try {
            var2 = getFileText(r);
        } finally {
            r.close();
        }

        return var2;
    }

}