public class Demo {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    public void methodA() {
        synchronized (lock1) {
            sleep();
            synchronized (lock2) {
                System.out.println("methodA acquired both locks");
            }
        }
    }

    public void methodB() {
        synchronized (lock2) {
            sleep();
            synchronized (lock1) {
                System.out.println("methodB acquired both locks");
            }
        }
    }

    private void sleep() {
        try { Thread.sleep(100); } catch (InterruptedException e) {}
    }

    public static void main(String[] args) {
        Demo demo = new Demo();
        new Thread(demo::methodA).start();
        new Thread(demo::methodB).start();
    }
}
