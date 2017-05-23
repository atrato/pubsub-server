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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.List;

/**
 * Created by david on 1/26/17.
 */
@Path("/pubsub")
public class PubsubRestProvider
{
  private static final Logger LOG = LoggerFactory.getLogger(PubsubRestProvider.class);

  @JsonIgnoreProperties(ignoreUnknown=true)
  public static class NullInfo
  {
    public static final NullInfo INSTANCE = new NullInfo();

    private NullInfo()
    {
    }
  }

  @GET
  @Path("topics")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PubsubBroker.TopicSummary> getTopics(@QueryParam("laterThan") Long timestamp, @Context HttpServletRequest request)
  {
    LOG.debug("getTopics");
    if (timestamp == null) {
      timestamp = -1L;
    }
    return PubsubServer.getBroker().getTopics(timestamp);
  }

  @GET
  @Path("topics/{topic}")
  @Produces(MediaType.APPLICATION_JSON)
  public PubsubBroker.TimedTopicData getTopicData(@PathParam("topic") String topic, @QueryParam("laterThan") Long timestamp, @QueryParam("poll") Boolean poll, @Context HttpServletRequest request)
  {
    if (timestamp == null) {
      timestamp = -1L;
    }
    //LOG.debug("topic: {} poll: {}", topic, poll);
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
    LOG.debug("topic: {} poll: {} data: {}", topic, poll, (data != null ? data.getData() : data));
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
