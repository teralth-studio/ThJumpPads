# CONSOLE.md

Estos son los formatos de mensajes que aparecerán en la consola, debes respetar los colores y el formato de los mensajes, si no se especifica un color, el mensaje será blanco (por defecto).

En caso de que el proyecto compile un error o una advertencia, el mensaje debe ser rojo y amarillo respectivamente, y debe contener la palabra "ERROR" o "WARNING" al inicio del mensaje.

En caso de que colores como formato legacy (ejemplo: &b), en la consola se debe adaptar a los colores de la consola, por ejemplo, &b es aqua, en la consola se debe mostrar en color aqua.

Lo primero que debe aparecer en la consola es el nombre del plugin junto a su autor. Por ejemplo:

*(Asumiendo que el nombre del plugin es "ThPluginName", el nombre dependerá de la carpeta del proyecto, no del nombre del jar)*

Siempre, cada mensaje que aparezca en consola debe tener un prefijo con el nombre del plugin, en color aqua, y entre corchetes. Por ejemplo: "&8[&9Th&bPluginName&8]". Solamente no aplicar el prefijo en el mensaje de abajo.

```
&8----------------------------------------------------------
ThPluginName &rby TeStudio
&8----------------------------------------------------------
```

Para el ascii superior, la parte de Th debe ser color &9, mientras que PluginName debe ser color &b.

Siguiente a eso, debe aparecer los mensajes del plugin, comenzando por el "Starting plugin..." en color &r:

```
&8[&9Th&bPluginName&8] &rStarting plugin...
```

Siguiente de eso, debe detectar la versión de java en la que se está ejecutando el plugin, y mostrar un mensaje en color &r indicando la versión de java. Por ejemplo:

```
&8[&9Th&bPluginName&8] &rJava version detected: &e1.8.0_281
```

Luego, debe detectar los addons que tiene el plugin (ej: PlaceholderAPI, Vault, etc), y mostrar los mensajes de inicialización y confirmación de inicialización de cada addon. Cabe recalcar que si el addon no está presente, no debe mostrar ningún mensaje con respecto a ese addon. Por ejemplo, si el plugin tiene soporte para PlaceholderAPI y Vault, y ambos addons están presentes, no debe mostrar mensajes. Ejemplo:

Asumindo que el plugin tiene soporte para PlaceholderAPI y Vault, y ambos addons están presentes, debe mostrar los siguientes mensajes:
```
&8[&9Th&bPluginName&8] &rDetected PlaceholderAPI, initializing...
&8[&9Th&bPluginName&8] &afPlaceholderAPI initialized successfully
&8[&9Th&bPluginName&8] &rDetected Vault, initializing...
&8[&9Th&bPluginName&8] &aVault initialized successfully
```

En caso de que Vault no esté presente (pero PlaceholderAPI si), debe mostrar los siguientes mensajes:
```
&8[&9Th&bPluginName&8] &rDetected PlaceholderAPI, initializing...
&8[&9Th&bPluginName&8] &afPlaceholderAPI initialized successfully
```

Y si no hay addons presentes, no debe mostrar ningún mensaje con respecto a los addons.

Cabe recalcar que, debes investigar el proyecto y encontrar todos los addons que tiene el plugin, y mostrar los mensajes de inicialización y confirmación de inicialización de cada addon, en caso de que estén presentes. En caso de que no estén presentes, no mostrar ningún mensaje con respecto a ese addon, como se mencionó anteriormente.

Finalmente, debe mostrar un mensaje en color &a indicando que el plugin se ha iniciado correctamente. Por ejemplo:

```
&8[&9Th&bPluginName&8] &aPlugin started successfully!
```

En caso de que el plugin tenga errores o advertencias, debe mostrar un mensaje en color &c indicando que el plugin no se ha iniciado correctamente. Por ejemplo:

En caso de que el plugin tenga errores:
```
&8[&9Th&bPluginName&8] &cThis is a error message...
```

En caso de que el plugin tenga advertencias:
```
&8[&9Th&bPluginName&8] &eThis is a message warning...
```

En caso de que el plugin se apague, debe mostrar un mensaje en color &c indicando que el plugin se ha apagado correctamente. Por ejemplo:

```
&8[&9Th&bPluginName&8] &cPlugin disabled!
```