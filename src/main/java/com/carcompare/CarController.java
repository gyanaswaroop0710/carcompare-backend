package com.carcompare;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CarController {
    private final CarRepository cars;
    private final FavoriteRepository favs;
    private final RecommendationService reco;

    public CarController(CarRepository cars, FavoriteRepository favs, RecommendationService reco) {
        this.cars = cars; this.favs = favs; this.reco = reco;
    }

    @GetMapping("/cars")
    public List<Car> list(@RequestParam(required = false) String q, @RequestParam(required = false) String brand,
                          @RequestParam(required = false) String fuelType, @RequestParam(required = false) String transmission,
                          @RequestParam(required = false) String bodyType, @RequestParam(required = false) Double minPrice,
                          @RequestParam(required = false) Double maxPrice, @RequestParam(required = false) Integer minSeats,
                          @RequestParam(required = false) String segment, @RequestParam(required = false) String transType,
                          @RequestParam(defaultValue = "priceLakh") String sort, @RequestParam(defaultValue = "asc") String dir) {
        Specification<Car> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("brand")), like), cb.like(cb.lower(root.get("model")), like)));
            }
            if (notBlank(brand)) p.add(cb.equal(cb.lower(root.get("brand")), brand.toLowerCase()));
            if (notBlank(fuelType)) p.add(cb.equal(cb.lower(root.get("fuelType")), fuelType.toLowerCase()));
            if (notBlank(transmission)) p.add(cb.equal(cb.lower(root.get("transmission")), transmission.toLowerCase()));
            if (notBlank(bodyType)) p.add(cb.equal(cb.lower(root.get("bodyType")), bodyType.toLowerCase()));
            if (notBlank(segment)) p.add(cb.equal(cb.lower(root.get("segment")), segment.toLowerCase()));
            if (notBlank(transType)) p.add(cb.equal(cb.lower(root.get("transmissionType")), transType.toLowerCase()));
            if (minPrice != null) p.add(cb.greaterThanOrEqualTo(root.<Double>get("priceLakh"), minPrice));
            if (maxPrice != null) p.add(cb.lessThanOrEqualTo(root.<Double>get("priceLakh"), maxPrice));
            if (minSeats != null) p.add(cb.greaterThanOrEqualTo(root.<Integer>get("seats"), minSeats));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Set<String> allowed = Set.of("priceLakh", "mileage", "powerBhp", "torqueNm", "engineCc", "safetyRating", "brand");
        String field = allowed.contains(sort) ? sort : "priceLakh";
        return cars.findAll(spec, Sort.by("desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC, field));
    }

    @GetMapping("/cars/{id}")
    public Car get(@PathVariable Long id) {
        return cars.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));
    }

    /** Up to 3 alternatives within 30% of this car's price, preferring the same body type. */
    @GetMapping("/cars/{id}/similar")
    public List<Car> similar(@PathVariable Long id) {
        Car base = get(id);
        return cars.findAll().stream()
                .filter(c -> !c.getId().equals(id) && Math.abs(c.getPriceLakh() - base.getPriceLakh()) <= base.getPriceLakh() * 0.3)
                .sorted(Comparator.comparingDouble((Car c) -> Math.abs(c.getPriceLakh() - base.getPriceLakh()) + (c.getBodyType().equals(base.getBodyType()) ? 0 : 3)))
                .limit(3).toList();
    }

    @GetMapping("/filters")
    public Map<String, Object> filters() {
        List<Car> all = cars.findAll();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("brands", all.stream().map(Car::getBrand).distinct().sorted().toList());
        m.put("fuelTypes", all.stream().map(Car::getFuelType).distinct().sorted().toList());
        m.put("transmissions", all.stream().map(Car::getTransmission).distinct().sorted().toList());
        m.put("bodyTypes", all.stream().map(Car::getBodyType).distinct().sorted().toList());
        m.put("segments", all.stream().map(Car::getSegment).filter(Objects::nonNull).distinct().sorted().toList());
        m.put("transmissionTypes", all.stream().map(Car::getTransmissionType).filter(Objects::nonNull).distinct().sorted().toList());
        return m;
    }

    @GetMapping("/compare")
    public List<Car> compare(@RequestParam List<Long> ids) {
        if (ids.size() < 2 || ids.size() > 4)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select 2 to 4 cars to compare");
        return cars.findAllById(ids);
    }

    @PostMapping("/recommendations")
    public List<RecommendationService.Result> recommend(@RequestBody RecommendationService.Request req) {
        return reco.recommend(req, cars.findAll());
    }

    // Favorites belong to the logged-in user (Bearer token required — see SecurityConfig).
    @GetMapping("/favorites")
    public List<Car> favorites(@AuthenticationPrincipal AppUser user) {
        List<Long> ids = favs.findByUserId(user.getId()).stream().map(Favorite::getCarId).toList();
        return cars.findAllById(ids);
    }

    @PostMapping("/favorites/{carId}")
    public void addFav(@AuthenticationPrincipal AppUser user, @PathVariable Long carId) {
        if (!cars.existsById(carId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found");
        if (favs.existsByUserIdAndCarId(user.getId(), carId)) return;
        try { favs.save(new Favorite(user.getId(), carId)); }
        catch (DataIntegrityViolationException ignored) { /* double-click race: already saved */ }
    }

    @DeleteMapping("/favorites/{carId}")
    @org.springframework.transaction.annotation.Transactional
    public void removeFav(@AuthenticationPrincipal AppUser user, @PathVariable Long carId) {
        favs.deleteByUserIdAndCarId(user.getId(), carId);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}