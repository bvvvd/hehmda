public class VolatileExample {
    private static boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (running) {
                // do some work
            }
            System.out.println("Stopped.");
        });

        worker.start();

        Thread.sleep(1000);
        running = false;
    }
}
