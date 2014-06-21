package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * HAML Tokiniser: This class is responsible for parsing the haml source into tokens
 */
public class Tokeniser {

    private final String name;
    private final String source;
    private final SourceBuffer buffer;
    private Token token;
    private Token prevToken;
    private Integer lineNumber;
    private Integer characterNumber;
    private String currentLine;

    private static Pattern CURRENT_LINE_MATCHER = Pattern.compile("[^\\n]*");

    // tokenMatchers
    private static Pattern WHITESPACE = Pattern.compile("[ \\t]+");
    private static Pattern ELEMENT = Pattern.compile("%[a-zA-Z][a-zA-Z0-9]*");
//        idSelector:       /#[a-zA-Z_\-][a-zA-Z0-9_\-]* /g,
//        classSelector:    /\.[a-zA-Z0-9_\-]+/g,
//        identifier:       /[a-zA-Z][a-zA-Z0-9\-]* /g,
//        quotedString:     /[\'][^\'\n]*[\']/g,
//        quotedString2:    /[\"][^\"\n]*[\"]/g,
//        comment:          /\-#/g,
//        escapeHtml:       /\&=/g,
//        unescapeHtml:     /\!=/g,
//        objectReference:  /\[[a-zA-Z_@][a-zA-Z0-9_]*\]/g,
//        doctype:          /!!!/g,
    private static Pattern CONTINUELINE = Pattern.compile("\\|[ \\t]*\\n");
    private static Pattern FILTER = Pattern.compile(":\\w+");

    private interface MatchedFn {
        public String match(String value);
    }

    public Tokeniser(String name, String source) {
        this.name = name;
        this.source = source;

        buffer = new SourceBuffer(source);
        lineNumber = 0;
        characterNumber = 0;
    }

    public Token getToken() {
        return token;
    }

    /**
     * Match and return the next token in the input buffer
     * @return Token
     */
    public Token getNextToken() {
        prevToken = token;
        token = null;

        if (buffer.empty()) {
            token = new Token(Token.TokenType.EOF);
        } else {
            initLine();

            if (token == null) {
                char ch = buffer.peek();
                char ch1 = buffer.peek(1);
                if (ch == 10 || (ch == 13 && ch1 == 10)) {
                    token = new Token(Token.TokenType.EOL);
                    if (ch == 13) {
                        token.setMatched(String.valueOf(ch) + ch1);
                        advanceCharsInBuffer(2);
                    } else {
                        token.setMatched(String.valueOf(ch));
                        advanceCharsInBuffer(1);
                    }
                }
            }

            matchMultiCharToken(WHITESPACE, new Token(Token.TokenType.WS), null);
            matchMultiCharToken(CONTINUELINE, new Token(Token.TokenType.CONTINUELINE), null);
            matchMultiCharToken(ELEMENT, new Token(Token.TokenType.ELEMENT), new MatchedFn() {
                @Override
                public String match(String value) {
                    return value.substring(1);
                }
            });

        /*
      @matchMultiCharToken(@tokenMatchers.idSelector, { idSelector: true, token: 'ID' }, (matched) -> matched.substring(1) )
      @matchMultiCharToken(@tokenMatchers.classSelector, { classSelector: true, token: 'CLASS' }, (matched) -> matched.substring(1) )
      @matchMultiCharToken(@tokenMatchers.identifier, { identifier: true, token: 'IDENTIFIER' })
      @matchMultiCharToken(@tokenMatchers.doctype, { doctype: true, token: 'DOCTYPE' })
      @matchMultiCharToken(@tokenMatchers.filter, { filter: true, token: 'FILTER' }, (matched) -> matched.substring(1) )

      if !@token
        str = @matchToken(@tokenMatchers.quotedString)
        str = @matchToken(@tokenMatchers.quotedString2) if not str
        if str
          @token = { string: true, token: 'STRING', tokenString: str.substring(1, str.length - 1), matched: str }
          @advanceCharsInBuffer(str.length)

      @matchMultiCharToken(@tokenMatchers.comment, { comment: true, token: 'COMMENT' })
      @matchMultiCharToken(@tokenMatchers.escapeHtml, { escapeHtml: true, token: 'ESCAPEHTML' })
      @matchMultiCharToken(@tokenMatchers.unescapeHtml, { unescapeHtml: true, token: 'UNESCAPEHTML' })
      @matchMultiCharToken(@tokenMatchers.objectReference, { objectReference: true, token: 'OBJECTREFERENCE' }, (matched) ->
        matched.substring(1, matched.length - 1)
      )

      if !@token and @buffer and @buffer.charAt(@bufferIndex) == '{'
        @matchJavascriptHash()

      @matchSingleCharToken('(', { openBracket: true, token: 'OPENBRACKET' })
      @matchSingleCharToken(')', { closeBracket: true, token: 'CLOSEBRACKET' })
      @matchSingleCharToken('=', { equal: true, token: 'EQUAL' })
      @matchSingleCharToken('/', { slash: true, token: 'SLASH' })
      @matchSingleCharToken('!', { exclamation: true, token: 'EXCLAMATION' })
      @matchSingleCharToken('-', { minus: true, token: 'MINUS' })
      @matchSingleCharToken('&', { amp: true, token: 'AMP' })
      @matchSingleCharToken('<', { lt: true, token: 'LT' })
      @matchSingleCharToken('>', { gt: true, token: 'GT' })
      @matchSingleCharToken('~', { tilde: true, token: 'TILDE' })

         */

            if (token == null) {
                token = new Token(Token.TokenType.UNKNOWN, String.valueOf(buffer.peek()));
                advanceCharsInBuffer(1);
            }

        }

        return token;
    }

