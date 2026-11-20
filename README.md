# Turno de Medianoche

Juego de terror 3D original para Unity/C#, pensado para Android en horizontal. No incluye personajes, nombres, música, modelos ni texturas de Five Nights at Freddy's: el escenario y el animatrónico **El Campanero** se construyen en tiempo de ejecución con geometría y texturas procedurales.

## Estado de esta versión

Prototipo jugable v0.1: exploración en primera persona, controles táctiles, linterna con batería, tres fusibles coleccionables, monstruo perseguidor, salida, victoria/derrota y temporizador. El contenido 3D se genera desde código al iniciar la escena. Aún requiere prueba en Unity y teléfono Android antes de considerarse terminado.

## Abrir y compilar

1. Abrir la carpeta del proyecto en Unity **2022.3.62f1** o una versión 2022.3 LTS compatible.
2. Instalar Android Build Support (SDK/NDK y OpenJDK) en Unity Hub.
3. Seleccionar Android, abrir `Assets/Scenes/Main.unity` y compilar como APK.

Paquete Android: `com.chapiodara.turnodemedianoche`. Orientación: paisaje. Resolución y sombras configuradas para priorizar móviles modestos.

## Controles

- MOVER: joystick virtual (o WASD en editor).
- MIRAR: arrastrar el panel derecho.
- CORRER: mantener el botón.
- LINTERNA: encender/apagar.
- Reúne los tres fusibles y llega a la puerta roja del fondo. La linterna ralentiza al Campanero cuando lo iluminas, pero su batería se agota.

## Compilación automatizada

El flujo manual de GitHub Actions usa Unity Builder y necesita un secreto de licencia Unity llamado `UNITY_LICENSE`; sin ese secreto no puede exportar el APK. El artefacto de CI no sustituye una prueba visual/táctil en un dispositivo Android.
