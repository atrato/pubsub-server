package io.atrato.pubsubserver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * Created by david on 1/23/17.
 */
public class PubsubBroker
{
  private static final Logger LOG = LoggerFactory.getLogger(PubsubBroker.class);
  private SetMultimap<String, PubsubSocket> topicToSubscribers = HashMultimap.create();
  private SetMultimap<PubsubSocket, String> subscriberToTopics = HashMultimap.create();
  private static ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
    objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
  }

  public static PubsubMessage parseMessage(String message) throws IOException
  {
    return objectMapper.readValue(message, PubsubMessage.class);
  }

  public synchronized void publish(String topic, String data)
  {
    Set<PubsubSocket> pubsubSockets = topicToSubscribers.get(topic);
    if (pubsubSockets != null && !pubsubSockets.isEmpty()) {
      try {
        PubsubMessage message = new PubsubMessage(PubsubMessage.Type.DATA, topic, data);
        String messageText = objectMapper.writeValueAsString(message);
        for (PubsubSocket socket : pubsubSockets) {
          LOG.debug("Sending message {} to subscriber {}", messageText, socket);
          socket.sendMessageAsync(messageText);
        }
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
    }
  }

  public synchronized void subscribe(PubsubSocket client, String topic)
  {
    LOG.debug("Client {} subscribes to topic {}", client, topic);
    topicToSubscribers.put(topic, client);
    subscriberToTopics.put(client, topic);
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

}
