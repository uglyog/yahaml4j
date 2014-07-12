package au.com.ogsoft.yahaml4j

import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.fail

public class FiltersTest extends BaseHamlTest {

    Haml haml

    @Before
    public void setup() {
        haml = new Haml()
        haml.setupStandardFilters()
    }

    @Test
    public void "renders the result of the filter function"() {
        def haml = haml.compileHaml("filters",
                "%h1\n" +
                "  %p\n" +
                "    :plain\n" +
                "      Does not parse the filtered text. This is useful for large blocks of text without HTML tags,\n" +
                "      when you don't want lines starting with . or - to be parsed.\n" +
                "  %span Other Contents", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<h1>\n" +
            "  <p>\n" +
            "    Does not parse the filtered text. This is useful for large blocks of text without HTML tags,\n" +
            "    when you don't want lines starting with . or - to be parsed.\n" +
            "  </p>\n" +
            "  <span>\n" +
            "    Other Contents\n" +
            "  </span>\n" +
            "</h1>\n")
    }

    @Test(expected = RuntimeException)
    public void "raises an error if the filter is not found"() {
        def haml = haml.compileHaml("filters",
                "%h1\n" +
                "  %p\n" +
                "    %p\n" +
                "       :unknown\n" +
                "           blah di blah di blah\n" +
                "  %span Other Contents", null)
        runScript(haml)
    }

    @Test
    public void "generates javascript filter blocks correctly"() {
        def haml = haml.compileHaml("filters",
                "%body\n" +
                "  :javascript\n" +
                "    // blah di blah di blah\n" +
                "    function () {\n" +
                "       return 'blah';\n" +
                "    }", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<body>\n" +
            "  <script type=\"text/javascript\">\n" +
            "    //<![CDATA[\n" +
            "      // blah di blah di blah\n" +
            "      function () {\n" +
            "         return 'blah';\n" +
            "      }\n" +
            "    //]]>\n" +
            "  </script>\n" +
            "</body>\n")
    }

    @Test
    public void "generates css filter blocks correctly"() {
        def haml = haml.compileHaml("filters",
                "%head\n" +
                "  :css\n" +
                "    /* blah di blah di blah * /\n" +
                "    .body {\n" +
                "      color: red;\n" +
                "    }", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<head>\n" +
            "  <style type=\"text/css\">\n" +
            "    /*<![CDATA[*/\n" +
            "      /* blah di blah di blah * /\n" +
            "      .body {\n" +
            "        color: red;\n" +
            "      }\n" +
            "    /*]]>*/\n" +
            "  </style>\n" +
            "</head>\n")
    }

    @Test
    public void "generates CDATA filter blocks correctly"() {
        def haml = haml.compileHaml("filters",
                "%body\n" +
                "  :cdata\n" +
                "    // blah di blah di blah\n" +
                "    function () {\n" +
                "      return 'blah';\n" +
                "    }", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<body>\n" +
            "  <![CDATA[\n" +
            "    // blah di blah di blah\n" +
            "    function () {\n" +
            "      return 'blah';\n" +
            "    }\n" +
            "  ]]>\n" +
            "</body>\n")
    }

    @Test
    public void "generates preserve filter blocks correctly"() {
        def haml = haml.compileHaml("filters",
                "%p\n" +
                "  :preserve\n" +
                "    Foo\n" +
                "    <pre>Bar\n" +
                "    Baz</pre>\n" +
                "    <a>Test\n" +
                "    Test\n" +
                "    </a>\n" +
                "    Other", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<p>\n" +
            "  Foo&#x000A; <pre>Bar&#x000A; Baz</pre>&#x000A; <a>Test&#x000A; Test&#x000A; </a>&#x000A; Other\n" +
            "</p>\n")
    }

