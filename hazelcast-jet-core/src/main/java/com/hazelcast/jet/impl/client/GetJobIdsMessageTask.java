/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.impl.client;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.JetGetJobIdsCodec;
import com.hazelcast.client.impl.protocol.codec.JetGetJobStatusCodec;
import com.hazelcast.instance.Node;
import com.hazelcast.jet.impl.operation.GetJobIdsOperation;
import com.hazelcast.nio.Connection;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.serialization.SerializationService;

public class GetJobIdsMessageTask extends AbstractJetMessageTask<JetGetJobIdsCodec.RequestParameters> {
    protected GetJobIdsMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection, JetGetJobIdsCodec::decodeRequest,
                o -> JetGetJobStatusCodec.encodeResponse((Data) o));
    }

    @Override
    protected Operation prepareOperation() {
        return new GetJobIdsOperation();
    }

    @Override
    public void onResponse(Object response) {
        SerializationService serializationService = nodeEngine.getSerializationService();
        sendResponse(serializationService.toData(response));
    }

    @Override
    public String getMethodName() {
        return "getStatus";
    }

    @Override
    public Object[] getParameters() {
        return new Object[0];
    }
}
