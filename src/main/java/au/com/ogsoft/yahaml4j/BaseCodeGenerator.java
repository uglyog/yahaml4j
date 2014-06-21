package au.com.ogsoft.yahaml4j;

import org.apache.commons.collections4.list.GrowthList;

import java.util.List;

/**
 * Common code shared across all code generators
 */
public abstract class BaseCodeGenerator implements HamlGenerator {

    protected final HamlOptions options;
    protected CodeBuffer outputBuffer;

    /*
    embeddedCodeBlockMatcher: /#{([^}]*)}/g
     */

    private List<Element> elementStack;
    private int indent;

    public BaseCodeGenerator(HamlOptions options) {
        this.options = options;
        outputBuffer = new CodeBuffer(this);
    }

    @Override
    public void initElementStack() {
        elementStack = new GrowthList<Element>();
    }

    @Override
    public CodeBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public int getIndent() {
        return indent;
    }

    /**
     * Set the current indent level
     */
    public void setIndent(int indent) {
        this.indent = indent;
    }

    @Override
    public List<Element> getElementStack() {
        return elementStack;
    }
}
