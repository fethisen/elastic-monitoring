package com.elastic.monitoring.controller;

import com.elastic.monitoring.model.Car;
import com.elastic.monitoring.service.CarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("api/car")
@RestController
@Slf4j
public class CarController {
    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    /**
     * Markaya göre araç listesi getir
     * GET /api/car?brand=Toyota
     */
    @GetMapping
    public ResponseEntity<List<Car>> getCarsByBrand(@RequestParam(name = "brand") String brand) {
        List<Car> cars = carService.getCarsByBrand(brand);
        if (cars.isEmpty()) {
            log.warn("No cars found for brand: {}", brand);
            return ResponseEntity.ok(cars); // Boş liste döner
        }
        return ResponseEntity.ok(cars);
    }

    /**
     * Yeni araç kaydet
     * POST /api/car
     */
    @PostMapping
    public ResponseEntity<Car> saveCar(@RequestBody Car car) {
        try {
            Car savedCar = carService.saveCar(car);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCar);
        } catch (IllegalArgumentException e) {
            log.error("Invalid car data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error saving car", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
