package com.tecsharp.apis.pois.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Contador de peticiones por API key y día (UTC). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "api_key_usage",
        uniqueConstraints = @UniqueConstraint(columnNames = {"api_key_id", "usage_date"}))
public class ApiKeyUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_id", nullable = false)
    private UUID apiKeyId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private Integer requests;
}
