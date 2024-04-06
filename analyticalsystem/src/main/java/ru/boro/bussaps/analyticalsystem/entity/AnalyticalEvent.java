package ru.boro.bussaps.analyticalsystem.entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ANALYTICAL_EVENT")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticalEvent {

    public AnalyticalEvent(String typeEvent,
                           String kindEvent,
                           String idClient,
                           Date dateCreate,
                           String approvedBy,
                           Date approvedDatetime,
                           Date loadDatetime) {
        this.typeEvent = typeEvent;
        this.kindEvent = kindEvent;
        this.idClient = idClient;
        this.dateCreate = dateCreate;
        this.approvedBy = approvedBy;
        this.approvedDatetime = approvedDatetime;
        this.loadDatetime = loadDatetime;
    }

    @Id
    @GeneratedValue
    private int id;
    @Column(name = "type_event")
    private String typeEvent;
    @Column(name = "kind_event")
    private String kindEvent;
    @Column(name = "id_client")
    private String idClient;
    @Column(name = "date_create")
    private Date dateCreate;
    @Column(name = "approved_by")
    private String approvedBy;
    @Column(name = "approved_datetime")
    private Date approvedDatetime;
    @Column(name = "load_datetime")
    private Date loadDatetime;
}
