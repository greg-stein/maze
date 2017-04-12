package com.example.neutrino.maze;

import android.graphics.PointF;
import android.graphics.RectF;
import android.support.v4.util.Pair;

import com.example.neutrino.maze.floorplan.Fingerprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import static com.example.neutrino.maze.WiFiTug.WiFiFingerprint;

/**
 * Created by Greg Stein on 4/9/2017.
 */

public class GeneticLocator {
    public static final double ENVIRONMENT_RADIUS = 7; // in meters
    private static final float MAX_DISTANCE_BETWEEN_GENES = 3; // in meters
    public static final int POPULATION_SIZE = 200;
    private static final int MAX_GENERATIONS = 100;
    private static final int MAX_MOVE_MAGNITUDE = 10;

    // For debug only
    public static PointF realHeadLocation;
    public static int headIndex;

    protected static class Chromosome extends ArrayList<PointF> {
        private boolean mFitnessRecalcNeeded = true;
        private float mFitness;

        public float getFitness() {
            if (mFitnessRecalcNeeded) {
                mFitness = GeneticLocator.fitness(this);
                mFitnessRecalcNeeded = false;
            }

            return mFitness;
        }

        public void mutate() {
            GeneticLocator.mutate(this);
            mFitnessRecalcNeeded = true;
        }

        public void crawl() {
            GeneticLocator.crawl(this);
            mFitnessRecalcNeeded = true;
        }

        public Pair<Chromosome, Chromosome> crossover(Chromosome parent) {
            Chromosome cloneThis = this.deepClone();
            Chromosome cloneOther = parent.deepClone();

            return GeneticLocator.crossover(cloneThis, cloneOther);
        }

        public Chromosome deepClone() {
            Chromosome cloned = new Chromosome();
            for (PointF p : this) {
                PointF clone = new PointF();
                clone.set(p);
                cloned.add(clone);
            }

            return cloned;
        }

        public static Chromosome originate(int dnaLength) {
            Chromosome originator = new Chromosome();

            PointF head = new PointF();
            head.x = random.nextFloat() * (geography.right - geography.left) + geography.left;
            head.y = random.nextFloat() * (geography.bottom - geography.top) + geography.top;

            originator.add(head);

            PointF lastGene = head;
            float minAngle = 0, maxAngle = 359;
            for (int gene = 0; gene < dnaLength - 1; gene++) {
                final float geneDistance = random.nextFloat() * MAX_DISTANCE_BETWEEN_GENES;
                final float geneAngle = random.nextFloat() * (maxAngle - minAngle) + minAngle;
                // TODO: cos and sin accept angles in RADIANS you stupid bastard! Fix that!
                final float x = (float) (geneDistance * Math.cos(geneAngle)) + lastGene.x;
                final float y = (float) (geneDistance * Math.sin(geneAngle)) + lastGene.y;
                final PointF newGene = new PointF(x, y);
                originator.add(newGene);

                lastGene = newGene;
                minAngle = geneAngle - 30;
                maxAngle = geneAngle + 30;
            }

            return originator;
        }
    }

    protected static class Population extends TreeSet<Chromosome> {
        public Chromosome[] chromosomes = new Chromosome[POPULATION_SIZE];

        Population() {
            super(new Comparator<Chromosome>() {
                @Override
                public int compare(Chromosome c1, Chromosome c2) {
                    int compare = Float.compare(c1.getFitness(), c2.getFitness());
                    // Make compare method consistent with equals which is NOT used by treeset
                    if (compare == 0 && !c1.equals(c2)) return 1;
                    return compare;
                }
            });
        }

        // This allows random access to chromosomes within population
        public void updateChromosomes() {chromosomes = this.toArray(chromosomes);}
    }

    private static List<WiFiFingerprint> history;
    private static Population population;
    private static Random random = new Random();
    public static int chromosomeLength;
    public static float mutationRate;
    public static float crossoverRate;
    public static float crawlingRate;
    private static float elitismRate;
    private static List<Fingerprint> world;
    private static RectF geography;

    static void genesis() {
        population = new Population();
        while (population.size() < POPULATION_SIZE) {
            population.add(Chromosome.originate(chromosomeLength));
        }
    }

