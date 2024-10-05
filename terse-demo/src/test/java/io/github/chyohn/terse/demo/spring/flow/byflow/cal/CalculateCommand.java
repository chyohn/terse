/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chyohn.terse.demo.spring.flow.byflow.cal;

import io.github.chyohn.terse.command.ICommandX;
import lombok.Getter;

@Getter
public class CalculateCommand implements ICommandX<Integer> {
    private int id;
    private final Op op;
    private final int x;
    private final int y;
    public CalculateCommand(int x, Op op, int y) {
        this.x = x;
        this.op = op;
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
