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

package io.github.chyohn.terse.command.result;

import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IResult;

/**
 *
 * @author qiang.shao
 * @since 1.0.0
 */
public class ResultUtils {
    private ResultUtils(){}

    public static IResult<String> failure(ICommand remoteRequest, String msg) {
        return new FailureResult(remoteRequest.getId(), msg);
    }


    public static <T> IResult<T> returnData(ICommand remoteRequest, T data) {
        return new DataResult<>(remoteRequest.getId(), data);
    }
}