    @Test
    public void "generates escape filter blocks correctly"() {
        def haml = haml.compileHaml("filters",
                "%p\n" +
                "  :escaped\n" +
                "    Foo\n" +
                "    <pre>'Bar'\n" +
                "    Baz</pre>\n" +
                "    <a>Test\n" +
                "    Test\n" +
                "    </a>\n" +
                "    Other&", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<p>\n" +
            "  Foo\n" +
            "  &lt;pre&gt;&#39;Bar&#39;\n" +
            "  Baz&lt;/pre&gt;\n" +
            "  &lt;a&gt;Test\n" +
            "  Test\n" +
            "  &lt;/a&gt;\n" +
            "  Other&amp;\n" +
            "</p>\n")
    }

    @Test
    public void "handles large blocks of text correctly"() {
        def haml = haml.compileHaml("filters",
                "%h1 Why would I use it?\n" +
                ".contents\n" +
                "  %div\n" +
                "    Webmachine Resource\n" +
                "    %pre\n" +
                "      :escaped\n" +
                "         def options\n" +
                "           {\n" +
                "             'Access-Control-Allow-Methods' => 'POST, OPTIONS',\n" +
                "             'Access-Control-Allow-Headers' => 'X-Requested-With, Content-Type'\n" +
                "           }\n" +
                "         end\n" +
                "            \n" +
                "         def finish_request\n" +
                "           response.headers['Access-Control-Allow-Origin'] = '*'\n" +
                "         end\n" +
                "            \n" +
                "         def allowed_methods\n" +
                "           ['GET', 'HEAD', 'POST', 'OPTIONS']\n" +
                "         end\n" +
                "            \n" +
                "         def malformed_request?\n" +
                "           puts \"malformed_request?\"\n" +
                "           body = request.body.to_s\n" +
                "           if body.nil?\n" +
                "             false\n" +
                "           else\n" +
                "             begin\n" +
                "               contract_attributes = JSON.parse(request.body.to_s)['contract']\n" +
                "               @contract = ContractFactory.contract_from contract_attributes\n" +
                "               !@contract.valid?\n" +
                "             rescue => e\n" +
                "               true\n" +
                "             end\n" +
                "         end\n" +
                "      %div", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<h1>\n" +
            "  Why would I use it?\n" +
            "</h1>\n" +
            "<div class=\"contents\">\n" +
            "  <div>\n" +
            "    Webmachine Resource\n" +
            "    <pre>\n" +
            "       def options\n" +
            "         {\n" +
            "           &#39;Access-Control-Allow-Methods&#39; =&gt; &#39;POST, OPTIONS&#39;,\n" +
            "           &#39;Access-Control-Allow-Headers&#39; =&gt; &#39;X-Requested-With, Content-Type&#39;\n" +
            "         }\n" +
            "       end\n" +
            "          \n" +
            "       def finish_request\n" +
            "         response.headers[&#39;Access-Control-Allow-Origin&#39;] = &#39;*&#39;\n" +
            "       end\n" +
            "          \n" +
            "       def allowed_methods\n" +
            "         [&#39;GET&#39;, &#39;HEAD&#39;, &#39;POST&#39;, &#39;OPTIONS&#39;]\n" +
            "       end\n" +
            "          \n" +
            "       def malformed_request?\n" +
            "         puts &quot;malformed_request?&quot;\n" +
            "         body = request.body.to_s\n" +
            "         if body.nil?\n" +
            "           false\n" +
            "         else\n" +
            "           begin\n" +
            "             contract_attributes = JSON.parse(request.body.to_s)[&#39;contract&#39;]\n" +
            "             @contract = ContractFactory.contract_from contract_attributes\n" +
            "             !@contract.valid?\n" +
            "           rescue =&gt; e\n" +
            "             true\n" +
            "           end\n" +
            "       end\n" +
            "      <div>\n" +
            "      </div>\n" +
            "    </pre>\n" +
            "  </div>\n" +
            "</div>\n")
    }

