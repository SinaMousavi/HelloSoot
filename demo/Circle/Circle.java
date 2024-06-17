
public class Circle {

    private int id;
    private static int circleCount = 0;
    private final double PI = 3.14;
    public int radius;

    public Circle(int radius) {
        circleCount++;
        this.id = circleCount;
        this.radius = radius;
    }

    public static void main(String[] args) {
        Circle circle = new Circle(5);
        int actualArea = circle.area(false);
        int integerArea = circle.area(true);
    }

    public int area() {
        int result = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                if (x * x + y * y <= radius * radius)
                    result++;
            }
        }
        return result;
    }

    public int area(boolean integerArea) {
        try {
            getCircleCount();
            if (integerArea)
                return area();
        } catch (Exception exception) {

        }
        return (int) (1.0 * radius * radius * PI);
    }


    public static int getCircleCount() {
        return circleCount;
    }

}

