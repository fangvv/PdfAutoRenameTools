package nlpr.cip;

import java.io.*;
import org.apache.pdfbox.text.*;
import java.util.function.*;
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
            this.wordSizes.add(new Pair(string, max_font_size, textPositions.get(0).getX(), textPositions.get(0).getY()));
        }
    }
    
    public String getTitle() {
        final List<Float> valid_pos = this.get_title_y_position();
        final List<String> char_lst = this.wordSizes.stream().filter(x -> valid_pos.contains(x.getY())).map((Function<? super Object, ?>)Pair::getCharacter).collect((Collector<? super Object, ?, List<String>>)Collectors.toList());
        final String title = utils.JoinString(char_lst, " ");
        return title;
    }
    
    private List<Float> get_title_y_position() {
        final float max_title_font = this.get_max_font();
        final Set<Float> output = this.wordSizes.stream().filter(wordSize -> wordSize.getFontSize() >= max_title_font).map((Function<? super Object, ?>)Pair::getY).collect((Collector<? super Object, ?, Set<Float>>)Collectors.toSet());
        final List<Float> valid_position = output.stream().sorted().limit(3L).collect((Collector<? super Object, ?, List<Float>>)Collectors.toList());
        final List<Float> output_position = new ArrayList<Float>();
        output_position.add(valid_position.get(0));
        for (int i = 1; i < valid_position.size() && valid_position.get(i) - valid_position.get(i - 1) < 2.0f * max_title_font; ++i) {
            output_position.add(valid_position.get(i));
        }
        return output_position;
    }
    
    private float get_max_font() {
        final List<Float> list = this.wordSizes.stream().map((Function<? super Object, ?>)Pair::getFontSize).distinct().sorted(Comparator.reverseOrder()).collect((Collector<? super Object, ?, List<Float>>)Collectors.toList());
        return list.get(list.size() / 3);
    }
}
