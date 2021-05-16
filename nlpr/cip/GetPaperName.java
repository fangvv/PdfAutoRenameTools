package nlpr.cip;

import java.io.*;
import org.apache.pdfbox.pdmodel.*;
import java.util.logging.*;
import java.util.function.*;
import java.util.*;

public class GetPaperName
{
    private static String GetTitleOfPDF(final String pdfFileName) {
        final File pdfFile = new File(pdfFileName);
        try {
            final PDDocument document = PDDocument.load(pdfFile);
            final TextLocationExtender stripper = new TextLocationExtender();
            stripper.setSortByPosition(true);
            stripper.setStartPage(0);
            stripper.setEndPage(1);
            stripper.getText(document);
            document.close();
            return clean_file_name(stripper.getTitle());
        }
        catch (Exception e) {
            System.err.println("this file is wrong!!!\t" + pdfFileName);
            return null;
        }
    }
    
    public static void changeName(final String input_file_name) {
        if (!input_file_name.endsWith(".pdf")) {
            return;
        }
        final String output_File_name = GetTitleOfPDF(input_file_name);
        if (output_File_name != null && output_File_name.replaceAll(" ", "").length() > 0 && output_File_name.length() < 250) {
            change_one_file_name(input_file_name, output_File_name);
        }
    }
    
    private static void change_one_file_name(final String original, final String target) {
        final File f = new File(original);
        final String c = f.getParent();
        File mm = new File(c + File.separator + target + ".pdf");
        int number = 1;
        while (!f.renameTo(mm)) {
            mm = new File(c + File.separator + target + "_" + number + ".pdf");
            if (++number > 10) {
                break;
            }
        }
        System.out.println("rename:" + f.getAbsolutePath() + "\tto\t" + mm.getAbsolutePath());
    }
    
    private static String clean_file_name(String input) {
        input = input.replace("- ", "");
        input = input.replaceAll("\\?|\u3001|\u2572|/|\\*|<|>|:", "_");
        return input;
    }
    
    public static void main(final String[] args) {
        final String Dirname = args[0];
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
        final List<String> files = utils.GetDirFiles(Dirname);
        files.parallelStream().forEach((Consumer<? super Object>)GetPaperName::changeName);
    }
}
