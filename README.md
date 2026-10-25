# Ejecución de Induztek Protocolos

Autores e integrantes: Jeicov Díaz y Constanza Lobos.

Aplicación académica para registrar visitas técnicas y pruebas de equipos eléctricos, revisar sus resultados y generar documentos de respaldo.

Funciones disponibles: perfiles de técnico, ingeniería, jefatura y cliente; formularios validados; visitas y protocolos; guardado local; fotografía y galería; ubicación; revisión y formalización; exportación de PDF.

Planificación: https://trello.com/b/qEZfwOsH/induztek-pruebas-y-entrega

## Ejecutar desde Android Studio

1. Descomprimir el archivo ZIP completo.
2. En Android Studio, seleccionar Open y abrir la carpeta android del proyecto. Si ya se abrió otra carpeta, cerrar ese proyecto y abrir android.
3. Configurar JDK 17 para Gradle e instalar Android SDK Platform 35 si Android Studio lo solicita.
4. Esperar a que termine la sincronización de Gradle. La descarga de dependencias requiere internet.
5. Seleccionar la configuración app.
6. Seleccionar un emulador o un teléfono Android 8.0 o superior. Para un teléfono conectado por USB, activar Depuración USB y aceptar la autorización del computador.
7. Pulsar Run y esperar a que se instale y abra Induztek Protocolos.
8. En Usuarios de demostración, seleccionar un perfil y pulsar Entrar. La selección completa las credenciales de prueba. La contraseña de demostración es Induztek2026!.
9. Para la demostración de terreno, entrar como Técnico · Matías Godoy Herrera. Para revisar como ingeniería, cerrar sesión y entrar como Ingeniero · Andrea Contreras Silva.
10. Para probar la galería, disponer de una imagen en el teléfono o emulador. Para probar ubicación, activar Ubicación y conceder el permiso cuando se solicite; en el emulador, configurar una ubicación en sus controles extendidos.

## Generar una APK

1. Abrir una terminal en la carpeta android.
2. En Windows, ejecutar gradlew.bat assembleDebug. En macOS o Linux, ejecutar ./gradlew assembleDebug.
3. Al terminar, encontrar la APK en android/app/build/outputs/apk/debug/app-debug.apk, tomando como referencia la raíz del proyecto.
4. Copiar la APK al teléfono y abrirla para instalar. Si se rechaza una actualización, conservar la instalación anterior hasta respaldar los datos necesarios.

## Ejecutar los servidores opcionales

La aplicación puede abrirse y guardar registros localmente sin ejecutar estos servidores.

1. Instalar e iniciar Docker con Docker Compose.
2. Abrir una terminal en la carpeta InduztekMVP, donde está compose.yaml.
3. Ejecutar docker compose up --build y esperar a que ambos servicios estén disponibles.
4. En la app, abrir Mi perfil y localizar Conexión.
5. Para el emulador Android del computador, usar http://10.0.2.2:8081/ en Servidor de equipos y http://10.0.2.2:8082/ en Servidor de protocolos.
6. Para un teléfono físico, conectar teléfono y computador a la misma red y sustituir 10.0.2.2 por la dirección IP local del computador. Permitir las conexiones a los puertos 8081 y 8082 en esa red.
7. Pulsar Guardar conexión y después Sincronizar ahora. Las direcciones HTTP indicadas corresponden a la compilación debug incluida.
8. Para detener los servidores, pulsar Ctrl+C en la terminal y ejecutar docker compose down.
