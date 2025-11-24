# Implementación de Agentes Generativos con Q-Learning (Dungeon Crawler)

Este proyecto es una implementación en Java de agentes autónomos basados en **Aprendizaje por Refuerzo (Q-Learning)**. Los agentes aprenden a navegar y tomar decisiones en niveles de un juego tipo *Dungeon Crawler* (mazmorras) basándose en diferentes "Personas" o estilos de juego.

El código implementa la metodología descrita en el paper de investigación:
**"Generative Agents for Player Decision Modeling in Games"**

El objetivo actual del proyecto es entrenar una personalidad específica (**Treasure Collector**) capaz de maximizar la recolección de tesoros mientras sobrevive en mapas complejos.

## 📋 Características del Proyecto

* **Algoritmo Q-Learning Tabular:** Implementación desde cero sin librerías externas de ML
* **Personas Procedurales:** Capacidad para entrenar distintos perfiles (Baseline, Runner, Survivalist, Monster Killer, Treasure Collector)
* **Sistema de Checkpoints:** El entrenamiento guarda el progreso periódicamente, permitiendo pausar y reanudar el proceso sin perder datos. (No implementado)
* **Optimización de Estado:** Representación eficiente del mapa usando `StringBuilder` para un entrenamiento rápido.
* **Soporte Multi-Mapa:** Capacidad para entrenar y validar agentes en múltiples niveles (mapas 0 al 10).

## 🛠️ Requisitos Previos

Para ejecutar este proyecto necesitas:

* **Java JDK 8** o superior.
* **NetBeans IDE** (Recomendado, ya que el proyecto mantiene la estructura de directorios nativa de NetBeans).
* **Git** (para control de versiones).

## 🚀 Instalación

1.  **Clonar el Repositorio:**
    Abre tu terminal y ejecuta:
    ```bash
    git clone [https://github.com/TU-USUARIO/TU-REPO.git](https://github.com/TU-USUARIO/TU-REPO.git)
    ```
    *(Reemplaza la URL con el link de tu repositorio)*.

2.  **Abrir en NetBeans:**
    * Inicia NetBeans IDE.
    * Ve a **File > Open Project**.
    * Selecciona la carpeta clonada (debería tener el icono de taza de café de Java).
    * Haz clic en **Open Project**.

---

## ⚙️ Guía de Ejecución

El flujo de trabajo consta de dos fases obligatorias: **1. Entrenamiento** y **2. Simulación**.

### Paso 1: Entrenamiento (`QTraining.java`)

El agente nace "en blanco". Debes ejecutar el entrenamiento para que genere su tabla de conocimiento (Q-Table).

1.  En NetBeans, navega a `Source Packages > experiment`.
2.  Haz clic derecho en **`QTraining.java`**.
3.  Selecciona **Run File**.

**¿Qué esperar durante el entrenamiento?**
* El sistema entrenará al agente en los mapas del **0 al 10**.
* Se ejecutarán **250,000 episodios** por cada mapa.
* **Salida:** Al finalizar, se generarán archivos `.ser` (ej: `TREASURE_COLLECTOR_map0.ser`) en la carpeta `trained_agents/`.

### Paso 2: Simulación (`SimulationMode.java`)

Una vez generados los "cerebros" (`.ser`), puedes ver al agente jugar.

1.  Navega a `Source Packages > experiment`.
2.  Haz clic derecho en **`SimulationMode.java`**.
3.  Selecciona **Run File**.

**Resultados:**
* El programa ejecutará 10 partidas de prueba en cada mapa usando el agente entrenado (con `epsilon = 0` para máxima eficiencia).
* Se generarán reportes y mapas de calor en la carpeta `testResults/`.
* Podrás ver en la consola métricas como `treasuresCollected` y `timesCompleted`.

---

## 📂 Estructura del Directorio

* **`dungeons/`**: Contiene los archivos de texto (`map0.txt` - `map10.txt`) que definen la estructura de los niveles (muros, monstruos, tesoros).
* **`trained_agents/`**: Carpeta generada automáticamente donde se almacenan las políticas aprendidas (archivos binarios `.ser`).
* **`testResults/`**: Carpeta de salida para los logs de simulación y heatmaps.
* **`src/controllers/`**:
    * `QLearningController.java`: El "cerebro" del agente. Contiene la tabla Q, la lógica de recompensas y la codificación del estado.
* **`src/experiment/`**:
    * `QTraining.java`: Script principal para el entrenamiento masivo.
    * `SimulationMode.java`: Script para validar el rendimiento de los agentes entrenados.
