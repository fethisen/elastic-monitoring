package com.elastic.monitoring.service;

import com.elastic.monitoring.model.Car;
import com.elastic.monitoring.model.Engine;
import com.elastic.monitoring.model.Tire;
import com.elastic.monitoring.util.RandomDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class CarService {
    List<String> BRANDS= List.of("Toyota","Honda","Ford");
    List<String> COLORS= List.of("Red","Black","White");
    List<String> TYPES= List.of("Sedan","SUV","MPV");
    List<String> ADDITIONAL_FEATURES = List.of("GPS","Alarm","Sunroof","Media Player","Leather seats");
    List<String> FUELS = List.of("Gas","Electric","Hybird");
    List<String> TIRE_MANUFACTURERS = List.of("Goodyear","Bridgestone","Dunlop");

    public Car generateCar(){
        String brand = BRANDS.get(ThreadLocalRandom.current().nextInt(BRANDS.size()));
        String color = COLORS.get(ThreadLocalRandom.current().nextInt(COLORS.size()));
        String type = TYPES.get(ThreadLocalRandom.current().nextInt(TYPES.size()));

        boolean available = ThreadLocalRandom.current().nextBoolean();
        int price = ThreadLocalRandom.current().nextInt(5000,12001);
        LocalDate firstReleaseDate = RandomDateUtil.generateRandomLocalDate();

        int randomCount = ThreadLocalRandom.current().nextInt(ADDITIONAL_FEATURES.size());
        List<String> additionalFeatures = new ArrayList<>();
        for (int i = 0; i<randomCount; i++){
            additionalFeatures.add(ADDITIONAL_FEATURES.get(i));
        }

        String fuel = FUELS.get(ThreadLocalRandom.current().nextInt(FUELS.size()));
        int horsePower = ThreadLocalRandom.current().nextInt(100,221);

        Engine engine = new Engine();
        engine.setFuelType(fuel);
        engine.setHorsePower(horsePower);

        List<Tire> tires = new ArrayList<>();
        for (int i = 0; i<3; i++){
            Tire tire = new Tire();
            String  manufacturer = TIRE_MANUFACTURERS.get(ThreadLocalRandom.current().nextInt(TIRE_MANUFACTURERS.size()));
            int size = ThreadLocalRandom.current().nextInt(15,18);
            int tirePrice = ThreadLocalRandom.current().nextInt(200,401);
            tire.setManufacturer(manufacturer);
            tire.setSize(size);
            tire.setPrice(tirePrice);

            tires.add(tire);
        }


        Car car = new Car(brand,color,type);
        car.setAvailable(available);
        car.setPrice(price);
        car.setFirstReleaseDate(firstReleaseDate);
        car.setAdditionalFeatures(additionalFeatures);
        car.setEngine(engine);
        car.setTires(tires);
        log.info("Car is : {} ", car.toString());
        return car;
    }
}
