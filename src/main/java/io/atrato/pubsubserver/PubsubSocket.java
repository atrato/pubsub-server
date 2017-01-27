package io.atrato.pubsubserver;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by david on 1/23/17.
 */
public class PubsubSocket extends WebSocketAdapter
{
  private static final Logger LOG = LoggerFactory.getLogger(PubsubSocket.class);

  @Override
  public void onWebSocketConnect(Session session)
  {
    super.onWebSocketConnect(session);
  }

  @Override
  public void onWebSocketText(String message)
  {
    super.onWebSocketText(message);
    PubsubMessage pubsubMessage;
    try {
      pubsubMessage = PubsubBroker.parseMessage(message);
    } catch (IOException ex) {
      LOG.warn("Cannot parse message. Ignoring. Content: {}", message, ex);
      return;
    }
    switch (pubsubMessage.getType()) {
      case PUBLISH:
        PubsubServer.getBroker().publish(pubsubMessage.getTopic(), pubsubMessage.getData());
        break;
      case SUBSCRIBE:
        PubsubServer.getBroker().subscribe(this, pubsubMessage.getTopic());
        break;
      case UNSUBSCRIBE:
        PubsubServer.getBroker().unsubscribe(this, pubsubMessage.getTopic());
        break;
      default:
        LOG.warn("Illegal message type \"{}\". Ignoring.", pubsubMessage.getType());
        break;
    }
  }

  @Override
  public void onWebSocketClose(int statusCode, String reason)
  {
    super.onWebSocketClose(statusCode, reason);
    PubsubServer.getBroker().invalidateClient(this);
  }

  @Override
  public void onWebSocketError(Throwable cause)
  {
    super.onWebSocketError(cause);
    LOG.error("Websocket error: ", cause);
  }

  public void sendMessageAsync(String message)
  {
    getRemote().sendStringByFuture(message);
  }
}
