# TeStudio - Minecraft Plugin Studio

## Rol

.\AGENTS\AGENTS\ROL.md

## Tarea

.\AGENTS\TASK\TASK.md

## Ubicación

(Esta ubicación de archivos es para que el agente pueda acceder a los archivos del proyecto y trabajar en base a ellos. No se debe modificar la ubicación de los archivos, ya que esto puede afectar el funcionamiento del agente).

## Cosas que debes saber.
Cualqueir cosa que se te solicite agentes (como DOC, PLAN, MEMORY, etc), debes buscarla en la sección de ./AGENTS.

## Reglas

- El codigo debe estar BIEN ordenado, con cada categoria con su package y sub-packages segun se necesite. Debes dejar el directorio principal del complemento lo mas limpio posible.
- No implementar notas en el código, solo debes documentar creando archivos en ./AGENTS/DOC/
- Cuando te pierdas relee la Tarea para no inventar cosas no solicitadas.
- Cree un archivo para tu memoria: ./AGENTS/MEMORY/MEMORY.md. Releelo cuando sea necesario. 
- Cuando estés en modo plan y regreses al modo accion debes editar ./AGENTS/PLAN/PLAN.md enviando ahi el plan a detalle, renombra los PLAN.md anteriores a PLAN<ID>.md y envialos a ./AGENTS/PLAN/OLD/. A menos que te lo pida no debes leer el archivo del plan actual, solo debes modificarlo.
- Debes cumplir siempre los requisitos de ./AGENTS/SEGURIDAD/SEGURIDAD.md
- No debes ejecutar tareas que no se te hayan asignado. Simplemente cumple el punto definido en "Tarea"
- No puedes modificar ./AGENTS/TASK/TASK.md, si necesitas asignarte tareas usa el MEMORY.md.
- Cuando se termine de crear el proyecto y tengas el conocimiento de todo el proyecto, debes crear un archivo ./AGENTS/DOC/README.md con la documentación completa del proyecto. En caso de que veas un archivo grande, debes dividirlo en varios archivos y crear un índice en el README.md (el nombre de los archivos debe ser descriptivo y en mayúsculas, por ejemplo: README.md, CONFIGURACION.md, COMANDOS.md, etc).
- Debes revisar las condiciones GIT que se encuentran en ./AGENTS/STANDARDS/GIT.md y cumplirlas.
- Debes cumplir las condiciones de la consola del servidor (SERVER CONSOLE) que se encuentran en ./AGENTS/STANDARDS/CONSOLE.md y cumplirlas.