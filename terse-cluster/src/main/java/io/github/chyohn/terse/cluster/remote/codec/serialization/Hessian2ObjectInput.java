package io.github.chyohn.terse.cluster.remote.codec.serialization;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;

import java.io.IOException;
import java.io.InputStream;

public class Hessian2ObjectInput implements ObjectInput {

    final Hessian2Input h2i;

    public Hessian2ObjectInput(InputStream in) {
        this.h2i = new Hessian2Input(in);
    }


    @Override
    public Object readObject() throws IOException {
        return h2i.readObject();
    }
    @Override
    public <T> T readObject(Class<T> cls) throws IOException {
        return (T) h2i.readObject(cls);
    }

    public void clean() {
        h2i.reset();
    }
}
