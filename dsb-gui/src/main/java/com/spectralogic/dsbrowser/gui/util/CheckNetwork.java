/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CheckNetwork {
    private static final Logger LOG = LoggerFactory.getLogger(CheckNetwork.class);
    private static final Pattern REPLACE = Pattern.compile(Constants.HTTPS_PREFIX, Pattern.LITERAL);

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
            return REPLACE.matcher(endpoint).replaceAll(Matcher.quoteReplacement(Constants.HTTP_PREFIX));
        } else if (endpoint.startsWith(Constants.HTTP_PREFIX)) {
            return endpoint;
        } else {
            return "http://" + endpoint;
        }
    }
}
