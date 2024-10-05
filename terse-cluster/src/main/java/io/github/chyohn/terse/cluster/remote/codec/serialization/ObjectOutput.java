package io.github.chyohn.terse.cluster.remote.codec.serialization;

import java.io.IOException;

public interface ObjectOutput {
    void writeObject(Object obj) throws IOException;

    void flushBuffer() throws IOException;
}
