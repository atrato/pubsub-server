/**
 * Copyright (c) 2017 Atrato, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.atrato.pubsubserver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by david on 1/23/17.
 */
public class PubsubBroker
{
  private static final Logger LOG = LoggerFactory.getLogger(PubsubBroker.class);
  private SetMultimap<String, PubsubSocket> topicToSubscribers = HashMultimap.create();
  private SetMultimap<PubsubSocket, String> subscriberToTopics = HashMultimap.create();
  private SetMultimap<String, AsyncContext> topicToOneTimeSubscribers = HashMultimap.create();
  private Map<AsyncContext, String> oneTimeSubscriberToTopic = Maps.newHashMap();
  private static ObjectMapper objectMapper = new ObjectMapper();

  public static class TimedTopicData
  {
    private long timestamp;
    private JsonNode data;

    public TimedTopicData()
    {
    }

    public TimedTopicData(long timestamp, JsonNode data)
    {
      this.timestamp = timestamp;
      this.data = data;
    }

    public long getTimestamp()
    {
      return timestamp;
    }

    public JsonNode getData()
    {
      return data;
    }
  }

  // topic data expires in 60 minutes
  private Cache<String, TimedTopicData> latestTopicData = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build();

  static {
    objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
    objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
  }

  public static PubsubMessage parseMessage(String message) throws IOException
  {
    return objectMapper.readValue(message, PubsubMessage.class);
  }

  public synchronized void publish(String topic, JsonNode data)
  {
    long now = System.currentTimeMillis();
    latestTopicData.put(topic, new TimedTopicData(now, data));
    Set<PubsubSocket> pubsubSockets = topicToSubscribers.get(topic);
    if (pubsubSockets != null && !pubsubSockets.isEmpty()) {
      try {
        PubsubMessage message = new PubsubMessage(PubsubMessage.Type.DATA, topic, data, now);
        String messageText = objectMapper.writeValueAsString(message);
        for (PubsubSocket socket : pubsubSockets) {
          LOG.debug("Sending message {} to subscriber {}", messageText, socket);
          socket.sendMessageAsync(messageText);
        }
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
    }
    Set<AsyncContext> asyncContexts = topicToOneTimeSubscribers.get(topic);
    if (asyncContexts != null && !asyncContexts.isEmpty()) {
      for (AsyncContext async : asyncContexts) {
        async.dispatch();
        oneTimeSubscriberToTopic.remove(async);
      }
      topicToOneTimeSubscribers.removeAll(topic);
    }
  }

  public synchronized void subscribe(PubsubSocket client, String topic)
  {
    LOG.debug("Client {} subscribes to topic {}", client, topic);
    topicToSubscribers.put(topic, client);
    subscriberToTopics.put(client, topic);
  }

  public synchronized void oneTimeSubscribe(AsyncContext client, String topic)
  {
    LOG.debug("Client {} subscribes to topic {}", client, topic);
    topicToOneTimeSubscribers.put(topic, client);
    oneTimeSubscriberToTopic.put(client, topic);
  }

  public synchronized void unsubscribe(PubsubSocket client, String topic)
  {
    LOG.debug("Client {} unsubscribes from topic {}", client, topic);
    topicToSubscribers.remove(topic, client);
    subscriberToTopics.remove(client, topic);
  }

  public synchronized void invalidateClient(PubsubSocket client)
  {
    LOG.debug("Invalidates client {}", client);
    Set<String> topics = subscriberToTopics.get(client);
    if (topics != null) {
      for (String topic : topics) {
        topicToSubscribers.remove(topic, client);
      }
    }
    subscriberToTopics.removeAll(client);
  }

  public synchronized void invalidateOneTimeSubscriber(AsyncContext client)
  {
    LOG.debug("Invalidates client {}", client);
    String topic = oneTimeSubscriberToTopic.get(client);
    if (topic != null) {
      topicToOneTimeSubscribers.remove(topic, client);
    }
    oneTimeSubscriberToTopic.remove(client);
  }

  public TimedTopicData getLatestTopicData(String topic, long laterThan)
  {
    TimedTopicData timedTopicData = latestTopicData.getIfPresent(topic);
    if (timedTopicData != null && timedTopicData.timestamp > laterThan) {
      return timedTopicData;
    } else {
      return null;
    }
  }
}
