package au.com.ogsoft.yahaml4j

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.is

class TokeniserTest {

    @Test
    public void "returns the current line from the buffer"() {
        Tokeniser tokeniser = new Tokeniser("Test", "1234567890\n2345678901\n\n\n");
        assertThat tokeniser.getCurrentLine(0), is("1234567890")
    }

    @Test
    public void "returns an empty current line when the buffer is empty"() {
        Tokeniser tokeniser = new Tokeniser("Test", "");
        assert tokeniser.getCurrentLine(0) == ""
    }

    @Test
    public void "returns the remaining contents when there is no final newline"() {
        Tokeniser tokeniser = new Tokeniser("Test", "1234567890");
        assert tokeniser.getCurrentLine(0) == "1234567890"
    }

    @Test
    public void "returns the current line from the buffer from the provided index"() {
        Tokeniser tokeniser = new Tokeniser("Test", "1234567890\n2345678901\n\n\n");
        assert tokeniser.getCurrentLine(10) == ""
        assert tokeniser.getCurrentLine(11) == "2345678901"
    }

    @Test
    public void "get next token returns EOF on empty buffer"() {
        Tokeniser tokeniser = new Tokeniser("Test", "");
        assert tokeniser.nextToken.type == Token.TokenType.EOF
    }

    @Test
    public void "get next token returns EOL on EOL"() {
        Tokeniser tokeniser = new Tokeniser("Test", "\n\r\n");
        assert tokeniser.nextToken.type == Token.TokenType.EOL
        assert tokeniser.nextToken.type == Token.TokenType.EOL
    }
}
