import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Головний клас
 */
public class task3 {

    /**
     * Інтерфейс відображення (фабрикованих об'єктів)
     */
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

        /** Не серіалізується */
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
     * Абстрактний клас (Product)
     */
    static abstract class ShapeResult implements Displayable, Serializable {
        protected ShapeData data;

        public ShapeResult(ShapeData data) {
            this.data = data;
        }

        public abstract void calculate();
    }

    /**
     * Конкретний продукт
     */
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

        @Override
        public String display() {
            return "TriangleSquare: " + data.toString();
        }
    }

    /**
     * Інтерфейс фабрики
     */
    interface ShapeFactory {
        ShapeResult create(ShapeData data);
    }

    /**
     * Конкретна фабрика
     */
    static class TriangleFactory implements ShapeFactory {
        @Override
        public ShapeResult create(ShapeData data) {
            return new TriangleSquare(data);
        }
    }

    /**
     * Колекція результатів
     */
    static List<ShapeResult> results = new ArrayList<>();

    /**
     * Збереження колекції
     */
    static void save(List<ShapeResult> list) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream("data.ser"))) {
            oos.writeObject(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Завантаження колекції
     */
    static List<ShapeResult> load() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream("data.ser"))) {
            return (List<ShapeResult>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Тест
     */
    static void test() {
        ShapeData data = new ShapeData("101"); // 5
        ShapeFactory factory = new TriangleFactory();
        ShapeResult result = factory.create(data);

        result.calculate();

        double expected = 25 + (Math.sqrt(3) / 4) * 25;

        if (Math.abs(data.getResult() - expected) < 0.0001)
            System.out.println("Тест пройдено");
        else
            System.out.println("Помилка");
    }

    /**
     * main
     */
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Введiть двiйкове число: ");
        String binary = sc.nextLine();

        ShapeData data = new ShapeData(binary);

        ShapeFactory factory = new TriangleFactory();
        ShapeResult result = factory.create(data);

        result.calculate();

        results.add(result);

        System.out.println("До збереження:");
        for (ShapeResult r : results)
            System.out.println(r.display());

        save(results);

        List<ShapeResult> restored = load();

        System.out.println("\nПiсля вiдновлення:");
        for (ShapeResult r : restored)
            System.out.println(r.display());

        test();

        sc.close();
    }
}