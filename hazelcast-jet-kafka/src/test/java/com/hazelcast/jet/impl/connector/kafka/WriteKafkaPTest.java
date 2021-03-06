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

package com.hazelcast.jet.impl.connector.kafka;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.processor.KafkaProcessors;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.processor.SourceProcessors.readMapP;
import static java.util.stream.IntStream.range;

@Category(QuickTest.class)
@RunWith(HazelcastSerialClassRunner.class)
public class WriteKafkaPTest extends KafkaTestSupport {

    @Test
    public void testWriteToTopic() throws Exception {
        String brokerConnectionString = createKafkaCluster();

        final String topic = randomName();
        createTopic(topic, 1);
        JetInstance instance = createJetMember();

        int messageCount = 20;
        Map<String, String> map = range(0, messageCount)
                .mapToObj(Integer::toString)
                .collect(Collectors.toMap(m -> m, m -> m));

        instance.getMap("source").putAll(map);
        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", readMapP("source"))
                           .localParallelism(1);

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", brokerConnectionString);
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());
        Vertex sink = dag.newVertex("sink", KafkaProcessors.<Entry<String, String>, String, String>writeKafkaP(
                topic, properties, Entry::getKey, Entry::getValue)).localParallelism(4);

        dag.edge(between(source, sink));

        Future<Void> future = instance.newJob(dag).getFuture();
        assertCompletesEventually(future);

        KafkaConsumer<String, String> consumer = createConsumer(brokerConnectionString, topic);
        ConsumerRecords<String, String> records = consumer.poll(100);
        for (ConsumerRecord<String, String> record : records) {
            Assert.assertTrue(map.containsValue(record.value()));
        }
    }
}
