//Test classes for metamodel generation
class SimpleClass1() {}
shared abstract class SimpleClass2(shared String name) {}
class SimpleClass3() extends Object() {
    shared actual Boolean equals(Object other) { return false; }
    hash => 0;
}
shared class SimpleClass4(Integer x=1) extends SimpleClass2("simple4") {
}
shared class Satisfy1() satisfies Comparable<Satisfy1> {
    shared actual Comparison compare(Satisfy1 other) { return equal; }
}
shared class Satisfy2() satisfies {Integer*} {
	iterator() => {1}.iterator();
	shared Satisfy2 clone { return this; }
}
class ParmTypes1<Element>(Element x) {}
class ParmTypes2<out Element>(Element* x)
		given Element satisfies Object {
}
class ParmTypes3<Type1,Type2>(Type1 a1, Type2 a2)
		given Type1 satisfies Number<Integer>
		given Type2 of String|Singleton<String> {
}
class ParmTypes4<out Element>(Element* elems) satisfies {Element*} {
	shared Element? primero = elems.first;
	iterator() => elems.iterator();
}
shared class Nested1() {
  shared class Nested2() {
    void innerMethod1(){}
  }
}

shared abstract class Algebraic1(name)
        of AlgOne | AlgTwo | AlgThree {
    shared String name;
}
shared class AlgOne() extends Algebraic1("one") {}
shared class AlgTwo() extends Algebraic1("two") {}
shared class AlgThree() extends Algebraic1("Three") {}

shared abstract class Algebraic2(name)
        of algobj1 | algobj2 {
    shared String name;
}
shared object algobj1 extends Algebraic2("one") satisfies {Integer*} {
    iterator() => {1,2,3}.iterator();
}
shared object algobj2 extends Algebraic2("two") satisfies {Integer+} {
    shared actual Iterator<Integer> iterator() => {4,5,6}.iterator();
}
