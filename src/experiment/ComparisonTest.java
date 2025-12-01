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

public class ComparisonTest {
	final int totalRuns = 10;
	final int maxActions = 300;
	
	String outputFolder = "./testResults/";
	
	// Métricas para cada agente
	class AgentMetrics {
		String agentName;
		double[] hpRemaining;
		double[] monstersKilled;
		double[] treasuresCollected;
		double[] potionsDrunk;
		double[] actionsTaken;
		double[] tilesExplored;
		int timesCompleted;
		
		public AgentMetrics(String name) {
			this.agentName = name;
			this.hpRemaining = new double[totalRuns];
			this.monstersKilled = new double[totalRuns];
			this.treasuresCollected = new double[totalRuns];
			this.potionsDrunk = new double[totalRuns];
			this.actionsTaken = new double[totalRuns];
			this.tilesExplored = new double[totalRuns];
			this.timesCompleted = 0;
		}
		
		public void updateMetrics(int run, PlayMap map, int actions) {
			hpRemaining[run] = map.getHero().getHitpoints();
			monstersKilled[run] = Matrix2D.count(map.getDeadMonsterArray());
			treasuresCollected[run] = Matrix2D.count(map.getDeadRewardArray());
			potionsDrunk[run] = Matrix2D.count(map.getDeadPotionArray());
			actionsTaken[run] = actions;
			tilesExplored[run] = Matrix2D.count(map.getAnyVisited());
			if(hpRemaining[run] > 0 && actions < 300) timesCompleted++;
		}
		
		public void printSummary() {
			System.out.println("\n========== " + agentName + " ==========");
			System.out.println("Times Completed: " + timesCompleted + "/" + totalRuns);
			System.out.println("Avg HP Remaining: " + StatisticUtils.average(hpRemaining));
			System.out.println("Avg Monsters Killed: " + StatisticUtils.average(monstersKilled));
			System.out.println("Avg Treasures Collected: " + StatisticUtils.average(treasuresCollected));
			System.out.println("Avg Potions Drunk: " + StatisticUtils.average(potionsDrunk));
			System.out.println("Avg Actions Taken: " + StatisticUtils.average(actionsTaken));
			System.out.println("Avg Tiles Explored: " + StatisticUtils.average(tilesExplored));
		}
		
		public String toCSV() {
			StringBuilder csv = new StringBuilder();
			csv.append(agentName).append(",");
			csv.append(timesCompleted).append(",");
			csv.append(StatisticUtils.average(hpRemaining)).append(",");
			csv.append(StatisticUtils.average(monstersKilled)).append(",");
			csv.append(StatisticUtils.average(treasuresCollected)).append(",");
			csv.append(StatisticUtils.average(potionsDrunk)).append(",");
			csv.append(StatisticUtils.average(actionsTaken)).append(",");
			csv.append(StatisticUtils.average(tilesExplored));
			return csv.toString();
		}
	}
	
	public void runComparison(String filename) {
		String[] temp = filename.split("/");
		String mapFile = temp[temp.length-1];
		String mapIdentifier = mapFile.replace(".txt", "");
		
		System.out.println("\n===============================================");
		System.out.println("TESTING MAP: " + mapIdentifier);
		System.out.println("===============================================");
		
		String asciiMap = "";
		try { 
			asciiMap = new Scanner(new File(filename)).useDelimiter("\\A").next(); 
		} catch(Exception e){
			System.out.println(e.toString());
			return;
		}
		
		Dungeon testDungeon = DungeonLoader.loadAsciiDungeon(asciiMap);
		
		// Test Q-Learning
		AgentMetrics qlearningMetrics = testAgent(testDungeon, mapIdentifier, "QLearning");
		
		// Test UCT
		AgentMetrics uctMetrics = testAgent(testDungeon, mapIdentifier, "UCT");
		
		// Print comparison
		qlearningMetrics.printSummary();
		uctMetrics.printSummary();
		
		// Save comparison report
		String comparisonReport = generateComparisonReport(mapIdentifier, qlearningMetrics, uctMetrics);
		try {
			writeFile(outputFolder + "/comparison_" + mapIdentifier + ".txt", comparisonReport);
			
			// CSV format for easy analysis
			String csvHeader = "Agent,TimesCompleted,AvgHP,AvgMonsters,AvgTreasures,AvgPotions,AvgActions,AvgTiles\n";
			String csvData = csvHeader + qlearningMetrics.toCSV() + "\n" + uctMetrics.toCSV();
			writeFile(outputFolder + "/comparison_" + mapIdentifier + ".csv", csvData);
		} catch(IOException e) {
			System.out.println("Error writing comparison report: " + e.toString());
		}
	}
	
