package com.distri.proyectoVenta.apis.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VentaControllerFechaTest {

    @Test
    void soloFecha_comoLaMandaElInputDateDelFront() {
        assertThat(VentaController.parseFecha("2026-10-01", false).toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(VentaController.parseFecha("2026-10-31", true).toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 10, 31, 0, 0).with(LocalTime.MAX));
    }

    @Test
    void fechaHoraConOffset() {
        assertThat(VentaController.parseFecha("2026-10-01T10:30:00-03:00", false).toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 30));
    }

    @Test
    void vacioEsSinFiltro_eInvalidoFalla() {
        assertThat(VentaController.parseFecha(null, false)).isNull();
        assertThat(VentaController.parseFecha(" ", true)).isNull();
        assertThatThrownBy(() -> VentaController.parseFecha("31/10/2026", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
