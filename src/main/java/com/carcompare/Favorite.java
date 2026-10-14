package com.carcompare;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "car_id"}))
public class Favorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "car_id", nullable = false) private Long carId;
    public Favorite() {}
    public Favorite(Long userId, Long carId) { this.userId = userId; this.carId = carId; }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCarId() { return carId; }
}
