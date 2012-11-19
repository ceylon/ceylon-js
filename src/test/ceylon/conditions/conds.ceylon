import check {...}

shared void test() {
    Boolean a = true;
    Boolean b = false;
    variable value count := 0;
    if (a && b) {
        fail("WTF?");
    } else if (a || b) {
        check(a||b, "boolean 1");
        count++;
    }
    String? c = "X";
    String? d = null;
    if (exists c && exists d) {
        fail("WTF exists");
    } else if (exists c1=c, exists c1[0]) {
        check(c1 == "X", "exists");
        count++;
    }
    String|Integer e = 1;
    if (is Integer e && exists d) {
        fail("WTF is");
    } else if (is Integer e, exists c) {
        check(e>0, "is");
        count++;
    }
    String[] f = {"a","b","c"};
    Integer[] g = {};
    if (nonempty f && nonempty g) {
        fail("WTF nonempty");
    } else if (nonempty f, !f.first.empty) {
        check(f.first.uppercased=="A", "nonempty");
        count++;
    }
    check(count==4, "some conditions were not met: " count " instead of 4");
    Integer|String zz = 1;
    assert(is Integer zz2=zz, zz2 > 0);
    check(zz2==1, "special -> boolean");
    //and now some comprehensions
    Sequence<Integer?> seq1 = { 1, 2, 3, null, 4, 5, null, 6, 7, null, 10};
    check({ for (i in seq1) if (exists i, i%2==0) i*10 }=={20,40,60,100},"comprehension [1]");
    // !is
    if (!is String zz) {
        check(zz>0, "!is");
    } else {
        fail("!is");
    }
    if (!is String zz3=zz) {
        check(zz3<2, "!is with variable [1]");
    } else {
        fail("!is (again)");
    }
    assert(!is String zz4=zz, zz4<3);
    //several conditions chained together
    Empty|Sequence<Integer?> seq2 = seq1;
    if (nonempty seq2, exists zz5=seq2.first) {
        check(zz5==1, "nonempty,exists");
    } else { fail("nonempty,exists"); }
    if (nonempty seq2, is Integer zz5=seq2.first, zz5>0) {
        check(zz5==1, "nonempty,is");
    } else { fail("nonempty,is"); }
    if (is String c, c.lowercased=="x") {
        check(true,"is,boolean");
    } else { fail("is,boolean"); }
    if (exists c, nonempty seq2, exists zz5=c[0]) {
        check(zz5==`X`,"exists,nonempty,boolean");
    } else { fail("exists,nonempty,boolean"); }
    results();
}
