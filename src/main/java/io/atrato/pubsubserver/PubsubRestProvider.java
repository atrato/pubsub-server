package io.atrato.pubsubserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.BooleanUtils;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * Created by david on 1/26/17.
 */
@Path("/pubsub")
public class PubsubRestProvider
{
  @JsonIgnoreProperties(ignoreUnknown=true)
  public static class NullInfo
  {
    public static final NullInfo INSTANCE = new NullInfo();

    private NullInfo()
    {
    }
  }

  //TODO: May want to add summary of all topics for /ws/v1/pubsub/topics

  @GET
  @Path("topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  public PubsubBroker.TimedTopicData getTopicData(@PathParam("topic") String topic, @QueryParam("laterThan") Long timestamp, @QueryParam("poll") Boolean poll, @Context HttpServletRequest request)
  {
    if (timestamp == null) {
      timestamp = -1L;
    }
    PubsubBroker.TimedTopicData data = PubsubServer.getBroker().getLatestTopicData(topic, timestamp);
    if (data == null) {
      if (BooleanUtils.isTrue(poll)) {
        AsyncContext asyncContext = request.startAsync();
        PubsubServer.getBroker().oneTimeSubscribe(asyncContext, topic);
        asyncContext.addListener(new AsyncListener()
        {
          @Override
          public void onComplete(AsyncEvent asyncEvent) throws IOException
          {
            PubsubServer.getBroker().invalidateOneTimeSubscriber(asyncEvent.getAsyncContext());
          }

          @Override
          public void onTimeout(AsyncEvent asyncEvent) throws IOException
          {
            PubsubServer.getBroker().invalidateOneTimeSubscriber(asyncEvent.getAsyncContext());
          }

          @Override
          public void onError(AsyncEvent asyncEvent) throws IOException
          {
            PubsubServer.getBroker().invalidateOneTimeSubscriber(asyncEvent.getAsyncContext());
          }

          @Override
          public void onStartAsync(AsyncEvent asyncEvent) throws IOException
          {
          }
        });
        return null;
      } else {
        throw new NotFoundException();
      }
    }
    return data;
  }

  @POST
  @Path("topics/{topic}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public NullInfo postTopicData(@PathParam("topic") String topic, JsonNode data)
  {
    PubsubServer.getBroker().publish(topic, data);
    return NullInfo.INSTANCE;
  }
}