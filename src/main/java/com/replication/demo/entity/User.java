package com.replication.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity(name = "users")
@NoArgsConstructor
@Getter @Setter
public class User {

    @Id @GeneratedValue
    @Column(name = "user_id")
    private Long id;

    private String name;

    private Integer age;

    @OneToMany(mappedBy = "owner")
    private List<Computer> computers;
}
