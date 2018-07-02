package com.goeswhere.dmnp.linenos;

class A {
    public static void main(String[] args) {
        try {
            new A().run();
        } catch (NullPointerException e) {
            LineNos.printStackTrace(e);
        }
    }

    private void run() {
        foo("five", null, "seven");
    }

    private void foo(String first, String second, String third) {
        System.out.println(first.substring(1) + second.toUpperCase() + third.toLowerCase());
    }
}
