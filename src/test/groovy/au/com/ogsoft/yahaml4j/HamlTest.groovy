package au.com.ogsoft.yahaml4j

import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.fail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.is

public class HamlTest {

    Haml haml

    @Before
    public void setup() {
        haml = new Haml()
    }

    @Test
    public void "empty template should return an empty string"() {
        def haml = haml.compileHaml("empty", "", null)
        def result = runScript(haml)
        assert result == ""
    }

    @Test
    public void "simple template"() {
        def haml = haml.compileHaml("simple", "%h1\n  %div\n    %p\n    %span", null)
        def result = runScript(haml)
        assert result ==
            "<h1>\n" +
            "  <div>\n" +
            "    <p>\n" +
            "    </p>\n" +
            "    <span>\n" +
            "    </span>\n" +
            "  </div>\n" +
            "</h1>\n"
    }

    @Test
    public void "simple template with text"() {
        def haml = haml.compileHaml("simple with text", "%h1\n  %div\n    %p This is \"some\" text\n      This is \"some\" text\n    This is some <div> text\n    \\%span\n    %span %h1 %h1 %h1", null)
        String result = runScript(haml)
        assertThat result, is(
            "<h1>\n" +
            "  <div>\n" +
            "    <p>\n" +
            "      This is \"some\" text\n" +
            "      This is \"some\" text\n" +
            "    </p>\n" +
            "    This is some <div> text\n" +
            "    %span\n" +
            "    <span>\n" +
            "      %h1 %h1 %h1\n" +
            "    </span>\n" +
            "  </div>\n" +
            "</h1>\n")
    }

    @Test
    public void "template with {} attributes"() {
        def haml = haml.compileHaml("attributes",
            "%h1\n" +
            "  %div{id: \"test\"}\n" +
            "    %p{id: 'test2', " +
            "        class: \"blah\", name: null, test: false, checked: false, selected: true} This is some text\n" +
            "      This is some text\n" +
            "    This is some div text\n" +
            "    %label(for = \"a\"){for: [\"b\", \"c\"]}/\n" +
            "    %div{id: ['test', 1], class: [model.name, \"class2\"], for: \"something\"}\n", null)
        String result = runScript(haml, "{ model: { name: 'class1' } }")
        assertThat result, is(
            "<h1>\n" +
            "  <div id=\"test\">\n" +
            "    <p id=\"test2\" selected=\"selected\" class=\"blah\">\n" +
            "      This is some text\n" +
            "      This is some text\n" +
            "    </p>\n" +
            "    This is some div text\n" +
            "    <label for=\"a-b-c\"/>\n" +
            "    <div id=\"test-1\" for=\"something\" class=\"class1 class2\">\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</h1>\n")
    }

    @Test
    public void "invalid template"() {
        try {
            def haml = haml.compileHaml("invalid",
                "%h1\n" +
                    "  %h2\n" +
                    "    %h3{%h3 %h4}\n" +
                    "      %h4\n" +
                    "        %h5", null)
            fail("Should have thrown an exception")
        } catch (RuntimeException e) {
            assertThat e.message, containsString("at line 3 and character 9:\n    %h3{%h3 %h4}\n--------^")
        }
    }

    @Test
    public void "invalid template 2"() {
        try {
            def haml = haml.compileHaml("invalid",
                "%h1\n" +
                    "  %h2\n" +
                    "    %h3{id: \"test\", class: \"test-class\"\n" +
                    "      %h4\n" +
                    "        %h5", null)
            fail("Should have thrown an exception")
        } catch (RuntimeException e) {
            assertThat e.message, containsString("at line 3 and character 19:\n    %h3{id: \"test\", class: \"test-class\"\n------------------^")
        }
    }

    @Test
    public void "invalid template 3"() {
        try {
            def haml = haml.compileHaml("invalid",
                    "%a#back(href=\"#\" class=\"button back)\n" +
                    "%span Back\n" +
                    "%a#continue(href=\"#\" class=\"button continue\")\n" +
                    "%span Save and Continue", null)
            fail("Should have thrown an exception")
        } catch (RuntimeException e) {
            assertThat e.message, containsString("at line 1 and character 24:\n%a#back(href=\"#\" class=\"button back)\n-----------------------^")
        }
    }

