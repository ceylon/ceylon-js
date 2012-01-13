void expect(Equality actual, Equality expected, String text) {
    if (actual == expected) {
        print("[ok] " + text + ": '" + actual.string + "'");
    } else {
        print("[NOT OK] " + text + ": actual='" + actual.string + "', expected='"
              + expected.string + "'");
    }
}
void succeed(String text) {
    print("[ok] " + text);
}
void fail(String text) {
    print("[NOT OK] " + text);
}

void test_if() {
    //True, with else
    if (true) {
        print("if(true) OK");
    } else {
        print("Never happen");
    }
    //False, with else
    if (false) {
        print("Never happen");
    } else {
        print("if(false) OK");
    }
    //without else
    if (2+2 == 4) {
        print("if without else OK");
    }
    //chained if's
    if (1+2 == 4) {
        print("can't happen");
    } else if (2+2 == 5) {
        print("No way");
    } else {
        print("Chained if's with else OK");
    }
    //chained if's without else
    if (1+2 == 4) {
        print("can't happen");
    } else if (2+2 == 4) {
        print("Chained if's without else OK");
    }
    //More complex conditions
    if (2>1 && 1<2) {
        print("if && OK");
    } else { print("if && FAIL"); }
    if ((1>2 || 1>0) && 1<2) {
        print("if (||)&& OK");
    } else { print("if (||)&& FAIL"); }
}

void test_while() {
    variable Integer i := 0;
    while (i < 2) {
        print("while OK");
        i := i+1;
    }
    while (i >= 2 && i < 4) {
        print("while OK");
        i := i+1;
    }
}

void testIfExists() {
    String? s1 = null;
    String? s2 = "";
    if (exists s1) {
        fail("if (exists x)");
    } else {
        succeed("if (exists x)");
    }
    if (exists s2) {
        succeed("if (exists x)");
    } else {
        fail("if (exists x)");
    }
    if (exists s3 = s1) {
        fail("if (exists x=y)");
    } else {
        succeed("if (exists x=y)");
    }
    String? s4 = "hi";
    if (exists s3 = s4) {
        expect(s3, "hi", "if (exists x=y)");
    } else {
        fail("if (exists x=y)");
    }
    if (exists s3 = s2) {
        if (exists s5 = s4) {
            expect(s3, "", "if (exists x=y) nested");
            expect(s5, "hi", "if (exists x=y) nested");
        } else {
            fail("if (exists x=y) nested");
        }
    } else {
        fail("if (exists x=y) nested");
    }
    if (exists len = s4?.size) {
        expect(len, 2, "if (exists x=expr)");
    } else {
        fail("if (exists x=expr)");
    }
}

shared void test() {
    print("--- Start flow control tests ---");
    test_if();
    test_while();
    testIfExists();
    print("--- End flow control tests ---");
}
