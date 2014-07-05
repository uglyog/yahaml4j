package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * HAML Tokiniser: This class is responsible for parsing the haml source into tokens
 */
public class Tokeniser {

    // tokenMatchers
    private static Pattern WHITESPACE = Pattern.compile("[ \\t]+");
    private static Pattern ELEMENT = Pattern.compile("%[a-zA-Z][a-zA-Z0-9]*");
    private static Pattern IDSELECTOR = Pattern.compile("#[a-zA-Z0-9_\\-]+");
    private static Pattern CLASSSELECTOR = Pattern.compile("\\.[a-zA-Z0-9_\\-]+");
    private static Pattern HTMLIDENTIFIER = Pattern.compile("[a-zA-Z][a-zA-Z0-9\\-]*");
    private static Pattern QUOTEDSTRING = Pattern.compile("'[^'\\n]*'");
    private static Pattern QUOTEDSTRING2 = Pattern.compile("\"[^\"\\n]*\"");
    private static Pattern COMMENT = Pattern.compile("\\-#");
    private static Pattern ESCAPEHTML = Pattern.compile("&=");
    private static Pattern UNESCAPEHTML = Pattern.compile("!=");
    private static Pattern OBJECTREF = Pattern.compile("\\[[a-zA-Z_][a-zA-Z0-9_]*\\]");
//        doctype:          /!!!/g,
    private static Pattern CONTINUELINE = Pattern.compile("\\|[ \\t]*\\n");
    private static Pattern FILTER = Pattern.compile(":\\w+");
    private static Pattern CODE_IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private final String name;
    private final String source;
    private final SourceBuffer buffer;
    private Token token;
    private Token prevToken;
    private Integer lineNumber;
    private Integer characterNumber;
    private String currentLine;

    private static Pattern CURRENT_LINE_MATCHER = Pattern.compile("[^\\n]*");

    private Mode mode;

    /**
     * Is the current token an end of line or end of input buffer
     */
    public boolean isEolOrEof() {
        return token.type == Token.TokenType.EOL || token.type == Token.TokenType.EOF;
    }

