package com.elastic.monitoring.service;

import com.elastic.monitoring.model.Car;
import com.elastic.monitoring.model.Engine;
import com.elastic.monitoring.model.Tire;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CarService {
    
    // In-memory storage - Thread-safe için ConcurrentHashMap kullanıyoruz
    private final ConcurrentHashMap<String, Car> carStorage = new ConcurrentHashMap<>();
    
    // Constructor'da örnek veriler oluşturalım
    public CarService() {
        initializeSampleData();
    }
    
    /**
     * Markaya göre araçları getir
     */
    public List<Car> getCarsByBrand(String brand) {
        log.debug("Searching cars for brand: {}", brand);
        
        if (brand == null || brand.trim().isEmpty()) {
            log.warn("Brand parameter is empty, returning empty list");
            return new ArrayList<>();
        }
        
        List<Car> filteredCars = carStorage.values().stream()
                .filter(car -> car.getBrand().equalsIgnoreCase(brand.trim()))
                .collect(Collectors.toList());
        
        log.debug("Found {} cars for brand: {}", filteredCars.size(), brand);
        return filteredCars;
    }
    
    /**
     * Yeni araç kaydet
     */
    public Car saveCar(Car car) {
        // Validasyon
        validateCar(car);
        
        // ID oluştur
        String carId = UUID.randomUUID().toString();
        car.setId(carId);
        
        // Kaydet
        carStorage.put(carId, car);
        
        log.info("Car saved successfully - ID: {}, Brand: {}, Color: {}, Type: {}", 
                carId, car.getBrand(), car.getColor(), car.getType());
        
        return car;
    }
    
    /**
     * Araç validasyonu
     */
    private void validateCar(Car car) {
        if (car == null) {
            throw new IllegalArgumentException("Car cannot be null");
        }
        
        if (car.getBrand() == null || car.getBrand().trim().isEmpty()) {
            throw new IllegalArgumentException("Car brand cannot be empty");
        }
        
        if (car.getColor() == null || car.getColor().trim().isEmpty()) {
            throw new IllegalArgumentException("Car color cannot be empty");
        }
        
        if (car.getType() == null || car.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Car type cannot be empty");
        }
        
        if (car.getPrice() <= 0) {
            throw new IllegalArgumentException("Car price must be greater than zero");
        }
        
        log.debug("Car validation passed");
    }
    
    /**
     * Örnek verilerle başlat
     */
    private void initializeSampleData() {
        log.info("Initializing sample car data...");
        
        // Toyota araçlar
        Car toyota1 = createCar("Toyota", "Red", "Sedan", 25000, true, 
                LocalDate.of(2023, 1, 15), "Gas", 180,
                List.of("GPS", "Alarm", "Sunroof"));
        
        Car toyota2 = createCar("Toyota", "White", "SUV", 35000, true,
                LocalDate.of(2023, 6, 20), "Hybrid", 200,
                List.of("GPS", "Leather seats", "Media Player"));
        
        Car toyota3 = createCar("Toyota", "Black", "Sedan", 28000, false,
                LocalDate.of(2022, 3, 10), "Electric", 220,
                List.of("GPS", "Alarm", "Sunroof", "Leather seats"));
        
        // Honda araçlar
        Car honda1 = createCar("Honda", "Blue", "Sedan", 23000, true,
                LocalDate.of(2023, 2, 5), "Gas", 170,
                List.of("GPS", "Media Player"));
        
        Car honda2 = createCar("Honda", "Silver", "SUV", 32000, true,
                LocalDate.of(2023, 8, 12), "Hybrid", 190,
                List.of("GPS", "Alarm", "Sunroof", "Leather seats"));
        
        // Ford araçlar
        Car ford1 = createCar("Ford", "Red", "MPV", 30000, true,
                LocalDate.of(2023, 4, 25), "Gas", 210,
                List.of("GPS", "Alarm"));
        
        Car ford2 = createCar("Ford", "Black", "SUV", 38000, true,
                LocalDate.of(2023, 7, 18), "Electric", 250,
                List.of("GPS", "Alarm", "Sunroof", "Media Player", "Leather seats"));
        
        // Araçları kaydet
        saveCar(toyota1);
        saveCar(toyota2);
        saveCar(toyota3);
        saveCar(honda1);
        saveCar(honda2);
        saveCar(ford1);
        saveCar(ford2);
        
        log.info("Sample data initialized - Total {} cars added", carStorage.size());
    }
    
    /**
     * Helper method - Araç oluştur
     */
    private Car createCar(String brand, String color, String type, int price, 
                          boolean available, LocalDate releaseDate, 
                          String fuelType, int horsePower, 
                          List<String> additionalFeatures) {
        
        Car car = new Car(brand, color, type);
        car.setPrice(price);
        car.setAvailable(available);
        car.setFirstReleaseDate(releaseDate);
        car.setAdditionalFeatures(additionalFeatures);
        
        // Engine
        Engine engine = new Engine(fuelType, horsePower);
        car.setEngine(engine);
        
        // Tires (4 adet)
        List<Tire> tires = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tires.add(new Tire("Michelin", 17, 300));
        }
        car.setTires(tires);
        
        return car;
    }
}
