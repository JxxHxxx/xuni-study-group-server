package com.xuni.core.group.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Capacity {
    protected static final Integer CAPACITY_MAX = 20;
    protected static final Integer CAPACITY_MIN = 1;

    private Integer totalCapacity;
    private Integer leftCapacity;

    public Capacity(Integer capacity) {
        this.totalCapacity = capacity;
        this.leftCapacity = capacity;
    }

    public static Capacity of(Integer capacity) {
        return new Capacity(capacity);
    }

    protected boolean hasNotTotalCapacityWithinRange(){
        return totalCapacity < CAPACITY_MIN || totalCapacity > CAPACITY_MAX;
    }

    protected void subtractOneLeftCapacity() {
        this.leftCapacity -= 1;
    }

    protected void addOneLeftCapacity() {
        this.leftCapacity += 1;
    }

    protected boolean hasNotLeftCapacity() {
        return this.leftCapacity <= 0;
    }
}
