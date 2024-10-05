package io.github.chyohn.terse.cluster.remote.codec.serialization;

import com.alibaba.com.caucho.hessian.io.Hessian2Output;

import java.io.IOException;
import java.io.OutputStream;

public class Hessian2ObjectOutput implements ObjectOutput {

    final Hessian2Output h2o;

    public Hessian2ObjectOutput(OutputStream out) {
        this.h2o = new Hessian2Output(out);
    }


    @Override
    public void writeObject(Object obj) throws IOException {
        this.h2o.writeObject(obj);
    }

    @Override
    public void flushBuffer() throws IOException {
        h2o.flushBuffer();
    }

}
