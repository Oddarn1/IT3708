public class Parameters {
    
    static final String problem = "p23";
    static final int populationSize = 70;
    static final int generationSpan = 50;
    static final int eliteSize = 3;
    static final int tournamentSize = 6;
    static final int parentSelectionSize = 30;
    static final double tournamentProb = 0.85;
    static final double crossoverProbability = 0.60;
    static final double intraDepotMutationProbability = 0.20;
    static final double interDepotMutationProbability = 0.25;
    static final double interDepotMutationRate = 10;
    static final double swappableCustomerDistance = 2;

    static final double alpha = 100; // Discount factor for number of active vehicles
    static final double beta = 0.001; // Discount factor for total route length

}
