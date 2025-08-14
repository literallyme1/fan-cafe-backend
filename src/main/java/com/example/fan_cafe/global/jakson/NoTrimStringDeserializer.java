package com.example.fan_cafe.global.jakson;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

public class NoTrimStringDeserializer extends StdScalarDeserializer<String> {

    public NoTrimStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return p.getValueAsString();
    }

    @Override
    public String getEmptyValue(DeserializationContext ctxt) {
        return "";
    }
}
