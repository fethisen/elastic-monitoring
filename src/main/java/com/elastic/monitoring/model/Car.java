package com.elastic.monitoring.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Car {
    private String brand;
    private String color;
    private String type;
    private int price;
    private boolean available;

    @JsonFormat(pattern = "dd-MMM-yyyy", timezone = "Europe/Istanbul")
    private LocalDate firstReleaseDate;

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)//Eğer boş ise, response de bu field görüntülenmeyecek
    private List<String> additionalFeatures;
    private Engine engine;
    private List<Tire> tires;


    public Car() {
    }

    public Car(String brand, String color, String type) {
        this.brand = brand;
        this.color = color;
        this.type = type;
    }
}
