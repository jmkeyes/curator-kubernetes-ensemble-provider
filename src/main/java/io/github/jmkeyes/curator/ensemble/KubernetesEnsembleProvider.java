package io.github.jmkeyes.curator.ensemble;

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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This {@link EnsembleProvider} preemptively resolves each of the hosts named
 * in the connection string to their respective IP addresses so the Curator
 * client can load balance between many members of the Zookeeper ensemble.
 */
public final class KubernetesEnsembleProvider implements EnsembleProvider {
    private static final Logger log = LoggerFactory.getLogger(KubernetesEnsembleProvider.class);

    private ConnectStringParser connectionStringParser;
    private Function<String, List<String>> resolver;

    /**
     * Initialize a new ensemble provider with the given connection string.
     *
     * @param connectionString The connection string provided to Curator.
     */
    public KubernetesEnsembleProvider(String connectionString) {
        setConnectionString(connectionString);
        setResolver((host) -> {
            try {
                return Arrays.stream(InetAddress.getAllByName(host))
                        .map(InetAddress::getHostAddress)
                        .toList();
            } catch (UnknownHostException e) {
                log.warn("Unresolvable host {} left in address set.", host, e);
                return List.of(host);
            }
        });
    }

    /**
     * Set the DNS resolver used by the {@link KubernetesEnsembleProvider}.
     * @param resolver A function receiving a hostname and returning a list of IPs.
     */
    public void setResolver(Function<String, List<String>> resolver) {
        this.resolver = resolver;
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
        // Iterate through the provided connection string and resolve hosts to IPs if possible.
        final SortedSet<String> addresses = connectionStringParser.getServerAddresses().stream()
                .flatMap(hostAndPort -> resolver.apply(hostAndPort.getHostName()).stream()
                        .map(address -> String.format("%s:%s", address, hostAndPort.getPort())))
                        .collect(Collectors.toCollection(TreeSet::new));

        // Build a new connection string from the resolved hosts.
        final StringBuilder connectStringBuilder = new StringBuilder();

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
