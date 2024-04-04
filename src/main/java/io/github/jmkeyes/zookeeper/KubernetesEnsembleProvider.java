package io.github.jmkeyes.zookeeper;

import com.google.common.base.Joiner;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.zookeeper.client.ConnectStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This {@link EnsembleProvider} preemptively resolves each of the hosts named
 * in the connection string to their respective IP addresses so the Curator
 * client can load balance between many members of the Zookeeper ensemble.
 */
public final class KubernetesEnsembleProvider implements EnsembleProvider {
    private static final Logger log = LoggerFactory.getLogger(KubernetesEnsembleProvider.class);

    private ConnectStringParser connectionStringParser;

    /**
     * Initialize a new ensemble provider with the given connection string.
     *
     * @param connectionString The connection string provided to Curator.
     */
    public KubernetesEnsembleProvider(String connectionString) {
        setConnectionString(connectionString);
    }

    @Override
    public void start() {
        // Nothing to see here.
    }

    @Override
    public void close() {
        // Nothing to see here.
    }

    /**
     * Signal that we support updating the Zookeeper connection string.
     *
     * @return Always returns true.
     */
    @Override
    public boolean updateServerListEnabled() {
        return true;
    }

    /**
     * Parse the given connection string and store it for later use.
     *
     * @param connectionString The Curator/Zookeeper connection string.
     */
    @Override
    public void setConnectionString(String connectionString) {
        this.connectionStringParser = new ConnectStringParser(connectionString);
    }

    /**
     * Resolve the connection string into a list of IP address and port pairs.
     *
     * @return A Curator/Zookeeper connection string.
     */
    @Override
    public String getConnectionString() {
        final StringBuilder connectStringBuilder = new StringBuilder();
        final SortedSet<String> addresses = new TreeSet<>();

        // Iterate through all hosts provided in the connection string.
        connectionStringParser.getServerAddresses().forEach(hostAndPort -> {
            try {
                // Resolve each host into its IP addresses and append the port number to each one.
                List<String> resolvedHosts = Arrays.stream(InetAddress.getAllByName(hostAndPort.getHostName()))
                        .map(address -> String.format("%s:%s", address.getHostAddress(), hostAndPort.getPort()))
                        .toList();

                addresses.addAll(resolvedHosts);
            } catch (UnknownHostException e) {
                log.warn("Unresolvable host {} left in address set.", hostAndPort, e);
                addresses.add(hostAndPort.toString());
            }
        });

        // Join all the resolved addresses into a comma-separated list.
        Joiner.on(",").appendTo(connectStringBuilder, addresses);

        // Append the Zookeeper chroot if it was provided.
        if (connectionStringParser.getChrootPath() != null) {
            connectStringBuilder.append(connectionStringParser.getChrootPath());
        }

        // Return the fully resolved connection string.
        return connectStringBuilder.toString();
    }
}
