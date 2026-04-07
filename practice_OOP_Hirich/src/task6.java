import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;

/* Анотація */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Info {
    String author();
    String description();
}

/**
 * Головний клас програми
 */
public class task6 {

    /* Інтерфейс відображення */
    interface Displayable {
        String display();
    }

    /* Observer */
    interface Observer {
        void update();
    }

    /* Observable */
    static class ObservableResults {
        private List<Observer> observers = new ArrayList<>();

        public void addObserver(Observer o) {
            observers.add(o);
        }

        public void notifyObservers() {
            for (Observer o : observers) {
                o.update();
            }
        }
    }

    /* Клас даних (Serializable) */
    static class ShapeData implements Serializable {
        private static final long serialVersionUID = 1L;

        private double side;
        private double result;
        
        /** transient поле не серіалізується */
        private transient String binaryInput;

        public ShapeData(String binaryInput) {
            this.binaryInput = binaryInput;
            this.side = Integer.parseInt(binaryInput, 2);
        }

        public double getSide() { return side; }
        public double getResult() { return result; }
        public void setResult(double result) { this.result = result; }

        public String toString() {
            return "Side=" + side + ", Result=" + result;
        }
    }

    /* Абстрактний клас продукту */
    static abstract class ShapeResult implements Displayable, Serializable {
        protected ShapeData data;

        public ShapeResult(ShapeData data) {
            this.data = data;
        }
        /* Основний метод обчислення */
        public abstract void calculate();

        /** Перевантаження (overloading) */
        public void calculate(double multiplier) {
            calculate();
            data.setResult(data.getResult() * multiplier);
        }
    }

    /* Конкретний клас (звичайний вивід) */
    static class TriangleSquare extends ShapeResult {
        public TriangleSquare(ShapeData data) {
            super(data);
        }

        /* Перевизначення методу (overriding) */
        @Override
        @Info(author = "Student", description = "Обчислення трикутника")
        public void calculate() {
            double a = data.getSide();
            double result = a * a + (Math.sqrt(3) / 4) * a * a;
            data.setResult(result);
        }

        public String display() {
            return "TriangleSquare: " + data;
        }
    }

    /* Табличне представлення результату */
    static class TableTriangleSquare extends TriangleSquare {
        private int width;

        public TableTriangleSquare(ShapeData data, int width) {
            super(data);
            this.width = width;
        }

        public String display() {
            String border = "-".repeat(width);
            return border + "\n" +
                    String.format("| %-10s | %-10s |", "Side", "Result") + "\n" +
                    border + "\n" +
                    String.format("| %-10.2f | %-10.2f |", data.getSide(), data.getResult()) + "\n" +
                    border;
        }
    }

    /* Інтерфейс фабрики */
    interface ShapeFactory {
        ShapeResult create(ShapeData data);
    }

    /* Звичайна фабрика */
    static class TriangleFactory implements ShapeFactory {
        public ShapeResult create(ShapeData data) {
            return new TriangleSquare(data);
        }
    }

    /* Фабрика таблиці */
    static class TableTriangleFactory implements ShapeFactory {
        private int width;

        public TableTriangleFactory(int width) {
            this.width = width;
        }

        public ShapeResult create(ShapeData data) {
            return new TableTriangleSquare(data, width);
        }
    }

    /* Інтерфейс команд */
    interface Command {
        void execute();
        void undo();
    }

    /* Singleton менеджер команд */
    static class CommandManager {
        private static CommandManager instance;
        private List<Command> history = new ArrayList<>();

        private CommandManager() {}

        public static CommandManager getInstance() {
            if (instance == null)
                instance = new CommandManager();
            return instance;
        }

        public void save(Command cmd) {
            history.add(cmd);
        }

        public void undo() {
            if (!history.isEmpty()) {
                Command cmd = history.remove(history.size() - 1);
                cmd.undo();
            }
        }
    }

    /* Worker Thread */
    static class WorkerThread extends Thread {
        private BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
        private volatile boolean running = true;

        public void addCommand(Command cmd) {
            queue.add(cmd);
        }

        public void stopWorker() {
            running = false;
            this.interrupt();
        }

