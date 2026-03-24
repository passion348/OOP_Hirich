import java.io.*;
import java.util.Scanner;

public class task2 {

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
            return "Side=" + side +
                   ", Result=" + result +
                   ", Binary=" + binaryInput;
        }
    }

    /**
     * Клас обчислення (агрегування)
     */
    static class Calculator {
        private ShapeData data;

        public Calculator(ShapeData data) {
            this.data = data;
        }

        /**
         * S = a² + (√3/4)a²
         */
        public void calculate() {
            double a = data.getSide();
            double result = a * a + (Math.sqrt(3) / 4) * a * a;
            data.setResult(result);
        }
    }

    /**
     * Збереження об'єкта
     */
    static void save(ShapeData data) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream("data.ser"))) {
            oos.writeObject(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Завантаження об'єкта
     */
    static ShapeData load() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream("data.ser"))) {
            return (ShapeData) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Тест правильності
     */
    static void test() {
        ShapeData data = new ShapeData("101"); // 5
        Calculator calc = new Calculator(data);
        calc.calculate();

        double expected = 25 + (Math.sqrt(3) / 4) * 25;

        if (Math.abs(data.getResult() - expected) < 0.0001)
            System.out.println("Тест пройдено");
        else
            System.out.println("Помилка");
    }

    /**
     * Головний метод
     */
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Введiть число у двiйковiй системi: ");
        String binary = sc.nextLine();

        ShapeData data = new ShapeData(binary);
        Calculator calc = new Calculator(data);

        calc.calculate();

        System.out.println("До серiалiзацiї: " + data);

        save(data);

        ShapeData restored = load();

        System.out.println("Пiсля десерiалiзацiї: " + restored);
        System.out.println("Binary буде null (transient)");

        // запуск тесту
        test();

        sc.close();
    }
}