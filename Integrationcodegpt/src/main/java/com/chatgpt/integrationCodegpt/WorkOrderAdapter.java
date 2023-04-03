package com.chatgpt.integrationCodegpt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;

@Component
public class WorkOrderAdapter {

    private final Logger logger = LoggerFactory.getLogger(WorkOrderAdapter.class);

    private static final String CACHE_PREFIX = "workorder:";
    private static final long CACHE_EXPIRY_IN_SECONDS = 60;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String maximoEndpoint = "https://run.mocky.io/v3/02041b0d-abac-4453-b638-494b08d92f51";

    private ObjectMapper objectMapper = new ObjectMapper();

    public WorkOrder fetchWorkOrderById(String workOrderId) {
        String cacheKey = CACHE_PREFIX + workOrderId;
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                logger.debug("Work order {} found in Redis cache", workOrderId);
                WorkOrder workOrder = objectMapper.readValue(cachedValue, WorkOrder.class);
                saveToMongo(workOrder);
                return workOrder;
            } else {
                logger.debug("Work order {} not found in Redis cache, fetching from Maximo", workOrderId);
                String url = maximoEndpoint + "/" + workOrderId;
                WorkOrder workOrder = restTemplate.getForObject(url, WorkOrder.class);
                String workOrderJson = objectMapper.writeValueAsString(workOrder);
                redisTemplate.opsForValue().set(cacheKey, workOrderJson, CACHE_EXPIRY_IN_SECONDS, TimeUnit.SECONDS);
                logger.debug("Work order {} fetched from Maximo and cached in Redis", workOrderId);
                saveToMongo(workOrder);
                return workOrder;
            }
        } catch (RedisConnectionFailureException e) {
            logger.error("Failed to connect to Redis", e);
            throw new MaximoAdapterException("Failed to connect to Redis", e);
        } catch (DataAccessException e) {
            logger.error("Failed to access Redis", e);
            throw new MaximoAdapterException("Failed to access Redis", e);
        } catch (Exception e) {
            logger.error("Failed to fetch work order for ID: {}", workOrderId, e);
            throw new MaximoAdapterException("Failed to fetch work order for ID: " + workOrderId, e);
        }
    }

    private void saveToMongo(WorkOrder workOrder) {
        try {
            mongoTemplate.save(workOrder);
            logger.debug("Work order {} saved to MongoDB", workOrder.getWorkorderid());
        } catch (DataAccessException e) {
            logger.error("Failed to save work order {} to MongoDB", workOrder.getWorkorderid(), e);
            throw new MaximoAdapterException("Failed to save work order to MongoDB", e);
        }
    }

}
