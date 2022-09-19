package org.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.annotation.Column;
import org.example.annotation.Id;
import org.example.annotation.Table;

@Table("products")
@ToString
@Getter
@Setter
@NoArgsConstructor
public class Product{
    @Id
    @Column("id")
    private Long id;

    @Column("name")
    private String name;

    public Product(Product product) {
        id = product.id;
        name = product.name;
    }

}
