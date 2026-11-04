# SEGURIDAD.md

## Reglas de seguridad obligatorias que debes cumplir

- Prohibido introducir vulnerabilidades de seguridad intencionales o conocidas en el código fuente.
- NO incluir credenciales de acceso (usuarios, contraseñas, tokens, API keys, secretos, etc.) directamente en el código fuente, independientemente de su propósito.
- Asegúrate de que todas las dependencias y librerías utilizadas provengan de fuentes confiables y estén libres de vulnerabilidades conocidas.
- Implementa validaciones y sanitización de entradas para prevenir ataques de inyección, XSS, CSRF y otras vulnerabilidades cuando corresponda.
- Prohibido incluir malware, spyware, ransomware, backdoors o cualquier comportamiento malicioso.
- Prohibido implementar mecanismos ocultos de acceso, ejecución o administración.
- No recopiles, almacenes ni transmitas información personal o sensible sin una justificación legítima y autorización correspondiente.
- No expongas información sensible en mensajes de consola, logs, excepciones o mensajes enviados a los jugadores.
- Implementa un manejo seguro de errores y excepciones, evitando revelar rutas internas, credenciales, consultas SQL o información de infraestructura.
- Aplica el principio de mínimo privilegio: cada funcionalidad debe utilizar únicamente los permisos y accesos estrictamente necesarios.
- No ejecutes comandos del sistema operativo ni procesos externos sin una necesidad justificada y autorización explícita del administrador.
- No permitas la ejecución de código arbitrario mediante comandos, configuraciones, solicitudes externas o entradas de jugadores.
- No realices conexiones externas, solicitudes HTTP ni transmisiones de información que no estén justificadas y documentadas.
- No descargues ni ejecutes archivos externos sin validar su procedencia, integridad y propósito.
- No modifiques, sobrescribas ni elimines archivos ajenos al funcionamiento autorizado del plugin.
- No permitas accesos no autorizados a archivos, directorios, bases de datos o recursos del servidor.
- Protege las operaciones administrativas mediante comprobaciones de permisos y autorización.
- No confíes en los datos enviados por jugadores, otros plugins, proxies o servicios externos sin validarlos.
- Mantén el código fuente y las dependencias libres de funcionalidades ocultas o comportamientos no documentados.
- No introduzcas actualizaciones que modifiquen el comportamiento del plugin de forma maliciosa o sin transparencia.
- Utiliza mecanismos criptográficos seguros y evita algoritmos obsoletos o inseguros para proteger información sensible.
- No utilices cifrado propio cuando existan implementaciones criptográficas estándar y confiables.
- Asegúrate de que las operaciones sensibles sean seguras frente a condiciones de carrera y accesos concurrentes.
- Evita operaciones bloqueantes, bucles infinitos y tareas innecesariamente pesadas que puedan afectar al rendimiento o disponibilidad del servidor.

## En caso de que el plugin utilice MySQL/MariaDB

- No incluyas credenciales de bases de datos en el código fuente.
- Asegúrate de que todas las consultas SQL estén protegidas contra inyecciones SQL mediante consultas preparadas (`PreparedStatement`).
- Nunca construyas consultas SQL concatenando directamente datos proporcionados por jugadores o usuarios.
- Configura las conexiones a la base de datos de manera segura y utiliza conexiones cifradas cuando sea posible.
- Utiliza cuentas de base de datos con los privilegios mínimos necesarios.
- No utilices cuentas administrativas como `root` para las conexiones del plugin.
- Implementa un manejo adecuado de errores y excepciones para evitar la exposición de información sensible.
- Cierra correctamente conexiones, declaraciones y resultados (`Connection`, `PreparedStatement` y `ResultSet`).
- Utiliza conexiones y operaciones asíncronas cuando corresponda para evitar bloquear el hilo principal del servidor.
- Implementa tiempos de espera (`timeouts`) para evitar conexiones bloqueadas indefinidamente.
- Valida los datos antes de almacenarlos o utilizarlos en consultas.
- Evita almacenar contraseñas en texto plano; utiliza algoritmos especializados de hashing como Argon2id, bcrypt o scrypt.
- No registres credenciales, consultas con información sensible ni datos privados en los logs.
- Implementa una gestión segura de transacciones para evitar inconsistencias y corrupción de datos.
- Gestiona correctamente las desconexiones, reconexiones y fallos de disponibilidad de la base de datos.

## En caso de que el plugin utilice SQLite

- No permitas la manipulación de rutas de bases de datos mediante entradas no confiables.
- Protege las consultas contra inyecciones SQL utilizando `PreparedStatement`.
- Gestiona correctamente las conexiones, transacciones y bloqueos de la base de datos.
- Evita operaciones de lectura y escritura que bloqueen el hilo principal del servidor.
- Protege los archivos de base de datos contra accesos no autorizados.
- Implementa copias de seguridad seguras cuando la funcionalidad lo requiera.
- No elimines ni sobrescribas bases de datos existentes sin autorización.

## En caso de que el plugin utilice conexiones HTTP o APIs externas

- Utiliza HTTPS para todas las comunicaciones que transmitan información sensible.
- Valida los certificados TLS y nunca desactives su verificación para evitar errores de conexión.
- No confíes en las respuestas recibidas de servicios externos sin validarlas.
- Implementa tiempos de espera para las solicitudes y conexiones.
- Limita el tamaño de las respuestas recibidas para prevenir consumo excesivo de memoria.
- Controla los redireccionamientos HTTP para evitar accesos a destinos no autorizados.
- Evita solicitudes a direcciones internas o locales cuando puedan provocar vulnerabilidades SSRF.
- No transmitas información personal, credenciales o datos del servidor sin autorización.
- Implementa mecanismos de control de solicitudes para evitar abusos y ataques de denegación de servicio.
- No descargues ni ejecutes contenido recibido de servicios externos sin verificar su integridad y procedencia.
- Documenta todos los servicios externos utilizados y el propósito de cada conexión.

