package com.distri.proyectodistri.apis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public class DateMapper {

    @Named("asOffsetDateTime")
    public OffsetDateTime asOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    @Named("asLocalDateTime")
    public LocalDateTime asLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.toLocalDateTime();
    }
    // --- NUEVOS para LocalDate ---
    @Named("asLocalDate")
    public LocalDate asLocalDate(LocalDate date) {
        return date; // misma referencia, evita error
    }

    @Named("asLocalDateCopy")
    public LocalDate asLocalDateCopy(LocalDate date) {
        return date != null ? LocalDate.of(date.getYear(), date.getMonth(), date.getDayOfMonth()) : null;
    }
}
