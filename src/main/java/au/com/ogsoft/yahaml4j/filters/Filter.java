package au.com.ogsoft.yahaml4j.filters;

import au.com.ogsoft.yahaml4j.HamlGenerator;
import au.com.ogsoft.yahaml4j.ParsePoint;

import java.util.List;

/**
 * HAML filters are functions that take 4 parameters
 * contents: The contents block for the filter an array of lines of text
 * generator: The current generator for the compiled function
 * indent: The current indent level
 * currentParsePoint: line and character counters for the current parse point in the input buffer
 */
public interface Filter {
    void execute(List<String> contents, HamlGenerator generator, Integer indent, ParsePoint parsePoint);
}