    @Test
    public void "template with () attributes"() {
        def haml = haml.compileHaml("attributes",
            "%h1\n" +
            "  %div(id = \"test\")\n" +
            "    %p(id=test2 class=\"blah\"\n selected=\"selected\") This is some text\n" +
            "      This is some text\n" +
            "    This is some div text\n" +
            "    %div(id=test){id: 1, class: [model.name, \"class2\"]}\n" +
            "    %a(href=\"#\" data-key=\"MOD_DESC\")/", null)
        String result = runScript(haml, "{ model: { name: 'class1' } }")
        assertThat result, is(
            "<h1>\n" +
            "  <div id=\"test\">\n" +
            "    <p id=\"test2\" selected=\"selected\" class=\"blah\">\n" +
            "      This is some text\n" +
            "      This is some text\n" +
            "    </p>\n" +
            "    This is some div text\n" +
            "    <div id=\"test-1\" class=\"class1 class2\">\n" +
            "    </div>\n" +
            "    <a data-key=\"MOD_DESC\" href=\"#\"/>\n" +
            "  </div>\n" +
            "</h1>\n")
    }

    @Test
    public void "template with id and class selectors"() {
        def haml = haml.compileHaml("attributes",
            "%h1\n" +
            "  #test.test\n" +
            "    %p#test.blah{id: 2, class: \"test\"} This is some text\n" +
            "      This is some text\n" +
            "    This is some div text\n" +
            "    .class1.class2/\n", null)
        String result = runScript(haml, "{ model: { name: 'class1' } }")
        assertThat result, is(
            "<h1>\n" +
            "  <div id=\"test\" class=\"test\">\n" +
            "    <p id=\"test-2\" class=\"blah test\">\n" +
            "      This is some text\n" +
            "      This is some text\n" +
            "    </p>\n" +
            "    This is some div text\n" +
            "    <div class=\"class1 class2\"/>\n" +
            "  </div>\n" +
            "</h1>\n")
    }

    @Test
    public void "template with self-closing tags"() {
        def haml = haml.compileHaml("self closing tags",
            "%div\n" +
            "  meta, img, link, script, br, and hr\n" +
            "  %meta\n" +
            "  %meta/\n" +
            "  %meta\n" +
            "    meta\n" +
            "  %img\n" +
            "  %img/\n" +
            "  %img\n" +
            "    img\n" +
            "  %link\n" +
            "  %link/\n" +
            "  %link\n" +
            "    link\n" +
            "  %br\n" +
            "  %br/\n" +
            "  %br\n" +
            "    br/\n" +
            "  %hr\n" +
            "  %hr/\n" +
            "  %hr\n" +
            "    hr\n" +
            "  %div/\n" +
            "  %p/\n", null)
        String result = runScript(haml)
        assertThat result, is(
            "<div>\n" +
            "  meta, img, link, script, br, and hr\n" +
            "  <meta/>\n" +
            "  <meta/>\n" +
            "  <meta>\n" +
            "    meta\n" +
            "  </meta>\n" +
            "  <img/>\n" +
            "  <img/>\n" +
            "  <img>\n" +
            "    img\n" +
            "  </img>\n" +
            "  <link/>\n" +
            "  <link/>\n" +
            "  <link>\n" +
            "    link\n" +
            "  </link>\n" +
            "  <br/>\n" +
            "  <br/>\n" +
            "  <br>\n" +
            "    br/\n" +
            "  </br>\n" +
            "  <hr/>\n" +
            "  <hr/>\n" +
            "  <hr>\n" +
            "    hr\n" +
            "  </hr>\n" +
            "  <div/>\n" +
            "  <p/>\n" +
            "</div>\n")
    }