	private AgentMetrics testAgent(Dungeon dungeon, String mapIdentifier, String agentType) {
		AgentMetrics metrics = new AgentMetrics(agentType);
		
		for(int i = 0; i < totalRuns; i++) {
			PlayMap testPlay = new PlayMap(dungeon);
			testPlay.startGame();
			
			Controller testAgent = null;
			
			if(agentType.equals("QLearning")) {
				QLearningController.Persona persona = QLearningController.Persona.TRYHARD;
				String policyFile = "./trained_agents/" + persona.name() + "_" + mapIdentifier + ".ser";
				
				testAgent = new QLearningController(testPlay, testPlay.getHero(), persona);
				
				if(new File(policyFile).exists()) {
					((QLearningController)testAgent).loadPolicy(policyFile);
					((QLearningController)testAgent).setEpsilon(0.0);
				} else {
					System.out.println("Warning: Policy not found for " + policyFile);
				}
			} else if(agentType.equals("UCT")) {
				testAgent = new UCT(testPlay, testPlay.getHero());
			}
			
			int actions = 0;
			while(!testPlay.isGameHalted() && actions < maxActions) {
				testPlay.updateGame(testAgent.getNextAction());
				actions++;
			}
			
			metrics.updateMetrics(i, testPlay, actions);
			
			// Save heatmap
			String visitMap = PlayVisualizer.renderHeatmapDungeon(testPlay);
			try {
				writeFile(outputFolder + "/" + agentType + "_run" + i + "_" + mapIdentifier + ".txt", visitMap);
			} catch(IOException e) {
				System.out.println("Error writing heatmap: " + e.toString());
			}
		}
		
		return metrics;
	}
	
	private String generateComparisonReport(String mapId, AgentMetrics qlearning, AgentMetrics uct) {
		StringBuilder report = new StringBuilder();
		report.append("===================================================\n");
		report.append("       COMPARISON REPORT: ").append(mapId).append("\n");
		report.append("===================================================\n\n");
		
		report.append("COMPLETION RATE:\n");
		report.append("  Q-Learning: ").append(qlearning.timesCompleted).append("/").append(totalRuns);
		report.append(" (").append(qlearning.timesCompleted * 10).append("%)\n");
		report.append("  UCT:        ").append(uct.timesCompleted).append("/").append(totalRuns);
		report.append(" (").append(uct.timesCompleted * 10).append("%)\n\n");
		
		report.append("AVERAGE HP REMAINING:\n");
		report.append("  Q-Learning: ").append(String.format("%.2f", StatisticUtils.average(qlearning.hpRemaining))).append("\n");
		report.append("  UCT:        ").append(String.format("%.2f", StatisticUtils.average(uct.hpRemaining))).append("\n\n");
		
		report.append("AVERAGE MONSTERS KILLED:\n");
		report.append("  Q-Learning: ").append(String.format("%.2f", StatisticUtils.average(qlearning.monstersKilled))).append("\n");
		report.append("  UCT:        ").append(String.format("%.2f", StatisticUtils.average(uct.monstersKilled))).append("\n\n");
		
		report.append("AVERAGE TREASURES COLLECTED:\n");
		report.append("  Q-Learning: ").append(String.format("%.2f", StatisticUtils.average(qlearning.treasuresCollected))).append("\n");
		report.append("  UCT:        ").append(String.format("%.2f", StatisticUtils.average(uct.treasuresCollected))).append("\n\n");
		
		report.append("AVERAGE ACTIONS TAKEN:\n");
		report.append("  Q-Learning: ").append(String.format("%.2f", StatisticUtils.average(qlearning.actionsTaken))).append("\n");
		report.append("  UCT:        ").append(String.format("%.2f", StatisticUtils.average(uct.actionsTaken))).append("\n\n");
		
		report.append("AVERAGE TILES EXPLORED:\n");
		report.append("  Q-Learning: ").append(String.format("%.2f", StatisticUtils.average(qlearning.tilesExplored))).append("\n");
		report.append("  UCT:        ").append(String.format("%.2f", StatisticUtils.average(uct.tilesExplored))).append("\n\n");
		
		// Winner analysis
		report.append("===================================================\n");
		report.append("WINNER ANALYSIS:\n");
		report.append("---------------------------------------------------\n");
		
		if(qlearning.timesCompleted > uct.timesCompleted) {
			report.append("Best Completion Rate: Q-Learning\n");
		} else if(uct.timesCompleted > qlearning.timesCompleted) {
			report.append("Best Completion Rate: UCT\n");
		} else {
			report.append("Best Completion Rate: TIE\n");
		}
		
		if(StatisticUtils.average(qlearning.treasuresCollected) > StatisticUtils.average(uct.treasuresCollected)) {
			report.append("Most Treasures: Q-Learning\n");
		} else if(StatisticUtils.average(uct.treasuresCollected) > StatisticUtils.average(qlearning.treasuresCollected)) {
			report.append("Most Treasures: UCT\n");
		} else {
			report.append("Most Treasures: TIE\n");
		}
		
		if(StatisticUtils.average(qlearning.actionsTaken) < StatisticUtils.average(uct.actionsTaken)) {
			report.append("Most Efficient (fewer actions): Q-Learning\n");
		} else if(StatisticUtils.average(uct.actionsTaken) < StatisticUtils.average(qlearning.actionsTaken)) {
			report.append("Most Efficient (fewer actions): UCT\n");
		} else {
			report.append("Most Efficient: TIE\n");
		}
		
		report.append("===================================================\n");
		
		return report.toString();
	}
	
	private void writeFile(String filename, String content) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(content);
		writer.close();
	}
	
	public static void main(String[] args) {
		ComparisonTest test = new ComparisonTest();
		
		// Test on all maps
		for(int i = 0; i <= 10; i++) {
			test.runComparison("./dungeons/map" + i + ".txt");
		}
		
		System.out.println("\n\n===============================================");
		System.out.println("COMPARISON TEST COMPLETED!");
		System.out.println("Results saved in: ./testResults/");
		System.out.println("===============================================");
	}
}
