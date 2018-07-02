package com.goeswhere.dmnp.linenos;

class B {
    public static void main(String[] args) {
        new B().run();
    }

    private void run() {
        foo("five", null, "seven");
    }

    private void foo(String first, String second, String third) {
        System.out.println(first.substring(1) + second.toUpperCase() + third.toLowerCase());
    }
}
