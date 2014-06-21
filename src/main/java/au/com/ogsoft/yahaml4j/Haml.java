package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HAML compiler for the JVM
 * Copyright 2011-12, Ronald Holshausen (https://github.com/uglyog)
 * Released under the MIT License (http://www.opensource.org/licenses/MIT)
 *
 * Main haml compiler implemtation
 */
class Haml {

    private static final Logger LOGGER = LoggerFactory.getLogger(Haml.class);

    /*
haml.CodeBuffer = CodeBuffer
haml.HamlRuntime = HamlRuntime
haml.filters = filters
     */

    private HamlGenerator generator;
    private Tokeniser tokeniser;

    public Haml() {

    }

    /**
     * Renders the provided HAML template
     * @param name Template name
     * @param haml HAML source in string form
     * @param options Options, can be null
     * @return Rendered template
     */
    public String compileHaml(String name, String haml, HamlOptions options) throws ScriptException, IOException {
        HamlOptions opt = options == null ? new HamlOptions() : options;
        tokeniser = new Tokeniser(name, haml);

        if (generator == null) {
            setGenerator(new JavascriptGenerator(name, opt));
        }

        return compile(tokeniser, generator, opt);
    }

    public HamlGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(HamlGenerator generator) {
        this.generator = generator;
    }

    private String compile(Tokeniser tokeniser, HamlGenerator generator, HamlOptions options) {

        generator.initElementStack();
        generator.initOutput();

        //  HAML -> WS* (
        //            TEMPLATELINE
        //            | DOCTYPE
        //            | IGNOREDLINE
        //            | EMBEDDEDCODE
        //            | CODE
        //            | COMMENTLINE
        //          )* EOF

        tokeniser.getNextToken();
        while (tokeniser.getToken().type != Token.TokenType.EOF) {
            if (tokeniser.getToken().type != Token.TokenType.EOL) {
                Integer indent = null;
                try {
                    indent = _whitespace(tokeniser);
                    generator.setIndent(indent);

                    if (tokeniser.getToken().type == Token.TokenType.EOL) {
                        generator.getOutputBuffer().append(HamlRuntime.indentText(indent) + tokeniser.getToken().getMatched());
                        tokeniser.getNextToken();
                    }
        /*
          else if tokeniser.token.doctype
            @_doctype(tokeniser, indent, generator)
          else if tokeniser.token.exclamation
            @_ignoredLine(tokeniser, indent, generator.elementStack, generator)
          else if tokeniser.token.equal or tokeniser.token.escapeHtml or tokeniser.token.unescapeHtml or
          tokeniser.token.tilde
            @_embeddedJs(tokeniser, indent, generator.elementStack, innerWhitespace: true, generator)
          else if tokeniser.token.minus
            @_jsLine(tokeniser, indent, generator.elementStack, generator)
          else if tokeniser.token.comment or tokeniser.token.slash
            @_commentLine(tokeniser, indent, generator.elementStack, generator)
          else if tokeniser.token.amp
            @_escapedLine(tokeniser, indent, generator.elementStack, generator)
          else if tokeniser.token.filter
            @_filter(tokeniser, indent, generator, options) */
                    else {
                        _templateLine(tokeniser, generator.getElementStack(), indent, generator, options);
                    }
                } catch (Exception e) {
                    ErrorOptions errorOptions = new ErrorOptions();
                    errorOptions.skipTo = indent;
                    _handleError(options, errorOptions, tokeniser, e);
                }

            } else {
                generator.getOutputBuffer().append(tokeniser.getToken().getMatched());
                tokeniser.getNextToken();
            }
        }

        _closeElements(0, generator.getElementStack(), tokeniser, generator);

        return generator.closeAndReturnOutput();
    }

    private void _handleError(HamlOptions options, ErrorOptions errorOptions, Tokeniser tokeniser, Exception error) {
        if (options != null && options.tolerateFaults) {
            LOGGER.error(error.getLocalizedMessage(), error);
            if (errorOptions != null && errorOptions.skipTo != null) {
                _skipToNextLineWithIndent(tokeniser, errorOptions.skipTo);
            }
        } else {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new RuntimeException(error);
            }
        }
    }

    private void _skipToNextLineWithIndent(Tokeniser tokeniser, int indent) {
        tokeniser.skipToEOLorEOF();
        tokeniser.getNextToken();
        int lineIndent = _whitespace(tokeniser);
        while (lineIndent > indent) {
            tokeniser.skipToEOLorEOF();
            tokeniser.getNextToken();
            lineIndent = _whitespace(tokeniser);
        }
        tokeniser.pushBackToken();
    }

    /**
     * TEMPLATELINE -> ([ELEMENT][IDSELECTOR][CLASSSELECTORS][ATTRIBUTES] [SLASH|CONTENTS])|(!CONTENTS) (EOL|EOF)
     */
    private void _templateLine(Tokeniser tokeniser, List<Element> elementStack, int indent, HamlGenerator generator, HamlOptions options) {

        if (tokeniser.getToken().type != Token.TokenType.EOL) {
            _closeElements(indent, elementStack, tokeniser, generator);
        }

        String identifier = _element(tokeniser);
        String id = null; // @_idSelector(tokeniser)
        List<String> classes = Collections.emptyList(); // @_classSelector(tokeniser)
        String objectRef = null; // @_objectReference(tokeniser)
        Map<String, String> attrList = Collections.emptyMap(); // @_attributeList(tokeniser, options)

        ParsePoint currentParsePoint = tokeniser.currentParsePoint();
        String attributesHash = null; // @_attributeHash(tokeniser)

        TagOptions tagOptions = new TagOptions();
        tagOptions.selfClosingTag = false;
        tagOptions.innerWhitespace = true;
        tagOptions.outerWhitespace = true;
        boolean lineHasElement = _lineHasElement(identifier, id, classes);

        /*if tokeniser.token.slash
          tagOptions.selfClosingTag = true
          tokeniser.getNextToken()
        if tokeniser.token.gt and lineHasElement
          tagOptions.outerWhitespace = false
          tokeniser.getNextToken()
        if tokeniser.token.lt and lineHasElement
          tagOptions.innerWhitespace = false
          tokeniser.getNextToken()*/

        if (lineHasElement) {
            if (!tagOptions.selfClosingTag) {
                // tagOptions.selfClosingTag = haml._isSelfClosingTag(identifier) and !haml._tagHasContents(indent, tokeniser)
            }
            _openElement(currentParsePoint, indent, identifier, id, classes, objectRef, attrList, attributesHash, elementStack,
                tagOptions, generator);
        }

        boolean hasContents = false;
        if (tokeniser.getToken().type == Token.TokenType.WS) {
            tokeniser.getNextToken();
        }

        /*if tokeniser.token.equal or tokeniser.token.escapeHtml or tokeniser.token.unescapeHtml
          @_embeddedJs(tokeniser, indent + 1, null, tagOptions, generator)
          hasContents = true
        else*/
          String contents = "";
          boolean shouldInterpolate = false;
          /*if tokeniser.token.exclamation
            tokeniser.getNextToken()
            contents = tokeniser.skipToEOLorEOF()
          else*/
            contents = tokeniser.skipToEOLorEOF();
            /*contents = contents.substring(1) if contents.match(/^\\/)
            shouldInterpolate = true*/

          hasContents = StringUtils.isNotEmpty(contents);
          String indentText = "";
          if (hasContents) {
              if (tagOptions.innerWhitespace && lineHasElement || (!lineHasElement && _parentInnerWhitespace(elementStack, indent))) {
                  indentText = HamlRuntime.indentText(identifier.length() > 0 ? indent + 1 : indent);
              } else {
                  contents = StringUtils.trim(contents);
              }
              generator.appendTextContents(indentText + contents, shouldInterpolate, currentParsePoint, null);
              generator.getOutputBuffer().append(_newline(tokeniser));
          }

          _eolOrEof(tokeniser);

        /*if tagOptions.selfClosingTag and hasContents
          @_handleError(options, null, tokeniser, haml.HamlRuntime.templateError(currentParsePoint.lineNumber, currentParsePoint.characterNumber,
                  currentParsePoint.currentLine, "A self-closing tag can not have any contents"))
             */

    }

    private void _eolOrEof(Tokeniser tokeniser) {
        if (tokeniser.getToken().type == Token.TokenType.EOL || tokeniser.getToken().type == Token.TokenType.CONTINUELINE) {
            tokeniser.getNextToken();
        } else if (tokeniser.getToken().type != Token.TokenType.EOF) {
            throw new RuntimeException(tokeniser.parseError("Expected EOL or EOF"));
        }
    }

    private String _newline(Tokeniser tokeniser) {
        if (tokeniser.getToken().type == Token.TokenType.EOL) {
            return tokeniser.getToken().getMatched();
        } else if (tokeniser.getToken().type == Token.TokenType.CONTINUELINE) {
            return tokeniser.getToken().getMatched().substring(1);
        } else {
            return "\n";
        }
    }

    private void _openElement(ParsePoint currentParsePoint, int indent, String identifier, String id,
                              List<String> classes, String objectRef, Map<String, String> attributeList,
                              String attributeHash, List<Element> elementStack, TagOptions tagOptions, HamlGenerator generator) {
        String element = identifier;
        if (StringUtils.isEmpty(element)) {
            element = "div";
        }

        boolean parentInnerWhitespace = _parentInnerWhitespace(elementStack, indent);
        boolean tagOuterWhitespace = tagOptions == null || tagOptions.outerWhitespace;
        if (!tagOuterWhitespace) {
            generator.getOutputBuffer().trimWhitespace();
        }
        if (indent > 0 && parentInnerWhitespace && tagOuterWhitespace) {
            generator.getOutputBuffer().append(HamlRuntime.indentText(indent));
        }
        generator.getOutputBuffer().append("<" + element);
        if (StringUtils.isNotEmpty(attributeHash) || StringUtils.isNotEmpty(objectRef)) {
            generator.generateCodeForDynamicAttributes(id, classes, attributeList, attributeHash, objectRef, currentParsePoint);
        } else {
            generator.getOutputBuffer().append(HamlRuntime.generateElementAttributes(null, id, classes, null, attributeList, null,
                currentParsePoint.lineNumber, currentParsePoint.characterNumber, currentParsePoint.currentLine));
        }
        if (tagOptions.selfClosingTag) {
            generator.getOutputBuffer().append("/>");
            if (tagOptions.outerWhitespace) {
                generator.getOutputBuffer().append("\n");
            }
        } else {
            generator.getOutputBuffer().append(">");
            Element el = new Element();
            el.tag = element;
            el.tagOptions = tagOptions;
            elementStack.set(indent, el);
            if (tagOptions.innerWhitespace) {
                generator.getOutputBuffer().append("\n");
            }
        }
    }

    private boolean _lineHasElement(String identifier, String id, List<String> classes) {
        return StringUtils.isNoneEmpty(identifier) || StringUtils.isNoneEmpty(id) || !classes.isEmpty();
    }

    private String _element(Tokeniser tokeniser) {
        String identifier = "";
        if (tokeniser.getToken().type == Token.TokenType.ELEMENT) {
            identifier = tokeniser.getToken().getTokenString();
            tokeniser.getNextToken();
        }
        return identifier;
    }

    private void _closeElements(int indent, List<Element> elementStack, Tokeniser tokeniser, HamlGenerator generator) {
        int i = elementStack.size() - 1;
        while (i >= indent) {
            _closeElement(i--, elementStack, tokeniser, generator);
        }
    }

    private void _closeElement(int indent, List<Element> elementStack, Tokeniser tokeniser, HamlGenerator generator) {
        if (elementStack.size() > indent && elementStack.get(indent) != null) {
            Element element = elementStack.get(indent);
            generator.setIndent(indent);
            if (element.htmlComment) {
                generator.getOutputBuffer().append(HamlRuntime.indentText(indent) + "-->" + element.eol);
            } else if (element.htmlConditionalComment) {
                generator.getOutputBuffer().append(HamlRuntime.indentText(indent) + "<![endif]-->" + element.eol);
            } else if (element.block) {
                generator.closeOffCodeBlock(tokeniser);
            } else if (element.fnBlock) {
                generator.closeOffFunctionBlock(tokeniser);
            } else {
                boolean innerWhitespace = element.tagOptions == null || element.tagOptions.innerWhitespace;
                if (innerWhitespace) {
                    generator.getOutputBuffer().append(HamlRuntime.indentText(indent));
                } else {
                    generator.getOutputBuffer().trimWhitespace();
                }
                generator.getOutputBuffer().append("</" + element.tag + ">");
                boolean outerWhitespace = element.tagOptions == null || element.tagOptions.outerWhitespace;
                if (_parentInnerWhitespace(elementStack, indent) && outerWhitespace) {
                    generator.getOutputBuffer().append("\n");
                }
            }
            elementStack.set(indent, null);
            generator.mark();
        }
    }

    private boolean _parentInnerWhitespace(List<Element> elementStack, int indent) {
        return indent == 0 || (elementStack.get(indent - 1) == null || elementStack.get(indent - 1).tagOptions == null
            || elementStack.get(indent - 1).tagOptions.innerWhitespace);
    }

    private int _whitespace(Tokeniser tokeniser) {
        int indent = 0;
        if (tokeniser.getToken().type == Token.TokenType.WS) {
            indent = tokeniser.calculateIndent(tokeniser.getToken().getTokenString());
            tokeniser.getNextToken();
        }
        return indent;
    }

    /*

  _doctype: (tokeniser, indent, generator) ->
    if tokeniser.token.doctype
      generator.outputBuffer.append(HamlRuntime.indentText(indent))
      tokeniser.getNextToken()
      tokeniser.getNextToken() if tokeniser.token.ws
      contents = tokeniser.skipToEOLorEOF()
      if contents and contents.length > 0
        params = contents.split(/\s+/)
        switch params[0]
          when 'XML'
            if params.length > 1
              generator.outputBuffer.append("<?xml version='1.0' encoding='#{params[1]}' ?>")
            else
              generator.outputBuffer.append("<?xml version='1.0' encoding='utf-8' ?>")
          when 'Strict' then generator.outputBuffer.append('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">')
          when 'Frameset' then generator.outputBuffer.append('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">')
          when '5' then generator.outputBuffer.append('<!DOCTYPE html>')
          when '1.1' then generator.outputBuffer.append('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">')
          when 'Basic' then generator.outputBuffer.append('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML Basic 1.1//EN" "http://www.w3.org/TR/xhtml-basic/xhtml-basic11.dtd">')
          when 'Mobile' then generator.outputBuffer.append('<!DOCTYPE html PUBLIC "-//WAPFORUM//DTD XHTML Mobile 1.2//EN" "http://www.openmobilealliance.org/tech/DTD/xhtml-mobile12.dtd">')
          when 'RDFa' then generator.outputBuffer.append('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">')
      else
        generator.outputBuffer.append('<!DOCTYPE html>')
      generator.outputBuffer.append(@_newline(tokeniser))
      tokeniser.getNextToken()

  _filter: (tokeniser, indent, generator, options) ->
    if tokeniser.token.filter
      filter = tokeniser.token.tokenString
      unless haml.filters[filter]
        @_handleError(options, skipTo: indent, tokeniser, tokeniser.parseError("Filter '#{filter}' not registered. Filter functions need to be added to 'haml.filters'."))
        return

      tokeniser.skipToEOLorEOF()
      tokeniser.getNextToken()
      i = haml._whitespace(tokeniser)
      filterBlock = []
      while (!tokeniser.token.eof and i > indent)
        tokeniser.pushBackToken()
        line = tokeniser.skipToEOLorEOF()
        filterBlock.push(HamlRuntime.trim(line, 2 * indent))
        tokeniser.getNextToken()
        i = haml._whitespace(tokeniser)
      haml.filters[filter](filterBlock, generator, indent, tokeniser.currentParsePoint())
      tokeniser.pushBackToken()

  _commentLine: (tokeniser, indent, elementStack, generator) ->
    if tokeniser.token.comment
      tokeniser.skipToEOLorEOF()
      tokeniser.getNextToken()
      i = @_whitespace(tokeniser)
      while (!tokeniser.token.eof and i > indent)
        tokeniser.skipToEOLorEOF()
        tokeniser.getNextToken()
        i = @_whitespace(tokeniser)
      tokeniser.pushBackToken() if i > 0
    else if tokeniser.token.slash
      haml._closeElements(indent, elementStack, tokeniser, generator)
      generator.outputBuffer.append(HamlRuntime.indentText(indent))
      generator.outputBuffer.append("<!--")
      tokeniser.getNextToken()
      contents = tokeniser.skipToEOLorEOF()

      generator.outputBuffer.append(contents) if contents and contents.length > 0

      if contents and (_.str || _).startsWith(contents, '[') and contents.match(/\]\s*$/)
        elementStack[indent] = htmlConditionalComment: true, eol: @_newline(tokeniser)
        generator.outputBuffer.append(">")
      else
        elementStack[indent] = htmlComment: true, eol: @_newline(tokeniser)

      if haml._tagHasContents(indent, tokeniser)
        generator.outputBuffer.append("\n")
      tokeniser.getNextToken()

  _escapedLine: (tokeniser, indent, elementStack, generator) ->
    if tokeniser.token.amp
      haml._closeElements(indent, elementStack, tokeniser, generator)
      generator.outputBuffer.append(HamlRuntime.indentText(indent))
      tokeniser.getNextToken()
      contents = tokeniser.skipToEOLorEOF()
      generator.outputBuffer.append(haml.HamlRuntime.escapeHTML(contents)) if (contents && contents.length > 0)
      generator.outputBuffer.append(@_newline(tokeniser))
      tokeniser.getNextToken()

  _ignoredLine: (tokeniser, indent, elementStack, generator) ->
    if tokeniser.token.exclamation
      tokeniser.getNextToken()
      indent += haml._whitespace(tokeniser) if tokeniser.token.ws
      haml._closeElements(indent, elementStack, tokeniser, generator)
      contents = tokeniser.skipToEOLorEOF()
      generator.outputBuffer.append(HamlRuntime.indentText(indent) + contents)

  _embeddedJs: (tokeniser, indent, elementStack, tagOptions, generator) ->
    haml._closeElements(indent, elementStack, tokeniser, generator) if elementStack
    if tokeniser.token.equal or tokeniser.token.escapeHtml or tokeniser.token.unescapeHtml or tokeniser.token.tilde
      escapeHtml = tokeniser.token.escapeHtml or tokeniser.token.equal
      perserveWhitespace = tokeniser.token.tilde
      currentParsePoint = tokeniser.currentParsePoint()
      tokeniser.getNextToken()
      expression = tokeniser.skipToEOLorEOF()
      indentText = HamlRuntime.indentText(indent)
      generator.outputBuffer.append(indentText) if !tagOptions or tagOptions.innerWhitespace
      generator.appendEmbeddedCode(indentText, expression, escapeHtml, perserveWhitespace, currentParsePoint)
      if !tagOptions or tagOptions.innerWhitespace
        generator.outputBuffer.append(@_newline(tokeniser))
        tokeniser.getNextToken() if tokeniser.token.eol

  _jsLine: (tokeniser, indent, elementStack, generator) ->
    if tokeniser.token.minus
      haml._closeElements(indent, elementStack, tokeniser, generator)
      tokeniser.getNextToken()
      line = tokeniser.skipToEOLorEOF()
      generator.setIndent(indent)
      generator.appendCodeLine(line, @_newline(tokeniser))
      tokeniser.getNextToken() if tokeniser.token.eol

      if generator.lineMatchesStartFunctionBlock(line)
        elementStack[indent] = fnBlock: true
      else if generator.lineMatchesStartBlock(line)
        elementStack[indent] = block: true

  _attributeHash: (tokeniser) ->
    attr = ''
    if tokeniser.token.attributeHash
      attr = tokeniser.token.tokenString
      tokeniser.getNextToken()
    attr

  _objectReference: (tokeniser) ->
    attr = ''
    if tokeniser.token.objectReference
      attr = tokeniser.token.tokenString
      tokeniser.getNextToken()
    attr

  # ATTRIBUTES -> ( ATTRIBUTE* )
  _attributeList: (tokeniser, options) ->
    attrList = {}
    if tokeniser.token.openBracket
      tokeniser.getNextToken()
      until tokeniser.token.closeBracket
        attr = haml._attribute(tokeniser)
        if attr
          attrList[attr.name] = attr.value
        else
          if tokeniser.token.ws or tokeniser.token.eol
            tokeniser.getNextToken()
          else if !tokeniser.token.closeBracket and !tokeniser.token.identifier
            @_handleError(options, null, tokeniser, tokeniser.parseError("Expecting either an attribute name to continue the attibutes or a closing " +
              "bracket to end"))
            return attrList
      tokeniser.getNextToken()
    attrList

  # ATTRIBUTE -> IDENTIFIER WS* = WS* STRING
  _attribute: (tokeniser) ->
    attr = null

    if tokeniser.token.identifier
      name = tokeniser.token.tokenString
      tokeniser.getNextToken()
      haml._whitespace(tokeniser)
      throw tokeniser.parseError("Expected '=' after attribute name") unless tokeniser.token.equal
      tokeniser.getNextToken();
      haml._whitespace(tokeniser)
      if !tokeniser.token.string and !tokeniser.token.identifier
        throw tokeniser.parseError("Expected a quoted string or an identifier for the attribute value")
      attr =
        name: name
        value: tokeniser.token.tokenString
      tokeniser.getNextToken()

    attr

  _isSelfClosingTag: (tag) ->
    tag in ['meta', 'img', 'link', 'script', 'br', 'hr']

  _tagHasContents: (indent, tokeniser) ->
    if !tokeniser.isEolOrEof()
      true
    else
      nextToken = tokeniser.lookAhead(1)
      nextToken.ws and nextToken.tokenString.length / 2 > indent

  hasValue: (value) ->
    value? && value isnt false

  attrValue: (attr, value) ->
    if attr in ['selected', 'checked', 'disabled'] then attr else value

  # IDSELECTOR = # ID
  _idSelector: (tokeniser) ->
    id = ''
    if tokeniser.token.idSelector
      id = tokeniser.token.tokenString
      tokeniser.getNextToken()
    id

  # CLASSSELECTOR = (.CLASS)+
  _classSelector: (tokeniser) ->
    classes = []

    while tokeniser.token.classSelector
      classes.push(tokeniser.token.tokenString)
      tokeniser.getNextToken()

    classes

     */

}
