package com.distri.proyectoVenta.apis.entities.venta;

import com.distri.proyectoVenta.apis.entities.base.EntidadBase;
import com.distri.proyectoVenta.apis.entities.cliente.Cliente;
import com.distri.proyectoVenta.apis.entities.seguridad.Usuario;
import com.distri.proyectoVenta.apis.entities.venta.VentaDetalle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Entity
@Table(name = "venta")
@Where(clause = "activo = true")
public class Venta extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta = LocalDateTime.now();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaDetalle> detalles = new ArrayList<>();

    @Column(name = "estado_pago", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PAGADO'")
    private String estadoPago = "PAGADO";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deudor_id")
    private Deudor deudor;

}
