package au.com.ogsoft.yahaml4j.filters;

import au.com.ogsoft.yahaml4j.HamlGenerator;
import au.com.ogsoft.yahaml4j.HamlRuntime;
import au.com.ogsoft.yahaml4j.ParsePoint;
import au.com.ogsoft.yahaml4j.ProcessOptions;

import java.util.List;

/**
 * Escape filter, renders the text in the block with html escaped
 */
public class EscapedFilter implements Filter {
    @Override
    public void execute(List<String> contents, HamlGenerator generator, Integer indent, ParsePoint parsePoint) {
        for (String line: contents) {
            ProcessOptions processOptions = new ProcessOptions();
            processOptions.escapeHTML = true;
            generator.appendTextContents(HamlRuntime.indentText(indent - 1) + line + '\n', true, parsePoint, processOptions);
        }
    }
}
