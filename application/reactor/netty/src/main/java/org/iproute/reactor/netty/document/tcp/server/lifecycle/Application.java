/*
 * Copyright (c) 2020-2022 VMware, Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iproute.reactor.netty.document.tcp.server.lifecycle;

import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

import java.util.concurrent.TimeUnit;

public class Application {

    public static void main(String[] args) {
        DisposableServer server =
                TcpServer.create()
                        .doOnConnection(conn ->
                                conn.addHandlerFirst(new ReadTimeoutHandler(10, TimeUnit.SECONDS))) //<1>
                        .doOnChannelInit((observer, channel, remoteAddress) ->
                                channel.pipeline()
                                        .addFirst(new LoggingHandler("reactor.netty.examples")))     //<2>
                        .bindNow();

        server.onDispose()
                .block();
    }
}