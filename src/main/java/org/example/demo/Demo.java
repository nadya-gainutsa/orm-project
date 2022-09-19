package org.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.example.Session;
import org.example.SessionFactory;
import org.example.demo.dto.Product;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;


@Slf4j
public class Demo {
    public static void main(String[] args) {
        DataSource dataSource = initPG();
        SessionFactory factory = new SessionFactory(dataSource);

        Session session = factory.createSession();
        Product product = session.find(Product.class, 5L);

        log.info("Product: {}", product);

        Product theSameProduct = session.find(Product.class, 5L);
        log.info("Product: {}", theSameProduct);

        session.close();
    }

    public static DataSource initPG() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/database_name");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");

        return dataSource;
    }
}
