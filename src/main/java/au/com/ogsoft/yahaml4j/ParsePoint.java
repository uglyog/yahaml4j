package au.com.ogsoft.yahaml4j;

public class ParsePoint {

    public int lineNumber;
    public int characterNumber;
    public String currentLine;

    public ParsePoint(Integer lineNumber, Integer characterNumber, String currentLine) {
        this.lineNumber = lineNumber;
        this.characterNumber = characterNumber;
        this.currentLine = currentLine;
    }
}
