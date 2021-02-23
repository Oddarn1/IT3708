public class Parameters {
    
    static final int populationSize = 400;
    static final int generationSpan = 3000;
    static final int eliteSize = 4;
    static final double crossoverProbability = 0.60;
    static final double intraDepotMutationProbability = 0.20;
    static final double interDepotMutationProbability = 0.25;
    static final double interDepotMutationRate = 10;

    static final double alpha = 100; // Discount factor for number of active vehicles
    static final double beta = 0.001; // Discount factor for total route length

}