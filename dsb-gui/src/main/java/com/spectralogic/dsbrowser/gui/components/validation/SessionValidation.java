package com.spectralogic.dsbrowser.gui.components.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionValidation {

    /**
     * Check if the given String is a valid port number
     *
     * @param portNumber
     * @return
     */
    public static boolean validatePort(final String portNumber) {
        final Pattern pattern;
        final Matcher matcher;
        final String PORT_PATTERN = "^([0-9]+)$";
        pattern = Pattern.compile(PORT_PATTERN);

        if (checkStringEmptyNull(portNumber)) {
            matcher = pattern.matcher(portNumber);
            return matcher.matches();
        }
        return false;
    }

    /**
     * Check session name is not empty and not null
     *
     * @param value
     * @return Flag
     */
    public static boolean checkStringEmptyNull(final String value) {
        boolean flag = false;

        if (value != null && !value.trim().isEmpty()) {
            flag = true;
            return flag;
        }
        return flag;
    }

    /**
     * Validating Text Field with IP Address
     *
     * @param ipAddress
     * @return
     */
    public static boolean validateIPAddress(final String ipAddress) {
        final Pattern pattern;
        final Matcher matcher;
        final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        pattern = Pattern.compile(IPADDRESS_PATTERN);

        if (checkStringEmptyNull(ipAddress)) {
            matcher = pattern.matcher(ipAddress);
            return matcher.matches();
        }
        return false;
    }
}
