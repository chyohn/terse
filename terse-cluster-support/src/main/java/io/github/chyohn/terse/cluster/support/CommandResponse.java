package io.github.chyohn.terse.cluster.support;

import io.github.chyohn.terse.command.IResult;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CommandResponse implements Serializable {
    IResult<?> result;
}
