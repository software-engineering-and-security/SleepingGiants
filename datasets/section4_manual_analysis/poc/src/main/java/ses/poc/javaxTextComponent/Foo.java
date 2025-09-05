package ses.poc.javaxTextComponent;

public class Foo {

    public void remove() {
        System.out.println(this.getClass());
    }

    public class Bar {
        public void remove() {
            Foo.this.remove();
        }
    }
}
