package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


public class CheckNetwork {

    public static boolean isReachable(final Ds3Client client) {
        try {
            final URL url = new URL("http://" + client.getConnectionDetails().getEndpoint());
            final URLConnection conn = url.openConnection();
            conn.connect();
            return true;
        } catch (final MalformedURLException e) {
            return false;
        } catch (final IOException e) {
            return false;
        }
    }
}
