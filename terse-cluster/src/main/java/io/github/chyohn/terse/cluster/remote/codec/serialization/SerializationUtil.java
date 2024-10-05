package io.github.chyohn.terse.cluster.remote.codec.serialization;

import java.io.InputStream;
import java.io.OutputStream;

public class SerializationUtil {

    public static ObjectOutput serialize(OutputStream output) {
        return new Hessian2ObjectOutput(output);
    }

    public static ObjectInput deserialize(InputStream input) {
        return new Hessian2ObjectInput(input);
    }
}
