package com.carcompare;

import org.springframework.stereotype.Service;
import java.util.*;

/** Computes a 0-100 recommendation score per car from the user's budget and preference weights. */
@Service
public class RecommendationService {

    public record Request(Double budgetLakh, String fuelType, Integer minSeats, String bodyType,
                          Map<String, Integer> weights, String profile) {}
    public record Result(Car car, int score, List<String> reasons, double costPerKm, int emi) {}

    private static Map<String, Integer> preset(String profile) {
        return switch (profile == null ? "" : profile) {
            case "student" -> Map.of("price", 5, "mileage", 5, "performance", 1, "safety", 2, "space", 1);
            case "family"  -> Map.of("price", 2, "mileage", 3, "performance", 1, "safety", 5, "space", 5);
            case "enthusiast" -> Map.of("price", 1, "mileage", 1, "performance", 5, "safety", 3, "space", 1);
            default -> Map.of("price", 3, "mileage", 3, "performance", 3, "safety", 3, "space", 3);
        };
    }

    private static final Map<String, Double> FUEL_RATE = Map.of("Petrol", 105.0, "Diesel", 92.0, "CNG", 90.0, "Hybrid", 105.0, "Electric", 9.0);
    /** Running cost in rupees per km, so EVs, CNG and diesel are compared fairly (raw mileage units differ). */
    static double costPerKm(Car c) { return FUEL_RATE.getOrDefault(c.getFuelType(), 100.0) / c.getMileage(); }
    /** Monthly EMI for 80% loan, 9% interest, 5 years. */
    static int emi(double lakh) {
        double p = lakh * 1e5 * 0.8, r = 0.09 / 12; int n = 60;
        return (int) Math.round(p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1));
    }

    public List<Result> recommend(Request r, List<Car> all) {
        Map<String, Integer> w = new HashMap<>(preset(r.profile()));
        if (r.weights() != null) w.putAll(r.weights());
        double sum = w.values().stream().mapToInt(Integer::intValue).sum();
        if (sum == 0) sum = 1;

        List<Car> pool = all.stream()
                .filter(c -> r.fuelType() == null || r.fuelType().isBlank() || c.getFuelType().equalsIgnoreCase(r.fuelType()))
                .filter(c -> r.minSeats() == null || c.getSeats() >= r.minSeats())
                .filter(c -> r.bodyType() == null || r.bodyType().isBlank() || c.getBodyType().equalsIgnoreCase(r.bodyType()))
                .toList();
        if (pool.isEmpty()) return List.of();

        double minP = pool.stream().mapToDouble(Car::getPriceLakh).min().orElse(0), maxP = pool.stream().mapToDouble(Car::getPriceLakh).max().orElse(1);
        double minCost = pool.stream().mapToDouble(RecommendationService::costPerKm).min().orElse(1);
        double maxPw = pool.stream().mapToInt(Car::getPowerBhp).max().orElse(1);
        double maxB = pool.stream().mapToInt(Car::getBootLitres).max().orElse(1);

        List<Result> out = new ArrayList<>();
        for (Car c : pool) {
            double price = r.budgetLakh() != null && r.budgetLakh() > 0
                    ? Math.max(0, 1 - c.getPriceLakh() / r.budgetLakh() * 0.8)  // cheaper relative to budget is better
                    : (maxP == minP ? 1 : 1 - (c.getPriceLakh() - minP) / (maxP - minP));
            double mileage = minCost / costPerKm(c);
            double perf = c.getPowerBhp() / maxPw;
            double safety = c.getSafetyRating() / 5.0;
            double space = (c.getSeats() / 7.0 + c.getBootLitres() / maxB) / 2;
            double score = (price * w.get("price") + mileage * w.get("mileage") + perf * w.get("performance")
                    + safety * w.get("safety") + space * w.get("space")) / sum * 100;

            List<String> reasons = new ArrayList<>();
            boolean over = r.budgetLakh() != null && r.budgetLakh() > 0 && c.getPriceLakh() > r.budgetLakh();
            if (over) {
                double overBy = (c.getPriceLakh() - r.budgetLakh()) / r.budgetLakh();
                score *= Math.max(0.1, 1 - overBy * 3);  // heavy penalty for exceeding budget
                reasons.add(String.format("⚠ ₹%.1fL over your budget", c.getPriceLakh() - r.budgetLakh()));
            } else if (r.budgetLakh() != null && r.budgetLakh() > 0) {
                reasons.add("Within budget");
            }
            if (safety >= 0.8) reasons.add(c.getSafetyRating() + "★ safety");
            if (mileage >= 0.8) reasons.add(String.format("Low running cost (₹%.1f/km)", costPerKm(c)));
            if (c.getSafetyRating() <= 2) reasons.add("⚠ Only " + c.getSafetyRating() + "★ safety");
            if (perf >= 0.8) reasons.add(c.getPowerBhp() + " bhp");
            if (space >= 0.75) reasons.add("Roomy cabin & boot");
            out.add(new Result(c, (int) Math.round(Math.min(100, score)), reasons, costPerKm(c), emi(c.getPriceLakh())));
        }
        out.sort(Comparator.<Result>comparingInt(Result::score).reversed().thenComparingDouble(x -> x.car().getPriceLakh()));
        return out.stream().limit(10).toList();
    }
}