package com.elastic.monitoring.controller;

import com.elastic.monitoring.model.Car;
import com.elastic.monitoring.service.CarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/car")
@RestController
@Slf4j
public class CarController {
    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping("generate")
    public Car generate(){
        log.info("running generate service");
        return carService.generateCar();
    }

    @PostMapping("echo")
    public String echo(@RequestBody Car car){
        log.info("Car is : {} ", car);
        return car.toString();
    }
}
