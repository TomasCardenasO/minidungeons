package controllers;

import dungeon.play.GameCharacter;
import dungeon.play.PlayMap;
import util.math2d.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Monte Carlo Tree Search algorithm implementing UCT method
 * Adaptado para MiniDungeons 
 * 
 * @author D.Vitonis (original)
 * @modified Adapted for MiniDungeons
 */
public class UCT extends Controller {
	private Random random = new Random();
	
	public UCT(PlayMap map, GameCharacter controllingChar){
		super(map, controllingChar, "UCTController");
	}
	
	/*
	 * rootNode is the starting point of the present state
	 */
	Node rootNode;
	
	/*
	 * currentNode refers to the node we work at every step
	 */
	Node currentNode;
	
	/*
	 * Exploration coefficient - higher values encourage more exploration
	 */
	private float C = (float) 1.4;
	
	/*
	 * Computational limit - increase for better decisions but slower performance
	 * Reduced from 500 to 100 for faster execution
	 */
	protected final int maxIterations = 100;
	
	/**
	 * Main method - required by Controller interface
	 */
	@Override
	public int getNextAction(){
		try {
			return runUCT();
		} catch (Exception e) {
			e.printStackTrace();
			return 0; // 
		}
	}
	
	/**
	 * run the UCT search and find the optimal action for the root node state
	 * @return
	 * @throws InterruptedException
	 */
	public int runUCT() throws InterruptedException{
		/*
		 * Create root node with the present state
		 */
		rootNode = new Node(map.clone());
		
		/*
		 * In computational budget limits apply search
		 */
		int iterations = 0;
		while(!Terminate(iterations)){
			TreePolicy();
			float reward = DefaultPolicy();
			Backpropagate(reward);
			iterations++;
		}
		
		/*
		 * Get the action that directs to the best node
		 * Apply exploitation to find the child with the highest average reward
		 */
		int bestAction = 0;
		double bestValue = Double.NEGATIVE_INFINITY;
		
		for(Node child : rootNode.children){
			double avgReward = child.timesvisited > 0 ? child.reward / child.timesvisited : 0;
			if(avgReward > bestValue){
				bestValue = avgReward;
				bestAction = child.parentAction;
			}
		}
		
		return bestAction;
	}
	
	/**
	 * Expand the nonterminal nodes with one available child. 
	 * Chose a node to expand with BestChild(C) method
	 */
	private void TreePolicy() {
		currentNode = rootNode;
		
		while(!TerminalState(currentNode.state)){
			if(!FullyExpanded(currentNode)){
				Expand();
				return;
			} else {
				BestChild(C);
			}
		}
	}
	
	/**
	 * Simulation of the game. Choose random actions up until the game is over
	 * @return reward (1 for win, 0 for loss)
	 */
	private float DefaultPolicy() {
		PlayMap simState = currentNode.state.clone();
		int steps = 0;
		int maxSteps = 15; // Reduced from 50 to 15 for faster performance
		
		while(!TerminalState(simState) && steps < maxSteps){
			int action = RandomAction(simState);
			simState.updateGame(action);
			steps++;
		}
		return getReward(simState);
	}

	/**
	 * Assign the received reward to every parent of the parent up to the rootNode
	 * Increase the visited count of every node included in backpropagation
	 * @param reward
	 */
	private void Backpropagate(float reward) {
		Node node = currentNode;
		while(node != null){
			node.timesvisited++;
			node.reward += reward;
			node = node.parent;
		}
	}
	
	/**
	 * Check if the node is fully expanded
	 * @param nt
	 * @return
	 */
	private boolean FullyExpanded(Node nt) {
		// A node is fully expanded if all 4 possible actions have been tried
		// or if no more valid actions are available
		if(nt.children.size() >= 4) return true;
		
		// Check if there are any untried valid actions
		for(int i = 0; i < 4; i++){
			boolean tried = false;
			for(Node child : nt.children){
				if(child.parentAction == i){
					tried = true;
					break;
				}
			}
			if(!tried){
				Point2D nextPos = nt.state.getHero().getNextPosition(i);
				if(nt.state.isValidMove(nextPos)){
					return false; // Found an untried valid action
				}
			}
		}
		return true; // All valid actions have been tried
	}

	/**
	 * Check if the state is the end of the game
	 * @param state
	 * @return
	 */
	private boolean TerminalState(PlayMap state) {
		return state.isGameHalted() || !state.getHero().isAlive();
	}
	
