package io.github.jmkeyes.curator.ensemble;

import org.apache.curator.utils.CloseableUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class KubernetesEnsembleProviderTest {
    private static final String CONNECTION_STRING =  "zookeeper:2181/chroot";
    private static final String[] HOSTS = {"127.0.0.1", "127.0.0.2", "127.0.0.3"};

    @Test
    public void testResolvesConnectionString() {
        final KubernetesEnsembleProvider ensembleProvider = new KubernetesEnsembleProvider(CONNECTION_STRING);

        // Override the resolver to provide our stub data.
        ensembleProvider.setResolver((host) -> {
            if (!Objects.equals(host, "zookeeper")) {
                fail("Asked to resolve host we didn't provide!");
            }

            return List.of(HOSTS);
        });

        try {
            final String connStr = ensembleProvider.getConnectionString();
            final String[] components = connStr.split("/");

            assertEquals(2, components.length);
            assertEquals("chroot", components[1]);

            final String[] hosts = components[0].split(",");

            for (int i = 0; i < hosts.length; i++) {
                String[] hostAndPort = hosts[i].split(":");
                assertEquals(hostAndPort[0], HOSTS[i]);
                assertEquals(hostAndPort[1], "2181");
            }
        } finally {
            CloseableUtils.closeQuietly(ensembleProvider);
        }
    }
}
