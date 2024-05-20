package Task1;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SquareCalculatorParallelStream {
    public static void main(String[] args) {
        int numNumbers = 10000;
        Random random = new Random();
        List<Integer> numbers = IntStream.range(0, numNumbers)
                .map(i -> random.nextInt(100))
                .boxed()
                .collect(Collectors.toList());

        numbers.parallelStream().forEach(number -> {
            System.out.println(number + "^2=" + (number * number));
        });
    }
}
