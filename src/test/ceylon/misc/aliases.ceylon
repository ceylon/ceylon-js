import check { ... }

shared alias Strinteger = String|Integer;

shared class AliasingClass() {
    shared interface AliasingIface {
        shared Boolean aliasingIface() { return true; }
    }
    shared class AliasingInner() {
        shared Boolean aliasingInner() { return true; }
    }
}

class AliasingSubclass() extends AliasingClass() {
    shared class InnerAlias() = AliasingInner;
    shared class SubAlias() extends InnerAlias() {}

    shared Boolean aliasingSubclass() {
        return SubAlias().aliasingInner();
    }
    shared interface AliasedIface = AliasingIface;
}

class AliasingSub2() extends AliasingSubclass() {
    shared AliasedIface iface {
        object aliased satisfies AliasedIface {
        }
        return aliased;
    }
}

interface Matrix<Cell> = Sequence<Sequence<Cell>>;
class Listleton<T>(List<T> l) = Singleton<List<T>>;

class MiMatrix(Integer gridSize) satisfies Matrix<Integer> {
    value sb = SequenceBuilder<Sequence<Integer>>();
    for (i in 1..gridSize) {
        assert(nonempty row={ for (j in 1..gridSize) j });
        sb.append(row);
    }
    Matrix<Integer> grid;
    if (nonempty g=sb.sequence) {
        grid=g;
    } else {
        grid={{1}};
    }
    shared actual Iterator<Sequence<Integer>> iterator { return grid.iterator; }
    shared actual String string = grid.string;
    shared actual Integer hash = grid.hash;
    shared actual Boolean equals(Object other) = grid.equals;
    shared actual Sequence<Integer>[] span(Integer from, Integer? to) = grid.span;
    shared actual Sequence<Integer>[] segment(Integer from, Integer length) = grid.segment;
    shared actual Matrix<Integer> reversed = grid.reversed;
    shared actual Integer lastIndex = grid.lastIndex;
    shared actual Sequence<Integer>? item(Integer i) = grid.item;
    shared actual Sequence<Integer>[] rest = grid.rest;
    shared actual Sequence<Integer> first = grid.first;
    shared actual MiMatrix clone { return this; }
}

void testAliasing() {
    print("testing type aliases");
    check(AliasingSubclass().aliasingSubclass(), "Aliased member class");
    class InnerSubalias() = AliasingSubclass;
    check(InnerSubalias().aliasingSubclass(), "Aliased top-level class");
    interface AliasedIface2 = AliasingClass.AliasingIface;
    Boolean use(AliasedIface2 aif) { return aif.aliasingIface(); }
    check(use(AliasingSub2().iface), "Aliased member interface");
    Strinteger xxxxx = 5;
    check(is Integer xxxxx, "Type alias");
    check(Listleton<Integer>({{1},{2},{3}}.first).string=="{ { 1 } }", "class alias");
    check(MiMatrix(2).string=="{ { 1, 2 }, { 1, 2 } }", "interface alias");
}
