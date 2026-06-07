// src/main/java/com/distri/proyectodistri/apis/mapper/NumberMapper.java
package com.distri.proyectodistri.apis.mapper;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class NumberMapper {
    public BigDecimal toBigDecimal(Double v)   { return v == null ? null : BigDecimal.valueOf(v); }
    public Double     toDouble(BigDecimal val) { return val == null ? null : val.doubleValue(); }
}
