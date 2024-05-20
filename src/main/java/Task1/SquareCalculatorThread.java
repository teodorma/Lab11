package Task1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SquareCalculatorThread extends Thread {
    private List<Integer> numbers;

    public SquareCalculatorThread(List<Integer> numbers) {
        this.numbers = numbers;
    }

    @Override
    public void run() {
        for (int number : numbers) {
            System.out.println(number + "^2=" + (number * number));
        }
    }

    public static void main(String[] args) {
        int numNumbers = 10000;
        List<Integer> numbers = new ArrayList<>(numNumbers);
        Random random = new Random();
        for (int i = 0; i < numNumbers; i++) {
            numbers.add(random.nextInt(100));
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        int chunkSize = numNumbers / numThreads;
        List<SquareCalculatorThread> threads = new ArrayList<>(numThreads);

        for (int i = 0; i < numThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numThreads - 1) ? numNumbers : start + chunkSize;
            threads.add(new SquareCalculatorThread(numbers.subList(start, end)));
        }

        for (SquareCalculatorThread thread : threads) {
            thread.start();
        }

        for (SquareCalculatorThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
