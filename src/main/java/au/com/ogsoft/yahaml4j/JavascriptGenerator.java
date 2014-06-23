package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONStringer;
import org.json.JSONWriter;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Code generator that generates a Javascript function body
 */
public class JavascriptGenerator extends BaseCodeGenerator {

    public static final Pattern CODE_BLOCK_END = Pattern.compile("[ \\t]*\\}");

    private String scriptName;

    public JavascriptGenerator(String scriptName, HamlOptions options) {
        super(options);
        this.scriptName = scriptName;
    }

    /**
     * Initialise the output buffer with any variables or code
     */
    public void initOutput() {
        getOutputBuffer().appendToOutputBuffer("function " + safeName(scriptName) + " (context) {\n");
        if (options.tolerateFaults) {
            getOutputBuffer().appendToOutputBuffer("  var handleError = haml.HamlRuntime._logError;");
        } else {
            getOutputBuffer().appendToOutputBuffer("  var handleError = haml.HamlRuntime._raiseError;");
        }

        getOutputBuffer().appendToOutputBuffer(
            "  var html = [];" +
                "  var hashFunction = null, hashObject = null, objRef = null, objRefFn = null;" +
                "  with (context || {}) {\n"
        );
    }

    private String safeName(String scriptName) {
        return StringUtils.replaceChars(scriptName, ' ', '_');
    }

    /**
     * Generate the code required to support a buffer flush
     * @param buffer buffer to flush
     * @return code fragment
     */
    @Override
    public String generateFlush(String buffer) {
        return "    html.push(\"" + escapeCode(buffer) + "\");\n";
    }

    /**
     * Flush and close the output buffer and return the contents
     */
    @Override
    public String closeAndReturnOutput() {
        getOutputBuffer().flush();
        return getOutputBuffer().output() + "  };\n  return html.join(\"\");\n}\n";
    }

    @Override
    public void mark() {

    }

    /**
     * Generate the code to close off a code block
     */
    @Override
    public void closeOffCodeBlock(Tokeniser tokeniser) {
        if (tokeniser.getToken().type != Token.TokenType.MINUS || tokeniser.matchToken(CODE_BLOCK_END) == null) {
            outputBuffer.flush();
            outputBuffer.appendToOutputBuffer(HamlRuntime.indentText(getIndent()) + "}\n");
        }
    }

    /**
     * Generate the code to close off a function parameter
     */
    @Override
    public void closeOffFunctionBlock(Tokeniser tokeniser) {
        if (tokeniser.getToken().type != Token.TokenType.MINUS || tokeniser.matchToken(CODE_BLOCK_END) == null) {
            outputBuffer.flush();
            outputBuffer.appendToOutputBuffer(HamlRuntime.indentText(getIndent()) + "});\n");
        }
    }

    /**
     * Generate the code for dynamic attributes ({} form)
     */
    @Override
    public void generateCodeForDynamicAttributes(String id, List<String> classes, Map<String, String> attributeList,
                                                 Map<String, String> attributeHash, String objectRef, ParsePoint currentParsePoint) {
        outputBuffer.flush();
        if (!attributeHash.isEmpty()) {
            String hashStr = "";
            for (Map.Entry<String, String> entry : attributeHash.entrySet()) {
                String key = entry.getKey();
                if (!hashStr.isEmpty()) {
                    hashStr += ", ";
                }
                hashStr += "\\\"" + key + "\\\": " + entry.getValue().replaceAll("[\"]", "\\\\\"").replaceAll("\n", "\\n");
            }
            outputBuffer.appendToOutputBuffer("    hashFunction = function () { return eval(\"hashObject = {" +
                hashStr + " }\"); }\n");
        } else {
            outputBuffer.appendToOutputBuffer("    hashFunction = null;\n");
        }
//        if objectRef.length > 0
//          @outputBuffer.appendToOutputBuffer('    objRefFn = function () { return eval("objRef = ' +
//            objectRef.replace(/"/g, '\\"') + '"); };\n')
//        else
//          @outputBuffer.appendToOutputBuffer('    objRefFn = null;\n');

        JSONWriter json = new JSONStringer().object();
        for(Map.Entry<String, String> attr: attributeList.entrySet()) {
            json.key(attr.getKey()).value(attr.getValue());
        }
        String attrbuteListJson = json.endObject().toString();

        outputBuffer.appendToOutputBuffer("    html.push(haml.HamlRuntime.generateElementAttributes(context, \"" +
          id + "\", [\"" +
          StringUtils.join(classes, "\",\"") + "\"], objRefFn, " +
          attrbuteListJson + ", hashFunction, " +
          currentParsePoint.lineNumber + ", " + currentParsePoint.characterNumber + ", \"" +
          escapeCode(currentParsePoint.currentLine) + "\", handleError));\n");
    }

    /**
     * Append the text contents to the buffer, expanding any embedded code
     */
    @Override
    public void appendTextContents(String text, boolean shouldInterpolate, ParsePoint currentParsePoint, ProcessOptions options) {
        if (shouldInterpolate && text.matches("#\\{[^}]*\\}")) {
            // @interpolateString(text, currentParsePoint, options)
        } else{
            outputBuffer.append(processText(text, options));
        }
    }

