package experiment;

import java.io.File;
import java.util.Scanner;
import dungeon.Dungeon;
import dungeon.DungeonLoader;
import dungeon.play.PlayMap;
import util.math2d.Matrix2D;
import controllers.QLearningController;
import controllers.QLearningController.Persona;

public class QTraining {

    final int TOTAL_EPISODES = 150000; 
    final int MAX_STEPS_PER_GAME = 200;
    
    // Configuración de mapas
    final int START_MAP_ID = 0;
    final int END_MAP_ID = 10;
    final String MAP_FOLDER = "./dungeons/";
    final String OUTPUT_FOLDER = "./trained_agents/";

    public void trainAllMapsAndPersonas(Persona p) {
        File folder = new File(OUTPUT_FOLDER);
        if (!folder.exists()) folder.mkdirs();

        // Bucle externo: Iterar por cada mapa disponible
        for (int mapId = START_MAP_ID; mapId <= END_MAP_ID; mapId++) {
            String mapFileName = "map" + mapId + ".txt";
            String fullPath = MAP_FOLDER + mapFileName;
            
            System.out.println("\n==========================================");
            System.out.println("LOADING MAP: " + mapFileName);
            System.out.println("==========================================");

            String asciiMap = "";
            try { 
                asciiMap = new Scanner(new File(fullPath)).useDelimiter("\\A").next(); 
            } catch(Exception e){ 
                System.err.println("Error loading map: " + fullPath);
                e.printStackTrace(); 
                continue; // Saltar al siguiente mapa si falla
            }
            
            Dungeon baseDungeon = DungeonLoader.loadAsciiDungeon(asciiMap);

            // Bucle interno: Entrenar cada personalidad en el mapa actual
            //for (Persona p : Persona.values()) {
            //    System.out.println("-> Training Persona: " + p.name() + " on " + mapFileName);
            //    trainAgent(baseDungeon, p, mapFileName);
            //}
            
            //Entrenar a una sola Persona
            System.out.println("-> Training Persona: " + p.name() + " on " + mapFileName);
            trainAgent(baseDungeon, p, mapFileName);
        }
    }

    private void trainAgent(Dungeon baseDungeon, Persona persona, String mapName) {
        PlayMap map = new PlayMap(baseDungeon);
        map.startGame(); 
        
        QLearningController agent = new QLearningController(map, map.getHero(), persona);

        // Definir nombre del Checkpoint (ej: ckpt_TREASURE_COLLECTOR_map0.ser)
        // Se guarda en la misma carpeta OUTPUT_FOLDER
        String mapIdStr = mapName.replace(".txt", "");
        String ckptName = OUTPUT_FOLDER + "ckpt_" + persona.name() + "_" + mapIdStr + ".ser";
        File ckptFile = new File(ckptName);

        // Verificar si existe checkpoint y cargar
        int startEpisode = 0;
        if (ckptFile.exists()) {
            startEpisode = agent.loadCheckpoint(ckptName);
        }

        // El bucle empieza donde nos quedamos (startEpisode) en lugar de 0
        for (int i = startEpisode; i < TOTAL_EPISODES; i++) {
            map.startGame();
            agent.updateHero(map.getHero()); 
            
            // Recálculo de Epsilon (funciona bien al resumir porque depende de 'i')
            double epsilon = 1.0;
            if (i > 2500) {
                 double progress = (double)(i - 2500) / (TOTAL_EPISODES - 2500);
                 epsilon = 1.0 - (progress * 0.9); 
                 if (epsilon < 0.1) epsilon = 0.1;
            }
            agent.setEpsilon(epsilon);

            int steps = 0;
            boolean done = false;

            int prevMonsters = Matrix2D.count(map.getDeadMonsterArray());
            int prevTreasures = Matrix2D.count(map.getDeadRewardArray());
            
            while (!done && steps < MAX_STEPS_PER_GAME) {
                String state = agent.getStateRepresentation();
                int action = agent.getNextAction();
                
                map.updateGame(action);
                
                boolean isDead = !map.getHero().isAlive();
                boolean isExit = map.isGameHalted() && !isDead;
                int currMonsters = Matrix2D.count(map.getDeadMonsterArray());
                int currTreasures = Matrix2D.count(map.getDeadRewardArray());
                
                boolean killed = (currMonsters > prevMonsters);
                boolean collected = (currTreasures > prevTreasures);
                boolean moved = true;

                double reward = agent.getReward(moved, killed, isDead, isExit, collected);
                String nextState = agent.getStateRepresentation();
                
                agent.updateQTable(state, action, reward, nextState);
                
                prevMonsters = currMonsters;
                prevTreasures = currTreasures;
                steps++;
                
                if (map.isGameHalted()) done = true;
            }
            
            // GUARDADO DE CHECKPOINT
            // Guardamos cada 10,000 episodios para no escribir en disco constantemente
            if (i % 15000 == 0 && i > 0) {
                agent.saveCheckpoint(ckptName, i);
                System.out.println("   Saved Checkpoint at " + i + "/" + TOTAL_EPISODES + " | QTable: " + agent.qTable.size());
            }
        }

        // Guardado Final (El archivo definitivo)
        String saveName = persona.name() + "_" + mapName.replace(".txt", "") + ".ser";
        agent.savePolicy(OUTPUT_FOLDER + saveName);
        
        // Borrar el checkpoint porque ya terminamos exitosamente
        if(ckptFile.exists()) {
            ckptFile.delete();
            System.out.println("   Training complete. Checkpoint removed.");
        }
    }

    public static void main(String[] args) {
        QTraining trainer = new QTraining();
        trainer.trainAllMapsAndPersonas(Persona.TREASURE_COLLECTOR);
    }
}