    public static void evolution() {
        genesis();

        // We leave elita untouched
//        int elitaSize = (int) (POPULATION_SIZE * elitismRate);

        List<Chromosome> nextGeneration = new ArrayList<>();
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            population.updateChromosomes();

            for (Chromosome parent : population) {
                if (random.nextFloat() < crossoverRate) {
                    Chromosome anotherParent = parent;
                    while (anotherParent == parent) {
                        final int randomIndex = random.nextInt(POPULATION_SIZE);
                        anotherParent = population.chromosomes[randomIndex];
                        if (anotherParent == null)
                            anotherParent = new Chromosome();
                    }

                    Pair<Chromosome, Chromosome> offsprings = parent.crossover(anotherParent);
                    nextGeneration.add(offsprings.first);
                    nextGeneration.add(offsprings.second);
                }
            }

            Iterator<Chromosome> it = population.iterator();
            while (it.hasNext()) {
                Chromosome mutant = it.next();
                if (random.nextFloat() < mutationRate) {
                    it.remove();
                    mutant.mutate();
                    nextGeneration.add(mutant);
                }
            }

            it = population.iterator();
            while (it.hasNext()) {
                Chromosome mutant = it.next();
                if (random.nextFloat() < crawlingRate) {
                    it.remove();
                    mutant.crawl();
                    nextGeneration.add(mutant);
                }
            }

            population.addAll(nextGeneration);
            nextGeneration.clear();
            while (population.size() > POPULATION_SIZE) population.pollLast();
            final Chromosome best = population.first();
            float error = (float) Math.hypot(realHeadLocation.x - best.get(headIndex).x, realHeadLocation.y - best.get(headIndex).y);
//            System.out.println(String.format("Generation %d best solution fitness: %.4f, error: %.4f", generation, best.getFitness(), error));
//            System.out.println(String.format("%.4f %.4f %.4f %.4f %.4f %.4f %.4f %.4f %.4f %.4f %.4f", best.getFitness(),
//                    best.get(0).x, best.get(0).y,
//                    best.get(1).x, best.get(1).y,
//                    best.get(2).x, best.get(2).y,
//                    best.get(3).x, best.get(3).y,
//                    best.get(4).x, best.get(4).y));
        }
    }

    static void setWorld(List<Fingerprint> world) {
        GeneticLocator.world = world;
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;

        for (Fingerprint f : world) {
            PointF center = f.getCenter();
            if (center.x < minX) minX = center.x;
            if (center.y < minY) minY = center.y;
            if (center.x > maxX) maxX = center.x;
            if (center.y > maxY) maxY = center.y;
        }

        geography = new RectF(minX, minY, maxX, maxY);
    }

    static void setHistory(List<WiFiFingerprint> history) {
        GeneticLocator.history = history;
        chromosomeLength = history.size();
    }

    static Pair<Chromosome, Chromosome> crossover(Chromosome parent1, Chromosome parent2) {
        int crossoverPoint = random.nextInt(chromosomeLength - 2) + 1;

        final float offsetX = parent1.get(crossoverPoint).x - parent2.get(crossoverPoint).x;
        final float offsetY = parent1.get(crossoverPoint).y - parent2.get(crossoverPoint).y;

        Chromosome offspring1 = new Chromosome();
        offspring1.addAll(parent1.subList(0, crossoverPoint));
        offspring1.addAll(parent2.subList(crossoverPoint, chromosomeLength));

        for (int i = crossoverPoint; i < chromosomeLength; i++) {
            PointF gene = offspring1.get(i);
            gene.offset(offsetX, offsetY);
        }

        Chromosome offspring2 = new Chromosome();
        offspring2.addAll(parent2.subList(0, crossoverPoint));
        offspring2.addAll(parent1.subList(crossoverPoint, chromosomeLength));

        for (int i = crossoverPoint; i < chromosomeLength; i++) {
            PointF gene = offspring2.get(i);
            gene.offset(-offsetX, -offsetY);
        }

        return new Pair<>(offspring1, offspring2);
    }

    static void mutate(Chromosome chromosome) {
        int geneIndex = random.nextInt(chromosomeLength);
        PointF gene = chromosome.get(geneIndex);
        gene.x += random.nextInt(5) - 2; // -2 -1 0 1 2
        gene.y += random.nextInt(5) - 2; // -2 -1 0 1 2
    }

    static void crawl(Chromosome chromosome) {
        int magnitude = random.nextInt(MAX_MOVE_MAGNITUDE);
        int angle = random.nextInt(360);
        PointF offset = new PointF((float)(magnitude * Math.cos(angle)), (float)(magnitude * Math.sin(angle)));

        for (PointF gene : chromosome) {
            gene.offset(offset.x, offset.y);
        }
    }

    private static float getEnvironmentDissimilarity(WiFiFingerprint fp, PointF p) {
        float dissimilaritiesSum = 0;
        int members = 0;

        for (Fingerprint f : world) {
            final PointF c = f.getCenter();
            if (Math.hypot(c.x - p.x, c.y - p.y) < ENVIRONMENT_RADIUS) {
                dissimilaritiesSum += WiFiTug.dissimilarity(fp, f.getFingerprint());
                members++;
            }
        }

        if (members == 0) return Float.MAX_VALUE;
        return dissimilaritiesSum / members;
    }

    // Lower values represent better fit. Hence minimization problem
    static float fitness(Chromosome chromosome) {
        int index = 0;
        float fitness = 0;

        for (WiFiFingerprint fp : history) {
            fitness += getEnvironmentDissimilarity(fp, chromosome.get(index++));
        }

        return fitness;
    }
}
