package io.atrato.pubsubserver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by david on 1/23/17.
 */
public class PubsubServer
{
  private static final String DEFAULT_HOST = "0.0.0.0";
  private static final int DEFAULT_PORT = 8890;

  private static final String CMD_OPTION_LISTEN_ADDRESS = "listenAddress";

  private String host = DEFAULT_HOST;
  private int port = DEFAULT_PORT;

  public static class PubsubServlet extends WebSocketServlet
  {
    @Override
    public void configure(WebSocketServletFactory factory)
    {
      factory.getPolicy().setIdleTimeout(1000000);
      factory.register(PubsubSocket.class);
    }
  }

  void init(String[] args) throws ParseException
  {
    Options options = new Options();
    options.addOption(CMD_OPTION_LISTEN_ADDRESS, true, "Address to listen to. Default is " + DEFAULT_HOST + ":" + DEFAULT_PORT);

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    String listenAddress = cmd.getOptionValue(CMD_OPTION_LISTEN_ADDRESS);

    if (listenAddress != null) {
      Pattern pattern = Pattern.compile("(.+:)?(\\d+)");
      Matcher matcher = pattern.matcher(listenAddress);

      if (matcher.find()) {
        String hostString = matcher.group(1);
        if (hostString != null) {
          host = hostString.substring(0, hostString.length() - 1);
        }
        port = Integer.valueOf(matcher.group(2));
      } else {
        throw new ParseException("listenAddress must be in this format: [host:]port");
      }
    }

  }

  void run() throws Exception
  {
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

    ServletHolder wsServlet = new ServletHolder(PubsubServlet.class);
    context.addServlet(wsServlet, "/pubsub");

    InetSocketAddress address = new InetSocketAddress(host, port);
    Server jettyServer = new Server(address);
    jettyServer.setHandler(context);

    try {
      jettyServer.start();
      jettyServer.join();
    } finally {
      jettyServer.destroy();
    }
  }

  public static void main(String[] args) throws Exception
  {
    PubsubServer server = new PubsubServer();
    server.init(args);
    server.run();
  }
}
