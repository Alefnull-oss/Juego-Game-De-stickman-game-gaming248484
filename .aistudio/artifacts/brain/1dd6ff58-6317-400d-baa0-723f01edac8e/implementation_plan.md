# Stickman Dismount: Físicas Avanzadas, Desmembramiento y Profundidad de Vehículos

Transformación integral del sistema vehicular para dotar a cada vehículo de masa destructible realista, suspensión y deformación articular, capacidad de romperse en piezas individuales tras impactos violentos, colisión directa para aplastar o atropellar al Stickman, y renderizado por capas con cabina envolvente para que el muñeco esté integrado dentro del vehículo en lugar de flotar por encima.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario:**
> - **Desmembramiento y Deformación Estructural**: Los vehículos abandonan las barras rígidas irrompibles. Cada vehículo se compone de chasis multibloque, suspensión deformable y ruedas con uniones rompibles (*breakable constraints*). Al sufrir impactos críticos contra terreno, muros o tras explosiones, el vehículo se despieza: ruedas sueltas salen disparadas rodando de forma autónoma y los paneles/ejes se desprenden rodando por la pista.
> - **Aplastamiento y Atropello Post-Eyección**: Se implementa detección de colisión física bidireccional entre las partículas y componentes del vehículo y las articulaciones del Stickman. Si el vehículo vuelca sobre el piloto, una rueda desprendida lo golpea o el chasis sale rodando y lo arrolla tras la eyección, se transmite daño masivo, fractura de huesos inmediata, sangre/chispas y transferencia de momento físico.
> - **Capas de Profundidad (Cabina Envolvente)**: El renderizado visual cambia de una sola capa plana a un sistema multiestrato:
>   1. **Capa Trasera**: Ruedas posteriores, chasis de fondo, asiento y respaldo interior.
>   2. **Capa Intermedia**: Stickman en postura de conducción (manos sujetando manillar/volante, torso apoyado en el asiento, piernas en pedales/estribos).
>   3. **Capa Frontal**: Carrocería frontal, parachoques, barras antivuelco, parabrisas/malla metálica y ruedas delanteras/exteriores. De este modo el Stickman se ve verdaderamente sentado *dentro* del vehículo.

---

### 1. Overview & Core Concept

- **Qué hace**: Convierte los vehículos de meros armazones rígidos en entidades físicas complejas, interactivas y destructibles. Los vehículos sufren daño por tensión mecánica en sus juntas, pueden doblarse, perder ruedas o partirse a la mitad; sus fragmentos se convierten en proyectiles peligrosos capaces de aplastar al Stickman en cualquier instante de la caída.
- **Público Objetivo**: Jugadores que buscan una experiencia de simulación de accidentes (*crash-test / dismount*) visceral, cómica, física y visualmente convincente al estilo de los clásicos de la saga Stair Dismount / Turbo Dismount.
- **Valor Principal**: Coherencia física y satisfacción táctil: ver cómo un *Muscle Car* o un *Carro de Compras* se despedaza contra los escalones o una mina, atrapando al Stickman en su interior o pasándole por encima tras eyectarlo.

---

### 2. User Experience & Visual Design

#### 2.1 Flujo del Usuario en Carrera
1. **Configuración en Muelles**: Al seleccionar un vehículo (Carro de Compras, Scooter, Moto Stunt, Muscle Car, Trineo Cohete), la vista previa muestra al Stickman acomodado naturalmente en el habitáculo/asiento con sus extremidades amarradas a los controles físicos.
2. **Impulso y Rodaje Inicial**: Al accionar el medidor de fuerza, el vehículo rueda por el terreno comprimiendo dinámicamente su suspensión y amortiguadores en cada bache o escalón.
3. **Colisión y Desmembramiento**:
   - En choques de media intensidad: los amortiguadores se comprimen al límite y el chasis sufre deformación plástica visible.
   - En colisiones de alta intensidad (> umbral de ruptura): los pernos de las ruedas o soportes del chasis se fracturan con sonido metálico y chispas. Las piezas sueltas continúan rodando con su propia velocidad angular.
4. **Interacción con el Piloto**:
   - Si la fuerza de desaceleración excede la resistencia del asiento, los agarres se rompen y el Stickman es catapultado hacia adelante.
   - Si el vehículo rota en el aire o choca contra una pared, el chasis o las ruedas pueden impactar violentamente la cabeza, pecho o extremidades del Stickman, detonando el sistema de rayos X y fracturas.

#### 2.2 Identidad Visual & Capas de Renderizado
- **Paleta y Tonalidades**:
  - Chasis Estructural Interior: Grafito oscuro técnico (`#1C1E22`) con tornillería y ejes contrastados.
  - Carrocería / Paneles Exteriores: Tonos industriales distintivos por vehículo (ej. Rojo Carmín para el Muscle Car, Acero Galvanizado para el Carro de Compras, Amarillo Hazard para la Moto Stunt, Naranja Cohete para el Trineo).
  - Rayaduras y Chispas: Emisión de partículas amarillas/blancas en los puntos donde el chasis roza el asfalto.
- **Estructura por Capas**:
  - `drawVehicleBackdrop()`: Ejes traseros, ruedas interiores, respaldos de cabina.
  - `drawStickmanWithXRay()`: Silueta anatómica del piloto con esqueleto X-Ray brillante.
  - `drawVehicleForeground()`: Protectores laterales, volante/manillar, capó, parachoques y ruedas delanteras.
  - `drawVehicleDebris()`: Piezas desprendidas que vuelan libremente con rotación y marcas de fractura.