## En caso de que el plugin gestione archivos

- Valida las rutas y nombres de archivos para prevenir ataques Path Traversal.
- No permitas acceder a directorios externos mediante rutas manipuladas.
- No sobrescribas archivos existentes sin una justificación y autorización.
- No elimines archivos ajenos a los datos administrados por el plugin.
- Valida el contenido de los archivos antes de procesarlo.
- Evita deserializar objetos Java procedentes de fuentes no confiables.
- Protege los archivos de configuración que contengan información sensible.
- No almacenes contraseñas, tokens o secretos en archivos de configuración sin las medidas de protección adecuadas.
- Implementa operaciones de escritura seguras para evitar corrupción de archivos ante interrupciones.

## En caso de que el plugin utilice comandos y permisos

- Verifica los permisos antes de ejecutar cualquier operación administrativa.
- No concedas permisos OP ni privilegios administrativos automáticamente.
- No permitas que jugadores ejecuten comandos administrativos mediante argumentos manipulados.
- Valida todos los argumentos y parámetros recibidos por los comandos.
- Evita ejecutar comandos del sistema operativo a partir de entradas proporcionadas por jugadores.
- No permitas eludir los sistemas de permisos de Bukkit, Spigot, Paper o Velocity.
- Evita exponer información administrativa a usuarios sin autorización.
- Asegúrate de que los comandos sensibles tengan permisos específicos y correctamente definidos.

## En caso de que el plugin utilice cifrado, autenticación o gestión de sesiones

- Nunca almacenes contraseñas en texto plano.
- Utiliza algoritmos de hashing diseñados específicamente para contraseñas, como Argon2id, bcrypt o scrypt.
- No utilices MD5, SHA-1 o SHA-256 sin un esquema especializado de hashing de contraseñas.
- Utiliza generadores criptográficamente seguros para crear tokens, claves y códigos de recuperación.
- No reutilices tokens de autenticación ni identificadores sensibles.
- Implementa expiración y revocación de sesiones cuando corresponda.
- Protege los procesos de autenticación contra ataques de fuerza bruta.
- No reveles si una cuenta existe mediante mensajes de error innecesariamente específicos.
- Protege los secretos criptográficos y nunca los incluyas directamente en el código fuente.

## En caso de que el plugin utilice Redis

- No expongas Redis públicamente sin las medidas de seguridad correspondientes.
- Configura autenticación y controles de acceso cuando estén disponibles.
- Utiliza conexiones cifradas cuando la infraestructura lo permita.
- No almacenes información sensible sin protección adecuada.
- Valida todos los datos recibidos desde Redis.
- Evita utilizar Redis como canal para ejecutar comandos o código arbitrario.
- Implementa tiempos de espera y manejo de desconexiones.
- No permitas que una conexión comprometida pueda ejecutar operaciones fuera de las necesarias para el plugin.

## En caso de que el plugin utilice actualizaciones automáticas

- Descarga actualizaciones únicamente desde fuentes oficiales y verificables.
- Verifica la integridad y autenticidad de los archivos descargados.
- No ejecutes ni reemplaces archivos sin validar previamente su contenido y procedencia.
- No instales actualizaciones automáticamente sin autorización o configuración explícita del administrador.
- No permitas que un servicio externo pueda modificar arbitrariamente el comportamiento del plugin.
- Informa claramente al administrador sobre las actualizaciones disponibles y los cambios relevantes.

## En caso de que el plugin utilice bStats o servicios de telemetría

- Documenta la información recopilada y transmitida.
- No recopiles información personal ni credenciales.
- No implementes mecanismos de seguimiento ocultos.
- Respeta las opciones de configuración y exclusión disponibles.
- No transmitas información adicional a la estrictamente necesaria para las estadísticas.

## Seguridad específica de Java y Minecraft

- No utilices mecanismos de reflexión para acceder o modificar información sensible sin una necesidad justificada.
- Evita utilizar APIs internas, obsoletas o inseguras cuando existan alternativas oficiales.
- Valida correctamente los datos recibidos mediante eventos, comandos, placeholders y mensajes.
- No confíes en los permisos o estados de jugadores proporcionados por fuentes externas sin verificarlos.
- Evita ejecutar tareas pesadas en el hilo principal (`Main Thread`).
- Gestiona correctamente las tareas asíncronas y evita acceder de manera insegura a APIs que requieren el hilo principal.
- Protege los sistemas de eventos contra ejecuciones repetidas, manipulaciones y abusos.
- No permitas que los placeholders ejecuten código o comandos arbitrarios.
- Mantén la compatibilidad con las versiones de Java y Minecraft declaradas sin introducir mecanismos de compatibilidad inseguros.

## Gestión y reporte de vulnerabilidades

- Toda vulnerabilidad identificada debe evaluarse y corregirse antes de publicar una versión estable.
- No publiques credenciales, secretos o información sensible en reportes de errores.
- Proporciona un canal privado para reportar vulnerabilidades de seguridad.
- No publiques detalles explotables de vulnerabilidades sin permitir previamente que sean corregidas.
- Revisa periódicamente las dependencias y el código fuente en busca de vulnerabilidades conocidas.
- Documenta las correcciones de seguridad relevantes en las notas de cada versión.

---

**IMPORTANTE:** Estas reglas son obligatorias durante todo el desarrollo, mantenimiento y actualización del proyecto. No deben omitirse por motivos de compatibilidad, rendimiento o conveniencia sin evaluar previamente los riesgos de seguridad.