    @Override
    public String scanEmbeddedCode(Tokeniser tokeniser) {
        StringBuilder result = new StringBuilder();
        SourceBuffer buffer = tokeniser.getBuffer();
        boolean done = false;
        int bcount = 0;
        while (!buffer.empty() && !done) {
            char ch = buffer.peek();
            if ((ch == ',' || ch == '}') && bcount == 0) {
                done = true;
            } else {
                if (ch == '[') {
                    bcount++;
                } else if (ch == ']') {
                    bcount--;
                    if (bcount < 0) {
                        bcount = 0;
                    }
                }
                result.append(buffer.get());
            }
        }

        if (buffer.empty()) {
            return null;
        } else {
            return result.toString();
        }
    }

    /**
     * process text based on escape and preserve flags
     */
    private String processText(String text, ProcessOptions options) {
        if (options != null && options.escapeHTML) {
            return null; // HamlRuntime.escapeHTML(text);
        } else if (options != null && options.perserveWhitespace) {
            return null; // HamlRuntime.perserveWhitespace(text);
        } else {
            return text;
        }
    }

    /**
     * Escape the line so it is safe to put into a javascript string
     */
    private String escapeCode(String jsString) {
        return StringEscapeUtils.escapeEcmaScript(jsString);
    }

    /*
  ###
    Append a line with embedded javascript code
  ###
  appendEmbeddedCode: (indentText, expression, escapeContents, perserveWhitespace, currentParsePoint) ->
    @outputBuffer.flush()

    @outputBuffer.appendToOutputBuffer(indentText + 'try {\n')
    @outputBuffer.appendToOutputBuffer(indentText + '    var value = eval("' +
      (_.str || _).trim(expression).replace(/"/g, '\\"').replace(/\\n/g, '\\\\n') + '");\n')
    @outputBuffer.appendToOutputBuffer(indentText + '    value = value === null ? "" : value;')
    if escapeContents
      @outputBuffer.appendToOutputBuffer(indentText + '    html.push(haml.HamlRuntime.escapeHTML(String(value)));\n')
    else if perserveWhitespace
      @outputBuffer.appendToOutputBuffer(indentText + '    html.push(haml.HamlRuntime.perserveWhitespace(String(value)));\n')
    else
      @outputBuffer.appendToOutputBuffer(indentText + '    html.push(String(value));\n')

    @outputBuffer.appendToOutputBuffer(indentText + '} catch (e) {\n');
    @outputBuffer.appendToOutputBuffer(indentText + '  handleError(haml.HamlRuntime.templateError(' +
      currentParsePoint.lineNumber + ', ' + currentParsePoint.characterNumber + ', "' +
      @escapeCode(currentParsePoint.currentLine) + '",\n')
    @outputBuffer.appendToOutputBuffer(indentText + '    "Error evaluating expression - " + e));\n')
    @outputBuffer.appendToOutputBuffer(indentText + '}\n')


  ###
    Append a line of code to the output buffer
  ###
  appendCodeLine: (line, eol) ->
    @outputBuffer.flush()
    @outputBuffer.appendToOutputBuffer(HamlRuntime.indentText(@indent))
    @outputBuffer.appendToOutputBuffer(line)
    @outputBuffer.appendToOutputBuffer(eol)

  ###
    Does the current line end with a function declaration?
  ###
  lineMatchesStartFunctionBlock: (line) -> line.match(/function\s*\((,?\s*\w+)*\)\s*\{\s*$/)

  ###
    Does the current line end with a starting code block
  ###
  lineMatchesStartBlock: (line) -> line.match(/\{\s*$/)

  ###
    Clean any reserved words in the given hash
  ###
  replaceReservedWordsInHash: (hash) ->
    resultHash = hash
    for reservedWord in ['class', 'for']
      resultHash = resultHash.replace(reservedWord + ':', '"' + reservedWord + '":')
    resultHash

  ###
    Generate a function from the function body
  ###
  generateJsFunction: (functionBody) ->
    try
      new Function('context', functionBody)
    catch e
      throw "Incorrect embedded code has resulted in an invalid Haml function - #{e}\nGenerated Function:\n#{functionBody}"

  ###
    Interpolate any embedded code in the text
  ###
  interpolateString: (text, currentParsePoint, options) ->
    index = 0
    result = @embeddedCodeBlockMatcher.exec(text)
    while result
      precheedingChar = text.charAt(result.index - 1) if result.index > 0
      precheedingChar2 = text.charAt(result.index - 2) if result.index > 1
      if precheedingChar is '\\' and precheedingChar2 isnt '\\'
        @outputBuffer.append(@processText(text.substring(index, result.index - 1), options)) unless result.index == 0
        @outputBuffer.append(@processText(result[0]), options)
      else
        @outputBuffer.append(@processText(text.substring(index, result.index)), options)
        @appendEmbeddedCode(HamlRuntime.indentText(@indent + 1), result[1], options.escapeHTML, options.perserveWhitespace, currentParsePoint)
      index = @embeddedCodeBlockMatcher.lastIndex
      result = @embeddedCodeBlockMatcher.exec(text)
    @outputBuffer.append(@processText(text.substring(index), options)) if index < text.length

     */

}
