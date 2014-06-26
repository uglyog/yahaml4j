package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Token {

    private String tokenString;

    public String getTokenString() {
        return tokenString;
    }

    public void setTokenString(String tokenString) {
        this.tokenString = tokenString;
    }

    public enum TokenType {
        EOF, WS, CONTINUELINE, UNKNOWN, ELEMENT, MINUS, OPENBRACE, CLOSEBRACE, COMMA, CODE_ID, COLON, IDSELECTOR,
        CLASSSELECTOR, SLASH, OPENBRACKET, CLOSEBRACKET, HTMLIDENTIFIER, EQUAL, STRING, EXCLAMATION, AMP, LT, GT,
        TILDE, COMMENT, EOL
    }

    public final TokenType type;
    private String matched;

    public Token(TokenType type, String matched) {
        this.type = type;
        this.matched = matched;
    }

    public Token(TokenType type) {
        this.type = type;
        this.matched = null;
    }

    public String getMatched() {
        return matched;
    }

    public void setMatched(String matched) {
        this.matched = matched;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
            append("tokenString", tokenString).
            append("type", type).
            append("matched", matched).
            toString();
    }
}
