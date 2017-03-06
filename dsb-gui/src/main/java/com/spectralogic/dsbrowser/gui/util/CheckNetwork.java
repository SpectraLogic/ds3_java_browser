package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public final class CheckNetwork {
    private static final Logger LOG = LoggerFactory.getLogger(CheckNetwork.class);

    /**
     * Tests to see if the non-secure data path port is reachable
     *
     * @return true if the endpoint is reachable, false otherwise
     */
    public static boolean isReachable(final Ds3Client client) {
        try {
            final String formattedUrlString = formatUrl(client.getConnectionDetails().getEndpoint());
            final URL url = new URL(formattedUrlString);
            final URLConnection conn = url.openConnection();
            conn.connect();
            return true;
        } catch (final IOException e) {
            LOG.error("Encountered an error when determining if " + client.getConnectionDetails().getEndpoint() + " is reachable", e);
            return false;
        }
    }

    /**
     * Formats an URL string so that it always starts with 'http'
     */
    public static String formatUrl(final String endpoint) {
        if (endpoint.startsWith(Constants.HTTPS_PREFIX)) {
            return endpoint.replace(Constants.HTTPS_PREFIX, Constants.HTTP_PREFIX);
        } else if (endpoint.startsWith(Constants.HTTP_PREFIX)) {
            return endpoint;
        } else {
            return "http://" + endpoint;
        }
    }
}
