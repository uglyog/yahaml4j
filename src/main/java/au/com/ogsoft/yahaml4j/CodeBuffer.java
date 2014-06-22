package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides buffering between the generated code and html contents
 */
public class CodeBuffer {

    private final HamlGenerator generator;
    private final StringBuilder buffer;
    private final StringBuilder outputBuffer;

    public CodeBuffer(HamlGenerator generator) {
        this.generator = generator;
        outputBuffer = new StringBuilder();
        buffer = new StringBuilder();
    }

    public void appendToOutputBuffer(String s) {
        if (StringUtils.isNoneEmpty(s)) {
            flush();
            outputBuffer.append(s);
        }
    }

    void flush() {
        if (buffer.length() > 0) {
            outputBuffer.append(generator.generateFlush(buffer.toString()));
        }
        buffer.delete(0, buffer.length());
    }

    public String output() {
        return outputBuffer.toString();
    }

    public void append(String str) {
        if (generator != null && buffer.length() == 0) {
            generator.mark();
        }

        if (StringUtils.isNotEmpty(str)) {
            buffer.append(str);
        }
    }

    public void trimWhitespace() {
        if (buffer.length() > 0) {
            int i = buffer.length() - 1;
            while (i > 0) {
                char ch = buffer.charAt(i);
                if (_isWhitespace(ch)) {
                    i--;
                } else if (i > 1 && (ch == 'n' || ch == 't') && (buffer.charAt(i - 1) == '\\')) {
                    i -= 2;
                } else {
                    break;
                }
            }
            if (i > 0 && i < buffer.length() - 1) {
                buffer.delete(i + 1, buffer.length());
            } else if (i == 0 && _isWhitespace(buffer.charAt(0))) {
                buffer.delete(0, buffer.length());
            }
        }
    }

    private boolean _isWhitespace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n';
    }

    public StringBuilder getBuffer() {
        return buffer;
    }
}
