package com.github.wolfforgan.common.http.server;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SystemUtils;

@Log4j2
public class NettyUtil {
    public static InetSocketAddress startServiceOnPort(int startPort, Function<Integer, InetSocketAddress> startService) throws Exception {
        return startServiceOnPort(startPort, startService, "", 128);
    }

    public static InetSocketAddress startServiceOnPort(int startPort, Function<Integer, InetSocketAddress> startService, String serverName) throws Exception {
        return startServiceOnPort(startPort, startService, serverName, 128);
    }

    /**
     * start service, if port is collision, retry 128 times
     * Tip: this function is copy from spark: org.apache.spark.util.Utils.scala#L2172
     * @param startPort
     * @param startService
     * @param maxRetries
     * @param serviceName
     * @return
     * @throws RuntimeException
     */
    public static InetSocketAddress startServiceOnPort(int startPort, Function<Integer, InetSocketAddress> startService, String serviceName, int maxRetries) throws Exception {
        if (startPort != 0 && (startPort < 1024 || startPort > 65536)) {
            throw new RuntimeException("startPort should be between 1024 and 65535 (inclusive), or 0 for a random free port.");
        }
        String serviceString = serviceName.isEmpty() ? "'?'" : "'" + serviceName + "'";
        for (int offset = 0; offset <= maxRetries; offset++) {
            // Do not increment port if startPort is 0, which is treated as a special port
            int tryPort = startPort == 0 ? startPort :
                    // If the new port wraps around, do not try a privilege port
                    ((startPort + offset - 1024) % (65536 - 1024)) + 1024;
            try {
                InetSocketAddress result = startService.apply(tryPort);
            } catch (RuntimeException e) {
                if (isBindCollision(e)) {
                    if (offset >= maxRetries) {
                        String exceptionMessage = String.format("%s: Service%s failed after " +
                                "%d retries! Consider explicitly setting the appropriate port for the " +
                                "service%s (for example spark.ui.port for SparkUI) to an available " +
                                "port or increasing spark.port.maxRetries.", e.getMessage(), serviceString, maxRetries, serviceString);
                        BindException exception = new BindException(exceptionMessage);
                        // restore original stack trace
                        exception.setStackTrace(e.getStackTrace());
                        throw exception;
                    }
                } else if (isNacosException(e)) {
                    throw new RuntimeException("fail to register", e.getCause());
                }
                log.warn("Service {} could not bind on port {}. Attempting port {}.", serviceString, tryPort, tryPort + 1);
            }
        }
        // Should never happen
        throw new RuntimeException(String.format("Failed to start service%s on port {}", serviceString));
    }

    public static InetAddress findLocalInetAddress() throws UnknownHostException, SocketException {
        InetAddress address = InetAddress.getLocalHost();
        if (address.isLoopbackAddress()) {
            List<NetworkInterface> activeNetworkIFs = Collections.list(NetworkInterface.getNetworkInterfaces());
            List<NetworkInterface> reOrderedNetworkIFs;
            if (SystemUtils.IS_OS_WINDOWS) {
                reOrderedNetworkIFs = activeNetworkIFs;
            } else {
                List<NetworkInterface> reversedNetworkIFs = new ArrayList<>(activeNetworkIFs);
                Collections.reverse(reversedNetworkIFs);
                reOrderedNetworkIFs = reversedNetworkIFs;
            }
            return reOrderedNetworkIFs.stream()
                    .filter(ni -> Collections.list(ni.getInetAddresses()).stream().anyMatch(ad -> !ad.isLinkLocalAddress() && !ad.isLoopbackAddress()))
                    .map(ni -> {
                        List<InetAddress> addresses = Collections.list(ni.getInetAddresses()).stream()
                                .filter(inetAddress -> !inetAddress.isLinkLocalAddress() && !inetAddress.isLoopbackAddress()).collect(Collectors.toList());

                        byte[] address1 = addresses.stream().filter(inetAddress -> inetAddress instanceof Inet4Address)
                                .findFirst().orElseGet(() -> addresses.get(0)).getAddress();
                        try {
                            return InetAddress.getByAddress(address1);
                        } catch (UnknownHostException e) {
                            return addresses.get(0);
                        }
                    }).findFirst().orElse(address);
        }
        return address;
    }

    private static boolean isBindCollision(Throwable throwable) {
        if (throwable instanceof BindException) {
            if (throwable.getMessage() != null) {
                return true;
            } else {
                return isBindCollision(throwable.getCause());
            }
        } else if (throwable instanceof Exception) {
            return isBindCollision(throwable.getCause());
        } else {
            return false;
        }
    }

    private static boolean isNacosException(Throwable throwable) {
        if (throwable instanceof NacosException) {
            if (throwable.getMessage() != null) {
                return true;
            } else {
                return isNacosException(throwable.getCause());
            }
        } else if (throwable instanceof Exception) {
            return isNacosException(throwable.getCause());
        } else {
            return false;
        }
    }
}
