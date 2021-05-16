package nlpr.cip;

import java.io.*;
import java.util.*;

public class utils
{
    public static List<String> GetDirFiles(final String dirName) {
        final File file = new File(dirName);
        List<String> output = new ArrayList<String>();
        output = ergodic(file, output);
        return output;
    }
    
    private static List<String> ergodic(final File file, final List<String> resultFileName) {
        final File[] files = file.listFiles();
        if (files == null) {
            return resultFileName;
        }
        for (final File f : files) {
            if (f.isDirectory()) {
                ergodic(f, resultFileName);
            }
            else {
                resultFileName.add(f.getPath());
            }
        }
        return resultFileName;
    }
    
    public static String JoinString(final List<String> list, final String conjunction) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String item : list) {
            if (first) {
                first = false;
            }
            else {
                sb.append(conjunction);
            }
            sb.append(item);
        }
        return sb.toString();
    }
}
