package ru.boro.busapps.modellibs.models;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AnalyticalEvent extends Event implements Serializable {

    private String typeEvent;
    private String kindEvent;
    private String idClient;
    private Date dateCreate;
    private String approvedBy;
    private Date approvedDateTime;
}