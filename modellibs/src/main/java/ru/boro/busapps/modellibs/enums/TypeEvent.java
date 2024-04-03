package ru.boro.busapps.modellibs.enums;

import java.util.Random;

public enum TypeEvent {

    VERIFICATED,
    AUTO;

    private static final Random random = new Random();

    public static TypeEvent randomType()  {
        TypeEvent[] typeEvents = values();
        return typeEvents[random.nextInt(typeEvents.length)];
    }
}