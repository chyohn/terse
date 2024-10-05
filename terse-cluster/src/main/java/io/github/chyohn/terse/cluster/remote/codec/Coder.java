package io.github.chyohn.terse.cluster.remote.codec;

import io.github.chyohn.terse.cluster.config.ConfigConstant;
import io.github.chyohn.terse.cluster.config.Environment;

import java.io.IOException;

public interface Coder {

    static Coder getInstance(Environment env) {
        String cn = env.getProperty(ConfigConstant.CLUSTER_CODER_CLASS, ConfigConstant.DEFAULT_CLUSTER_CODER_CLASS);
        try {
            Class clazz = Class.forName(cn);
            return (Coder) clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    byte[] encode(Object obj) throws IOException;

    <T> T decode(byte[] data)  throws IOException;

}
