package com.gamevault.util;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/** Generates random unique Long IDs with at least 8 digits.
 *  Configure the target table via the "table" parameter in @GenericGenerator. */
public class RandomIdGenerator implements IdentifierGenerator {

    private static final long MIN = 10_000_000L;
    private static final long MAX = 9_007_199_254_740_991L; // Number.MAX_SAFE_INTEGER

    private String table;

    @Override
    public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) {
        table = parameters.getProperty("table", "activities");
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        while (true) {
            long id = ThreadLocalRandom.current().nextLong(MIN, MAX);
            Long count = session
                    .createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE id = " + id, Long.class)
                    .uniqueResult();
            if (count == null || count == 0L) {
                return id;
            }
        }
    }
}
