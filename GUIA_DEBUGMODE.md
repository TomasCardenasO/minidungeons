# Guía de Uso: DebugMode con Selección de Agentes

## Descripción

`DebugMode` ahora permite seleccionar diferentes agentes de IA mediante la terminal, sin necesidad de modificar y recompilar el código.

---

## Uso Básico

### Sintaxis General

```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode [AGENTE] [MAPA]
```

**Parámetros:**
- `[AGENTE]`: Tipo de agente a ejecutar (opcional, por defecto: `UCT`)
- `[MAPA]`: Archivo del dungeon (opcional, por defecto: `./dungeons/map0.txt`)

---

## Ejemplos de Ejecución

### Ejecutar con valores por defecto
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode
```
- Agente: `UCT`
- Mapa: `map0.txt`

### Ejecutar con UCT explícitamente
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode UCT
```

### Ejecutar con Q-Learning
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode QLearning
```

### Ejecutar con Pathfinding
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode Pathfinding
```

### Ejecutar con Random
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode Random
```

### Ejecutar con Roomba
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode Roomba
```

### Ejecutar con Zombie
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode Zombie
```

### Cambiar el mapa
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode UCT ./dungeons/map5.txt
```

### Ejecutar Q-Learning en map3
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode QLearning ./dungeons/map3.txt
```

---

## Agentes Disponibles

| Nombre | Alias | Descripción |
|--------|-------|-------------|
| `UCT` | `MCTS` | Monte Carlo Tree Search - Planificación por simulaciones |
| `QLearning` | `Q-Learning` | Aprendizaje por refuerzo (requiere entrenamiento previo) |
| `Pathfinding` | `Path` | Navegación con algoritmo de pathfinding |
| `Random` | - | Acciones aleatorias |
| `Roomba` | - | Movimiento tipo aspiradora |
| `Zombie` | - | Movimiento tipo zombie |

**Nota:** Los nombres de agentes **no son case-sensitive** (puedes usar `uct`, `UCT`, o `Uct`).

---

## Compilación

Si modificas el código, recompila con:

```bash
javac -cp "lib\ai_path.jar" -d build\classes -sourcepath src src\experiment\DebugMode.java
```

**Nota:** Si Java no está en tu PATH del sistema, usa el path completo del ejecutable `javac` de tu instalación de JDK.

---

## Mapas Disponibles

Los dungeons se encuentran en el directorio `./dungeons/`:

- `map0.txt`
- `map1.txt`
- `map2.txt`
- `map3.txt`
- `map4.txt`
- `map5.txt`
- `map6.txt`
- `map7.txt`
- `map8.txt`
- `map9.txt`
- `map10.txt`

---

## Notas Importantes

### Q-Learning
El agente `QLearning` usa la personalidad **BASELINE** por defecto. Si necesitas otras personalidades (RUNNER, SURVIVALIST, etc.), deberás entrenar agentes específicos usando `QTraining.java`.

### Límite de Acciones
DebugMode ejecuta un máximo de **300 acciones** por defecto. Si el agente no completa el dungeon en ese tiempo, la ejecución termina automáticamente.

---

## Ventajas del Sistema Actual

✅ **No requiere recompilar** para cambiar de agente  
✅ **Fácil comparación** entre diferentes agentes en el mismo mapa  
✅ **Scripts automatizables** para testing  
✅ **Valores por defecto sensatos** (UCT en map0)  

---

## Solución de Problemas

### Error: "Unknown agent type"
Si introduces un nombre de agente inválido, el sistema:
1. Muestra un mensaje de error
2. Lista los agentes disponibles
3. Usa `UCT` como fallback

**Ejemplo:**
```bash
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode InvalidAgent

# Output:
# Unknown agent type: InvalidAgent
# Available agents: UCT, QLearning, Pathfinding, Random, Roomba, Zombie
# Using UCT as default...
```

### Error de compilación
Si encuentras errores al compilar, asegúrate de:
1. Tener Java 8 instalado
2. Usar el classpath correcto (`lib\ai_path.jar`)
3. El directorio `build\classes` existe

---

## Comparación de Agentes

Para comparar el rendimiento de diferentes agentes en el mismo dungeon, puedes ejecutar:

```bash
# Probar UCT
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode UCT ./dungeons/map0.txt

# Probar Q-Learning
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode QLearning ./dungeons/map0.txt

# Probar Pathfinding
java -cp "build\classes;lib\ai_path.jar" experiment.DebugMode Pathfinding ./dungeons/map0.txt
```

Observa:
- Número de acciones hasta completar
- Salud final del héroe
- Tesoros recolectados
- Monstruos eliminados

---

**Última actualización:** 30 de Noviembre, 2025
