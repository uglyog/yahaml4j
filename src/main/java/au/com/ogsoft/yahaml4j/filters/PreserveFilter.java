package au.com.ogsoft.yahaml4j.filters;

import au.com.ogsoft.yahaml4j.HamlGenerator;
import au.com.ogsoft.yahaml4j.HamlRuntime;
import au.com.ogsoft.yahaml4j.ParsePoint;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * Preserve filter, preserved blocks of text aren't indented, and newlines are replaced with the HTML escape code for newlines
 */
public class PreserveFilter implements Filter {
    @Override
    public void execute(List<String> contents, HamlGenerator generator, Integer indent, ParsePoint parsePoint) {
        generator.appendTextContents(HamlRuntime.indentText(indent), false, parsePoint, null);
        Collection<Object> collect = CollectionUtils.collect(contents, new Transformer<String, Object>() {
            @Override
            public Object transform(String input) {
                return input.substring(2);
            }
        });
        generator.appendTextContents(StringUtils.join(collect, "&#x000A; ") + '\n', true, parsePoint, null);
    }
}
