package au.com.ogsoft.yahaml4j

import org.apache.commons.io.IOUtils

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class BaseHamlTest {

    Object runScript(String haml, String context = "{}") {
        ScriptEngineManager factory = new ScriptEngineManager()
        ScriptEngine engine = factory.getEngineByName("JavaScript")
        engine.eval(IOUtils.toString(getClass().getResourceAsStream("/underscore.js")))
        engine.eval(IOUtils.toString(getClass().getResourceAsStream("/underscore.string.js")))
        engine.eval(IOUtils.toString(getClass().getResourceAsStream("/haml-runtime.js")))
        def result = engine.eval("var fn = " + haml + "; fn(" + context + ");")
        result
    }

}
