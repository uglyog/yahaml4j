package au.com.ogsoft.yahaml4j;

import java.util.List;
import java.util.Map;

public interface HamlGenerator {

    void initElementStack();

    void initOutput();

    String generateFlush(String buffer);

    String closeAndReturnOutput();

    CodeBuffer getOutputBuffer();

    /**
     * Save the current indent level if required
     */
    void mark();

    /**
     * Set the current indent level
     */
    void setIndent(int indent);

    List<Element> getElementStack();

    void closeOffCodeBlock(Tokeniser tokeniser);

    void closeOffFunctionBlock(Tokeniser tokeniser);

    void generateCodeForDynamicAttributes(String id, List<String> classes, Map<String, String> attributeList,
                                          Map<String, String> attributeHash, String objectRef, ParsePoint currentParsePoint);

    void appendTextContents(String text, boolean shouldInterpolate, ParsePoint currentParsePoint, ProcessOptions options);

    /**
     * Scan the token stream for a valid block of code
     */
    String scanEmbeddedCode(Tokeniser tokeniser);
}
