package org.acme.producer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.acme.model.ItemCart;
import org.acme.model.ProductSell;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;

/**
 * A bean producing random temperature data every second.
 * The values are written to a Kafka topic (temperature-values).
 * Another topic contains the name of weather stations (weather-stations).
 * The Kafka configuration is specified in the application configuration.
 */
@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    static long id = 1;

    public static synchronized long incrementCartId() {
        id = id + 1;
        return id;
    }

    private Random random = new Random();

    private List<ProductSell> products = List.of(
            new ProductSell(1L, "AMD", "AMD Ryzen 9 7950X"),
            new ProductSell(2L, "AMD", "AMD Ryzen 9 7900X"),
            new ProductSell(3L, "AMD", "AMD Ryzen 9 7700X"),
            new ProductSell(4L, "AMD", "AMD Ryzen 9 7600X"),
            new ProductSell(5L, "Intel", "Intel Core i9 13900K"),
            new ProductSell(6L, "Intel", "Intel Core i7 13700K"),
            new ProductSell(7L, "Intel", "Intel Core i5 13600K"),
            new ProductSell(8L, "Intel", "Intel Core i3 13500"));

    @Outgoing("product-list")
    public Multi<Record<Long, ProductSell>> products() {
        return Multi.createFrom().items(products.stream()
                .map(m -> Record.of(m.id, m)));

    }

    @Outgoing("item-cart")
    public Multi<Record<Long, ItemCart>> items() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(500))
                .onOverflow().drop()
                .map(tick -> {
                    int randomNumber = getRandomNumberFrom(1, 10);

                    ProductSell product = products.get(random.nextInt(products.size()));
                    ItemCart build = ItemCart.builder().id(incrementCartId())
                            .productSell(product)
                            .quantity(randomNumber)
                            .price(new BigDecimal(BigInteger.valueOf(new Random().nextInt(100001)), 2)).build();

                    LOG.infof("Sent: %s", build);
                    return Record.of(build.id, build);
                });

    }

    public static int getRandomNumberFrom(int min, int max) {
        Random foo = new Random();
        int randomNumber = foo.nextInt((max + 1) - min) + min;

        return randomNumber;
    }
}
