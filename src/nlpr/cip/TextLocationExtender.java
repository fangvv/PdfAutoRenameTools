package nlpr.cip;

import java.io.*;
import org.apache.pdfbox.text.*;

import java.util.stream.*;
import java.util.*;

public class TextLocationExtender extends PDFTextStripper
{
    public List<Pair> wordSizes;
    
    public TextLocationExtender() throws IOException {
        this.wordSizes = new ArrayList<Pair>();
    }
    
    @Override
    protected void writeString(final String string, final List<TextPosition> textPositions) throws IOException {
        float max_font_size = -1.0f;
        for (final TextPosition textPosition : textPositions) {
            if (textPosition.getFontSizeInPt() > max_font_size && textPosition.getDir() == 0.0 && !string.equals(" ")) {
                max_font_size = textPosition.getFontSizeInPt();
            }
        }
        if (max_font_size > 0.0f) {
            //System.out.println(string + " " + max_font_size + " " + textPositions.get(0).getX() + " " + textPositions.get(0).getY());
            this.wordSizes.add(new Pair(string, max_font_size, textPositions.get(0).getX(), textPositions.get(0).getY()));
        }
    }
    
    public String getTitle() {
        final List<Float> valid_pos = this.get_title_y_position();
        final List<String> char_lst = this.wordSizes.stream().filter(x -> valid_pos.contains(x.getY())).map(Pair::getCharacter).collect(Collectors.toList());
        final String title = utils.JoinString(char_lst, " ");
        return title;
    }
    
    private List<Float> get_title_y_position() {
        final float max_title_font = this.get_max_font();
        //System.out.println(max_title_font);
        final Set<Float> output = this.wordSizes.stream().filter(wordSize -> wordSize.getFontSize() >= max_title_font).map(Pair::getY).collect(Collectors.toSet());
//        System.out.println(" ");
//        for (Float y: output)
//        {
//            System.out.println(y);
//        }
//        System.out.println(" ");
        final List<Float> valid_position = output.stream().sorted().limit(3L).collect(Collectors.toList());

        final List<Float> output_position = new ArrayList<Float>();

        float title_size = Float.MIN_VALUE;

        for (Float y: valid_position)
        {
            //System.out.println(y);
            //System.out.println("test");

            //先找到这个位置的字体大小
            for (int i = 0; i < this.wordSizes.size(); i++) {
                Pair p = (Pair)this.wordSizes.get(i);
                if (p.getY() == y)
                {
                    if (title_size == Float.MIN_VALUE)
                    {
                        output_position.add(y);
                        title_size = p.getFontSize();
                        //System.out.println(title_size);
                    }
                    else if (p.getFontSize() == title_size)
                    {
                        output_position.add(y);
                    }
                    break;
                }

            }
        }

        return output_position;
    }
    
    private float get_max_font() {
        final List<Float> list = this.wordSizes.stream().map(Pair::getFontSize).distinct().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        //list.forEach(System.out::println);
        return list.get(list.size() / 3);
    }
}
