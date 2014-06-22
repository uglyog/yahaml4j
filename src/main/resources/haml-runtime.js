// Generated by CoffeeScript 1.6.2
/*
  clientside HAML compiler for Javascript and Coffeescript (Version 5)

  Copyright 2011-12, Ronald Holshausen (https://github.com/uglyog)
  Released under the MIT License (http://www.opensource.org/licenses/MIT)
*/


(function() {
  var HamlRuntime, haml, root, _ref,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  root = this;

  /*
    Haml runtime functions. These are used both by the compiler and the generated template functions
  */


  HamlRuntime = {
    /*
      Taken from underscore.string.js escapeHTML, and replace the apos entity with character 39 so that it renders
      correctly in IE7
    */

    escapeHTML: function(str) {
      return String(str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, "&#39;");
    },
    /*
      Provides the implementation to preserve the whitespace as per the HAML reference
    */

    perserveWhitespace: function(str) {
      var i, out, re, result;

      re = /<[a-zA-Z]+>[^<]*<\/[a-zA-Z]+>/g;
      out = '';
      i = 0;
      result = re.exec(str);
      if (result) {
        while (result) {
          out += str.substring(i, result.index);
          out += result[0].replace(/\n/g, '&#x000A;');
          i = result.index + result[0].length;
          result = re.exec(str);
        }
        out += str.substring(i);
      } else {
        out = str;
      }
      return out;
    },
    /*
      Generates a error message including the current line in the source where the error occurred
    */

    templateError: function(lineNumber, characterNumber, currentLine, error) {
      var i, message;

      message = error + " at line " + lineNumber + " and character " + characterNumber + ":\n" + currentLine + '\n';
      i = 0;
      while (i < characterNumber - 1) {
        message += '-';
        i++;
      }
      message += '^';
      return message;
    },
    /*
      Generates the attributes for the element by combining all the various sources together
    */

    generateElementAttributes: function(context, id, classes, objRefFn, attrList, attrFunction, lineNumber, characterNumber, currentLine, handleError) {
      var attr, attributes, className, e, ex, hash, html, object, objectId, value;

      if (handleError == null) {
        handleError = this._raiseError;
      }
      attributes = {};
      attributes = this.combineAttributes(attributes, 'id', id);
      if (classes.length > 0 && classes[0].length > 0) {
        attributes = this.combineAttributes(attributes, 'class', classes);
      }
      if (attrList != null) {
        for (attr in attrList) {
          if (!__hasProp.call(attrList, attr)) continue;
          value = attrList[attr];
          attributes = this.combineAttributes(attributes, attr, value);
        }
      }
      if (objRefFn != null) {
        try {
          object = objRefFn.call(context, context);
          if (object != null) {
            objectId = null;
            if (object.id != null) {
              objectId = object.id;
            } else if (object.get) {
              objectId = object.get('id');
            }
            attributes = this.combineAttributes(attributes, 'id', objectId);
            className = null;
            if (object['class']) {
              className = object['class'];
            } else if (object.get) {
              className = object.get('class');
            }
            attributes = this.combineAttributes(attributes, 'class', className);
          }
        } catch (_error) {
          e = _error;
          handleError(haml.HamlRuntime.templateError(lineNumber, characterNumber, currentLine, "Error evaluating object reference - " + e));
        }
      }
      if (attrFunction != null) {
        try {
          hash = attrFunction.call(context, context);
          if (hash != null) {
            hash = this._flattenHash(null, hash);
            for (attr in hash) {
              if (!__hasProp.call(hash, attr)) continue;
              value = hash[attr];
              attributes = this.combineAttributes(attributes, attr, value);
            }
          }
        } catch (_error) {
          ex = _error;
          handleError(haml.HamlRuntime.templateError(lineNumber, characterNumber, currentLine, "Error evaluating attribute hash - " + ex));
        }
      }
      html = '';
      if (attributes) {
        for (attr in attributes) {
          if (!__hasProp.call(attributes, attr)) continue;
          if (haml.hasValue(attributes[attr])) {
            if ((attr === 'id' || attr === 'for') && attributes[attr] instanceof Array) {
              html += ' ' + attr + '="' + _(attributes[attr]).flatten().join('-') + '"';
            } else if (attr === 'class' && attributes[attr] instanceof Array) {
              html += ' ' + attr + '="' + _(attributes[attr]).flatten().join(' ') + '"';
            } else {
              html += ' ' + attr + '="' + haml.attrValue(attr, attributes[attr]) + '"';
            }
          }
        }
      }
      return html;
    },
    /*
      Returns a white space string with a length of indent * 2
    */

    indentText: function(indent) {
      var i, text;

      text = '';
      i = 0;
      while (i < indent) {
        text += '  ';
        i++;
      }
      return text;
    },
    /*
      Combines the attributes in the attributres hash with the given attribute and value
      ID, FOR and CLASS attributes will expand to arrays when multiple values are provided
    */

    combineAttributes: function(attributes, attrName, attrValue) {
      var classes;

      if (haml.hasValue(attrValue)) {
        if (attrName === 'id' && attrValue.toString().length > 0) {
          if (attributes && attributes.id instanceof Array) {
            attributes.id.unshift(attrValue);
          } else if (attributes && attributes.id) {
            attributes.id = [attributes.id, attrValue];
          } else if (attributes) {
            attributes.id = attrValue;
          } else {
            attributes = {
              id: attrValue
            };
          }
        } else if (attrName === 'for' && attrValue.toString().length > 0) {
          if (attributes && attributes['for'] instanceof Array) {
            attributes['for'].unshift(attrValue);
          } else if (attributes && attributes['for']) {
            attributes['for'] = [attributes['for'], attrValue];
          } else if (attributes) {
            attributes['for'] = attrValue;
          } else {
            attributes = {
              'for': attrValue
            };
          }
        } else if (attrName === 'class') {
          classes = [];
          if (attrValue instanceof Array) {
            classes = classes.concat(attrValue);
          } else {
            classes.push(attrValue);
          }
          if (attributes && attributes['class']) {
            attributes['class'] = attributes['class'].concat(classes);
          } else if (attributes) {
            attributes['class'] = classes;
          } else {
            attributes = {
              'class': classes
            };
          }
        } else if (attrName !== 'id') {
          attributes || (attributes = {});
          attributes[attrName] = attrValue;
        }
      }
      return attributes;
    },
    /*
      Flattens a deeply nested hash into a single hash by combining the keys with a minus
    */

    _flattenHash: function(rootKey, object) {
      var attr, flattenedValue, key, keys, newKey, newValue, result, value;

      result = {};
      if (this._isHash(object)) {
        for (attr in object) {
          if (!__hasProp.call(object, attr)) continue;
          value = object[attr];
          keys = [];
          if (rootKey != null) {
            keys.push(rootKey);
          }
          keys.push(attr);
          key = keys.join('-');
          flattenedValue = this._flattenHash(key, value);
          if (this._isHash(flattenedValue)) {
            for (newKey in flattenedValue) {
              if (!__hasProp.call(flattenedValue, newKey)) continue;
              newValue = flattenedValue[newKey];
              result[newKey] = newValue;
            }
          } else {
            result[key] = flattenedValue;
          }
        }
      } else if (rootKey != null) {
        result[rootKey] = object;
      } else {
        result = object;
      }
      return result;
    },
    _isHash: function(object) {
      return (object != null) && typeof object === 'object' && !(object instanceof Array || object instanceof Date);
    },
    _logError: function(message) {
      return typeof console !== "undefined" && console !== null ? console.log(message) : void 0;
    },
    _raiseError: function(message) {
      throw new Error(message);
    },
    /*
      trims the first number of characters from a string
    */

    trim: function(str, chars) {
      return str.substring(chars);
    }
  };

  haml = {
    hasValue: function(value) {
      return (value != null) && value !== false;
    },
    attrValue: function(attr, value) {
      if (attr === 'selected' || attr === 'checked' || attr === 'disabled') {
        return attr;
      } else {
        return value;
      }
    }
  };
  haml.HamlRuntime = HamlRuntime;

  if ((typeof module !== "undefined" && module !== null ? module.exports : void 0) != null) {
    module.exports = haml;
  } else {
    root.haml = haml;
  }

}).call(this);
