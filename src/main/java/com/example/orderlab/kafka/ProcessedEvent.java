package com.example.orderlab.kafka;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="processed_events", uniqueConstraints=@UniqueConstraint(columnNames="eventId"))
public class ProcessedEvent { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String eventId; @Column(nullable=false) private Instant processedAt; protected ProcessedEvent(){} public ProcessedEvent(String eventId){this.eventId=eventId;this.processedAt=Instant.now();} }