    /**
     * Look ahead a number of tokens and return the token found
     */
    public Token lookAhead(int numberOfTokens) {
        Token token = null;
        if (numberOfTokens > 0) {
            Token currentToken = this.token;
            Token prevToken = this.prevToken;
            String currentLine = this.currentLine;
            int lineNumber = this.lineNumber;
            int characterNumber = this.characterNumber;
            buffer.mark();

            int i = 0;
            while (i++ < numberOfTokens) {
                token = getNextToken();
            }

            this.token = currentToken;
            this.prevToken = prevToken;
            this.currentLine = currentLine;
            this.lineNumber = lineNumber;
            this.characterNumber = characterNumber;
            buffer.reset();
        }
        return token;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void clearMode() {
        this.mode = null;
    }

    public enum Mode {
        ATTRLIST, ATTRHASH
    }

    private interface MatchedFn {
        public String match(String value);
    }

    public Tokeniser(String name, String source) {
        this.name = name;
        this.source = source;

        buffer = new SourceBuffer(source);
        lineNumber = 0;
        characterNumber = 0;
        currentLine = null;
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

            matchMultiCharToken(WHITESPACE, Token.TokenType.WS, null);

            if (this.mode == null) {
                matchMultiCharToken(CONTINUELINE, Token.TokenType.CONTINUELINE, null);
                matchMultiCharToken(ELEMENT, Token.TokenType.ELEMENT, new MatchedFn() {
                    @Override
                    public String match(String value) {
                        return value.substring(1);
                    }
                });
                matchMultiCharToken(IDSELECTOR, Token.TokenType.IDSELECTOR, new MatchedFn() {
                    @Override
                    public String match(String value) {
                        return value.substring(1);
                    }
                });
                matchMultiCharToken(CLASSSELECTOR, Token.TokenType.CLASSSELECTOR, new MatchedFn() {
                    @Override
                    public String match(String value) {
                        return value.substring(1);
                    }
                });
                matchMultiCharToken(COMMENT, Token.TokenType.COMMENT, null);
                matchMultiCharToken(ESCAPEHTML, Token.TokenType.ESCAPEHTML, null);
                matchMultiCharToken(UNESCAPEHTML, Token.TokenType.UNESCAPEHTML, null);
                matchMultiCharToken(OBJECTREF, Token.TokenType.OBJECTREF, new MatchedFn() {
                    @Override
                    public String match(String value) {
                        return value.substring(1, value.length() - 1);
                    }
                });
            }

            if (this.mode == Mode.ATTRHASH) {
                matchMultiCharToken(CODE_IDENTIFIER, Token.TokenType.CODE_ID, null);
            }

            if (this.mode == Mode.ATTRLIST) {
                matchMultiCharToken(HTMLIDENTIFIER, Token.TokenType.HTMLIDENTIFIER, null);

                if (token == null) {
                    String str = matchToken(QUOTEDSTRING);
                    if (str == null) {
                        str = matchToken(QUOTEDSTRING2);
                    }
                    if (str != null) {
                        token = new Token(Token.TokenType.STRING, str);
                        token.setTokenString(str.substring(1, str.length() - 1));
                        advanceCharsInBuffer(str.length());
                    }
                }
            }

                /*
              @matchMultiCharToken(@tokenMatchers.doctype, { doctype: true, token: 'DOCTYPE' })
              @matchMultiCharToken(@tokenMatchers.filter, { filter: true, token: 'FILTER' }, (matched) -> matched.substring(1) )
              */

            matchSingleCharToken('{', Token.TokenType.OPENBRACE);
            matchSingleCharToken('}', Token.TokenType.CLOSEBRACE);
            matchSingleCharToken(',', Token.TokenType.COMMA);
            matchSingleCharToken(':', Token.TokenType.COLON);
            matchSingleCharToken('/', Token.TokenType.SLASH);
            matchSingleCharToken('(', Token.TokenType.OPENBRACKET);
            matchSingleCharToken(')', Token.TokenType.CLOSEBRACKET);
            matchSingleCharToken('=', Token.TokenType.EQUAL);
            matchSingleCharToken('!', Token.TokenType.EXCLAMATION);
            matchSingleCharToken('-', Token.TokenType.MINUS);
            matchSingleCharToken('&', Token.TokenType.AMP);
            matchSingleCharToken('<', Token.TokenType.LT);
            matchSingleCharToken('>', Token.TokenType.GT);
            matchSingleCharToken('~', Token.TokenType.TILDE);

            if (token == null) {
                token = new Token(Token.TokenType.UNKNOWN, String.valueOf(buffer.peek()));
                advanceCharsInBuffer(1);
            }

        }

        return token;
    }

    /**
     * Match a single character token
     */
    private void matchSingleCharToken(char c, Token.TokenType tokenType) {
        if (token == null && buffer.peek() == c) {
            token = new Token(tokenType);
            token.setTokenString(String.valueOf(c));
            token.setMatched(String.valueOf(c));
            advanceCharsInBuffer(1);
        }
    }

    /**
     * Match a multi-character token
     */
    private void matchMultiCharToken(Pattern matcher, Token.TokenType tokenType, MatchedFn fn) {
        if (this.token == null) {
            String matched = buffer.matchRegex(matcher);
            if (matched != null) {
                this.token = new Token(tokenType);
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
        if (StringUtils.isEmpty(currentLine)) {
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

    /**
     * Pushes back the current token onto the front of the input buffer
     */
    public void pushBackToken() {
        if (token.type != Token.TokenType.EOF) {
            buffer.position(-token.getMatched().length());
            token = prevToken;
        }
    }

    /**
     * Skips all characters until the provided character is reached, returning the skipped string
     */
    public String skipToChars(String ch) {
        StringBuilder result = new StringBuilder();
        while (!buffer.empty() && ch.indexOf(buffer.peek()) == -1) {
            result.append(buffer.get());
        }

        if (buffer.empty()) {
            return null;
        } else {
            return result.toString();
        }
    }

    public SourceBuffer getBuffer() {
        return buffer;
    }

    /*

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
