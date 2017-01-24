# Atrato Pub/Sub Server

## How to test

* Run the server
```bash
mvn exec:java
```

* Run Chrome and install the "Simple WebSocket Client" extension

* Open the "Simple WebSocket Client" extension

* In "Server Location", type `ws://localhost:8890/pubsub` as the URL, Open

* Try subscribing to a topic

  * In the Request box, type
  
```json
{"type":"subscribe", "topic":"x.y.z"}
```

  * Press the Send button
  
* Try publishing data to a topic

  * Copy the chrome-extension URL in your browser
  * Open a new Chrome window
  * Paste the URL 
  * In "Server Location", type `ws://localhost:8890/pubsub` as the URL, Open
  * (Now you have a separate pubsub client)
  * In the Request box, type
  
```json
{"type":"publish", "topic":"x.y.z", "data":"HELLO WORLD" }
```

* Verify the other client gets the message

  * Go back to the previous WebSocket Client window and see if the Message Log box contains the HELLO WORLD message
  
## Notes

* Simple WebSocket Client does not detect closed connection by the server. If you restart the server, you will have to close and open the connection again.