package com.example.userservice.config

import feign.codec.Encoder
import feign.form.spring.SpringFormEncoder
import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.SpringEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate

@Configuration
class FeignEncoderConfig {

    @Bean
    @Primary
    fun feignEncoder(converters: ObjectFactory<HttpMessageConverters>): Encoder {
        return SpringFormEncoder(SpringEncoder(converters))
    }

    @Bean
    fun httpMessageConverters(): ObjectFactory<HttpMessageConverters> {
        return ObjectFactory {
            HttpMessageConverters(RestTemplate().messageConverters)
        }
    }
}
