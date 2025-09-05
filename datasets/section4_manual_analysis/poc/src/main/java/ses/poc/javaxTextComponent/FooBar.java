package ses.poc.javaxTextComponent;

public class FooBar extends Foo{

    public void remove() {
        System.out.println("child");
        System.out.println(this.getClass());
    }

}