---

### 3. Key Product Decisions & Trade-Offs

- **Decisión 1: Restricciones de Distancia Rompibles con Fatiga Estructural**
  - *Enfoque*: Cada perno/barra del vehículo cuenta con un `breakThreshold` ajustado a su masa y resistencia real. Las ruedas se conectan con muelles de doble paso que se parten ante fuerzas cortantes elevadas.
  - *Por qué*: Proporciona desmembramiento natural basado en la física verlet sin requerir un motor de física externo pesado.
  - *Alternativas Descartadas*: Cajas rígidas indivisibles (comportamiento actual) o animaciones predefinidas de rotura (anti-físicas).

- **Decisión 2: Colisión Partícula-Cuerpo para Aplastamiento (Vehículo vs Stickman)**
  - *Enfoque*: En cada sub-paso de la simulación física (subSteps = 4), se evalúa la distancia entre las partículas del chasis/ruedas y las articulaciones del Stickman. Si la distancia es menor que la suma de radios y hay velocidad relativa alta, se aplica una fuerza elástica de rechazo instantánea y se transmite impulso a los huesos, detonando fracturas y sacudida de cámara.
  - *Por qué*: Resuelve el requerimiento del usuario de que el vehículo pueda atropellar, aplastar o golpear al Stickman tras la eyección o en vuelcos.

- **Decisión 3: Arquitectura Multicapa para Vehículos en Canvas**
  - *Enfoque*: Modularizar `VehicleBody` en `rearComponents`, `seatAnchor`, `frontComponents` y `detachedDebris`, renderizando en dos pases ordenados alrededor del Stickman.
  - *Por qué*: Corrige el problema de que el Stickman se dibujaba siempre por delante como una calcomanía superpuesta.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           RagdollSimulation Loop                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌────────────────────────┐                    ┌─────────────────────────────┐  │
│  │      VehicleBody       │                    │         RagdollBody         │  │
│  │ - Chassis Particles    │                    │ - 15 Skeletal Particles     │  │
│  │ - Breakable Struts     │                    │ - 19 Bone Constraints       │  │
│  │ - Detachable Wheels    │                    │ - Bone Fracture Tracking    │  │
│  │ - Debris Particle Pool │                    │ - Center of Mass Tracking   │  │
│  └───────────┬────────────┘                    └──────────────┬──────────────┘  │
│              │                                                │                 │
│              │       ┌────────────────────────────────┐       │                 │
│              └──────►│ Vehicle-Ragdoll Collision Step │◄──────┘                 │
│                      │ - Inter-particle impulse       │                         │
│                      │ - Crush & run-over damage      │                         │
│                      │ - Bone fracture trigger        │                         │
│                      └────────────────────────────────┘                         │
│                                       │                                         │
│                                       ▼                                         │
│                      ┌────────────────────────────────┐                         │
│                      │      DismountCanvas (M3)       │                         │
│                      │ 1. drawVehicleBackdrop()       │                         │
│                      │ 2. drawStickmanWithXRay()      │                         │
│                      │ 3. drawVehicleForeground()     │                         │
│                      │ 4. drawDetachedDebris()        │                         │
│                      │ 5. drawImpactParticles()       │                         │
│                      └────────────────────────────────┘                         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Cambios Principales en Archivos:
1. **`com.example.physics.VehicleBody`**:
   - Añadir soporte para partes rompibles: parachoques delantero, parachoques trasero, techo/jaula antivuelco, rueda delantera y rueda trasera.
   - Lista de `debrisConstraints` y `detachedParts` para piezas que se desprenden y siguen simulando física verlet de forma independiente.
   - Cálculo de puntos de apoyo y absorción de energía en deformación.
2. **`com.example.physics.RagdollSimulation`**:
   - Integración de bucle de colisión inter-cuerpos `checkVehicleRagdollCollisions()`: calcula impactos entre partículas del vehículo y articulaciones del Stickman (cabeza, cuello, pecho, pelvis, rodillas).
   - Generación de sonidos de choque metálico pesado, chirrido de metal, crujido de huesos y chispas al aplastar al piloto.
3. **`com.example.ui.DismountCanvas`**:
   - Desacoplar `drawVehicle` en `drawVehicleRearLayer` y `drawVehicleFrontLayer` con diseño estético de habitáculo, ruedas detalladas con llantas y dibujo neumático, carrocería estilizada y piezas desprendidas con bordes mellados.

---

### Verification Plan

1. **Prueba de Compilación**:
   - Ejecutar `compile_applet` tras completar las modificaciones del modelo físico y de renderizado.
2. **Verificación de Físicas y Desmembramiento**:
   - Iniciar simulación con *Muscle Car* o *Carro de Compras* en la escalera o rampa con baches.
   - Verificar que al impactar contra el suelo o muros, las ruedas y partes del chasis se rompen y salen rodando.
3. **Verificación de Atropello y Aplastamiento**:
   - Verificar que si el vehículo cae sobre el Stickman o lo embiste rodando tras eyectarlo, se causan fracturas de huesos con X-Ray rojo, audio de impacto y sacudida de pantalla.
4. **Verificación de Profundidad Visual**:
   - Constatar que el Stickman se ve correctamente dentro de la cabina (chasis detrás de él, brazos en el manillar/volante y paneles/barras por delante).
5. **Pruebas Automatizadas**:
   - Ejecutar suite de pruebas unitarias (`gradle :app:testDebugUnitTest`).