    @Test
    public void "template with unescaped HTML"() {
        def haml = haml.compileHaml("unescaped",
            "%h1 !<div>\n" +
            "  !#test.test\n" +
            "    !%p#test.blah{id: 2, class: \"test\"} This is some text\n" +
            "      !This is some text\n" +
            "!    This is some <div> text\n" +
            "!    <div class=\"class1 class2\"></div>\n", null)
        String result = runScript(haml)
        assertThat result, is(
            "<h1>\n" +
            "  <div>\n" +
            "  #test.test\n" +
            "    %p#test.blah{id: 2, class: \"test\"} This is some text\n" +
            "      This is some text\n" +
            "</h1>\n" +
            "    This is some <div> text\n" +
            "    <div class=\"class1 class2\"></div>\n"
        )
    }

    @Test
    public void "template with comments"() {
        def haml = haml.compileHaml("comments",
            ".main\n" +
            "  / This is a comment\n" +
            "  /\n" +
            "    %span\n" +
            "      = errorTitle\n" +
            "  -# .clear\n" +
            "      %span= errorHeading\n" +
            "  -#  = var label = \"Calculation: \"; return label + (1 + 2 * 3)\n" +
            "  -#  = [\"hi\", \"there\", \"reader!\"]\n" +
            "  -#  = evilScript \n" +
            "  /[if IE]  \n" +
            "    %a(href = \"http://www.mozilla.com/en-US/firefox/\" )\n" +
            "      %h1 Get Firefox\n", null)
        String result = runScript(haml, "{errorTitle: \"An error's a terrible thing\"}")
        assertThat result, is(
            "<div class=\"main\">\n" +
            "  <!-- This is a comment  -->\n" +
            "  <!--\n" +
            "    <span>\n" +
            "      = errorTitle\n" +
            "    </span>\n" +
            "  -->\n" +
            "  <!--[if IE]  >\n" +
            "    <a href=\"http://www.mozilla.com/en-US/firefox/\">\n" +
            "      <h1>\n" +
            "        Get Firefox\n" +
            "      </h1>\n" +
            "    </a>\n" +
            "  <![endif]-->\n" +
            "</div>\n"
        )
    }

    @Test
    public void "Escaping HTML"() {
        def haml = haml.compileHaml("Escaping HTML",
            ".main\n" +
            "  <div>\n" +
            "    &  <p>\n" +
            "    &  </p>\n" +
            "    &  <span>\n" +
            "    &    <script>alert(\"I'm evil!\");\n" +
            "    &  </span>\n" +
            "  </div>\n", null)
        String result = runScript(haml)
        assertThat result, is(
            "<div class=\"main\">\n" +
            "  <div>\n" +
            "      &lt;p&gt;\n" +
            "      &lt;/p&gt;\n" +
            "      &lt;span&gt;\n" +
            "        &lt;script&gt;alert(&quot;I&#39;m evil!&quot;);\n" +
            "      &lt;/span&gt;\n" +
            "  </div>\n" +
            "</div>\n"
        )
    }

    private Object runScript(String haml, String context = "{}") {
        ScriptEngineManager factory = new ScriptEngineManager()
        ScriptEngine engine = factory.getEngineByName("JavaScript")
        engine.eval(IOUtils.toString(getClass().getResourceAsStream("/underscore.js")))
        engine.eval(IOUtils.toString(getClass().getResourceAsStream("/underscore.string.js")))
        engine.eval(IOUtils.toString(getClass().getResourceAsStream("/haml-runtime.js")))
        def result = engine.eval("var fn = " + haml + "; fn(" + context + ");")
        result
    }