    @Test
    public void "handles large blocks of text with escaped interpolate markers correctly"() {
        def haml = haml.compileHaml("filters",
            "%h1 Why would I use it?\n" +
            ".contents\n" +
            "  %div\n" +
            "    Sinatra webapp\n" +
            "    %pre(class=\"brush: ruby\")\n" +
            "      :plain\n" +
            "        post '/contract_proposals' do\n" +
            "          begin\n" +
            "            contract_attributes = JSON.parse(request.body.read)['contract']\n" +
            "            contract = ContractFactory.contract_from contract_attributes\n" +
            "            \n" +
            "            if contract.valid?\n" +
            "              contract.generate_pdf(File.join(settings.public_folder, PDF_SUBDIR))\n" +
            "              logger.info %{action=contract_created_from_condor, account_manager=\"\\#{contract.account_manager.name}\", agent_code=\\#{contract.agency.agent_code}}\n" +
            "              return [201, contract.to_json(request.base_url)]\n" +
            "            else\n" +
            "              logger.error %{action=create_contract_proposal_failure, error_message=\"\\#{contract.validation_errors}\"}\n" +
            "              logger.error contract_attributes.to_json\n" +
            "              return [400, {errors: contract.validation_errors}.to_json]\n" +
            "            end\n" +
            "          rescue Exception => e\n" +
            "            request.body.rewind\n" +
            "            logger.error %{action=create_contract_proposal_failure, error_message=\"\\#{e}\"}\n" +
            "            logger.error request.body.read\n" +
            "            logger.error e\n" +
            "            return [400, \"Sorry, but I couldn't generate your contract proposal!\"]\n" +
            "          end\n" +
            "        end\n" +
            "        \n" +
            "        options '/contract_proposals' do\n" +
            "          headers['Access-Control-Allow-Methods'] = 'POST, OPTIONS'\n" +
            "          headers['Access-Control-Allow-Headers'] = 'X-Requested-With, Content-Type'\n" +
            "        end", null)
        String result = runScript(haml)
        MatcherAssert.assertThat result, is(
            "<h1>\n" +
            "  Why would I use it?\n" +
            "</h1>\n" +
            "<div class=\"contents\">\n" +
            "  <div>\n" +
            "    Sinatra webapp\n" +
            "    <pre class=\"brush: ruby\">\n" +
            "      post '/contract_proposals' do\n" +
            "        begin\n" +
            "          contract_attributes = JSON.parse(request.body.read)['contract']\n" +
            "          contract = ContractFactory.contract_from contract_attributes\n" +
            "          \n" +
            "          if contract.valid?\n" +
            "            contract.generate_pdf(File.join(settings.public_folder, PDF_SUBDIR))\n" +
            "            logger.info %{action=contract_created_from_condor, account_manager=\"#{contract.account_manager.name}\", agent_code=#{contract.agency.agent_code}}\n" +
            "            return [201, contract.to_json(request.base_url)]\n" +
            "          else\n" +
            "            logger.error %{action=create_contract_proposal_failure, error_message=\"#{contract.validation_errors}\"}\n" +
            "            logger.error contract_attributes.to_json\n" +
            "            return [400, {errors: contract.validation_errors}.to_json]\n" +
            "          end\n" +
            "        rescue Exception => e\n" +
            "          request.body.rewind\n" +
            "          logger.error %{action=create_contract_proposal_failure, error_message=\"#{e}\"}\n" +
            "          logger.error request.body.read\n" +
            "          logger.error e\n" +
            "          return [400, \"Sorry, but I couldn't generate your contract proposal!\"]\n" +
            "        end\n" +
            "      end\n" +
            "      \n" +
            "      options '/contract_proposals' do\n" +
            "        headers['Access-Control-Allow-Methods'] = 'POST, OPTIONS'\n" +
            "        headers['Access-Control-Allow-Headers'] = 'X-Requested-With, Content-Type'\n" +
            "      end\n" +
            "    </pre>\n" +
            "  </div>\n" +
            "</div>\n")
    }

}
