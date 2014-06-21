package au.com.ogsoft.yahaml4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceBuffer {

    private final String source;
    private int index;
    private int mark;

    public SourceBuffer(String source) {
        this.source = source;
        index = 0;
    }


    public boolean empty() {
        return index >= source.length();
    }

    public int mark() {
        mark = index;
        return mark;
    }

    public char get() {
        return source.charAt(index++);
    }

    public void position(int i) {
        index += i;
    }

    public void reset() {
        index = mark;
    }

    public int position() {
        return index;
    }

    public String subSequence(Integer index, int pos) {
        return source.substring(this.index + index, pos);
    }

    /**
     * Try to match a token with the given regexp
     */
    public String matchRegex(Pattern matcher) {
        Matcher m = matcher.matcher(source).region(index, source.length());
        if (m.lookingAt()) {
            return m.group();
        }
        return null;
    }

    public char peek() {
        return source.length() > index ? source.charAt(index) : 0;
    }

    public char peek(int i) {
        return source.length() > index + i ? source.charAt(index + i) : 0;
    }
}
