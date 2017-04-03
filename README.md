# Atrato Pub/Sub Server

Atrato Pub/Sub Server supports both WebSocket and HTTP polling for pub/sub. 

## WebSocket

### Publishing

The publish request is in this format:
```json
{"type":"publish", "topic":"{topic}", "data":{json-data}}
```

This publishes the {json-data} to the topic {topic}

### Subscribing

The subscribe request is this format:
```json
{"type":"subscribe", "topic":"{topic}"}
```

When the data for the given topic is available, the client will receive the data in the form of:
```json
{"type":"data", "topic":"{topic}", "data":{json-data}, "timestamp":{timestamp}}
```

## HTTP

### Publishing
```
curl -XPOST -H 'Content-Type: application/json' -d '{json-data}' 'http://localhost:8890/ws/v1/pubsub/topics/{topic}'
```

This publishes the {json-data} to the topic {topic}.

### Getting data

```
curl 'http://localhost:8890/ws/v1/pubsub/topics/{topic}?poll={true/false}&laterThan={timestampMillis}'
```

This returns the latest data on the given topic. If {timestampMillis} is specified, it will only return the topic
data if the data is newer than the specified timestamp.

If {poll} is false, it will return 404 if the topic does not exist or if the topic does not contain data that is later
then {timestampMillis}. Otherwise, it will suspend the response until a given topic has data.

Note that by default, a topic will expire after 60 minutes if no messages have been published to the given topic.

## How to build and test

* Build the project

        mvn package


* Run the server

        mvn exec:java


* Run Chrome and install the "Simple WebSocket Client" extension

* Open the "Simple WebSocket Client" extension

* In "Server Location", type `ws://localhost:8890/pubsub` as the URL, Open

* Try subscribing to a topic

  * In the Request box, type
  
        {"type":"subscribe", "topic":"x.y.z"}

  * Press the Send button
  
* Try publishing data to a topic

  * Copy the chrome-extension URL in your browser
  * Open a new Chrome window
  * Paste the URL 
  * In "Server Location", type `ws://localhost:8890/pubsub` as the URL, Open
  * (Now you have a separate pubsub client)
  * In the Request box, type

        {"type":"publish", "topic":"x.y.z", "data":"HELLO WORLD" }

* Verify the other client gets the message

  * Go back to the previous WebSocket Client window and see if the Message Log box contains the HELLO WORLD message

* Verify getting data using HTTP in a similar way.

  * Getting data, always returns immediately

        curl 'http://localhost:8890/ws/v1/pubsub/topics/x.y.z'

  
  * Getting data, poll until data is available (use a timestamp that makes sense)

        curl 'http://localhost:8890/ws/v1/pubsub/topics/x.y.z?poll=true&laterThan=1485502388000'


* Verify HTTP publishing
 
  * Either use HTTP or WebSocket to subscribe to topic "x.y.z" (see above).
  * Then publish a piece of data to topic "x.y.z": 

        curl -XPOST -H 'Content-Type: application/json' -d '{ "hello": "world" }' 'http://localhost:8890/ws/v1/pubsub/topics/x.y.z'
  * Verify the client is getting the data.

## Notes

* Simple WebSocket Client does not detect closed connection by the server. If you restart the server, you will have to close and open the connection again.
