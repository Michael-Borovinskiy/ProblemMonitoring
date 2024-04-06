package ru.boro.busapps.problmonitoring.service.consumer;

import ru.boro.busapps.modellibs.models.AnalyticalEvent;
import ru.boro.busapps.modellibs.models.BusinessEvent;

public interface KfkEventListener {

    AnalyticalEvent approve(BusinessEvent msg);

    String getEmployee();

}
