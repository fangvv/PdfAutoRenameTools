package nlpr.cip;

import java.io.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import java.util.logging.*;
import java.util.function.*;
import java.util.*;

public class Main {

    public static String GetTitleOfPDF(final String pdfFileName) {
        final File pdfFile = new File(pdfFileName);
        try (final PDDocument document = Loader.loadPDF(pdfFile)) {
            final TextLocationExtender stripper = new TextLocationExtender();
            stripper.setSortByPosition(true);
            stripper.setStartPage(0);
            stripper.setEndPage(1);
            stripper.getText(document);
            //System.out.println(stripper.getTitle());
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
            final String new_file_name = output_File_name + ".pdf";
            if (new_file_name.equalsIgnoreCase(new File(input_file_name).getName())) {
                return;
            }
            change_one_file_name(input_file_name, new_file_name);
        }
    }

    /**
     * 把 original 重命名为 target（target 必须是含扩展名的完整文件名）。
     * 若目标名已占用，则在扩展名前追加 _1.._10 重试。
     * @return 实际落盘的文件名；失败（含重试耗尽或重命名抛错）时返回 null。
     */
    public static String change_one_file_name(final String original, final String target) {
        final File f = new File(original);
        final String c = f.getParent();
        final int dot = target.lastIndexOf('.');
        final String base = dot >= 0 ? target.substring(0, dot) : target;
        final String ext = dot >= 0 ? target.substring(dot) : "";
        File mm = new File(c + File.separator + target);
        try {
            int number = 1;
            while (!f.renameTo(mm)) {
                mm = new File(c + File.separator + base + "_" + number + ext);
                if (++number > 10) {
                    System.err.println("rename failed (too many conflicts): " + f.getAbsolutePath());
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("rename error: " + f.getAbsolutePath() + "\t" + e);
            return null;
        }
        System.out.println("rename:" + f.getAbsolutePath() + "\tto\t" + mm.getAbsolutePath());
        return mm.getName();
    }

    public static String clean_file_name(String input) {
        if (input == null) {
            return null;
        }
        input = input.replace("- ", "");
        // 替换 Windows/Linux 下不允许出现在文件名里的字符
        input = input.replaceAll("\\?|\u3001|\u2572|/|\\\\|\\*|<|>|:|\\||\"", "_");
        // 去掉控制字符、压缩多余空白
        input = input.replaceAll("[\\x00-\\x1F]", "").replaceAll("\\s{2,}", " ").trim();
        // Windows 不允许文件名以空格或点结尾
        input = input.replaceAll("[. ]+$", "");
        return input;
    }

    public static void main(final String[] args) {
        final String Dirname = args[0];
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
        final List<String> files = utils.GetDirFiles(Dirname);
        files.parallelStream().forEach(Main::changeName);
    }
}
