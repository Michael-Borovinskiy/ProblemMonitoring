package ru.boro.busapps.modellibs.enums;

import java.util.Random;

public enum KindVerificatedType {

    REVENUE_DECREASE,
    SUPPLIER_LOSS,
    FINANCIAL_RISKS,
    TURNOVER_INCREASE,
    ACCOUNT_LOCK;

    private static final Random random = new Random();

    public static String randomType()  {

        KindVerificatedType[] kindVerificatedTypes = values();

        return kindVerificatedTypes[random.nextInt(kindVerificatedTypes.length)].toString();
    }
}