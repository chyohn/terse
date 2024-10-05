package io.github.chyohn.terse.cluster.support;

import io.github.chyohn.terse.command.ICommand;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CommandRequest implements Serializable {
    ICommand command;
}
