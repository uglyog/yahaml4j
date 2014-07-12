package au.com.ogsoft.yahaml4j.filters;

import au.com.ogsoft.yahaml4j.HamlGenerator;
import au.com.ogsoft.yahaml4j.HamlRuntime;
import au.com.ogsoft.yahaml4j.ParsePoint;

import java.util.List;

/**
 * Plain filter, just renders the text in the block
 */
public class PlainFilter implements Filter {

    @Override
    public void execute(List<String> contents, HamlGenerator generator, Integer indent, ParsePoint parsePoint) {
        for (String line: contents) {
            generator.appendTextContents(HamlRuntime.indentText(indent - 1) + line + "\n", true, parsePoint, null);
        }
    }

}
