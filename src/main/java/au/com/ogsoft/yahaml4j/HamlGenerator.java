package au.com.ogsoft.yahaml4j;

public interface HamlGenerator {

    void initElementStack();

    void initOutput();

    String generateFlush(String buffer);

    String closeAndReturnOutput();
}
