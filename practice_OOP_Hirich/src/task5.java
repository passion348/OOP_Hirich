import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
/**
 * Головний клас програми
 */
public class task5 {

    /** Інтерфейс відображення */
    interface Displayable {
        String display();
    }

    /**
     * Клас даних
     */
    static class ShapeData implements Serializable {
        private static final long serialVersionUID = 1L;

        private double side;
        private double result;
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

    /**
     * Абстрактний результат
     */
    static abstract class ShapeResult implements Displayable, Serializable {
        protected ShapeData data;

        public ShapeResult(ShapeData data) {
            this.data = data;
        }

        public abstract void calculate();

        public void calculate(double multiplier) {
            calculate();
            data.setResult(data.getResult() * multiplier);
        }
    }

    static class TriangleSquare extends ShapeResult {

        public TriangleSquare(ShapeData data) {
            super(data);
        }

        @Override
        public void calculate() {
            double a = data.getSide();
            double result = a * a + (Math.sqrt(3) / 4) * a * a;
            data.setResult(result);
        }

        public String display() {
            return "TriangleSquare: " + data;
        }
    }

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

    interface ShapeFactory {
        ShapeResult create(ShapeData data);
    }

    static class TriangleFactory implements ShapeFactory {
        public ShapeResult create(ShapeData data) {
            return new TriangleSquare(data);
        }
    }

    static class TableTriangleFactory implements ShapeFactory {
        private int width;

        public TableTriangleFactory(int width) {
            this.width = width;
        }

        public ShapeResult create(ShapeData data) {
            return new TableTriangleSquare(data, width);
        }
    }

    /** ================= COMMAND ================= */

    interface Command {
        void execute();
        void undo();
    }

    /**
     * Singleton менеджер команд
     */
    static class CommandManager {
        private static CommandManager instance;
        private List<Command> history = new ArrayList<>();

        private CommandManager() {}

        public static CommandManager getInstance() {
            if (instance == null)
                instance = new CommandManager();
            return instance;
        }

        public void executeCommand(Command cmd) {
            cmd.execute();
            history.add(cmd);
        }

        public void undo() {
            if (!history.isEmpty()) {
                Command cmd = history.remove(history.size() - 1);
                cmd.undo();
            }
        }
    }

    /**
     * Масштабування
     */
    static class ScaleCommand implements Command {
        private List<ShapeResult> list;
        private double factor;

        public ScaleCommand(List<ShapeResult> list, double factor) {
            this.list = list;
            this.factor = factor;
        }

        public void execute() {
            for (ShapeResult r : list)
                r.data.setResult(r.data.getResult() * factor);
        }

        public void undo() {
            for (ShapeResult r : list)
                r.data.setResult(r.data.getResult() / factor);
        }
    }

    /**
     * Сортування
     */
    static class SortCommand implements Command {
        private List<ShapeResult> list;
        private List<ShapeResult> backup;

        public SortCommand(List<ShapeResult> list) {
            this.list = list;
        }

        public void execute() {
            backup = new ArrayList<>(list);
            list.sort((a, b) -> Double.compare(a.data.getResult(), b.data.getResult()));
        }

        public void undo() {
            list.clear();
            list.addAll(backup);
        }
    }

    /**
     * Нормалізація
     */
    static class NormalizeCommand implements Command {
        private List<ShapeResult> list;
        private List<Double> backup = new ArrayList<>();

        public NormalizeCommand(List<ShapeResult> list) {
            this.list = list;
        }

        public void execute() {
            double max = list.stream().mapToDouble(r -> r.data.getResult()).max().orElse(1);

            for (ShapeResult r : list) {
                backup.add(r.data.getResult());
                r.data.setResult(r.data.getResult() / max);
            }
        }

        public void undo() {
            for (int i = 0; i < list.size(); i++)
                list.get(i).data.setResult(backup.get(i));
        }
    }

    /**
     * Пошук
     */
    static class SearchCommand implements Command {
        private List<ShapeResult> list;
        private double target;

        public SearchCommand(List<ShapeResult> list, double target) {
            this.list = list;
            this.target = target;
        }

        public void execute() {
            for (ShapeResult r : list) {
                if (r.data.getResult() == target)
                    System.out.println("Знайдено: " + r.display());
            }
        }

        public void undo() {
            System.out.println("Search не має undo");
        }
    }

    /**
     * Макрокоманда
     */
    static class MacroCommand implements Command {
        private List<Command> commands = new ArrayList<>();

        public void add(Command cmd) {
            commands.add(cmd);
        }

        public void execute() {
            for (Command c : commands)
                c.execute();
        }

        public void undo() {
            for (int i = commands.size() - 1; i >= 0; i--)
                commands.get(i).undo();
        }
    }

    /**
     * Меню
     */
    static class Menu {
        public void show() {
            System.out.println("\n1-Масштабування");
            System.out.println("2-Сортування");
            System.out.println("3-Нормалiзацiя");
            System.out.println("4-Пошук");
            System.out.println("5-Макрокоманда");
            System.out.println("6-Undo");
            System.out.println("0-Вихiд");
        }
    }

    static List<ShapeResult> results = new ArrayList<>();

    static void save(List<ShapeResult> list) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data.ser"))) {
            oos.writeObject(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static List<ShapeResult> load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("data.ser"))) {
            return (List<ShapeResult>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Тест
     */
    static class TestShape {
        public static void run() {
            ShapeData data = new ShapeData("101");
            ShapeResult r = new TriangleFactory().create(data);
            r.calculate();

            double expected = 25 + (Math.sqrt(3) / 4) * 25;

            System.out.println(Math.abs(data.getResult() - expected) < 0.001 ? "OK" : "FAIL");
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Введiть двiйкове число: ");
        String binary = sc.nextLine();

        ShapeData data = new ShapeData(binary);

        System.out.println("1-Звичайний 2-Таблиця");
        int choice = sc.nextInt();

        ShapeFactory factory;

        if (choice == 1) {
            factory = new TriangleFactory();
        } else {
            System.out.print("Ширина: ");
            factory = new TableTriangleFactory(sc.nextInt());
        }

        ShapeResult result = factory.create(data);
        result.calculate();
        results.add(result);

        CommandManager manager = CommandManager.getInstance();
        Menu menu = new Menu();

        while (true) {
            menu.show();
            int cmd = sc.nextInt();

            switch (cmd) {
                case 1:
                    System.out.print("Коеф: ");
                    manager.executeCommand(new ScaleCommand(results, sc.nextDouble()));
                    break;
                case 2:
                    manager.executeCommand(new SortCommand(results));
                    break;
                case 3:
                    manager.executeCommand(new NormalizeCommand(results));
                    break;
                case 4:
                    System.out.print("Шукати: ");
                    manager.executeCommand(new SearchCommand(results, sc.nextDouble()));
                    break;
                case 5:
                    MacroCommand macro = new MacroCommand();
                    macro.add(new ScaleCommand(results, 2));
                    macro.add(new SortCommand(results));
                    manager.executeCommand(macro);
                    break;
                case 6:
                    manager.undo();
                    break;
                case 0:
                    save(results);
                    TestShape.run();
                    return;
            }

            System.out.println("\nДанi:");
            for (ShapeResult r : results)
                System.out.println(r.display());
        }
    }
}