package com.geoideas.ds;

import org.json.JSONObject;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class Submission2Json {

    public static void main(String[] args) throws IOException, XMLStreamException {
//        var xform = Files.readAllLines(Path.of("demo1.xml")).stream().reduce("", String::concat);
//        var form = Files.readAllLines(Path.of("form.json")).stream().reduce("", String::concat);
//        var count = 100;
//        var total = 0;
//        for(var i = 0; i < count; i++) {
//            var start = System.currentTimeMillis();
//            var converter = new JsonConverter(new JSONObject(form), xform.trim());
//            converter.convert();
//            var end = System.currentTimeMillis() - start;
//            total += end;
//            System.out.println("time : "+end);
//        }
//        System.out.println("Average : "+total/count);
    }

}
