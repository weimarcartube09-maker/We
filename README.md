# TopoCalc Topografía
Aplicación Android nativa para cálculos topográficos de campo.

## Módulos incluidos
- Calculadora básica.
- Línea base entre dos puntos: ΔX, ΔY, ΔZ, distancia horizontal, distancia inclinada, azimut en grados y DMS, pendiente % y ángulo vertical.
- Replanteo: desde estación a punto objetivo.
- Radiación: estación + azimut + distancia + ángulo vertical.
- Puntos a línea: proyección ortogonal, progresiva/estación y offset.
- Nivelación: altura de instrumento y cota de punto.
- Círculo por 3 puntos.

## Convenciones
- Coordenadas: X=Norte, Y=Este, Z=Cota.
- Azimut: medido desde el Norte, en sentido horario, 0–360°.
- Pendiente % = ΔZ / distancia horizontal × 100.
- Distancia inclinada se obtiene con la distancia horizontal y ΔZ cuando corresponda.

## Compilación
Abrir la carpeta en Android Studio y ejecutar Build > Build APK(s).
