package io.github.chyohn.terse.cluster.remote.message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private Object data;
    private Throwable e;
}
