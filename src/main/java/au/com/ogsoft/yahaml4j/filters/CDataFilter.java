package au.com.ogsoft.yahaml4j.filters;


import au.com.ogsoft.yahaml4j.HamlGenerator;
import au.com.ogsoft.yahaml4j.HamlRuntime;
import au.com.ogsoft.yahaml4j.ParsePoint;

import java.util.List;

/**
 * Wraps the filter block in a CDATA tag
 */
public class CDataFilter implements Filter {
    @Override
    public void execute(List<String> contents, HamlGenerator generator, Integer indent, ParsePoint parsePoint) {
        generator.getOutputBuffer().append(HamlRuntime.indentText(indent) + "<![CDATA[\n");
        for (String line: contents) {
            generator.appendTextContents(HamlRuntime.indentText(indent) + line + '\n', true, parsePoint, null);
        }
        generator.getOutputBuffer().append(HamlRuntime.indentText(indent) + "]]>\n");
    }
}
