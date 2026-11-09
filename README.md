# TopoBT actualizado · versión 1.9

Proyecto Android para trabajo topográfico con Bluetooth SPP.

## Funciones añadidas

- Perfiles independientes para Sokkia CX, Topcon GM, Topcon ES-105, Leica TS/TCR/TCRM, GeoMax y Topcon GTS-323.
- El perfil se recuerda por dirección Bluetooth, de modo que cada estación conserva su protocolo.
- **Medir** solicita una lectura; **Medir y guardar** solicita una lectura y la guarda al recibir respuesta. No hay lectura automática continua ni comando manual.
- Vista en vivo de la lectura recibida (distancia, Hz y V); E/N/Z se calculan cuando la estación y orientación están configuradas.
- Esquema cenital de la estación y línea de mira.
- Captura de lecturas desde los formularios de los programas.
- En cada programa, acciones **CALCULAR** (datos manuales), **MEDIR** y **REGISTRO** (medir y guardar).
- En Área, los registros medidos agregan vértices; al repetir el primer vértice o cerrar el polígono se muestran las áreas 2D y 3D.
- Programa **Sección vial / placa** con coordenadas medida y eje, kilometraje, azimut, pendientes, ángulo incluido, espesor, desplazamientos, ensanchamiento, orientación y gráfica de replanteo.
- En Sección vial, el punto se proyecta sobre el eje y su perpendicular definida por el azimut y el ángulo incluido; el alzado aplica pendiente longitudinal, pendiente de placa y espesor ingresados.
- Selector de objetivo Prisma/Sin prisma y altura de prisma para los cálculos de coordenadas.
- Cálculo de área de polígono, curvas circulares y espirales; entrada opcional de varios PI en filas.
- Carpetas de trabajo y exportación de puntos a TXT mediante el selector de archivos de Android.

## Comunicación de instrumentos

En modo automático, la app intenta identificar el perfil por el nombre Bluetooth y lo recuerda para ese equipo. También se puede elegir marca/modelo en el selector antes de conectar. Los perfiles SDR de Sokkia CX y Topcon GM/ES solicitan distancia y ángulos con `0x11`. Leica TS/TCR/TCRM y GeoMax usan el perfil GSI, que solicita Hz, V y distancia con `GET/M/WI21/WI22/WI31`; las respuestas se procesan al terminar CR, LF o ETX. El cálculo de E/N/Z usa los datos configurados en **Estación**.

La app solicita lecturas automáticamente al conectar, usando el protocolo del perfil seleccionado. Los valores de coordenadas de estación, azimut de referencia y alturas de instrumento/prisma se mantienen como datos de configuración introducidos por el operador; la app no fabrica esos valores si el equipo no los entrega.

En Leica GSI, el selector de Prisma/Sin prisma envía al instrumento los parámetros GSI de prisma y modo EDM; la aplicación pide confirmar visualmente el modo en la estación, porque el envío no equivale a una lectura de confirmación. En Sokkia, Topcon y GeoMax se actualizan los cálculos locales; no se envía un comando de modo EDM que no esté confirmado para ese perfil.

El Topcon GTS-323 aparece como perfil separado. El manual público describe salida por la tecla REC y remite el control desde un colector a un manual de interfaz GTS opcional; por eso el botón **Medir** no transmite un comando especulativo para ese modelo. Los perfiles Topcon ES/GM usan SDR; configure el tipo de comunicación compatible en el instrumento.

## Compilar

Para usar GitHub, descomprime el ZIP y sube el contenido del proyecto a la raíz del repositorio. Deben quedar `app/`, `build.gradle`, `settings.gradle` y `.github/workflows/build.yml` en ese nivel; subir solo el ZIP como archivo no ejecuta el flujo.

Después, abre **Acciones**, selecciona **Build APK** y revisa la ejecución más reciente. Cuando termine correctamente, el APK aparece en el artefacto `TopoBT-apk-v1.9`. El flujo usa Android Gradle Plugin 9.4.0, Gradle 9.6.1 y JDK 17.

También se puede abrir el proyecto en Android Studio y compilar `app` como APK debug.
