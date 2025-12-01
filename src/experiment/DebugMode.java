package experiment;

import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import controllers.*;

import dungeon.Dungeon;
import dungeon.DungeonLoader;
import dungeon.play.PlayMap;
import dungeon.visualization.PlayVisualizer;

import util.math2d.Matrix2D;
import util.statics.StatisticUtils;

public class DebugMode {
	final int maxActions = 300;
	
	public void runTest(String filename, String agentType){
		String[] temp = filename.split("/");
		String mapFile = temp[temp.length-1];
		
		String asciiMap = "";
		try { 
			asciiMap = new Scanner(new File(filename)).useDelimiter("\\A").next(); 
		} catch(Exception e){
			System.out.println(e.toString());
		}
		Dungeon testDungeon = DungeonLoader.loadAsciiDungeon(asciiMap);
		PlayMap testPlay = new PlayMap(testDungeon);
		testPlay.startGame();

		Controller testAgent = createAgent(agentType, testPlay);

		int actions = 0;

		System.out.println(testPlay.toASCII(true));
		while(!testPlay.isGameHalted() && actions<maxActions){
			testPlay.updateGame(testAgent.getNextAction());
			actions++;
			System.out.println("----- ACTION "+actions+" -----");
			System.out.println(testPlay.toASCII(true));
			//System.out.println(PlayVisualizer.renderHeatmapDungeon(testPlay));
		}
	}
	
	private Controller createAgent(String agentType, PlayMap playMap) {
		switch(agentType.toLowerCase()) {
			case "uct":
			case "mcts":
				return new UCT(playMap, playMap.getHero());
			case "qlearning":
			case "q-learning":
				return new QLearningController(playMap, playMap.getHero(), QLearningController.Persona.BASELINE);
			case "pathfinding":
			case "path":
				return new PathfindingController(playMap, playMap.getHero());
			case "random":
				return new RandomController(playMap, playMap.getHero());
			case "roomba":
				return new RoombaController(playMap, playMap.getHero());
			case "zombie":
				return new ZombieController(playMap, playMap.getHero());
			default:
				System.out.println("Unknown agent type: " + agentType);
				System.out.println("Available agents: UCT, QLearning, Pathfinding, Random, Roomba, Zombie");
				System.out.println("Using UCT as default...");
				return new UCT(playMap, playMap.getHero());
		}
	}
	
	public static void main(String[] args) {
		DebugMode exp = new DebugMode();
		
		// Default values
		String mapFile = "./dungeons/map0.txt";
		String agentType = "UCT";
		
		// Parse command line arguments
		if(args.length > 0) {
			agentType = args[0];
		}
		if(args.length > 1) {
			mapFile = args[1];
		}
		
		System.out.println("Running DebugMode with agent: " + agentType + " on map: " + mapFile);
		exp.runTest(mapFile, agentType);
	}
}
