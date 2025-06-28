package com.MediConnect.Service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class OtpCacheService {

    @Cacheable(value = "OTP_CACHE", key = "#key")
    public String getCachedOTP(String key) {
        return null;
    }

    @CachePut(value = "OTP_CACHE", key = "#key")
    public String cacheOTP(String key, String otp) {
        return otp;
    }

    @CacheEvict(value = "OTP_CACHE", key = "#key")
    public void clearOTP(String key) {
    }
}
