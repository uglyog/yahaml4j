package au.com.ogsoft.yahaml4j

import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.is

public class CodeBufferTest {

    CodeBuffer buffer

    @Before
    public void setup() {
        buffer = new CodeBuffer(null)
    }

    @Test
    public void "trims the whitespace from the end of the string"() {
        buffer.append("some text to trim \t\n")
        buffer.trimWhitespace()
        assert buffer.buffer.toString() == "some text to trim"
    }

    @Test
    public void "trims down to the empty string"() {
        buffer.append("     \t\n  ")
        buffer.trimWhitespace()
        assert buffer.buffer.toString() == ""
    }

    @Test
    public void "does not blow away single characters in the buffer"() {
        buffer.append(">")
        buffer.trimWhitespace()
        assertThat buffer.buffer.toString(), is(">")
    }

}
