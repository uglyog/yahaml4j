package au.com.ogsoft.yahaml4j

import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat

public class ErrorHandlingTest extends BaseHamlTest {

    Haml haml

    @Before
    public void setup() {
        haml = new Haml()
    }

    @Test(expected = javax.script.ScriptException.class)
    public void "with fault tolerance off will raise an exception with an js runtime error"() {
        def options = new HamlOptions()
        options.tolerateFaults = false
        def haml = haml.compileHaml("error template", ".value><= null.toString()", options)
        runScript(haml)
    }

    @Test
    public void "with fault tolerance on will not raise an exception with an js runtime error"() {
        def options = new HamlOptions()
        options.tolerateFaults = true
        def haml = haml.compileHaml("error template", ".value><= null.toString()", options)
        def result = runScript(haml)
        assert result == '<div class="value"></div>'
    }

    @Test(expected = RuntimeException.class)
    public void "with fault tolerance off will raise an exception with an error in the attribute hash"() {
        def options = new HamlOptions()
        options.tolerateFaults = false
        def haml = haml.compileHaml("error template", ".value{this is not a hash}><", options)
        runScript(haml)
    }

    @Test
    public void "with fault tolerance on will not raise an exception with an error in the attribute hash"() {
        def options = new HamlOptions()
        options.tolerateFaults = true
        def haml = haml.compileHaml("error template", ".value{this is not a hash}><", options)
        def result = runScript(haml)
        assert result == '<div class="value"></div>'
    }

    @Test(expected = RuntimeException.class)
    public void "with fault tolerance off will raise an exception with an self-closing tag with content"() {
        def options = new HamlOptions()
        options.tolerateFaults = false
        def haml = haml.compileHaml("error template", ".p/ test", options)
        runScript(haml)
    }

    @Test
    public void "with fault tolerance on will not raise an exception with an self-closing tag with content"() {
        def options = new HamlOptions()
        options.tolerateFaults = true
        def haml = haml.compileHaml("error template", ".p/ test", options)
        def result = runScript(haml)
        assert result == '<div class="p"/>\ntest\n'
    }

    @Test(expected = RuntimeException.class)
    public void "with fault tolerance off will raise an exception with no closing attribute list"() {
        def options = new HamlOptions()
        options.tolerateFaults = false
        def haml = haml.compileHaml("error template", ".p(a=\"b\"", options)
        runScript(haml)
    }

    @Test
    public void "with fault tolerance on will not raise an exception with no closing attribute list"() {
        def options = new HamlOptions()
        options.tolerateFaults = true
        def haml = haml.compileHaml("error template", ".p(a=\"b\"", options)
        def result = runScript(haml)
        assertThat result, is("<div a=\"b\" class=\"p\">\n</div>\n")
    }

    @Test(expected = RuntimeException.class)
    public void "with fault tolerance off will raise an exception with an invalid attribute list"() {
        def options = new HamlOptions()
        options.tolerateFaults = false
        def haml = haml.compileHaml("error template", ".p(a=\"b\" =)", options)
        runScript(haml)
    }

    @Test
    public void "with fault tolerance on will not raise an exception with an invalid attribute list"() {
        def options = new HamlOptions()
        options.tolerateFaults = true
        def haml = haml.compileHaml("error template", ".p(a=\"b\" =)", options)
        def result = runScript(haml)
        assertThat result, is('<div a="b" class="p">\n</div>\n')
    }

    @Test(expected = RuntimeException.class)
    public void "with fault tolerance off will raise an exception with a missing closing bracket"() {
        def options = new HamlOptions()
        options.tolerateFaults = false
        def haml = haml.compileHaml("error template", ".p(a=\"b\"\n" +
            "  .o Something not seen\n" +
            ".r(a=\"b\")\n" +
            "  You should see me\n" +
            ".q\n" +
            "  You should see me", options)
        runScript(haml)
    }

    @Test
    public void "with fault tolerance on will not raise an exception with a missing closing bracket"() {
        def options = new HamlOptions()
        options.tolerateFaults = true
        def haml = haml.compileHaml("error template", ".p(a=\"b\"\n" +
            "  .o Something not seen\n" +
            ".r(a=\"b\")\n" +
            "  You should see me\n" +
            ".q\n" +
            "  You should see me", options)
        def result = runScript(haml)
        assertThat result, is('<div a="b" class="p">\n' +
                '</div>\n' +
                '<div a="b" class="r">\n' +
                '  You should see me\n' +
                '</div>\n' +
                '<div class="q">\n' +
                '  You should see me\n' +
                '</div>\n')
    }

    /*

  describe 'with an unknown filter', ->

    beforeEach ->
      @haml = '''
              .p><
                :unknown
                  this is not the filter you where looking for
                test
              '''

    it 'raises an exception in normal mode', ->
      expect(=> haml.compileHaml(source: @haml)()).toThrow()

    it 'does not raise an exception in fault tolerant mode', ->
      expect(=> @result = haml.compileHaml(source: @haml, tolerateFaults: true)()).not.toThrow()
      expect(@result).toBe('<div class="p">test</div>')

     */

}
