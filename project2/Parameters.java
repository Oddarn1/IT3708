public class Parameters {
    
    static final String problem = "p09";
    static final int populationSize = 15;
    static final int generationSpan = 100;
    static final int eliteSize = 1;
    static final int tournamentSize = 5;
    static final int parentSelectionSize = 5;
    static final double tournamentProb = 0.85;
    static final double crossoverProbability = 0.60;
    static final double mutationProbability = 0.20;
    static final double interDepotMutationRate = 10;
    static final double swappableCustomerDistance = 2.5;

    static final double alpha = 100; // Discount factor for number of active vehicles
    static final double beta = 0.001; // Discount factor for total route length

}
