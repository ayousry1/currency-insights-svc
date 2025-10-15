package com.sumerge.currency;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CurrencyController {
    private static final Map<String, Map<String, Double>> cache = new ConcurrentHashMap<>();
    private static final Map<String, Integer> rateLimit = new ConcurrentHashMap<>();
    private static final Map<String, Long> rateLimitReset = new ConcurrentHashMap<>();
    private static final Map<String, String> refreshTokens = new ConcurrentHashMap<>();
    private static final List<String> SUPPORTED = Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD");
    private static final String EXCHANGE_API = "https://api.exchangerate-api.com/v4/latest/";
    private static final String FRANKFURTER_API = "https://api.frankfurter.app/latest?from=";
    private static final int RATE_LIMIT = 10;
    private static final int EXPIRY = 15 * 60 * 1000;
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final Map<String, String> userRoles = new HashMap<>();
    static {
        userRoles.put("user", ROLE_USER);
        userRoles.put("admin", ROLE_ADMIN);
    }
    private RestTemplate restTemplate = new RestTemplate();

    private boolean checkRateLimit(String user) {
        long now = System.currentTimeMillis();
        if (!rateLimitReset.containsKey(user) || now > rateLimitReset.get(user)) {
            rateLimitReset.put(user, now + 60_000);
            rateLimit.put(user, 0);
        }
        int count = rateLimit.getOrDefault(user, 0);
        if (count >= RATE_LIMIT) return false;
        rateLimit.put(user, count + 1);
        return true;
    }

    private boolean isAdmin(String token) {
        return getRole(token).equals(ROLE_ADMIN);
    }

    private String getRole(String token) {
        if (token == null) return "";
        if (token.contains("admin")) return ROLE_ADMIN;
        return ROLE_USER;
    }

    private boolean isTokenExpired(String token) {
        if (token == null) return true;
        try {
            String[] parts = token.split(":");
            long ts = Long.parseLong(parts[1]);
            return System.currentTimeMillis() > ts + EXPIRY;
        } catch (Exception e) {
            return true;
        }
    }

    private String generateToken(String user) {
        return user + ":" + System.currentTimeMillis();
    }

    private String generateRefreshToken(String user) {
        String uuid = UUID.randomUUID().toString();
        refreshTokens.put(user, uuid);
        return uuid;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestParam String user) {
        if (!userRoles.containsKey(user)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad user");
        String token = generateToken(user);
        String refresh = generateRefreshToken(user);
        Map<String, String> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("refreshToken", refresh);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<?> refresh(@RequestParam String user, @RequestParam String refreshToken) {
        if (!refreshTokens.containsKey(user) || !refreshTokens.get(user).equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad refresh token");
        }
        String token = generateToken(user);
        Map<String, String> resp = new HashMap<>();
        resp.put("token", token);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/rates/supported")
    public ResponseEntity<?> supported(@RequestHeader("Authorization") String token) {
        System.out.println("inside !!");
        if (isTokenExpired(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Expired token");
        if (!checkRateLimit(token)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit");
        return ResponseEntity.ok(SUPPORTED);
    }

    @GetMapping("/api/rates/latest/{base}")
    public ResponseEntity<?> latest(@RequestHeader("Authorization") String token, @PathVariable String base) {
        if (isTokenExpired(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Expired token");
        if (!checkRateLimit(token)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit");
        if (cache.containsKey(base)) return ResponseEntity.ok(cache.get(base));
        Map<String, Double> rates = fetchRates(base);
        cache.put(base, rates);
        return ResponseEntity.ok(rates);
    }

    @GetMapping("/api/rates/insights/{base}")
    public ResponseEntity<?> insights(@RequestHeader("Authorization") String token, @PathVariable String base) {
        if (isTokenExpired(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Expired token");
        if (!checkRateLimit(token)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit");
        Map<String, Double> rates = cache.getOrDefault(base, fetchRates(base));
        cache.put(base, rates);
        Map<String, Object> result = calculateInsights(rates);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/api/admin/refresh")
    public ResponseEntity<?> refreshCache(@RequestHeader("Authorization") String token) {
        if (!isAdmin(token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not admin");
        cache.clear();
        return ResponseEntity.ok("Cache refreshed");
    }

    private Map<String, Double> fetchRates(String base) {
        try {
            String url = EXCHANGE_API + base;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            Map<String, Double> rates = (Map<String, Double>) resp.get("rates");
            return rates;
        } catch (Exception e) {
            try {
                String url = FRANKFURTER_API + base;
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                Map<String, Double> rates = (Map<String, Double>) resp.get("rates");
                return rates;
            } catch (Exception ex) {
                return new HashMap<>();
            }
        }
    }

    private Map<String, Object> calculateInsights(Map<String, Double> rates) {
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(rates.entrySet());
        sorted.sort(Map.Entry.comparingByValue());
        List<String> strongest = new ArrayList<>();
        List<String> weakest = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            if (i < 3) strongest.add(sorted.get(i).getKey());
            if (i >= sorted.size() - 3) weakest.add(sorted.get(i).getKey());
        }
        double avg = 0;
        for (double v : rates.values()) avg += v;
        avg = rates.size() == 0 ? 0 : avg / rates.size();
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (double v : rates.values()) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double vi = avg == 0 ? 0 : (max - min) / avg;
        double css = 100 - (vi * 100);
        if (css < 0) css = 0;
        if (css > 100) css = 100;
        String rec = css > 85 ? "Very stable currency base." : css >= 60 ? "Moderately stable base currency." : "Volatile base currency, exercise caution.";
        Map<String, Object> result = new HashMap<>();
        result.put("strongest", strongest);
        result.put("weakest", weakest);
        result.put("avgRate", avg);
        result.put("volatilityIndex", vi);
        result.put("currencyStabilityScore", css);
        result.put("recommendation", rec);
        return result;
    }
}

