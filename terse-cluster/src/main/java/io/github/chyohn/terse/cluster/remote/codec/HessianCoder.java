package io.github.chyohn.terse.cluster.remote.codec;

import io.github.chyohn.terse.cluster.remote.codec.serialization.ObjectInput;
import io.github.chyohn.terse.cluster.remote.codec.serialization.ObjectOutput;
import io.github.chyohn.terse.cluster.remote.codec.serialization.SerializationUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessianCoder implements Coder {
    @Override
    public byte[] encode(Object obj) throws IOException {
        // 序列化数据
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutput oo = SerializationUtil.serialize(bout);
        oo.writeObject(obj);
        oo.flushBuffer();
        return bout.toByteArray();
    }

    @Override
    public <T> T decode(byte[] data) throws IOException {

        ByteArrayInputStream bi = new ByteArrayInputStream(data);
        ObjectInput oi = SerializationUtil.deserialize(bi);
        return (T)oi.readObject();
    }
}
