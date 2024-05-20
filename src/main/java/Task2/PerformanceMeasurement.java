package Task2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerformanceMeasurement {
    public static void main(String[] args) {
        int[] sizes = {10, 1000, 10000, 10000000};
        String filename = "performance_results.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("+--------------------+----------+-------------+\n");
            writer.write("|       Method       | Elements |   Duration  |\n");
            writer.write("+--------------------+----------+-------------+\n");
            for (int size : sizes) {
                List<Integer> numbers = generateRandomNumbers(size);

                // Measure sequential execution time
                long sequentialTime = measureSequential(numbers);
                writer.write(String.format("| %-18s | %-8d | %-11d |\n", "Sequential", size, sequentialTime));
                writer.write("+--------------------+----------+-------------+\n");

                // Measure Thread execution time
                long threadTime = measureThread(numbers);
                writer.write(String.format("| %-18s | %-8d | %-11d |\n", "Thread", size, threadTime));
                writer.write("+--------------------+----------+-------------+\n");

                // Measure parallelStream execution time
                long parallelStreamTime = measureParallelStream(numbers);
                writer.write(String.format("| %-18s | %-8d | %-11d |\n", "ParallelStream", size, parallelStreamTime));
                writer.write("+--------------------+----------+-------------+\n");

                // Measure CompletableFuture execution time
                long completableFutureTime = measureCompletableFuture(numbers);
                writer.write(String.format("| %-18s | %-8d | %-11d |\n", "CompletableFuture", size, completableFutureTime));
                writer.write("+--------------------+----------+-------------+\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Integer> generateRandomNumbers(int size) {
        Random random = new Random();
        return IntStream.range(0, size)
                .map(i -> random.nextInt(100))
                .boxed()
                .collect(Collectors.toList());
    }

    private static long measureSequential(List<Integer> numbers) {
        long start = System.nanoTime();
        for (int number : numbers) {
            int square = number * number;
        }
        long end = System.nanoTime();
        return end - start;
    }

    private static long measureThread(List<Integer> numbers) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        int chunkSize = numbers.size() / numThreads;
        List<Thread> threads = new ArrayList<>();

        long start = System.nanoTime();
        for (int i = 0; i < numThreads; i++) {
            int startIdx = i * chunkSize;
            int endIdx = (i == numThreads - 1) ? numbers.size() : startIdx + chunkSize;
            List<Integer> sublist = numbers.subList(startIdx, endIdx);
            Thread thread = new Thread(() -> {
                for (int number : sublist) {
                    int square = number * number;
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long end = System.nanoTime();
        return end - start;
    }

    private static long measureParallelStream(List<Integer> numbers) {
        long start = System.nanoTime();
        numbers.parallelStream().forEach(number -> {
            int square = number * number;
        });
        long end = System.nanoTime();
        return end - start;
    }

    private static long measureCompletableFuture(List<Integer> numbers) {
        ExecutorService executor = Executors.newWorkStealingPool();
        long start = System.nanoTime();
        List<CompletableFuture<Void>> futures = numbers.stream()
                .map(number -> CompletableFuture.runAsync(() -> {
                    int square = number * number;
                }, executor))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long end = System.nanoTime();
        executor.shutdown();
        return end - start;
    }
}
