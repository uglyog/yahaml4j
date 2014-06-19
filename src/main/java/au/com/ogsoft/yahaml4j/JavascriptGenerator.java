package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Map;

/**
 * Code generator that generates a Javascript function body
 */
public class JavascriptGenerator extends BaseCodeGenerator {

    private Buffer outputBuffer;

    public JavascriptGenerator(Map<String, Object> options) {
        super(options);
        outputBuffer = new Buffer(this);
    }

    /**
     * Initialise the output buffer with any variables or code
     */
    public void initOutput() {
        if (optionEnabled("tolerateFaults")) {
            outputBuffer.appendToOutputBuffer("  var handleError = haml.HamlRuntime._logError;");
        } else {
            outputBuffer.appendToOutputBuffer("  var handleError = haml.HamlRuntime._raiseError;");
        }

        outputBuffer.appendToOutputBuffer(
            "  var html = [];" +
            "  var hashFunction = null, hashObject = null, objRef = null, objRefFn = null;" +
            "  with (context || {}) {"
        );
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
        outputBuffer.flush();
        return outputBuffer.output() + "  }\n  return html.join(\"\");\n";
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
    Generate the code to close off a code block
  ###
  closeOffCodeBlock: (tokeniser) ->
    unless tokeniser.token.minus and tokeniser.matchToken(/\s*\}/g)
      @outputBuffer.flush()
      @outputBuffer.appendToOutputBuffer(HamlRuntime.indentText(@indent) + '}\n')

  ###
    Generate the code to close off a function parameter
  ###
  closeOffFunctionBlock: (tokeniser) ->
    unless tokeniser.token.minus and tokeniser.matchToken(/\s*\}/g)
      @outputBuffer.flush()
      @outputBuffer.appendToOutputBuffer(HamlRuntime.indentText(@indent) + '});\n')

  ###
    Generate the code for dynamic attributes ({} form)
  ###
  generateCodeForDynamicAttributes: (id, classes, attributeList, attributeHash, objectRef, currentParsePoint) ->
    @outputBuffer.flush()
    if attributeHash.length > 0
      attributeHash = @replaceReservedWordsInHash(attributeHash)
      @outputBuffer.appendToOutputBuffer('    hashFunction = function () { return eval("hashObject = ' +
        attributeHash.replace(/"/g, '\\"').replace(/\n/g, '\\n') + '"); };\n')
    else
      @outputBuffer.appendToOutputBuffer('    hashFunction = null;\n')
    if objectRef.length > 0
      @outputBuffer.appendToOutputBuffer('    objRefFn = function () { return eval("objRef = ' +
        objectRef.replace(/"/g, '\\"') + '"); };\n')
    else
      @outputBuffer.appendToOutputBuffer('    objRefFn = null;\n');

    @outputBuffer.appendToOutputBuffer('    html.push(haml.HamlRuntime.generateElementAttributes(context, "' +
      id + '", ["' +
      classes.join('","') + '"], objRefFn, ' +
      JSON.stringify(attributeList) + ', hashFunction, ' +
      currentParsePoint.lineNumber + ', ' + currentParsePoint.characterNumber + ', "' +
      @escapeCode(currentParsePoint.currentLine) + '", handleError));\n')

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
    Set the current indent level
  ###
  setIndent: (indent) -> @indent = indent

  ###
    Save the current indent level if required
  ###
  mark: () ->

  ###
    Append the text contents to the buffer, expanding any embedded code
  ###
  appendTextContents: (text, shouldInterpolate, currentParsePoint, options = {}) ->
    if shouldInterpolate and text.match(/#{[^}]*}/)
      @interpolateString(text, currentParsePoint, options)
    else
      @outputBuffer.append(@processText(text, options))

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

  ###
    process text based on escape and preserve flags
  ###
  processText: (text, options) ->
    if options?.escapeHTML
      haml.HamlRuntime.escapeHTML(text)
    else if options?.perserveWhitespace
      haml.HamlRuntime.perserveWhitespace(text)
    else
      text

     */

}
