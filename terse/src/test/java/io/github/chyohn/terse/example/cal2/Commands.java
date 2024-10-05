package io.github.chyohn.terse.example.cal2;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.command.ICommandX;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Commands {

    public static void main(String[] args) {
        List<AddCommand> commands = new ArrayList<>();
        commands.add(new AddCommand(1, 2));
        commands.add(new AddCommand(5, 3));
        Terse.commandInvoker().run(commands, result -> {
            AddCommand command = commands.get(result.getId());
            System.out.printf("%s + %s = %s", command.getX(), command.getY(), result.getValue());
        });

        Terse.commandInvoker().asyncRun(new AddCommand(1, 2), result -> {
            System.out.println(result);
        });

        Terse.commandInvoker().asyncRun("my_pool", () -> {
            System.out.println("Callable thread name: " + Thread.currentThread().getName());
            return true;
        }, result -> {
            System.out.printf("callback result: %s,  thread name: %s \n", result, Thread.currentThread().getName());
        });
    }

    @Getter
    public static abstract class CalculateCommand implements ICommandX<Integer> {
        private int id;
        private final int x;
        private final int y;
        public CalculateCommand(int x,int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public void setId(int id) {
            this.id = id;
        }
    }
    // 加法命令
    static class AddCommand extends CalculateCommand {
        public AddCommand(int x, int y) {
            super(x, y);
        }
    }

    // 减法命令
    static class SubCommand extends CalculateCommand {
        public SubCommand(int x, int y) {
            super(x, y);
        }
    }

    // 乘法命令
    static class MulCommand extends CalculateCommand {
        public MulCommand(int x, int y) {
            super(x, y);
        }
    }

    // 除法命令
    static class DivCommand extends CalculateCommand {
        public DivCommand(int x, int y) {
            super(x, y);
        }
    }
}