        public void run() {
            while (running) {
                try {
                    Command cmd = queue.take();
                    cmd.execute();
                    CommandManager.getInstance().save(cmd);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    static class UndoCommand implements Command {
        public void execute() {
            CommandManager.getInstance().undo();
            task6.observable.notifyObservers();
        }
        
        public void undo() {}
    }

    /* Масштабування */
    static class ScaleCommand implements Command {
        private List<ShapeResult> list;
        private double factor;

        public ScaleCommand(List<ShapeResult> list, double factor) {
            this.list = list;
            this.factor = factor;
        }

        public void execute() {
            list.parallelStream().forEach(r ->
                    r.data.setResult(r.data.getResult() * factor));
            task6.observable.notifyObservers();
        }

        public void undo() {
            list.forEach(r ->
                    r.data.setResult(r.data.getResult() / factor));
        }
    }

    /* Сортування */
    static class SortCommand implements Command {
        private List<ShapeResult> list;
        private List<ShapeResult> backup;

        public SortCommand(List<ShapeResult> list) {
            this.list = list;
        }

        public void execute() {
            backup = new ArrayList<>(list);
            list.sort(Comparator.comparingDouble(r -> r.data.getResult()));
            task6.observable.notifyObservers();
        }

        public void undo() {
            list.clear();
            list.addAll(backup);
        }
    }

    /* Паралельна нормалізація */
    static class NormalizeCommand implements Command {
        private List<ShapeResult> list;
        private List<Double> backup = new ArrayList<>();

        public NormalizeCommand(List<ShapeResult> list) {
            this.list = list;
        }

        public void execute() {
            double max = list.parallelStream()
                    .mapToDouble(r -> r.data.getResult())
                    .max().orElse(1);

            backup.clear();

            list.parallelStream().forEach(r -> {
                synchronized (backup) {
                    backup.add(r.data.getResult());
                }
                r.data.setResult(r.data.getResult() / max);
            });

            task6.observable.notifyObservers();
        }

        public void undo() {
            for (int i = 0; i < list.size(); i++)
                list.get(i).data.setResult(backup.get(i));
        }
    }

    /* Паралельний пошук */
    static class SearchCommand implements Command {
        private List<ShapeResult> list;
        private double target;

        public SearchCommand(List<ShapeResult> list, double target) {
            this.list = list;
            this.target = target;
        }

        public void execute() {
            list.parallelStream()
                    .filter(r -> Math.abs(r.data.getResult() - target) < 0.0001)
                    .forEach(r -> System.out.println("Знайдено: " + r.display()));
            task6.observable.notifyObservers();
        }

        public void undo() {
            System.out.println("Search не має undo");
        }
    }

    /* Макрокоманда */
    static class MacroCommand implements Command {
        private List<Command> commands = new ArrayList<>();

        public void add(Command cmd) {
            commands.add(cmd);
        }

        public void execute() {
            for (Command c : commands)
                c.execute();
            task6.observable.notifyObservers();
        }

        public void undo() {
            for (int i = commands.size() - 1; i >= 0; i--)
                commands.get(i).undo();
        }
    }

    /* пошук мінімуму, максимуму, обчислення середнього значення */
    static class StatsCommand implements Command {
        private List<ShapeResult> list;

        public StatsCommand(List<ShapeResult> list) {
            this.list = list;
        }

        public void execute() {
            double min = list.parallelStream().mapToDouble(r -> r.data.getResult()).min().orElse(0);
            double max = list.parallelStream().mapToDouble(r -> r.data.getResult()).max().orElse(0);
            double avg = list.parallelStream().mapToDouble(r -> r.data.getResult()).average().orElse(0);

            System.out.println("Min=" + min + " Max=" + max + " Avg=" + avg);
            task6.observable.notifyObservers();
        }

        public void undo() {}
    }

    /* Фільтр */
    static class FilterCommand implements Command {
        private List<ShapeResult> list;
        private double threshold;

        public FilterCommand(List<ShapeResult> list, double threshold) {
            this.list = list;
            this.threshold = threshold;
        }

        public void execute() {
            list.parallelStream()
                    .filter(r -> r.data.getResult() > threshold)
                    .forEach(r -> System.out.println("Filtered: " + r.display()));
            task6.observable.notifyObservers();
        }

        public void undo() {}
    }

    /* Меню */
    static class Menu {
        public void show() {
            System.out.println("\n1-Масштабування");
            System.out.println("2-Сортування");
            System.out.println("3-Нормалiзацiя");
            System.out.println("4-Пошук");
            System.out.println("5-Макрокоманда");
            System.out.println("6-Статистика");
            System.out.println("7-Фiльтр");
            System.out.println("8-Undo");
            System.out.println("9-Ще число");
            System.out.println("0-Вихiд");
        }
    }

    static void showAnnotation() {
        try {
            Method m = TriangleSquare.class.getMethod("calculate");
            if (m.isAnnotationPresent(Info.class)) {
                Info info = m.getAnnotation(Info.class);
                System.out.println("Автор: " + info.author());
                System.out.println("Опис: " + info.description());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /* Колекція результатів */
    static List<ShapeResult> results = new ArrayList<>();

    static ObservableResults observable = new ObservableResults();

    /* Збереження */
    static void save(List<ShapeResult> list) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data.ser"))) {
            oos.writeObject(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Завантаження */
    static List<ShapeResult> load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("data.ser"))) {
            return (List<ShapeResult>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /* Тест */
    static class TestShape {
        public static void run() {
            ShapeData data = new ShapeData("101");
            ShapeResult r = new TriangleFactory().create(data);
            r.calculate();

            double expected = 25 + (Math.sqrt(3) / 4) * 25;
            System.out.println(Math.abs(data.getResult() - expected) < 0.001 ? "TEST OK" : "TEST FAIL");
        }
    }

    public static void main(String[] args) {
    task6.WorkerThread worker = new task6.WorkerThread();
    worker.start();

    showAnnotation(); // рефлексія

    new gui(worker, results, observable);
}
}