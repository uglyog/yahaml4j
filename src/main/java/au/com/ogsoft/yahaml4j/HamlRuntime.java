package au.com.ogsoft.yahaml4j;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Haml runtime functions. These are used both by the compiler and the generated template functions
 */
public class HamlRuntime {

    /**
     * Generates a error message including the current line in the source where the error occurred
     */
    public static String templateError(Integer lineNumber, Integer characterNumber, String currentLine, String error) {
        String message = error + " at line " + lineNumber + " and character " + characterNumber +
                    ":\n" + currentLine + "\n";
        int i = 0;
        while (i < characterNumber - 1) {
            message += "-";
            i++;
        }
        message += "^";

        return message;
    }

    /**
     * Returns a white space string with a length of indent * 2
     */
    public static String indentText(int indent) {
        String text = "";
        int i = 0;
        while (i < indent) {
            text += "  ";
            i++;
        }
        return text;
    }

    /**
     * Generates the attributes for the element by combining all the various sources together
     */
    public static String generateElementAttributes(Object context, String id, List<String> classes, Object objectRef,
                                                   Map<String, String> attributeList, Object attributeFn, int lineNumber,
                                                   int characterNumber, String currentLine) {

        Map<String, Object> attributes = new HashMap<String, Object>();

        combineAttributes(attributes, "id", id);
        if (!classes.isEmpty() && StringUtils.isNotEmpty(classes.get(0))) {
            combineAttributes(attributes, "class", classes);
        }

        if (attributeList != null) {
            for (Map.Entry<String, String> attr : attributeList.entrySet()) {
                combineAttributes(attributes, attr.getKey(), attr.getValue());
            }
        }

        /*if objRefFn?
          try
            object = objRefFn.call(context, context)
            if object?
              objectId = null
              if object.id?
                objectId = object.id
              else if object.get
                objectId = object.get('id')
              attributes = @combineAttributes(attributes, 'id', objectId)
              className = null
              if object['class']
                className = object['class']
              else if object.get
                className = object.get('class')
              attributes = @combineAttributes(attributes, 'class', className)
          catch e
            handleError haml.HamlRuntime.templateError(lineNumber, characterNumber, currentLine, "Error evaluating object reference - #{e}")

        if attrFunction?
          try
            hash = attrFunction.call(context, context)
            if hash?
              hash = @_flattenHash(null, hash)
              attributes = @combineAttributes(attributes, attr, value) for own attr, value of hash
          catch ex
            handleError haml.HamlRuntime.templateError(lineNumber, characterNumber, currentLine, "Error evaluating attribute hash - #{ex}")
        */

        StringBuilder html = new StringBuilder();
        for (Map.Entry<String, Object> entry: attributes.entrySet()) {
            String attr = entry.getKey();
            if (hasValue(attributes.get(attr))) {
                if ((attr.equals("id") || attr.equals("for")) && (attributes.get(attr) instanceof List)) {
                    List<String> list = (List<String>) attributes.get(attr);
                    html.append(' ').append(attr).append("=\"").append(StringUtils.join(flatten(list), '-')).append('"');
                } else if (attr == "class" && attributes.get(attr) instanceof List) {
                    List<String> list = (List<String>) attributes.get(attr);
                    html.append(' ').append(attr).append("=\"").append(StringUtils.join(flatten(list), ' ')).append('"');
                } else {
                    html.append(' ').append(attr).append("=\"").append(attrValue(attr, attributes.get(attr))).append('"');
                }
            }
        }

        return html.toString();
    }

    private static List flatten(List list) {
        List newlist = new ArrayList();
        for(Object item: list) {
            if (item instanceof List) {
                newlist.addAll(flatten((List) item));
            } else {
                newlist.add(item);
            }
        }
        return newlist;
    }

    /**
     * Combines the attributes in the attributes hash with the given attribute and value
     * ID, FOR and CLASS attributes will expand to arrays when multiple values are provided
     */
    private static void combineAttributes(Map<String, Object> attributes, String attrName, Object attrValue) {
        if (hasValue(attrValue)) {
            if (attrName.equals("id") && StringUtils.isNotEmpty(String.valueOf(attrValue))) {
                if (!attributes.isEmpty() && attributes.get("id") instanceof List) {
                    List ids = (List) attributes.get("id");
                    ids.add(attrValue);
                } else if (!attributes.isEmpty() && attributes.containsKey("id")) {
                    List ids = Arrays.asList(attributes.get("id"), attrValue);
                    attributes.put("id", ids);
                } else {
                    attributes.put("id", attrValue);
                }
            } else if (attrName.equals("for") && StringUtils.isNotEmpty(String.valueOf(attrValue))) {
                if (!attributes.isEmpty() && attributes.get("for") instanceof List) {
                    List ids = (List) attributes.get("for");
                    ids.add(attrValue);
                } else if (!attributes.isEmpty() && attributes.containsKey("for")) {
                    List ids = Arrays.asList(attributes.get("for"), attrValue);
                    attributes.put("for", ids);
                } else {
                    attributes.put("for", attrValue);
                }
            } else if (attrName.equals("class")) {
                List classes = new ArrayList();
                if (attrValue instanceof List) {
                    classes.addAll((List) attrValue);
                } else {
                    classes.add(attrValue);
                }
                if ( attributes.containsKey("class")) {
                    ((List) attributes.get("class")).addAll(classes);
                } else {
                    attributes.put("class", classes);
                }
            } else if (!attrName.equals("id")) {
                attributes.put(attrName, attrValue);
            }
        }
    }

    private static String attrValue(String attr, Object value) {
        return attr.equals("selected") || attr.equals("checked") || attr.equals("disabled") ? attr : String.valueOf(value);
    }

    /*
    ###

  ###
  indentText: (indent) ->
    text = ''
    i = 0
    while i < indent
      text += '  '
      i++
    text

     */

    /*

  ###
    Taken from underscore.string.js escapeHTML, and replace the apos entity with character 39 so that it renders
    correctly in IE7
  ###
  escapeHTML: (str) ->
    String(str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, "&#39;")

  ###
    Provides the implementation to preserve the whitespace as per the HAML reference
  ###
  perserveWhitespace: (str) ->
    re = /<[a-zA-Z]+>[^<]*<\/[a-zA-Z]+>/g
    out = ''
    i = 0
    result = re.exec(str)
    if result
      while result
        out += str.substring(i, result.index)
        out += result[0].replace(/\n/g, '&#x000A;')
        i = result.index + result[0].length
        result = re.exec(str)
      out += str.substring(i)
    else
      out = str
    out

  ###

  ###
    Flattens a deeply nested hash into a single hash by combining the keys with a minus
  ###
  _flattenHash: (rootKey, object) ->
    result = {}
    if @_isHash(object)
      for own attr, value of object
        keys = []
        keys.push(rootKey) if rootKey?
        keys.push(attr)
        key = keys.join('-')
        flattenedValue = @_flattenHash(key, value)
        if @_isHash(flattenedValue)
          result[newKey] = newValue for own newKey, newValue of flattenedValue
        else
          result[key] = flattenedValue
    else if rootKey?
      result[rootKey] = object
    else
      result = object
    result

  _isHash: (object) ->
    object? and typeof object == 'object' and not (object instanceof Array or object instanceof Date)

  _logError: (message) -> console?.log(message)

  _raiseError: (message) -> throw new Error(message)

  ###
    trims the first number of characters from a string
  ###
  trim: (str, chars) -> str.substring(chars)



     */

    private static boolean hasValue(Object value) {
        return (value != null) && (((value instanceof Boolean) && (Boolean) value) || (!(value instanceof Boolean)));
    }

}
