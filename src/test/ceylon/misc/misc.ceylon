import check { check }

shared interface X {
    shared void helloWorld() {
       print("hello world");
    }
}

shared class Foo(name) {
    shared String name;
    variable value counter=0;
    shared Integer count { return counter; }
    shared void inc() { counter=counter+1; }
    shared default String printName() =>
        "foo.name=" + name;
    shared default actual String string = "Foo(``name``)";
    inc();
}

shared class Bar() extends Foo("Hello") satisfies X {
    shared actual String printName() {
        return "bar.name=" + name + ","
            + (super of Foo).printName() +  ","
            + super.printName();
    }
    shared class Inner() {
        shared actual String string = "creating inner class of: " 
            + outer.name;
        shared void incOuter() {
            inc();
        }
    }
}

String printBoth(String x, String y) {
    return x + ", " + y;
}

void doIt(void f()) {
    f(); f();
}

object foob {
    shared String name="Gavin";
}

void printAll(String* strings) {}

class F(String name) => Foo(name);

shared Integer var() { return 5; }

//Issue #249
{Integer*} container249 = [object249.int];
shared object object249{
  shared Integer int = 1;
}

//For testing of 461
shared class TestClass461 {
  shared new TestConstructor() {}
  shared void test() => check(true, "Import toplevel constructor");
}
