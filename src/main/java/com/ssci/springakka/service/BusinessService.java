package com.ssci.springakka.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void perform(Object o) throws InterruptedException {
        Thread.sleep(1000);
        logger.info("Perform: {}", o);
    }
}