	/**
	 * Calculate reward for a given state
	 * Uses multiple heuristics to evaluate game state quality
	 * @param state
	 * @return reward value
	 */
	private float getReward(PlayMap state) {
		// Terminal states
		if(!state.getHero().isAlive()) return -10.0f; // Death is very bad
		if(state.isGameHalted()) return 100.0f; // Winning is excellent
		
		// Intermediate state evaluation
		float reward = 0.0f;
		
		// Health is valuable - normalize by max HP (30)
		reward += (state.getHero().getHitpoints() / 30.0f) * 5.0f;
		
		// Count treasures collected (compare dead rewards)
		int treasures = countTreasuresCollected(state);
		reward += treasures * 3.0f;
		
		// Count monsters killed
		int monstersKilled = countMonstersKilled(state);
		reward += monstersKilled * 2.0f;
		
		// Encourage exploration - reward for tiles explored
		reward += 0.5f;
		
		return reward;
	}
	
	/**
	 * Count treasures collected in current state
	 */
	private int countTreasuresCollected(PlayMap state) {
		int count = 0;
		boolean[][] deadRewards = state.getDeadRewardArray();
		for(int i = 0; i < deadRewards.length; i++){
			for(int j = 0; j < deadRewards[i].length; j++){
				if(deadRewards[i][j]) count++;
			}
		}
		return count;
	}
	
	/**
	 * Count monsters killed in current state
	 */
	private int countMonstersKilled(PlayMap state) {
		int count = 0;
		boolean[][] deadMonsters = state.getDeadMonsterArray();
		for(int i = 0; i < deadMonsters.length; i++){
			for(int j = 0; j < deadMonsters[i].length; j++){
				if(deadMonsters[i][j]) count++;
			}
		}
		return count;
	}

	/**
	 * Choose the best child according to the UCT value
	 * Assign it as a currentNode
	 * @param c
	 */
	private void BestChild(float c) {
		Node nt = currentNode;
		Node bestChild = null;
		double bestValue = Double.NEGATIVE_INFINITY;
		
		for(Node child : nt.children){
			double uctValue = UCTvalue(child, c);
			if(uctValue > bestValue){
				bestValue = uctValue;
				bestChild = child;
			}
		}
		
		currentNode = bestChild;
	}

	/**
	 * Calculate UCT value for the best child choosing
	 * Formula: Q(n)/N(n) + c * sqrt(ln(N(parent))/N(n))
	 * @param n
	 * @param c
	 * @return
	 */
	private float UCTvalue(Node n, float c) {
		if(n.timesvisited == 0) return Float.POSITIVE_INFINITY;
		
		double exploitation = (double)n.reward / n.timesvisited;
		double exploration = c * Math.sqrt(Math.log(n.parent.timesvisited) / n.timesvisited);
		
		return (float)(exploitation + exploration);
	}

	/**
	 * Expand the current node by adding new child to the currentNode
	 */
	private void Expand() {
		/*
		 * Choose untried action
		 */
		int action = UntriedAction(currentNode);
		
		/*
		 * Create a child, populate it and add to the node
		 */
		PlayMap nextState = currentNode.state.clone();
		nextState.updateGame(action);
		
		Node child = new Node(nextState);
		child.parent = currentNode;
		child.parentAction = action;
		currentNode.children.add(child);
		currentNode = child;
	}

	/**
	 * Returns the first untried action of the node
	 * @param n
	 * @return
	 */
	private int UntriedAction(Node n) {
		outer:
		for (int i=0;i<4;i++){
			for (int k=0;k<n.children.size();k++){
				if (n.children.get(k).parentAction == i){
					continue outer;
				}
			}
			Point2D nextPos = n.state.getHero().getNextPosition(i);
			if (n.state.isValidMove(nextPos))
				return i;
		}
		return -1;
	}

	/**
	 * Check if the algorithm is to be terminated, e.g. reached number of iterations limit
	 * @param i
	 * @return
	 */
	private boolean Terminate(int i) {
		if (i>maxIterations) return true;
		return false;
	}

	/**
	 * Used in game simulation to pick random action for the agent
	 * @param state
	 * @return action
	 */
	private int RandomAction(PlayMap state) {
		int action = random.nextInt(4);
		int attempts = 0;
        while (attempts < 10){
        	Point2D nextPos = state.getHero().getNextPosition(action);
        	if(state.isValidMove(nextPos)){
        		return action;
        	}
        	action = random.nextInt(4);
        	attempts++;
        }
        return 0; // Default to UP if no valid move found
	}
	
	/**
	 * Class to store node information, e.g.
	 * state, children, parent, accumulative reward, visited times
	 * @author dariusv
	 *
	 */
	private class Node{
		
		public PlayMap state;
		public List<Node> children = new ArrayList<Node>();
		public Node parent = null;
		public int parentAction=-1;
		public float reward =0;
		public int timesvisited = 0;
		
		
		Node(PlayMap state){
			this.state = state;
		}
	}

	
	// Note: The original Map class has been removed as we now use PlayMap from the dungeon framework
}
