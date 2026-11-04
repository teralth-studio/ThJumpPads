# MEMORY.md

- El plugin que se cree debe pasar todas las pruebas y validaciones para todas las versiones Java disponibles. Desde la versión java predefinida en configuración (deberás escanear el proyecto y comprobar cual es el bytecode base) hasta la versión mas reciente de java. Por ejemplo, si el proyecto tiene como bytecode base Java 8, deberás probarlo en Java 8, 11, 16, 17, 21 y 25.

- Solamente se debe generar UN SOLO JAR; ese mismo jar debe ser compatible con todas las versiones de java mencionadas anteriormente. No se deben generar varios jars para cada versión de java.

El plugin debe mantener la funcion.