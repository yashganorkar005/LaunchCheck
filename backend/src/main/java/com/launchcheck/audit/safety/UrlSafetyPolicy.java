package com.launchcheck.audit.safety;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class UrlSafetyPolicy {
    private static final Set<String> SCHEMES = Set.of("http", "https");
    private final Predicate<InetAddress> addressPredicate;
    private final AddressResolver resolver;

    public UrlSafetyPolicy(GlobalAddressPolicy addressPolicy, AddressResolver resolver) {
        this(addressPolicy::isPermitted, resolver);
    }

    public UrlSafetyPolicy(Predicate<InetAddress> addressPredicate, AddressResolver resolver) {
        this.addressPredicate = addressPredicate;
        this.resolver = resolver;
    }

    public TargetUrl parseAndValidate(String raw) {
        URI uri;
        try { uri = URI.create(raw); } catch (IllegalArgumentException exception) { throw new UnsafeTargetException("MALFORMED_URL"); }
        if (uri.getUserInfo() != null || uri.getHost() == null || !SCHEMES.contains(uri.getScheme()) || isIpLiteral(uri.getHost())) throw new UnsafeTargetException("UNSAFE_URL");
        int port = uri.getPort() == -1 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
        if ((!"http".equals(uri.getScheme()) || port != 80) && (!"https".equals(uri.getScheme()) || port != 443)) throw new UnsafeTargetException("UNSUPPORTED_PORT");
        String rawPath = uri.getRawPath();
        if (rawPath == null || rawPath.isEmpty()) {
            rawPath = "/";
        }
        String path = rawPath + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        return new TargetUrl(uri.getScheme().toLowerCase(Locale.ROOT), uri.getHost().toLowerCase(Locale.ROOT), port, path);
    }

    public List<InetAddress> resolveAndValidate(TargetUrl target) {
        try {
            List<InetAddress> resolved = resolver.resolve(target.host());
            if (resolved.isEmpty() || resolved.stream().anyMatch(address -> !addressPredicate.test(address))) throw new UnsafeTargetException("UNSAFE_DESTINATION");
            return List.copyOf(resolved);
        } catch (java.net.UnknownHostException exception) {
            throw new UnsafeTargetException("DNS_FAILURE");
        }
    }

    private boolean isIpLiteral(String host) {
        return host.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.') || host.contains(":");
    }
}
