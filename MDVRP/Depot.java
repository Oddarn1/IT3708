import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import data_classes.*;

public class Depot{

    public final int id, maxLoad, maxVehicles, maxDuration, x, y;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Customer> swappableCustomers = new ArrayList<>();
    
    public Depot(int id, int maxVehicles, int maxDuration, int maxLoad, int x, int y) {
        this.id = id;
        this.maxLoad = maxLoad;
        this.maxDuration = maxDuration;
        this.maxVehicles = maxVehicles;
        this.x = x;
        this.y = y;
    }

    public Depot(Depot depot, List<Vehicle> vehicles) {
        this.id = depot.id;
        this.maxLoad = depot.maxLoad;
        this.maxDuration = depot.maxDuration;
        this.maxVehicles = depot.maxVehicles;
        this.x = depot.x;
        this.y = depot.y;
        this.swappableCustomers = depot.getSwappableCustomers();
        this.vehicles = vehicles;
    }

    public Vehicle getVehicleById(int id) {
        return vehicles.stream()
                       .filter(v -> v.id == id)
                       .findAny()
                       .orElse(null);
    }

    public boolean removeCustomer(Customer c) {
        boolean removed = vehicles.stream()
                                  .anyMatch(vehicle -> vehicle.removeCustomer(c));
        return removed;
    }

    public Vehicle randomRoute(){
        return getAllVehicles().get(Utils.randomInt(getAllVehicles().size()));
    }


    public boolean insertAtMostFeasible(Customer customer) {
        List<Tuple<Tuple<Vehicle, Integer>, Double>> feasible = Collections.synchronizedList(new ArrayList<>());
        for (int i=0; i<this.vehicles.size(); i++) {
            Vehicle v = this.vehicles.get(i);
            ThreadedInsertion ti = new ThreadedInsertion(v, customer, feasible);
            Population.executor.execute(ti);
        }
        if (feasible.size() == 0) {
            return false;
        }
        Vehicle v = null;
        int index = -1;
        double minFit = Integer.MAX_VALUE;
        synchronized (feasible){
            for (Tuple<Tuple<Vehicle, Integer>, Double> t : feasible){
                if (t.y < minFit){
                    v = t.x.x;
                    index = t.x.y;
                    minFit = t.y;
                }
            }
        }
        v.insertCustomer(customer, index);
        return true;
    }

    public double getDistanceDeviation(){
        if (this.maxDuration == 0){
            return 0.0;
        }
        return vehicles.stream()
                       .map(v-> Fitness.getVehicleFitness(v, this))
                       .reduce(0.0, (tot, element) -> tot + (element > this.maxDuration ? element - this.maxDuration : 0.0));
    }

    public List<Vehicle> getAllVehicles() {
        return this.vehicles;
    }
    
    public void addVehicle(Vehicle v) {
        if (this.vehicles.size() >= this.maxVehicles) {
            throw new IllegalStateException("Too many vehicles");
        }
        v.setDepot(this);
        this.vehicles.add(v);
    }
    
    public void intraDepotMutation() {
        int randInt = Utils.randomInt(3);
        if (randInt == 0) {
            boolean mutationSuccessful = false;
            int tries = 0;
            while (!mutationSuccessful && tries < 10) {
                try {
                    reversalMutation();
                    mutationSuccessful = true;
                } catch (IllegalStateException e) {
                    tries++;
                }
            }
        } else if (randInt == 1) {
            singleCustomerRerouting();
        } else {
            swapping();
        }
    }

