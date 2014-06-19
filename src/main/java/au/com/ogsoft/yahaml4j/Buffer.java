package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides buffering between the generated code and html contents
 */
public class Buffer {

    private final HamlGenerator generator;
    private final StringBuilder buffer, outputBuffer;

    public Buffer(HamlGenerator generator) {
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

    /*

  append: (str) ->
    @generator.mark() if @generator? and @buffer.length == 0
    @buffer += str if str?.length > 0

  flush: () ->
    @outputBuffer += @generator.generateFlush(@buffer) if @buffer?.length > 0
    @buffer = ''

  trimWhitespace: () ->
    if @buffer.length > 0
      i = @buffer.length - 1
      while i > 0
        ch = @buffer.charAt(i)
        if @_isWhitespace(ch)
          i--
        else if i > 1 and (ch == 'n' or ch == 't') and (@buffer.charAt(i - 1) == '\\')
          i -= 2
        else
          break
      if i > 0 and i < @buffer.length - 1
        @buffer = @buffer.substring(0, i + 1)
      else if i == 0 and @_isWhitespace(@buffer.charAt(0))
        @buffer = ''

  _isWhitespace: (ch) ->
    ch == ' ' or ch == '\t' or ch == '\n'

     */
}
