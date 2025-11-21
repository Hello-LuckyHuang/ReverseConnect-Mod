package com.hxzhitang.reverseconnect.proxy.tools;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class IPAddressParser {

    // IPv4正则表达式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    // IPv6正则表达式（简化版）
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$|^([0-9a-fA-F]{1,4}:){1,7}:$|^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$|^:((:[0-9a-fA-F]{1,4}){1,7}|:)$");

    /**
     * 解析IP地址和端口
     * @param address 包含IP地址和端口的字符串，格式如："192.168.1.1:8080" 或 "[2001:db8::1]:8080"
     * @return IPAddressInfo 对象，包含解析后的信息
     * @throws IllegalArgumentException 如果地址格式无效
     */
    public static IPAddressInfo parseAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("地址不能为空");
        }

        String ipAddress;
        int port = -1; // -1 表示没有端口

        // 检查是否是IPv6地址（包含方括号）
        if (address.startsWith("[") && address.contains("]:")) {
            // IPv6地址格式: [2001:db8::1]:8080
            int bracketEnd = address.indexOf(']');
            if (bracketEnd == -1) {
                throw new IllegalArgumentException("IPv6地址缺少闭合方括号");
            }

            ipAddress = address.substring(1, bracketEnd);
            String portStr = address.substring(bracketEnd + 2); // 跳过"]:"

            try {
                port = Integer.parseInt(portStr);
                validatePort(port);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的端口号: " + portStr);
            }

            validateIPv6Address(ipAddress);

        } else if (address.contains(":")) {
            // 可能是IPv4:端口 或 IPv6地址（无端口）
            int lastColonIndex = address.lastIndexOf(':');

            // 尝试解析端口
            try {
                String possiblePort = address.substring(lastColonIndex + 1);
                port = Integer.parseInt(possiblePort);
                validatePort(port);

                // 如果成功解析端口，则剩余部分是IP地址
                ipAddress = address.substring(0, lastColonIndex);

                // 确定是IPv4还是IPv6
                if (isValidIPv4(ipAddress)) {
                    validateIPv4Address(ipAddress);
                } else {
                    validateIPv6Address(ipAddress);
                }

            } catch (IllegalArgumentException e) {
                // 如果没有端口，则整个字符串是IPv6地址
                ipAddress = address;
                validateIPv6Address(ipAddress);
            }
        } else {
            // 只有IP地址，没有端口
            ipAddress = address;
            if (isValidIPv4(ipAddress)) {
                validateIPv4Address(ipAddress);
            } else {
                throw new IllegalArgumentException("无效的IP地址格式: " + address);
            }
        }

        return new IPAddressInfo(ipAddress, port);
    }

    /**
     * 验证IPv4地址
     */
    private static void validateIPv4Address(String ipAddress) {
        if (!isValidIPv4(ipAddress)) {
            throw new IllegalArgumentException("无效的IPv4地址: " + ipAddress);
        }

        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            if (!(addr instanceof Inet4Address)) {
                throw new IllegalArgumentException("不是有效的IPv4地址: " + ipAddress);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析IPv4地址: " + ipAddress);
        }
    }

    /**
     * 验证IPv6地址
     */
    private static void validateIPv6Address(String ipAddress) {
        if (!isValidIPv6(ipAddress)) {
            throw new IllegalArgumentException("无效的IPv6地址: " + ipAddress);
        }

        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            if (!(addr instanceof Inet6Address)) {
                throw new IllegalArgumentException("不是有效的IPv6地址: " + ipAddress);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无法解析IPv6地址: " + ipAddress);
        }
    }

    /**
     * 验证端口号
     */
    private static void validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("端口号必须在0-65535之间: " + port);
        }
    }

    /**
     * 检查是否是有效的IPv4地址
     */
    public static boolean isValidIPv4(String ipAddress) {
        return IPV4_PATTERN.matcher(ipAddress).matches();
    }

    /**
     * 检查是否是有效的IPv6地址
     */
    public static boolean isValidIPv6(String ipAddress) {
        return IPV6_PATTERN.matcher(ipAddress).matches();
    }

    /**
     * IP地址信息类
     */
    public static class IPAddressInfo {
        private final String ipAddress;
        private final int port;

        public IPAddressInfo(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }

        public boolean hasPort() {
            return port != -1;
        }

        public boolean isIPv4() {
            return isValidIPv4(ipAddress);
        }

        public boolean isIPv6() {
            return isValidIPv6(ipAddress);
        }

        @Override
        public String toString() {
            if (hasPort()) {
                if (isIPv6()) {
                    return "[" + ipAddress + "]:" + port;
                } else {
                    return ipAddress + ":" + port;
                }
            } else {
                return ipAddress;
            }
        }
    }
}
