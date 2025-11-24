package controllers;

import java.io.*;
import java.util.HashMap;
import java.util.Random;
import dungeon.play.PlayMap;
import dungeon.play.GameCharacter;
import util.math2d.Matrix2D;
import java.util.ArrayList;
import java.util.List;

public class QLearningController extends Controller implements Serializable {
    private static final long serialVersionUID = 1L;

    // Tabla Q: Clave = String (Estado), Valor = double[] (Q-values para UP, RIGHT, DOWN, LEFT)
    public HashMap<String, double[]> qTable;
    
    // Parámetros del Paper
    private double alpha = 0.5;
    private double gamma = 0.9;
    private double epsilon = 0.0; // Se gestionará externamente durante el entrenamiento

    private Random random;
    
    // Persona actual (para definir recompensas)
    public enum Persona { BASELINE, RUNNER, SURVIVALIST, MONSTER_KILLER, TREASURE_COLLECTOR }
    private Persona currentPersona;

    public QLearningController(PlayMap map, GameCharacter controllingChar, Persona persona) {
        super(map, controllingChar, "QLearning_" + persona.name());
        this.currentPersona = persona;
        this.qTable = new HashMap<>();
        this.random = new Random();
    }

    // Constructor para cargar un agente ya entrenado
    public QLearningController(PlayMap map, GameCharacter controllingChar, String label) {
        super(map, controllingChar, label);
        this.currentPersona = Persona.BASELINE; //Se usa BASELINE por defecto
        this.qTable = new HashMap<>(); // Debería sobrescribirse con loadPolicy
        this.random = new Random();
    }

    @Override
    public int getNextAction() {
        String state = getStateRepresentation();

        // Exploración
        if (random.nextDouble() < epsilon) {
            return random.nextInt(4);
        }

        if (!qTable.containsKey(state)) {
            qTable.put(state, new double[]{0, 0, 0, 0});
            return random.nextInt(4); 
        }

        double[] qValues = qTable.get(state);
        double maxQ = -Double.MAX_VALUE;

        // Paso 1: Encontrar el valor máximo
        for (double v : qValues) {
            if (v > maxQ) maxQ = v;
        }

        // Paso 2: Recolectar todos los índices que empatan con el máximo
        List<Integer> bestActions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (qValues[i] == maxQ) {
                bestActions.add(i);
            }
        }

        // Paso 3: Elegir uno al azar uniformemente
        return bestActions.get(random.nextInt(bestActions.size()));
    }

    // Actualización Q-Learning: Q(s,a) <- Q(s,a) + alpha * [reward + gamma * max(Q(s', a')) - Q(s,a)]
    public void updateQTable(String state, int action, double reward, String nextState) {
        double[] qValues = qTable.getOrDefault(state, new double[]{0, 0, 0, 0});
        double currentQ = qValues[action];

        double maxNextQ = 0.0;
        if (qTable.containsKey(nextState)) {
            double[] nextQValues = qTable.get(nextState);
            maxNextQ = -Double.MAX_VALUE;
            for (double v : nextQValues) {
                if (v > maxNextQ) maxNextQ = v;
            }
        }

        double newQ = currentQ + alpha * (reward + gamma * maxNextQ - currentQ);
        qValues[action] = newQ;
        qTable.put(state, qValues);
    }

    public String getStateRepresentation() {
        // Usamos StringBuilder para evitar crear miles de Strings basura
        // Capacidad aproximada: 12x12 tiles + un margen para HP
        StringBuilder sb = new StringBuilder(160); 
        
        int width = map.getMapSizeX();
        int height = map.getMapSizeY();

        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                if(!map.isPassable(x,y)){ 
                    sb.append('#'); 
                } else if(map.isHero(x,y)){
                    sb.append('@'); 
                } else if(map.isMonster(x,y)){
                    sb.append('m'); 
                } else if(map.isReward(x,y)){
                    sb.append('r'); 
                } else if(map.isPotion(x,y)){
                    sb.append('p'); 
                } else if(map.isExit(x,y)){ // Verificar salida antes que entrada por si se solapan visualmente
                    sb.append('X'); 
                } else if(map.isEntrance(x,y)){
                    sb.append('E'); 
                } else {
                    sb.append('.');
                }               
            }
        }
        sb.append("HP:").append(getAbstractHP());
        return sb.toString();
    }
    
    private int getAbstractHP() {
    int hp = controllingChar.getHitpoints();
    if (hp <= 5) return 0; // Cubre 1-5 y muertos (<=0)
    if (hp <= 14) return 1;
    if (hp <= 30) return 2;
    return 3; // 31+
}

    // Tabla 2 del Paper: Recompensas
    public double getReward(boolean moved, boolean killedMonster, boolean wasKilled, boolean reachedExit, boolean collectedTreasure) {
        double r = 0;

        switch (currentPersona) {
            case BASELINE:
                if (reachedExit) r += 0.5;
                break;
            case RUNNER:
                if (reachedExit) r += 0.5;
                if (moved) r -= 0.01; // Costo por movimiento
                break;
            case SURVIVALIST:
                if (reachedExit) r += 0.5;
                if (wasKilled) r -= 1.0;
                if (moved) r -= 0.01;
                break;
            case MONSTER_KILLER:
                if (killedMonster) r += 1.0;
                if (reachedExit) r += 0.5;
                if (wasKilled) r -= 0.5;
                if (moved) r -= 0.01;
                break;
            case TREASURE_COLLECTOR:
                if (collectedTreasure) r += 1.0;
                if (reachedExit) r += 0.5;
                if (wasKilled) r -= 0.5;
                if (moved) r -= 0.01;
                break;
        }
        return r;
    }
    
    // Añadir en QLearningController.java
    public void updateHero(GameCharacter newHero) {
        this.controllingChar = newHero;
    }

    public void setEpsilon(double e) { this.epsilon = e; }
    
    // Métodos para guardar/cargar la política aprendida
    public void savePolicy(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(qTable);
            System.out.println("Policy saved to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadPolicy(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            this.qTable = (HashMap<String, double[]>) ois.readObject();
            System.out.println("Policy loaded from " + filename + " with " + qTable.size() + " states.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}