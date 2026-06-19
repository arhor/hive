package io.github.arhor.esphome.client.async.internal;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.github.arhor.esphome.client.proto.ConnectRequest;
import io.github.arhor.esphome.client.proto.ConnectResponse;
import io.github.arhor.esphome.client.proto.HelloRequest;
import io.github.arhor.esphome.client.proto.HelloResponse;

import java.util.Map;

public class EspHomeProtobufRegistry {

    private static final Map<Integer, Parser<? extends MessageLite>> TYPE_TO_PARSER = Map.ofEntries(
        Map.entry(EspHomeMessageType.HELLO_REQUEST, HelloRequest.parser()),
        Map.entry(EspHomeMessageType.HELLO_RESPONSE, HelloResponse.parser()),
        Map.entry(EspHomeMessageType.CONNECT_REQUEST, ConnectRequest.parser()),
        Map.entry(EspHomeMessageType.CONNECT_RESPONSE, ConnectResponse.parser())
    );

    private static final Map<Class<? extends MessageLite>, Integer> CLASS_TO_TYPE = Map.ofEntries(
        Map.entry(HelloRequest.class, EspHomeMessageType.HELLO_REQUEST),
        Map.entry(HelloResponse.class, EspHomeMessageType.HELLO_RESPONSE),
        Map.entry(ConnectRequest.class, EspHomeMessageType.CONNECT_REQUEST),
        Map.entry(ConnectResponse.class, EspHomeMessageType.CONNECT_RESPONSE)
    );

    public static Parser<? extends MessageLite> getParser(final int type) {
        return TYPE_TO_PARSER.get(type);
    }

    public static int getMessageType(final Class<? extends MessageLite> clazz) {
        return CLASS_TO_TYPE.getOrDefault(clazz, -1);
    }
}
