import java.net.URL;
import java.net.URLClassLoader;
import java.lang.annotation.Annotation;

public class CheckComponent {
    public static void main(String[] args) throws Exception {
        URL[] urls = { new URL("file:///c:/Proyecto Personal/Ferreteria/SistemaWebYDistribuido/Proyecto-Venta/target/unzipped/BOOT-INF/classes/") };
        try (URLClassLoader cl = new URLClassLoader(urls)) {
            Class<?> clazz = cl.loadClass("com.distri.proyectoVenta.apis.mapper.ClienteMapperImpl");
            System.out.println("Class loaded: " + clazz.getName());
            for (Annotation a : clazz.getAnnotations()) {
                System.out.println("Annotation: " + a.toString());
            }
        }
    }
}
