package com.paytm.wallet.service.cache.impl;

import com.paytm.wallet.service.cache.CacheService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheServiceImpl implements CacheService {

    @Override
    @Cacheable(value = "wallets")
    public String getCachedValue(String key) {
        return key;
    }
}
