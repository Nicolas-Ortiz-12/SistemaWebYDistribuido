<%@ page contentType="text/html;charset=UTF-8" %>
<%--
  Página de diagnóstico del cluster de UI.
  - Muestra qué Tomcat (jvmRoute) atendió el pedido -> comprueba el sticky session.
  - Mantiene un contador en la sesión -> al apagar un Tomcat el otro sigue
    con el mismo valor, lo que comprueba la replicación de sesiones.
--%>
<%
    Integer visitas = (Integer) session.getAttribute("visitas");
    visitas = (visitas == null) ? 1 : visitas + 1;
    session.setAttribute("visitas", visitas);
    String nodo = System.getProperty("jvmRoute", "?");
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Estado del cluster</title>
    <style>
        body { font-family: system-ui, sans-serif; background: #f5f6f8; color: #1f2328; margin: 0; padding: 32px; }
        .card { max-width: 520px; margin: auto; background: #fff; border-radius: 12px; padding: 24px 28px; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
        h1 { font-size: 20px; margin: 0 0 16px; }
        dt { font-size: 12px; text-transform: uppercase; color: #6b7280; margin-top: 12px; }
        dd { margin: 2px 0 0; font-size: 18px; font-family: ui-monospace, monospace; word-break: break-all; }
        .nodo { font-size: 32px; font-weight: 700; color: #2563eb; }
    </style>
</head>
<body>
<div class="card">
    <h1>Cluster de UI</h1>
    <dl>
        <dt>Atendido por</dt>
        <dd class="nodo"><%= nodo %></dd>
        <dt>ID de sesión</dt>
        <dd><%= session.getId() %></dd>
        <dt>Visitas en esta sesión (replicado)</dt>
        <dd><%= visitas %></dd>
        <dt>Servidor</dt>
        <dd><%= request.getLocalAddr() %>:<%= request.getLocalPort() %></dd>
    </dl>
</div>
</body>
</html>
