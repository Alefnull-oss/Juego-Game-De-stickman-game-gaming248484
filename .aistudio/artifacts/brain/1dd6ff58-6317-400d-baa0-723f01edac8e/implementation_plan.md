# Stickman Dismount: Horizontal Studio Edition & Auto Slow-Motion

Actualización mayor para transformar el juego a orientación horizontal (Landscape), adoptando la estética sobria de estudio fotográfico con fondo gris en degradado, cubos translúcidos flotantes, silueta oscura de stickman con flecha naranja de empuje direccionable, y un sistema automático de cámara lenta cinematográfica al 25% con zoom focal en impactos graves y fracturas óseas.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> - **Estética & Toque Único**: Fondo de estudio gris con viñeta y cubos tenues, stickman en silueta oscura con **destello interno de Rayos X translúcido** que ilumina los huesos rotos en rojo neón dentro del cuerpo al sufrir impactos.
> - **Controles de Lanzamiento**: Botón circular START estilo tiza; pulsar una vez para iniciar la oscilación del medidor de potencia y pulsar de nuevo para ejecutar el desmonte.
> - **Cámara Lenta Automática**: Al detectar fracturas o impactos críticos, el motor reduce la velocidad al 25% (0.25x) y la cámara realiza un zoom suave centrado en el punto de impacto durante 1.5 segundos antes de volver al tiempo real.
> - **Orientación**: Bloqueo en pantalla horizontal (`sensorLandscape`) con interfaz panorámica adaptada.

---

### 1. Overview & Visual Redesign

- **Orientación Horizontal (Landscape)**:
  - Configuración en `AndroidManifest.xml` con `android:screenOrientation="sensorLandscape"`.
  - Distribución panorámica amplia que optimiza la visibilidad de la pendiente, el vuelo del ragdoll y la caída por las escaleras o rampas.
- **Composición Inspirada en la Imagen de Referencia**:
  - **Fondo de Estudio**: Degradado gris suave (`#C8CDD0` a `#9EA7AD`) con viñeta radial oscura en los bordes y cubos flotantes semi-transparentes de fondo.
  - **Terreno y Escaleras**: Plataforma y peldaños sólidos en gris oscuro industrial (`#4E545C` a `#3A3F47`).
  - **Flecha Naranja de Impulso**: Flecha vectorial angular (`#FF9500`) sobre el punto de empuje del stickman que indica la dirección del lanzamiento.
  - **Botones de Control Estilo Tiza/Sketch**:
    - Esquina superior izquierda: Monedas doradas con icono y botón de pausa.
    - Esquina superior derecha: Marcador minimalista de puntuación: `SCORE: 0` y `BEST: XXXX`.
    - Esquina inferior izquierda: Botones ovalados oscuros: `[PREV POSE]`, `[NEXT POSE]`, `[CHARACTER]`, `[VEHICLES]`, `[ESCENARIOS / TRAMPAS]`, `[RAYOS X]`.
    - Esquina inferior derecha: Gran botón circular oscuro `START` con texto "Tap and hold / Pulse para cargar" e indicador perimetral de potencia.

---

### 2. Auto Slow-Motion & Unique Visual Touch

- **Detector de Impacto Severo & Cámara Lenta**:
  - Si una colisión supera el umbral de tensión de un hueso o genera una fuerza mayor a `13f`, se activa el estado `SlowMotionMode`.
  - La escala temporal `timeDilation` se interpola suavemente a `0.25f`.
  - La cámara hace un zoom focal dinámico del 100% al 135% centrado en la articulación impactada.
  - Durante 1.5 segundos se muestra un indicador sutil `SLOW-MO IMPACT` y una viñeta roja leve en pantalla.
  - Transcurridos los 1.5s, la velocidad se restaura gradualmente a `1.0f`.
- **Toque Único: Esqueleto de Rayos X Interno**:
  - El stickman se dibuja como una silueta oscura mate con textura de muñeco de pruebas.
  - En condiciones normales, el interior es opaco.
  - Al recibir daño o fracturarse un hueso, la capa interna de rayos X emerge como un destello bioluminiscente: el hueso fracturado parpadea en rojo neón brillante visible a través de la silueta, desprendiendo chispas óseas y activando respuesta háptica.

---

### 3. Technical Architecture & State Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DismountMainScreen (Landscape)                  │
│  ┌───────────────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │   Top HUD (Coins/Best)│  │   Camera Canvas  │  │  Big START Dock │  │
│  │  - Golden Currency    │  │  - Studio Fog    │  │  - Power Ring   │  │
│  │  - Score & Best Record│  │  - Floating Cubes│  │  - Push Arrow  │  │
│  └───────────────────────┘  │  - Ragdoll / X-Ray│  └─────────────────┘  │
│                             │  - Dynamic Zoom  │                       │
│                             └────────┬─────────┘                       │
│                                      │                                 │
│  ┌───────────────────────────────────▼──────────────────────────────┐  │
│  │                           GameViewModel                          │  │
│  │  - timeDilationState: Float (1.0f -> 0.25f -> 1.0f)              │  │
│  │  - slowMoCountdown: Float (1.5s timer)                           │  │
│  │  - pushPointIndex: Int (Cráneo, Torso, Pelvis, Piernas)          │  │
│  │  - zoomTarget: Float (1.0x -> 1.35x)                             │  │
│  └───────────────────────────────────┬──────────────────────────────┘  │
│                                      │                                 │
│  ┌───────────────────────────────────▼──────────────────────────────┐  │
│  │                        RagdollSimulation                         │  │
│  │  - Multiplied dt: dt * timeDilation                              │  │
│  │  - Impulse applied at selected push particle                     │  │
│  │  - Severe impact listener triggering slow-mo callback            │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

### Verification Plan

1. **Compilación**: Ejecutar `compile_applet` tras aplicar los cambios en el manifiesto, vista, renderizado y modelo.
2. **Pruebas de Funcionalidad**:
   - Comprobar que la app se ejecuta en formato apaisado (Landscape).
   - Verificar la interacción de cambio de pose mediante `PREV POSE` y `NEXT POSE`.
   - Probar el botón `START`: primer toque inicia la carga de potencia, segundo toque dispara el lanzamiento en la dirección de la flecha naranja.
   - Observar el paso a cámara lenta automática (0.25x) y zoom en el momento exacto en que el stickman sufre una fractura o choque fuerte.
   - Comprobar el destello de rayos X dentro de la silueta negra del stickman.
   - Ejecutar la suite de pruebas unitarias (`gradle :app:testDebugUnitTest`).
