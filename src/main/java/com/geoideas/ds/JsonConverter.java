package com.geoideas.ds;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class JsonConverter {
    public static final String COUNT = "_count";
    long start = 0;
    long total = 0;
    public static final String SURVEY = "survey";
    public static final String ROOT = "data";
    public static final String PREFIX = "/";
    public static final String XFORM_ID_STRING = "_xform_id_string";
    public static final String ID = "id";
    public static final String XFORM_VERSION = "_xform_version";
    public static final String VERSION = "version";
    public static final String NAME = "name";
    public static final String PARENT1 = "parent";
    public static final String PARENT = "parent";
    public static final String TYPE = "type";
    private JSONObject form;
    private String xform;
    private JSONObject submission;
    private Map<String, JSONObject> formMap;

    private class RepeatParent {
        String name;
        JSONArray parent;
        public RepeatParent(String name, JSONArray parent) {
            this.name = name;
            this.parent = parent;
        }
    }

    public JsonConverter(JSONObject form, String xform) {
        this.form = form;
        this.xform = xform.trim();
        this.submission = new JSONObject();
        var survey = this.form.getJSONObject(SURVEY);
        this.formMap = survey.keySet().stream()
                .map(field -> Map.of(formMapFormat(field), survey.getJSONObject(field)))
                .reduce(new HashMap<>(), (total, next) -> {
                    total.putAll(next);
                    return total;
                });
    }

    public JSONObject convert() throws XMLStreamException {
        var factory = XMLInputFactory.newInstance();
        var reader = factory.createXMLEventReader(new StringReader(xform));
        var currentField = "";
        while(reader.hasNext()) {
            var element = reader.nextEvent();
            if(element.isStartElement()) {
                var tag = element.asStartElement();
                var name = tag.getName().getLocalPart();
                if(name.equals(ROOT)) {
                    submission.put(XFORM_ID_STRING, tag.getAttributeByName(new QName(ID)).getValue());
                    var version = tag.getAttributeByName(new QName(VERSION));
                    if(version != null) submission.put(XFORM_VERSION, version.getValue());
                }
                else currentField = name;
                addIfRepeat(name, submission);
                if(formMap.containsKey(name)) {
                    var formField = formMap.get(name);
                    var formFieldCount = formField.has(COUNT) ? formField.getInt(COUNT)+1 : 0;
                    formField.put(COUNT, formFieldCount);
                }
            }
            else if(element.isCharacters()) {
                var data = element.asCharacters().getData();
                if(currentField.equals("instanceID"))
                    submission.put("meta/instanceID", data);
                addField(currentField, element.asCharacters().getData());
                currentField = "";
            }
        }
        return submission;
    }

    private boolean addIfRepeat(String name, JSONObject sub) {
        var field = formMap.getOrDefault(name, null);
        if(field == null) return false;
        if(isRepeat(field)) {
            var formattedName = formatName(field.getString(NAME));
            if(!sub.has(formattedName))
                sub.put(formattedName, new JSONArray());
            return true;
        }
        return false;
    }

    private void addField(String fieldName, String value) {
        var field = formMap.getOrDefault(fieldName, null);
        if(field == null) return;
        if(!inOrIsRepeat(field)) {
            addToSubmission(submission, field, value);
        }
        else {
            var repeatP = firstRepeatParent(field);
            var repeat = repeatP.parent;
            var repeatSize = repeat.length();
            JSONObject childSubmission = repeatSize == 0
                    ? null : repeat.getJSONObject(repeatSize - 1);
            var childIndex = (repeatSize - 1);
            var repeatField = form.getJSONObject(SURVEY).getJSONObject(repeatP.name);

            var formFieldCount = formMap.get(fieldName).getInt(COUNT);
            if((childSubmission == null) || (childSubmission != null && formFieldCount > childIndex)) {
                childSubmission = new JSONObject();
                repeat.put(childSubmission);
            }
            if(repeatField.has(PARENT)) {
                var parentRepeat = firstRepeatParent(repeatField).parent;
                childSubmission.put("_repeat_parent_index", parentRepeat.length());
            }
            addToSubmission(childSubmission, field, value);
        }
    }

    private void addToSubmission(JSONObject sub, JSONObject field, String value) {
        var name = field.getString(NAME);
        switch (field.getString(TYPE)) {
            case "begin repeat":
                sub.put(formatName(name), new JSONArray());
                break;
            case "integer":
            case "range":
                sub.put(formatName(name), Integer.valueOf(value));
                break;
            case "decimal":
                sub.put(formatName(name), Double.valueOf(value));
                break;
            default: sub.put(formatName(name), value);
        }
    }

    private RepeatParent firstRepeatParent(JSONObject field) {
        var parentName = field.getString(PARENT);
        var parent = form.getJSONObject(SURVEY).getJSONObject(parentName);
        if(isRepeat(parent)) return new RepeatParent(parentName, submission.getJSONArray(formatName(parentName)));
        else return firstRepeatParent(parent);
    }

    private boolean inOrIsRepeat(JSONObject json) {
        var notEmpty = !json.isEmpty();
        var isRepeat = false;
        if(notEmpty) {
            if(json.has(PARENT)) {
                var parent = json.getString(PARENT);
                isRepeat = inOrIsRepeat(formMap.get(formMapFormat(parent)));
            }
            else isRepeat = isRepeat(json);
        }
        return isRepeat;
    }

    private boolean isRepeat(JSONObject json) {
        return !json.isEmpty() && json.getString(TYPE).startsWith("begin repeat");
    }

    private String formatName(String name) {
        return name.substring(1);
    }

    private String formMapFormat(String field) {
        return field.substring(field.lastIndexOf(PREFIX)+1);
    }

}