    /**
     * Two cutpoints are selected in the chromosome associated with the depot, 
     * and the genetic material between these two cutpoints is reversed.
     */
    private void reversalMutation() {
        List<Integer> vehicleIndices = new ArrayList<>();
        List<Customer> allCustomersFromAllVehicles = new ArrayList<>();
        List<Vehicle> emptyVehicles = new ArrayList<>();
        /* 
        *  Iterate through all vehicles and flatten to 1D list of all customers, while tracking which
        *  vehicles should contain which customers after mutation 
        */
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getCustomers().size() < 1) {
                emptyVehicles.add(vehicle);
                continue;
            }
            allCustomersFromAllVehicles.addAll(vehicle.getCustomers());
            vehicleIndices.add(allCustomersFromAllVehicles.size()); // Keep indices for vehicles for permutation
        }
        if (allCustomersFromAllVehicles.size() <= 1){
            throw new IllegalStateException("If depot only has 1 vehicle then cutpoint will be equal");
        }
        // Select two random (inequal) cutpoints in the list of all customers.
        final Tuple<Integer, Integer> cutpoints = Utils.randomCutpoints(allCustomersFromAllVehicles.size());
        int lowerBound = cutpoints.x < cutpoints.y ? cutpoints.x : cutpoints.y;
        int upperBound = lowerBound == cutpoints.x ? cutpoints.y : cutpoints.x;
        // Swap each i-th customer from each cutpoint
        for (int i = 0; i < (int) Math.floor((upperBound-lowerBound)/2); i++) {
            Customer customer1 = allCustomersFromAllVehicles.get(lowerBound+i);
            Customer customer2 = allCustomersFromAllVehicles.get(upperBound-i);
            allCustomersFromAllVehicles.set(lowerBound+i, customer2);
            allCustomersFromAllVehicles.set(upperBound-i, customer1);
        }
        // Iterate through all vehicles and add their corresponding new customers
        List<Vehicle> mutatedVehicleList = new ArrayList<>();
        Iterator<Vehicle> vehicleIterator = vehicles.iterator();
        int prevIndex = 0;
        for (Integer index : vehicleIndices) {
            mutatedVehicleList.add(new Vehicle(vehicleIterator.next(), allCustomersFromAllVehicles.subList(prevIndex, index)));
            prevIndex = index;
        }
        mutatedVehicleList.addAll(emptyVehicles);
        this.vehicles = mutatedVehicleList;
    }

    /**
     * Re-routing involves randomly selecting one customer, and removing that customer from the existing route. 
     * The customer is then inserted in the best feasible insertion location within the entire chromosome. 
     * This involves computing the total cost of insertion at every insertion locale, 
     * which finally re-inserts the customer in the most feasible location.
     */
    private void singleCustomerRerouting() {
        Vehicle randVehicle = Utils.randomPick(vehicles, (vehicle -> vehicle.getCustomers().size() >= 1));
        if (randVehicle == null) return;

        Customer randCustomer = randVehicle.getCustomers().get(Utils.randomInt(randVehicle.getCustomers().size()));
        int customerIndex = randVehicle.getCustomers().indexOf(randCustomer);
        randVehicle.removeCustomer(randCustomer);

        boolean inserted = insertAtMostFeasible(randCustomer);
        if (!inserted) {
            randVehicle.insertCustomer(randCustomer, customerIndex);
        }
    }

    /** 
     * This simple mutation operator selects two random routes and 
     * swaps one randomly chosen customer from one route to another.
    */
    private void swapping() {
        Vehicle randVehicle1 = Utils.randomPick(vehicles, (vehicle -> vehicle.getCustomers().size() >= 1));
        if (randVehicle1 == null) return;

        Vehicle randVehicle2 = Utils.randomPick(vehicles, (vehicle -> vehicle.getCustomers().size() >= 1 && vehicle != randVehicle1));
        if (randVehicle2 == null) return; // Assume no swap mutations possible

        // Generate two random numbers to pick customers to swap
        int randCustomer1 = Utils.randomInt(randVehicle1.getCustomers().size());
        int randCustomer2 = Utils.randomInt(randVehicle2.getCustomers().size());
        // Swap customers
        Customer customer1 = randVehicle1.getCustomers().get(randCustomer1);
        Customer customer2 = randVehicle2.getCustomers().get(randCustomer2);

        randVehicle1.getCustomers().set(randCustomer1, customer2);
        randVehicle2.getCustomers().set(randCustomer2, customer1);
    }

    public List<Customer> getAllCustomersInVehicles() {
        return vehicles.stream()
                       .flatMap(vehicle -> vehicle.getCustomers().stream())
                       .collect(Collectors.toList());
    }

    public void addSwappableCustomer(Customer customer) {
        if (!swappableCustomers.contains(customer)) {
            swappableCustomers.add(customer);
        } else {
            throw new IllegalArgumentException("Customer should not be added as a swappable customer to a Depot more than once!");
        }
    }

    public double getLoadDeviation(){
        return this.vehicles.stream().map(v->v.getLoad() > v.maxLoad ? v.getLoad() - v.maxLoad : 0.0).reduce(0.0, (acc, el) -> acc + el);
    }

    public List<Customer> getSwappableCustomers() {
        return this.swappableCustomers;
    }

    @Override
    public String toString() {
        return "Depot ID" + id + "\n Depot x:" + x + "\n Depot y:" + y;
    }

    @Override
    public Depot clone() {
        List<Vehicle> vehicles = new ArrayList<>();
        Depot depot = new Depot(this, vehicles);
        for (Vehicle v: this.vehicles) {
            depot.addVehicle(v.clone());
        }
        return depot;
    }

}
