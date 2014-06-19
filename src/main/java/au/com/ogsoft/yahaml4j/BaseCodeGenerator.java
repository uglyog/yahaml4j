package au.com.ogsoft.yahaml4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Common code shared across all code generators
 */
public abstract class BaseCodeGenerator implements HamlGenerator {

    private final Map<String, Object> options;

    /*
    embeddedCodeBlockMatcher: /#{([^}]*)}/g
     */

    private Deque elementStack;

    public BaseCodeGenerator(Map<String, Object> options) {
        this.options = options;
    }

    @Override
    public void initElementStack() {
        elementStack = new ArrayDeque();
    }

    protected boolean optionEnabled(String option) {
        return options.containsKey(option) && (Boolean) options.get(option);
    }
}
