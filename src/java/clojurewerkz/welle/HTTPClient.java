package clojurewerkz.welle;

import com.basho.riak.client.http.RiakClient;
import com.basho.riak.client.http.RiakConfig;
import com.basho.riak.client.raw.http.HTTPClientAdapter;

/**
 * Just like {@link com.basho.riak.client.raw.http.HTTPClientAdapter} but exposes its configuration
 */
public class HTTPClient extends HTTPClientAdapter {
  protected final RiakConfig config;

  public HTTPClient(RiakClient client) {
    super(client);
    this.config = client.getConfig();
  }

  public RiakConfig getConfig() {
    return config;
  }

  public String getBaseUrl() {
    return config.getBaseUrl();
  }
}
