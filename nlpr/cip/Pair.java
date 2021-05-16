package nlpr.cip;

public class Pair
{
    String character;
    Float fontSize;
    Float X;
    Float Y;
    
    public String getCharacter() {
        return this.character;
    }
    
    public Float getFontSize() {
        return this.fontSize;
    }
    
    public Float getY() {
        return this.Y;
    }
    
    Pair(final String character, final float fontSize, final float X, final float Y) {
        this.character = character;
        this.fontSize = fontSize;
        this.X = X;
        this.Y = Y;
    }
}