    /*

  describe 'template with Javascript evaluation', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="evaluation">\n' +
        '.box.error\n' +
        '  %span\n' +
        '    = errorTitle\n' +
        '  .clear\n' +
        '    - var label = "Calculation: ";\n' +
        '    %span= errorHeading\n' +
        '    = label + (1 + 2 * 3)\n' +
        '    = ["hi", "there", "reader!"]\n' +
        '    = evilScript \n' +
        '    %span&= errorHeading\n' +
        '    &= label + (1 + 2 * 3)\n' +
        '    &= ["hi", "there", "reader!"]\n' +
        '    &= evilScript \n' +
        '    %span!= errorHeading\n' +
        '    != label + (1 + 2 * 3)\n' +
        '    != ["hi", "there", "reader!"]\n' +
        '    != evilScript \n' +
        '</script>')

    for generator in ['javascript', 'productionjavascript']
      do (generator) ->
        it 'should render the correct html for ' + generator, () ->
          html = haml.compileHaml(sourceId: 'evaluation', generator: generator)({
              errorTitle: "Error Title",
              errorHeading: "Error Heading <div>div text</div>",
              evilScript: '<script>alert("I\'m evil!");</script>'
            })
          expect(html).toEqual(
            '\n' +
            '<div class="box error">\n' +
            '  <span>\n' +
            '    Error Title\n' +
            '  </span>\n' +
            '  <div class="clear">\n' +
            '    <span>\n' +
            '      Error Heading &lt;div&gt;div text&lt;/div&gt;\n' +
            '    </span>\n' +
            '    Calculation: 7\n' +
            '    hi,there,reader!\n' +
            '    &lt;script&gt;alert(&quot;I&#39;m evil!&quot;);&lt;/script&gt;\n' +
            '    <span>\n' +
            '      Error Heading &lt;div&gt;div text&lt;/div&gt;\n' +
            '    </span>\n' +
            '    Calculation: 7\n' +
            '    hi,there,reader!\n' +
            '    &lt;script&gt;alert(&quot;I&#39;m evil!&quot;);&lt;/script&gt;\n' +
            '    <span>\n' +
            '      Error Heading <div>div text</div>\n' +
            '    </span>\n' +
            '    Calculation: 7\n' +
            '    hi,there,reader!\n' +
            '    <script>alert("I\'m evil!");</script>\n' +
            '  </div>\n' +
            '</div>\n')

  describe 'template with Javascript code lines', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="evaluation">\n' +
        '.main\n' +
        '  - var foo = "hello";\n' +
        '  - foo += " world";\n' +
        '  %span\n' +
        '    = foo\n' +
        '</script>\n' +
        '<script type="text/template" id="evaluation-with-loops">\n' +
        '.main\n' +
        '  - _(["Option 1", "Option 2", "Option 3"]).each(function (option) {\n' +
        '    %span= option\n' +
        '  - });\n' +
        '  - for (var i = 0; i < 5; i++) {\n' +
        '    %p= i\n' +
        '  - }\n' +
        '</script>' +
        '<script type="text/template" id="evaluation-using-context">\n' +
        '.main\n' +
        '  - var foo = model.foo;\n' +
        '  - foo += " world";\n' +
        '  %span\n' +
        '    = foo\n' +
        '</script>' +
      '<script type="text/template" id="attribute-hash-evaluation-using-outer-scope">\n' +
        '.main\n' +
        '  - var foo = "hello world";\n' +
        '  %span{someattribute: foo}\n' +
        '</script>')

    for generator in ['javascript', 'productionjavascript']
      do (generator) ->
        it 'should render the correct html using locally defined variables for ' + generator, () ->
          html = haml.compileHaml(sourceId: 'evaluation', generator: generator)()
          expect(html).toEqual(
            '\n<div class="main">\n' +
            '  <span>\n' +
            '    hello world\n' +
            '  </span>\n' +
            '</div>\n')

        it 'should render the correct html when the template has loops for ' + generator, () ->
          html = haml.compileHaml('evaluation-with-loops', generator: generator)()
          expect(html).toEqual(
            '\n<div class="main">\n' +
            '    <span>\n' +
            '      Option 1\n' +
            '    </span>\n' +
            '    <span>\n' +
            '      Option 2\n' +
            '    </span>\n' +
            '    <span>\n' +
            '      Option 3\n' +
            '    </span>\n' +
            '    <p>\n' +
            '      0\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      1\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      2\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      3\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      4\n' +
            '    </p>\n' +
            '</div>\n')

        it 'should provide access to the context within inline javascript for ' + generator, () ->
          model = { foo: "hello" }
          html = haml.compileHaml(sourceId: 'evaluation-using-context', generator: generator).call(null, {model: model})
          expect(html).toEqual(
            '\n<div class="main">\n' +
            '  <span>\n' +
            '    hello world\n' +
            '  </span>\n' +
            '</div>\n')

        it 'should be able to access variables declared as part of the haml for ' + generator, () ->
          model = { foo: "hello" }
          html = haml.compileHaml(sourceId: 'attribute-hash-evaluation-using-outer-scope', generator: generator).call(null, {model: model})
          expect(html).toEqual(
            '\n<div class="main">\n' +
            '  <span someattribute="hello world">\n' +
            '  </span>\n' +
            '</div>\n')

  describe 'template with Javascript code lines and no closing blocks', () ->

    beforeEach () ->
      setFixtures(
        '<script type="text/template" id="evaluation-with-loops">\n' +
        '.main\n' +
        '  - _(["Option 1", "Option 2", "Option 3"]).each(function (option) {\n' +
        '    %span= option\n' +
        '  - for (var i = 0; i < 5; i++) {\n' +
        '    %p= i\n' +
        '</script>')

    for generator in ['javascript', 'productionjavascript']
      do (generator) ->
        it 'should render the correct html when the template has loops for ' + generator, () ->
          html = haml.compileHaml(sourceId: 'evaluation-with-loops', generator: generator)()
          expect(html).toEqual(
            '\n<div class="main">\n' +
            '    <span>\n' +
            '      Option 1\n' +
            '    </span>\n' +
            '    <span>\n' +
            '      Option 2\n' +
            '    </span>\n' +
            '    <span>\n' +
            '      Option 3\n' +
            '    </span>\n' +
            '    <p>\n' +
            '      0\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      1\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      2\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      3\n' +
            '    </p>\n' +
            '    <p>\n' +
            '      4\n' +
            '    </p>\n' +
            '</div>\n')

  describe 'Whitespace Removal: > and <', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="whitespace-removal">\n' +
        '%blockquote<\n' +
        '  %div\n' +
        '    Foo!\n' +
        '%img\n' +
        '%img>\n' +
        '%img\n' +
        '%p<= "Foo\\nBar"\n' +
        '%img\n' +
        '%pre><\n' +
        '  foo\n' +
        '  bar\n' +
        '%img\n' +
        '</script>')

    it 'should render the correct html', () ->
      html = haml.compileHaml('whitespace-removal')()
      expect(html).toEqual(
        '\n<blockquote><div>\n' +
        '    Foo!\n' +
        '  </div></blockquote>\n' +
        '<img/><img/><img/>\n' +
        '<p>Foo\n' +
        'Bar</p>\n' +
        '<img/><pre>foo\n' +
        'bar</pre><img/>\n')

  describe 'template with object reference', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="object-reference">\n' +
        '%h1\n' +
        '  %div[test]\n' +
        '    %p[test2] This is some text\n' +
        '      This is some text\n' +
        '    This is some div text\n' +
        '    .class1[test3]{id: 1, class: "class3", for: "something"}\n' +
        '</script>')

    it 'should render the correct html', () ->
      html = haml.compileHaml('object-reference')({
        test: {
          id: 'test'
        },
        test2: {
          id: 'test2',
          'class': 'blah'
        },
        test3: {
          attributes: {
            id: 'test',
            'class': 'class2'
          },
          get: (name) -> @attributes[name]
        }
      })
      expect(html).toEqual(
        '\n<h1>\n' +
        '  <div id="test">\n' +
        '    <p id="test2" class="blah">\n' +
        '      This is some text\n' +
        '      This is some text\n' +
        '    </p>\n' +
        '    This is some div text\n' +
        '    <div class="class1 class2 class3" id="test-1" for="something">\n' +
        '    </div>\n' +
        '  </div>\n' +
        '</h1>\n')

  describe 'coffescript template with object reference', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="object-reference">\n' +
        '%h1\n' +
        '  %div[@test]\n' +
        '    %p[@test2] This is some text\n' +
        '      This is some text\n' +
        '    This is some div text\n' +
        '    .class1[@test3]{id: 1, class: "class3", for: "something"}\n' +
        '</script>')

    it 'should render the correct html', () ->
      html = haml.compileCoffeeHaml('object-reference').call({
        test: {
          id: 'test'
        },
        test2: {
          id: 'test2',
          'class': 'blah'
        },
        test3: {
          attributes: {
            id: 'test',
            'class': 'class2'
          },
          get: (name) -> @attributes[name]
        }
      })
      expect(html).toEqual(
        '\n<h1>\n' +
        '  <div id="test">\n' +
        '    <p id="test2" class="blah">\n' +
        '      This is some text\n' +
        '      This is some text\n' +
        '    </p>\n' +
        '    This is some div text\n' +
        '    <div class="class1 class2 class3" id="test-1" for="something">\n' +
        '    </div>\n' +
        '  </div>\n' +
        '</h1>\n')

  describe 'html 5 data attributes', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="html5-attributes">\n' +
        '%h1\n' +
        '  %div{id: "test"}\n' +
        '    %p{id: \'test2\', data: {\n' +
        '        class: "blah", name: null, test: false, checked: false, selected: true}} This is some text\n' +
        '</script>')

    it 'should render the correct html', () ->
      html = haml.compileHaml('html5-attributes')()
      expect(html).toEqual(
        '\n<h1>\n' +
        '  <div id="test">\n' +
        '    <p id="test2" data-class="blah" data-selected="true">\n' +
        '      This is some text\n' +
        '    </p>\n' +
        '  </div>\n' +
        '</h1>\n')

  describe 'without template', () ->
    it 'should render the correct html', () ->
      expect(haml.compileStringToJs("%div")()).toEqual('<div>\n</div>\n')

  describe 'whitespace preservation', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="whitespace-preservation">\n' +
        '%h1\n' +
        '  %div\n' +
        '    ~ "Foo\\n<pre>Bar\\nBaz</pre>\\n<a>Test\\nTest\\n</a>\\nOther"\n' +
        '</script>')

    it 'should render the correct html', () ->
      html = haml.compileHaml('whitespace-preservation')()
      expect(html).toEqual(
        '\n<h1>\n' +
        '  <div>\n' +
        '    Foo\n' +
        '<pre>Bar&#x000A;Baz</pre>\n' +
        '<a>Test&#x000A;Test&#x000A;</a>\n' +
        'Other\n' +
        '  </div>\n' +
        '</h1>\n')

  describe 'doctype', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="doctype">\n' +
        '!!! XML\n' +
        '!!! XML iso-8859-1\n' +
        '!!!\n' +
        '!!! 1.1\n' +
        '%html\n' +
        '</script>')

    it 'should render the correct html', () ->
      html = haml.compileHaml('doctype')()
      expect(html).toEqual(
        '\n<?xml version=\'1.0\' encoding=\'utf-8\' ?>\n' +
        '<?xml version=\'1.0\' encoding=\'iso-8859-1\' ?>\n' +
        '<!DOCTYPE html>\n' +
        '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">\n' +
        '<html>\n</html>\n')

  describe 'Multiline code blocks', () ->

    beforeEach () ->
      setFixtures('<script type="text/template" id="multiline">\n' +
        '%whoo\n' +
        '  %hoo=                           |\n' +
        '    "I think this might get " +   |\n' +
        '    "pretty long so I should " +  |\n' +
        '    "probably make it " +         |\n' +
        '    "multiline so it doesn\'t " + |\n' +
        '    "look awful."                 |\n' +
        '  %p This is short.\n' +
        '</script>')

    for generator in ['javascript', 'productionjavascript']
      it 'should render the correct html for ' + generator, () ->
        html = haml.compileHaml('multiline', generator: generator)()
        expect(html).toEqual(
          '\n<whoo>\n' +
          '  <hoo>\n' +
          '    I think this might get pretty long so I should probably make it multiline so it doesn&#39;t look awful.\n' +
          '  </hoo>\n' +
          '  <p>\n' +
          '    This is short.\n' +
          '  </p>\n' +
          '</whoo>\n')

    it 'with coffescript should render the correct html', () ->
      html = haml.compileCoffeeHaml('multiline')()
      expect(html).toEqual(
        '\n<whoo>\n' +
        '  <hoo>\n' +
        '    I think this might get pretty long so I should probably make it multiline so it doesn&#39;t look awful.\n' +
        '  </hoo>\n' +
        '  <p>\n' +
        '    This is short.\n' +
        '  </p>\n' +
        '</whoo>\n')


     */

}
