package ru.boro.busapps.modellibs.enums;

import java.util.Random;

public enum KindAutoType {

    LAW_SUITS,
    BANKRUPTCY;

    private static final Random random = new Random();

    public static String randomType()  {

        KindAutoType[] kindAutoTypes = new KindAutoType[]{LAW_SUITS, LAW_SUITS, LAW_SUITS, LAW_SUITS,
                LAW_SUITS, LAW_SUITS, LAW_SUITS, LAW_SUITS, LAW_SUITS, BANKRUPTCY, LAW_SUITS, LAW_SUITS};

        return kindAutoTypes[random.nextInt(kindAutoTypes.length)].toString();
    }

}