    /**
     * Match a multi-character token
     */
    private void matchMultiCharToken(Pattern matcher, Token token, MatchedFn fn) {
        if (this.token == null) {
            String matched = buffer.matchRegex(matcher);
            if (matched != null) {
                this.token = token;
                this.token.setMatched(matched);
                if (fn != null) {
                    this.token.setTokenString(fn.match(matched));
                } else {
                    this.token.setTokenString(matched);
                }
                advanceCharsInBuffer(matched.length());
            }
        }
    }

    /**
     * Advances the input buffer pointer by a number of characters, updating the line and character counters
     */
    private void advanceCharsInBuffer(int numChars) {
        int i = 0;
        while (i < numChars) {
            char ch = buffer.get();
            char ch1 = buffer.peek();
            if (ch == 13 && ch1 == 10) {
                buffer.position(1);
                lineNumber++;
                characterNumber = 0;
                currentLine = getCurrentLine(i);
                i++;
            } else if (ch == 10) {
                lineNumber++;
                characterNumber = 0;
                currentLine = getCurrentLine(i);
            } else {
                characterNumber++;
            }
            i++;
        }
    }

    /**
     * Initilise the line and character counters
     */
    private void initLine() {
        if (StringUtils.isNotEmpty(currentLine)) {
            currentLine = getCurrentLine(0);
            lineNumber = 1;
            characterNumber = 0;
        }
    }

    /**
     * Returns the current line in the input buffer
     */
    String getCurrentLine(Integer index) {
        buffer.mark();
        if (index > 0) {
            buffer.position(index);
        }
        while (!buffer.empty() && buffer.get() != '\n');
        int pos = buffer.position();
        buffer.reset();

        if (pos > index) {
            return StringUtils.stripEnd(buffer.subSequence(index, pos), "\n");
        }

        return "";
    }

    /**
     * Calculate the indent level of the provided whitespace
     */
    public int calculateIndent(String whitespace) {
        int indent = 0;
        int i = 0;
        while (i < whitespace.length()) {
            if (whitespace.charAt(i) == 9) {
                indent += 2;
            } else {
                indent++;
            }
            i++;
        }

        return (int) Math.floor((indent + 1) / 2);
    }

    /**
     * Try to match a token with the given regexp
     */
    public String matchToken(Pattern regex) {
        return buffer.matchRegex(regex);
    }

    /**
     * Returns the current line and character counters
     */
    public ParsePoint currentParsePoint() {
        return new ParsePoint(lineNumber, characterNumber, currentLine);
    }

