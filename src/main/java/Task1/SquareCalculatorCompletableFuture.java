package Task1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SquareCalculatorCompletableFuture {
    public static void main(String[] args) {
        int numNumbers = 10000;
        List<Integer> numbers = new ArrayList<>(numNumbers);
        Random random = new Random();
        for (int i = 0; i < numNumbers; i++) {
            numbers.add(random.nextInt(100));
        }

        ExecutorService executor = Executors.newWorkStealingPool();
        List<CompletableFuture<Void>> futures = numbers.stream()
                .map(number -> CompletableFuture.runAsync(() -> {
                    System.out.println(number + "^2=" + (number * number));
                }, executor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }
}
