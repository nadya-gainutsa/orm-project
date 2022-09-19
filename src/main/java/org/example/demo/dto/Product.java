package org.example.demo.dto;

import lombok.ToString;
import org.example.annotation.Column;
import org.example.annotation.Table;

@Table("products")
@ToString
public class Product {
    @Column
    private Long id;

    @Column
    private String name;

}
