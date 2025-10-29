package com.sumerge.currency;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//todo: add export example
//todo: remove resttemplate and add feign client
//todo: hardcode the export format
//todo: remove the swagger
//todo:

@RestController
public class CurrencyController {
    private static final List<String> SUPPORTED = Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD");
    private static final String EXCHANGE_API = "https://api.exchangerate-api.com/v4/latest/";
    private static final String FRANKFURTER_API = "https://api.frankfurter.app/latest?from=";
    private RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/api/rates/supported")
    public ResponseEntity<?> supported() {
        System.out.println("inside !!");
        return ResponseEntity.ok(SUPPORTED);
    }

    @GetMapping("/api/rates/latest/{base}")
    public ResponseEntity<?> latest(@PathVariable String base) {
        Map<String, Double> rates = fetchRates(base);
        return ResponseEntity.ok(rates);
    }

    @GetMapping("/api/rates/insights/{base}")
    public ResponseEntity<?> insights(@PathVariable String base) {
        Map<String, Double> rates = fetchRates(base);
        Map<String, Object> result = calculateInsights(rates);
        return ResponseEntity.ok(result);
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
