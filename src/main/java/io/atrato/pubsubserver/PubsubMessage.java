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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by david on 1/23/17.
 */
public class PubsubMessage
{
  enum Type
  {
    PUBLISH("publish"),
    SUBSCRIBE("subscribe"),
    UNSUBSCRIBE("unsubscribe"),
    DATA("data");

    private final String name;

    Type(String s)
    {
      name = s;
    }

    @Override
    public String toString()
    {
      return this.name;
    }
  }

  private Type type;
  private String topic;
  private JsonNode data;
  private long timestamp;

  public PubsubMessage()
  {
  }

  public PubsubMessage(Type type, String topic, JsonNode data, long timestamp)
  {
    this.type = type;
    this.topic = topic;
    this.data = data;
    this.timestamp = timestamp;
  }

  public Type getType()
  {
    return type;
  }

  public String getTopic()
  {
    return topic;
  }

  public JsonNode getData()
  {
    return data;
  }

  public long getTimestamp()
  {
    return timestamp;
  }

  public void setType(Type type)
  {
    this.type = type;
  }

  public void setTopic(String topic)
  {
    this.topic = topic;
  }

  public void setData(JsonNode data)
  {
    this.data = data;
  }

  public void setTimestamp(long timestamp)
  {
    this.timestamp = timestamp;
  }
}
