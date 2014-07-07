package au.com.ogsoft.yahaml4j

import org.junit.Before
import org.junit.Test

public class CodeGeneratorTest extends BaseHamlTest {

    Haml haml

    @Before
    public void setup() {
        haml = new Haml()
    }

    @Test(expected = javax.script.ScriptException.class)
    public void "with fault tolerance off will raise an exception on error"() {
        def options = new HamlOptions()
        options.tolerateFaults = false
        def haml = haml.compileHaml("error template", ".test= null.toString()", options)
        runScript(haml)
    }

    @Test
    public void "with fault tolerance on will not raise an exception on error"() {
        def options = new HamlOptions()
        options.tolerateFaults = true
        def haml = haml.compileHaml("error template", ".test= null.toString()", options)
        runScript(haml)
    }

}
