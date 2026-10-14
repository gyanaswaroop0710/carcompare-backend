package com.carcompare;

import jakarta.persistence.*;

@Entity
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String brand, model, fuelType, transmission, bodyType, imageColor;
    private double priceLakh;       // ex-showroom price, ₹ lakh
    private double mileage;         // km/l (km/kg for CNG, km/charge-equivalent for EV approximated)
    private int powerBhp, safetyRating, seats, bootLitres;
    @Column(name = "model_year") private int year;
    @Column(length = 400) private String features;
    private int torqueNm, engineCc;            // engineCc = 0 for electric
    private String transmissionType, segment;  // e.g. AMT / CVT / DCT / Torque Converter / e-CVT / Single-speed; Entry Level / Luxury ...

    public Car() {}
    public Car(String brand, String model, int year, String bodyType, String fuelType, String transmission,
               double priceLakh, double mileage, int powerBhp, int safetyRating, int seats, int bootLitres,
               String features, String imageColor) {
        this.brand = brand; this.model = model; this.year = year; this.bodyType = bodyType; this.fuelType = fuelType;
        this.transmission = transmission; this.priceLakh = priceLakh; this.mileage = mileage; this.powerBhp = powerBhp;
        this.safetyRating = safetyRating; this.seats = seats; this.bootLitres = bootLitres; this.features = features;
        this.imageColor = imageColor;
    }
    public void setSpecs(int torqueNm, int engineCc, String transmissionType, String segment) {
        this.torqueNm = torqueNm; this.engineCc = engineCc; this.transmissionType = transmissionType; this.segment = segment;
    }
    public int getTorqueNm() { return torqueNm; }
    public int getEngineCc() { return engineCc; }
    public String getTransmissionType() { return transmissionType; }
    public String getSegment() { return segment; }
    public Long getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getFuelType() { return fuelType; }
    public String getTransmission() { return transmission; }
    public String getBodyType() { return bodyType; }
    public String getImageColor() { return imageColor; }
    public double getPriceLakh() { return priceLakh; }
    public double getMileage() { return mileage; }
    public int getPowerBhp() { return powerBhp; }
    public int getSafetyRating() { return safetyRating; }
    public int getSeats() { return seats; }
    public int getBootLitres() { return bootLitres; }
    public int getYear() { return year; }
    public String getFeatures() { return features; }
}