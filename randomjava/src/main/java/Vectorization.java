import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

public class Vectorization {
    public static void main(String[] args) {
        int iterations = 10_000;

        List<Long> results = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            results.add(test());
        }

        System.out.println(results.get(iterations - 1));
    }

    private static long test() {
        int size = 128;

        int[] rand = new int[size];
        for (int i = 0; i < size; i++) {
            rand[i] = RandomUtils.nextInt(0, Integer.MAX_VALUE / 128);
        }

        long acc = 0L;
        for (int i = 0; i < size; i++) {
            acc += rand[i];
        }

        return acc;
    }
}
