import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Головний клас програми
 * Демонструє Factory Method, поліморфізм, перевизначення та перевантаження
 */
public class task4 {

    /** Інтерфейс відображення */
    interface Displayable {
        String display();
    }

    /**
     * Клас даних (Serializable)
     */
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

        @Override
        public String toString() {
            return "Side=" + side + ", Result=" + result;
        }
    }

    /**
     * Абстрактний клас продукту
     */
    static abstract class ShapeResult implements Displayable, Serializable {
        protected ShapeData data;

        public ShapeResult(ShapeData data) {
            this.data = data;
        }

        /** Основний метод обчислення */
        public abstract void calculate();

        /** Перевантаження (overloading) */
        public void calculate(double multiplier) {
            calculate();
            data.setResult(data.getResult() * multiplier);
        }
    }

    /**
     * Конкретний клас (звичайний вивід)
     */
    static class TriangleSquare extends ShapeResult {

        public TriangleSquare(ShapeData data) {
            super(data);
        }

        /** Перевизначення методу (overriding) */
        @Override
        public void calculate() {
            double a = data.getSide();
            double result = a * a + (Math.sqrt(3) / 4) * a * a;
            data.setResult(result);
        }

        @Override
        public String display() {
            return "TriangleSquare: " + data.toString();
        }
    }

    /**
     * Табличне представлення результату
     */
    static class TableTriangleSquare extends TriangleSquare {

        private int width;

        public TableTriangleSquare(ShapeData data, int width) {
            super(data);
            this.width = width;
        }

        @Override
        public String display() {
            String border = "-".repeat(width);
            return border + "\n" +
                   String.format("| %-10s | %-10s |", "Side", "Result") + "\n" +
                   border + "\n" +
                   String.format("| %-10.2f | %-10.2f |", data.getSide(), data.getResult()) + "\n" +
                   border;
        }
    }

    /** Інтерфейс фабрики */
    interface ShapeFactory {
        ShapeResult create(ShapeData data);
    }

    /** Звичайна фабрика */
    static class TriangleFactory implements ShapeFactory {
        public ShapeResult create(ShapeData data) {
            return new TriangleSquare(data);
        }
    }

    /** Фабрика таблиці */
    static class TableTriangleFactory implements ShapeFactory {
        private int width;

        public TableTriangleFactory(int width) {
            this.width = width;
        }

        public ShapeResult create(ShapeData data) {
            return new TableTriangleSquare(data, width);
        }
    }

    /** Колекція результатів */
    static List<ShapeResult> results = new ArrayList<>();

    /** Збереження */
    static void save(List<ShapeResult> list) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data.ser"))) {
            oos.writeObject(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Завантаження */
    static List<ShapeResult> load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("data.ser"))) {
            return (List<ShapeResult>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Клас тестування
     */
    static class TestShape {
        public static void run() {
            ShapeData data = new ShapeData("101");
            ShapeFactory factory = new TriangleFactory();
            ShapeResult result = factory.create(data);

            result.calculate();

            double expected = 25 + (Math.sqrt(3) / 4) * 25;

            if (Math.abs(data.getResult() - expected) < 0.0001)
                System.out.println("Тест пройдено");
            else
                System.out.println("Помилка");
        }
    }

    /**
     * Головний метод
     */
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Введiть двiйкове число: ");
        String binary = sc.nextLine();

        ShapeData data = new ShapeData(binary);

        System.out.println("1 - Звичайний вивiд");
        System.out.println("2 - Таблиця");
        int choice = sc.nextInt();

        ShapeFactory factory;

        if (choice == 1) {
            factory = new TriangleFactory();
        } else {
            System.out.print("Ширина таблицi: ");
            int width = sc.nextInt();
            factory = new TableTriangleFactory(width);
        }

        ShapeResult result = factory.create(data);

        result.calculate(); // dynamic dispatch

        results.add(result);

        System.out.println("\nДо збереження:");
        for (ShapeResult r : results)
            System.out.println(r.display());

        save(results);

        List<ShapeResult> restored = load();

        System.out.println("\nПiсля вiдновлення:");
        for (ShapeResult r : restored)
            System.out.println(r.display());

        TestShape.run();

        sc.close();
    }
}
