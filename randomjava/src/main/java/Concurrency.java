import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class Concurrency {
    public static void main(String[] args) {
        ExecutorService exec = Executors.newFixedThreadPool(4);

        CompletableFuture<String> first = CompletableFuture.supplyAsync(
                getStringSupplier("string 1"), exec);
        CompletableFuture<String> second = CompletableFuture.supplyAsync(
                getStringSupplier("string 2"), exec);
        CompletableFuture<String> third = CompletableFuture.supplyAsync(
                getStringSupplier("string 3"), exec);

        CompletableFuture<Void> res = CompletableFuture
                .allOf(first, second, third)
                .thenAccept((empty) -> {
                    System.out.println("woohoo! we're done!");
                });

        res.join();

        exec.shutdown();
    }

    private static Supplier<String> getStringSupplier(String input) {
        return () -> {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(5000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return input;
        };
    }

    private static void thenCombineExample() {
        CompletableFuture.supplyAsync(() -> "first string")
                .thenCombine(delayedString(), (first, second) -> first + second)
                .thenAccept(System.out::println);
    }

    private static CompletableFuture<String> delayedString() {
        return CompletableFuture.completedFuture("I might have taken a while!");
    }

    private static void thenComposeExample() {
        CompletableFuture.supplyAsync(() -> "first string")
                .thenCompose(Concurrency::delayedTransform)
                .thenAccept(System.out::println);
    }

    private static CompletableFuture<String> delayedTransform(String input) {
        return CompletableFuture
                .completedFuture("fixed: " + input)
                .thenApply((str) -> "really fixed: " + str);
    }
}
