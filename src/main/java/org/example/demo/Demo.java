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
        Product product = session.find(Product.class, 1L);

        log.info("Product: {}", product);

        Product theSameProduct = session.find(Product.class, 1L);
        log.info("Product: {}", theSameProduct);

        theSameProduct.setName("New laptop");

        session.close();

        Session session1 = factory.createSession();
        product = session.find(Product.class, 1L);
        log.info("Product: {}", product);
        session1.close();

    }

    public static DataSource initPG() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/test");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");

        return dataSource;
    }
}
