package io.github.chyohn.terse.cluster.remote.codec.serialization;

import java.io.IOException;

public interface ObjectInput {
    Object readObject() throws IOException;
    <T> T readObject(Class<T> cls) throws IOException;
}
