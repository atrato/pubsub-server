package io.atrato.pubsubserver;

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
  private String data;

  public PubsubMessage()
  {
  }

  public PubsubMessage(Type type, String topic, String data)
  {
    this.type = type;
    this.topic = topic;
    this.data = data;
  }

  public Type getType()
  {
    return type;
  }

  public String getTopic()
  {
    return topic;
  }

  public String getData()
  {
    return data;
  }

  public void setType(Type type)
  {
    this.type = type;
  }

  public void setTopic(String topic)
  {
    this.topic = topic;
  }

  public void setData(String data)
  {
    this.data = data;
  }

}
