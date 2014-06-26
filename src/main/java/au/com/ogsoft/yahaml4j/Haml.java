package au.com.ogsoft.yahaml4j;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.*;

/**
 * HAML compiler for the JVM
 * Copyright 2011-12, Ronald Holshausen (https://github.com/uglyog)
 * Released under the MIT License (http://www.opensource.org/licenses/MIT)
 *
 * Main haml compiler implemtation
 */
class Haml {

    private static final Logger LOGGER = LoggerFactory.getLogger(Haml.class);
    private static final List<String> SELF_CLOSING_TAGS = Arrays.asList("meta", "img", "link", "script", "br", "hr");

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
        /*
          else if tokeniser.token.doctype
            @_doctype(tokeniser, indent, generator)
          else if tokeniser.token.equal or tokeniser.token.escapeHtml or tokeniser.token.unescapeHtml or
          tokeniser.token.tilde
            @_embeddedJs(tokeniser, indent, generator.elementStack, innerWhitespace: true, generator)
          else if tokeniser.token.minus
            @_jsLine(tokeniser, indent, generator.elementStack, generator)*/
                    } else if (tokeniser.getToken().type == Token.TokenType.COMMENT || tokeniser.getToken().type == Token.TokenType.SLASH) {
                        _commentLine(tokeniser, indent, generator.getElementStack(), generator);
          /*else if tokeniser.token.amp
            @_escapedLine(tokeniser, indent, generator.elementStack, generator)
          else if tokeniser.token.filter
            @_filter(tokeniser, indent, generator, options) */
                    } else {
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

    private void _commentLine(Tokeniser tokeniser, Integer indent, List<Element> elementStack, HamlGenerator generator) {
        if (tokeniser.getToken().type == Token.TokenType.COMMENT) {
            tokeniser.skipToEOLorEOF();
            tokeniser.getNextToken();
            int i = _whitespace(tokeniser);
            while (tokeniser.getToken().type != Token.TokenType.EOF && i > indent) {
                tokeniser.skipToEOLorEOF();
                tokeniser.getNextToken();
                i = _whitespace(tokeniser);
            }
            if (i > 0) {
                tokeniser.pushBackToken();
            }
        } else if (tokeniser.getToken().type == Token.TokenType.SLASH) {
            _closeElements(indent, elementStack, tokeniser, generator);
            generator.getOutputBuffer().append(HamlRuntime.indentText(indent));
            generator.getOutputBuffer().append("<!--");
            tokeniser.getNextToken();
            String contents = tokeniser.skipToEOLorEOF();

            if (StringUtils.isNotEmpty(contents)) {
                generator.getOutputBuffer().append(contents);
            }

            if (StringUtils.isNotEmpty(contents) && contents.startsWith("[") && contents.matches(".*\\]\\s*$")) {
                Element el = new Element();
                el.htmlConditionalComment = true;
                el.eol = _newline(tokeniser);
                elementStack.set(indent, el);
                generator.getOutputBuffer().append(">");
            } else {
                Element el = new Element();
                el.htmlComment = true;
                el.eol = _newline(tokeniser);
                elementStack.set(indent, el);
            }

            if (_tagHasContents(indent, tokeniser)) {
                generator.getOutputBuffer().append("\n");
            }
            tokeniser.getNextToken();
        }
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
    private void _templateLine(Tokeniser tokeniser, List<Element> elementStack, int indent, HamlGenerator generator,
                               HamlOptions options) {

        if (tokeniser.getToken().type != Token.TokenType.EOL) {
            _closeElements(indent, elementStack, tokeniser, generator);
        }

        String identifier = _element(tokeniser);
        String id = _idSelector(tokeniser);
        List<String> classes = _classSelector(tokeniser);
        String objectRef = null; // @_objectReference(tokeniser)
        Map<String, String> attrList = _attributeList(tokeniser, options);

        ParsePoint currentParsePoint = tokeniser.currentParsePoint();
        Map<String, String> attributesHash = _attributeHash(tokeniser, options);

        TagOptions tagOptions = new TagOptions();
        tagOptions.selfClosingTag = false;
        tagOptions.innerWhitespace = true;
        tagOptions.outerWhitespace = true;
        boolean lineHasElement = _lineHasElement(identifier, id, classes);

        if (tokeniser.getToken().type == Token.TokenType.SLASH) {
            tagOptions.selfClosingTag = true;
            tokeniser.getNextToken();
        }
        if (tokeniser.getToken().type == Token.TokenType.GT && lineHasElement) {
            tagOptions.outerWhitespace = false;
            tokeniser.getNextToken();
        }
        if (tokeniser.getToken().type == Token.TokenType.LT && lineHasElement) {
            tagOptions.innerWhitespace = false;
            tokeniser.getNextToken();
        }

        if (lineHasElement) {
            if (!tagOptions.selfClosingTag) {
                tagOptions.selfClosingTag = _isSelfClosingTag(identifier) && !_tagHasContents(indent, tokeniser);
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
          if (tokeniser.getToken().type == Token.TokenType.EXCLAMATION) {
              tokeniser.getNextToken();
              contents = tokeniser.skipToEOLorEOF();
          } else {
              contents = tokeniser.skipToEOLorEOF();
              if (contents.startsWith("\\")) {
                  contents = contents.substring(1);
              }
              shouldInterpolate = true;
          }

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

        if (tagOptions.selfClosingTag && hasContents) {
            _handleError(options, null, tokeniser, new RuntimeException(HamlRuntime.templateError(
                currentParsePoint.lineNumber, currentParsePoint.characterNumber,
                currentParsePoint.currentLine, "A self-closing tag can not have any contents")));
        }
    }

    // ATTRIBUTES -> ( ATTRIBUTE* )
    private Map<String, String> _attributeList(Tokeniser tokeniser, HamlOptions options) {
        Map<String, String> attrList = new HashMap<String, String>();

        if (tokeniser.getToken().type == Token.TokenType.OPENBRACKET) {
            tokeniser.setMode(Tokeniser.Mode.ATTRLIST);
            tokeniser.getNextToken();
            while (tokeniser.getToken().type != Token.TokenType.CLOSEBRACKET) {
                Map.Entry<String, String> attr = _attribute(tokeniser, options);
                if (attr != null) {
                    attrList.put(attr.getKey(), attr.getValue());
                } else {
                    if (tokeniser.getToken().type == Token.TokenType.WS || tokeniser.getToken().type == Token.TokenType.EOL) {
                        tokeniser.getNextToken();
                    } else if (tokeniser.getToken().type != Token.TokenType.CLOSEBRACKET && tokeniser.getToken().type != Token.TokenType.HTMLIDENTIFIER) {
                        tokeniser.clearMode();
                        _handleError(options, null, tokeniser, new RuntimeException(
                                tokeniser.parseError("Expecting either an attribute name to continue the attributes or a closing " +
                                        "bracket to end")));
                        return attrList;
                    }
                }
            }
            tokeniser.getNextToken();
        }

        tokeniser.clearMode();
        return attrList;
    }

    // ATTRIBUTE -> IDENTIFIER WS* = WS* STRING
    private Map.Entry<String, String> _attribute(Tokeniser tokeniser, HamlOptions options) {
        Map.Entry<String, String> attr = null;

        if (tokeniser.getToken().type == Token.TokenType.HTMLIDENTIFIER) {
            String name = tokeniser.getToken().getTokenString();
            tokeniser.getNextToken();
            _whitespace(tokeniser);
            if (tokeniser.getToken().type != Token.TokenType.EQUAL) {
                _handleError(options, null, tokeniser,
                        new RuntimeException(tokeniser.parseError("Expected equals \"=\" after attribute name")));
                return null;
            }
            tokeniser.getNextToken();
            _whitespace(tokeniser);
            if (tokeniser.getToken().type != Token.TokenType.HTMLIDENTIFIER && tokeniser.getToken().type != Token.TokenType.STRING) {
                _handleError(options, null, tokeniser,
                        new RuntimeException(tokeniser.parseError("Expected a quoted string or an identifier for the attribute value")));
                return null;
            }
            attr = new DefaultMapEntry<String, String>(name, tokeniser.getToken().getTokenString());
            tokeniser.getNextToken();
        }

        return attr;
    }

    private boolean _tagHasContents(int indent, Tokeniser tokeniser) {
        if (!tokeniser.isEolOrEof()) {
            return true;
        } else {
            Token nextToken = tokeniser.lookAhead(1);
            return nextToken.type == Token.TokenType.WS && nextToken.getTokenString().length() / 2 > indent;
        }
    }

    private boolean _isSelfClosingTag(String identifier) {
        return SELF_CLOSING_TAGS.contains(identifier);
    }

    // CLASSSELECTOR = (.CLASS)+
    private List<String> _classSelector(Tokeniser tokeniser) {
        List<String> classes = new ArrayList<String>();

        while(tokeniser.getToken().type == Token.TokenType.CLASSSELECTOR) {
            classes.add(tokeniser.getToken().getTokenString());
            tokeniser.getNextToken();
        }

        return classes;
    }

    // IDSELECTOR = # ID
    private String _idSelector(Tokeniser tokeniser) {
        String id = "";
        if (tokeniser.getToken().type == Token.TokenType.IDSELECTOR) {
            id = tokeniser.getToken().getTokenString();
            tokeniser.getNextToken();
        }
        return id;
    }

    private Map<String, String> _attributeHash(Tokeniser tokeniser, HamlOptions options) {
        Map<String, String> hash = new HashMap<String, String>();
        // HASH -> "{ WS* HASH_ENTRY ( "," WS* HASH_ENTRY )*  "}"
        if (tokeniser.getToken().type == Token.TokenType.OPENBRACE) {
            tokeniser.setMode(Tokeniser.Mode.ATTRHASH);
            tokeniser.getNextToken();
            _whitespace(tokeniser);
            _hashEntry(hash, tokeniser, null);
            while (tokeniser.getToken().type == Token.TokenType.COMMA) {
                tokeniser.getNextToken();
                _whitespace(tokeniser);
                _hashEntry(hash, tokeniser, options);
            }
            if (tokeniser.getToken().type != Token.TokenType.CLOSEBRACE) {
                _handleError(options, null, tokeniser,
                    new RuntimeException(tokeniser.parseError("Expected a closing brace (}) to end an attribute hash")));
            }
            tokeniser.getNextToken();
        }
        tokeniser.clearMode();
        return hash;
    }

    private void _hashEntry(Map<String, String> hash, Tokeniser tokeniser, HamlOptions options) {
        // HASH_ENTRY -> IDENTIFIER WS* ":" WS* !("," "}")
        if (tokeniser.getToken().type != Token.TokenType.CODE_ID) {
            _handleError(options, null, tokeniser,
                new RuntimeException(tokeniser.parseError("Hash keys must be normal identifiers")));
        } else {
            String id = tokeniser.getToken().getTokenString();
            tokeniser.getNextToken();
            _whitespace(tokeniser);
            if (tokeniser.getToken().type != Token.TokenType.COLON) {
                _handleError(options, null, tokeniser,
                    new RuntimeException(tokeniser.parseError("Expected a colon (:) after a Hash key")));
            } else {
                String value = generator.scanEmbeddedCode(tokeniser);
                if (value == null) {
                    _handleError(options, null, tokeniser,
                        new RuntimeException(tokeniser.parseError("Expected a closing brace (}) to end an attribute hash or a comma (,) to continue onto another entry")));
                } else {
                    hash.put(id, value);
                    tokeniser.getNextToken();
                }
            }
        }
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
                              Map<String, String> attributeHash, List<Element> elementStack, TagOptions tagOptions, HamlGenerator generator) {
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
        if (!attributeHash.isEmpty() || StringUtils.isNotEmpty(objectRef)) {
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
        return indent == 0 || indent >= elementStack.size() || (elementStack.get(indent - 1) == null || elementStack.get(indent - 1).tagOptions == null
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
          when "XML"
            if params.length > 1
              generator.outputBuffer.append("<?xml version="1.0" encoding="#{params[1]}" ?>")
            else
              generator.outputBuffer.append("<?xml version="1.0" encoding="utf-8" ?>")
          when "Strict" then generator.outputBuffer.append("<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">")
          when "Frameset" then generator.outputBuffer.append("<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">")
          when "5" then generator.outputBuffer.append("<!DOCTYPE html>")
          when "1.1" then generator.outputBuffer.append("<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">")
          when "Basic" then generator.outputBuffer.append("<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML Basic 1.1//EN" "http://www.w3.org/TR/xhtml-basic/xhtml-basic11.dtd">")
          when "Mobile" then generator.outputBuffer.append("<!DOCTYPE html PUBLIC "-//WAPFORUM//DTD XHTML Mobile 1.2//EN" "http://www.openmobilealliance.org/tech/DTD/xhtml-mobile12.dtd">")
          when "RDFa" then generator.outputBuffer.append("<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN" "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">")
      else
        generator.outputBuffer.append("<!DOCTYPE html>")
      generator.outputBuffer.append(@_newline(tokeniser))
      tokeniser.getNextToken()

  _filter: (tokeniser, indent, generator, options) ->
    if tokeniser.token.filter
      filter = tokeniser.token.tokenString
      unless haml.filters[filter]
        @_handleError(options, skipTo: indent, tokeniser, tokeniser.parseError("Filter "#{filter}" not registered. Filter functions need to be added to "haml.filters"."))
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

  _escapedLine: (tokeniser, indent, elementStack, generator) ->
    if tokeniser.token.amp
      haml._closeElements(indent, elementStack, tokeniser, generator)
      generator.outputBuffer.append(HamlRuntime.indentText(indent))
      tokeniser.getNextToken()
      contents = tokeniser.skipToEOLorEOF()
      generator.outputBuffer.append(haml.HamlRuntime.escapeHTML(contents)) if (contents && contents.length > 0)
      generator.outputBuffer.append(@_newline(tokeniser))
      tokeniser.getNextToken()

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

  _objectReference: (tokeniser) ->
    attr = ""
    if tokeniser.token.objectReference
      attr = tokeniser.token.tokenString
      tokeniser.getNextToken()
    attr

     */

}
