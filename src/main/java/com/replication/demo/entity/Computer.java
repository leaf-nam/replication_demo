package com.replication.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "computer")
@NoArgsConstructor
@Getter @Setter
public class Computer {

    @Id @GeneratedValue
    @Column(name = "computer_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private ComputerType type;

    @Column(name = "os")
    private String OS;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;
}