    /**
     * Skips to the end of the line and returns the string that was skipped
     */
    public String skipToEOLorEOF() {
        String text = "";
        if (token.type != Token.TokenType.EOF && token.type != Token.TokenType.EOL) {
            if (StringUtils.isNotEmpty(token.getMatched())) {
                text += token.getMatched();
            }
            String line = buffer.matchRegex(CURRENT_LINE_MATCHER);
            if (StringUtils.isNotEmpty(line)) {
                String contents = StringUtils.stripEnd(line, null);
                if (StringUtils.endsWith(contents, "|")) {
                    text += contents.substring(0, contents.length() - 1);
                    advanceCharsInBuffer(contents.length() - 1);
                    getNextToken();
//                    text += parseMultiLine();
                } else {
                    text += line;
                    advanceCharsInBuffer(line.length());
                    getNextToken();
                }
            }
        }
        return text;
    }

    /**
     * Returns an error string filled out with the line and character counters
     */
    public String parseError(String error) {
        return HamlRuntime.templateError(lineNumber, characterNumber, currentLine, error);
    }

    public void pushBackToken() {

    }

    /*

    ###
    Match a single character token
    ###
    matchSingleCharToken: (ch, token) ->
            if !@token and @buffer.charAt(@bufferIndex) == ch
    @token = token
    @token.tokenString = ch
    @token.matched = ch
    @advanceCharsInBuffer(1)

    ###
    Look ahead a number of tokens and return the token found
    ###
    lookAhead: (numberOfTokens) ->
    token = null
            if numberOfTokens > 0
    currentToken = @token
    prevToken = @prevToken
    currentLine = @currentLine
    lineNumber = @lineNumber
    characterNumber = @characterNumber
    bufferIndex = @bufferIndex

    i = 0
    token = this.getNextToken() while i++ < numberOfTokens

    @token = currentToken
    @prevToken = prevToken
    @currentLine = currentLine
    @lineNumber = lineNumber
    @characterNumber = characterNumber
    @bufferIndex = bufferIndex
            token

    ###
    Parses a multiline code block and returns the parsed text
    ###
    parseMultiLine: ->
    text = ''
            while @token.continueLine
    @currentLineMatcher.lastIndex = @bufferIndex
    line = @currentLineMatcher.exec(@buffer)
    if line and line.index == @bufferIndex
    contents = (_.str || _).rtrim(line[0])
    if (_.str || _).endsWith(contents, '|')
    text += contents.substring(0, contents.length - 1)
    @advanceCharsInBuffer(contents.length - 1)
    @getNextToken()
    text

    ###
    Pushes back the current token onto the front of the input buffer
    ###
    pushBackToken: ->
            if !@token.eof
    @bufferIndex -= @token.matched.length
    @token = @prevToken

    ###
    Is the current token an end of line or end of input buffer
    ###
    isEolOrEof: ->
    @token.eol or @token.eof

    ###
    Match a Javascript Hash {...}
    ###
    matchJavascriptHash: ->
    currentIndent = @calculateCurrentIndent()
    i = @bufferIndex + 1
    characterNumberStart = @characterNumber
    lineNumberStart = @lineNumber
    braceCount = 1
            while i < @buffer.length and (braceCount > 1 or @buffer.charAt(i) isnt '}')
    ch = @buffer.charAt(i)
    chCode = @buffer.charCodeAt(i)
    if ch == '{'
    braceCount++
    i++
            else if ch == '}'
    braceCount--
    i++
            else if chCode == 10 or chCode == 13
    i++
            else
    i++
            if i == @buffer.length
    @characterNumber = characterNumberStart + 1
    @lineNumber = lineNumberStart
    throw @parseError('Error parsing attribute hash - Did not find a terminating "}"')
    else
    @token =
    attributeHash: true
    token: 'ATTRHASH'
    tokenString: @buffer.substring(@bufferIndex, i + 1)
    matched: @buffer.substring(@bufferIndex, i + 1)
    @advanceCharsInBuffer(i - @bufferIndex + 1)

    ###
    Calculate the indent value of the current line
    ###
    calculateCurrentIndent: ->
    @tokenMatchers.whitespace.lastIndex = 0
    result = @tokenMatchers.whitespace.exec(@currentLine)
    if result?.index == 0
    @calculateIndent(result[0])
    else
            0

     */

}
