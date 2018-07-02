package com.goeswhere.dmnp.linenos;

public class B {
    public static void main(String[] args) {
        new B().run();
    }

    public void run() {
        foo("five", null, "seven");
    }

    public void foo(String first, String second, String third) {
        System.out.println(first.substring(1) + second.toUpperCase() + third.toLowerCase());
    }
}
