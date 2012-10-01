import functions { ... }
import check { results }

shared void test() {
    helloWorld();
    helloWorld{};
    hello("world");
    hello { name="world"; };
    helloAll("someone", "someone else");
    helloAll { "someone", "someone else" };
    String s1 = toString(99);
    String s2 = toString { obj=99; };    
    Float f1 = add(1.0, -1.0);
    Float f2 = add { x=1.0; y=-1.0; };
    void p(Integer i) {
        print(i);
    }
    repeat(10,p);
    testNamedArguments();
    testQualified();
    results();
}
