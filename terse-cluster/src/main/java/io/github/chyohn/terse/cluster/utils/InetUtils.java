package io.github.chyohn.terse.cluster.utils;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Objects;

@Slf4j
public class InetUtils {

    public static final boolean PREFER_IPV6_ADDRESSES = Boolean.parseBoolean(
        System.getProperty("java.net.preferIPv6Addresses"));

    @Getter
    private static String selfIp;

    static {
        selfIp = getLocalIP();
    }

    public static void main(String[] args) {
        System.out.println(selfIp);
    }

    public static String getLocalIP() {

        String localIP = Objects.requireNonNull(findFirstNonLoopbackAddress()).getHostAddress();

        if (PREFER_IPV6_ADDRESSES && !localIP.startsWith(IPV6_START_MARK)
            && !localIP.endsWith(IPV6_END_MARK)) {
            localIP = IPV6_START_MARK + localIP + IPV6_END_MARK;
            if (localIP.contains(PERCENT_SIGN_IN_IPV6)) {
                localIP = localIP.substring(0, localIP.indexOf(PERCENT_SIGN_IN_IPV6))
                    + IPV6_END_MARK;
            }
        }
        return localIP;
    }

    public static InetAddress findFirstNonLoopbackAddress() {

        try {
            for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
                nics.hasMoreElements(); ) {
                NetworkInterface ifc = nics.nextElement();
                if (!ifc.isUp()) {
                    continue;
                }
                for (Enumeration<InetAddress> addrs = ifc.getInetAddresses(); addrs.hasMoreElements(); ) {
                    InetAddress address = addrs.nextElement();
                    boolean isLegalIpVersion =
                        PREFER_IPV6_ADDRESSES ? address instanceof Inet6Address
                            : address instanceof Inet4Address;
                    if (isLegalIpVersion && !address.isLoopbackAddress() && !address.isSiteLocalAddress()) {
                        return address;
                    }
                }
            }
        } catch (IOException ex) {
            log.error("Cannot get first non-loopback address", ex);
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.error("Unable to retrieve localhost", e);
        }

        return null;
    }

    public static final String IPV6_START_MARK = "[";
    public static final String IPV6_END_MARK = "]";
    public static final String PERCENT_SIGN_IN_IPV6 = "%";

    public static final String IP_PORT_SPLITER = ":";

    public static String[] splitIPPortStr(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("ip and port string cannot be empty!");
        }
        String[] serverAddrArr;
        if (str.startsWith(IPV6_START_MARK) && str.contains(IPV6_END_MARK)) {
            if (str.endsWith(IPV6_END_MARK)) {
                serverAddrArr = new String[1];
                serverAddrArr[0] = str;
            } else {
                serverAddrArr = new String[2];
                serverAddrArr[0] = str.substring(0, (str.indexOf(IPV6_END_MARK) + 1));
                serverAddrArr[1] = str.substring((str.indexOf(IPV6_END_MARK) + 2));
            }
        } else {
            serverAddrArr = str.split(IP_PORT_SPLITER);
        }
        return serverAddrArr;
    }
}
