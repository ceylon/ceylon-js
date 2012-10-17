function initType(a,b,c,d,e,f,g,h,i,j,k,l){}//IGNORE
function initTypeProtoI(a,b,c,d,e,f){}//IGNORE
function initTypeProto(a,b,c,d,e,f,g){}//IGNORE
function inheritProto(a,b,c){}//IGNORE
function exists(x){}//IGNORE
function Exception(){}//IGNORE
function isOfType(a,b){}//IGNORE
function getBottom(){}//IGNORE
function String$(x,l){}//IGNORE
function TypeCategory(a,b){}//IGNORE
function ArraySequence(x){}//IGNORE
var exports,Container,$finished,Cloneable,smaller,larger,Correspondence,Object$,IdentifiableObject;//IGNORE
var Iterable,Iterator;//IGNORE

function Set(wat) {
    return wat;
}
initTypeProtoI(Set, 'ceylon.language::Set', Collection, Cloneable);
var Set$proto = Set.$$.prototype;
Set$proto.superset = function(set) {
    var iter = set.getIterator();
    var elem; while ((elem = iter.next()) !== $finished) {
        if (!this.contains(elem)) {
            return false;
        }
    }
    return true;
}
Set$proto.subset = function(set) {
    var iter = this.getIterator();
    var elem; while ((elem = iter.next()) !== $finished) {
        if (!set.contains(elem)) {
            return false;
        }
    }
    return true;
}
Set$proto.equals = function(other) {
    if (isOfType(other, 'ceylon.language::Set')) {
        if (other.getSize().equals(this.getSize())) {
            var iter = this.getIterator();
            var elem; while ((elem = iter.next()) !== $finished) {
                if (!other.contains(elem)) {
                    return false;
                }
            }
            return true;
        }
    }
    return false;
}
Set$proto.getHash = function() {
    var hc = 1;
    var iter=this.getIterator();
    var elem;while((elem=iter.next())!=$finished) {
        hc*=31;
        hc+=elem.getHash();
    }
    return hc;
}
exports.Set=Set;

function Map(wat) {
    return wat;
}
initTypeProtoI(Map, 'ceylon.language::Map', Collection, Correspondence, Cloneable);
var Map$proto = Map.$$.prototype;
Map$proto.equals = function(other) {
    if (isOfType(other, 'ceylon.language::Map') && other.getSize().equals(this.getSize())) {
        var iter = this.getIterator();
        var entry; while ((entry = iter.next()) !== $finished) {
            var oi = other.item(entry.getKey());
            if (oi === null || !entry.getItem().equals(oi)) {
                return false;
            }
        }
        return true;
    }
    return false;
}
Map$proto.getHash = function() {
    var hc=1;
    var iter=this.getIterator();
    var elem; while((elem=iter.next())!=$finished) {
        hc*=31;
        hc += elem.getHash();
    }
    return hc;
}

Map$proto.getValues = function() { return MapValues(this); }
function MapValues(map) {
    var val = new MapValues.$$;
    val.map = map;
    return val;
}
initTypeProto(MapValues, 'ceylon.language::MapValues', IdentifiableObject, Collection);
var MapValues$proto = MapValues.$$.prototype;
MapValues$proto.getSize = function() { return this.map.getSize(); }
MapValues$proto.getEmpty = function() { return this.map.getEmpty(); }
MapValues$proto.getClone = function() { return this; }
MapValues$proto.getIterator = function() { return MapValuesIterator(this.map); }
function MapValuesIterator(map) {
    var iter = new MapValuesIterator.$$;
    iter.it = map.getIterator();
    return iter;
}
initTypeProto(MapValuesIterator, 'ceylon.language::MapValuesIterator', IdentifiableObject, Iterator);
MapValuesIterator.$$.prototype.next = function() {
    var entry = this.it.next();
    return (entry!==$finished) ? entry.getItem() : $finished;
}

Map$proto.getKeys = function() { return KeySet(this); }
function KeySet(map) {
    var set = new KeySet.$$;
    set.map = map;
    return set;
}
initTypeProto(KeySet, 'ceylon.language::KeySet', IdentifiableObject, Set);
var KeySet$proto = KeySet.$$.prototype;
KeySet$proto.getSize = function() { return this.map.getSize(); }
KeySet$proto.getEmpty = function() { return this.map.getEmpty(); }
KeySet$proto.contains = function(elem) { return this.map.defines(elem); }
KeySet$proto.getClone = function() { return this; }
KeySet$proto.getIterator = function() { return KeySetIterator(this.map); }
function KeySetIterator(map) {
    var iter = new KeySetIterator.$$;
    iter.it = map.getIterator();
    return iter;
}
initTypeProto(KeySetIterator, 'ceylon.language::KeySetIterator', IdentifiableObject, Iterator);
KeySetIterator.$$.prototype.next = function() {
    var entry = this.it.next();
    return (entry!==$finished) ? entry.getKey() : $finished;
}
KeySet$proto.union = function(other) {
    var set = hashSetFromMap(this.map);
    set.addAll(other);
    return set;
}
KeySet$proto.intersection = function(other) {
    var set = HashSet();
    var it = this.map.getIterator();
    var entry;
    while ((entry=it.next()) !== $finished) {
        var key = entry.getKey();
        if (other.contains(key)) { set.add(key); }
    }
    return set;
}
KeySet$proto.exclusiveUnion = function(other) {
    var set = this.complement(other);
    var it = other.getIterator();
    var elem;
    while ((elem=it.next()) !== $finished) {
        if (!this.map.defines(elem)) { set.add(elem); }
    }
    return set;
}
KeySet$proto.complement = function(other) {
    var set = HashSet();
    var it = this.map.getIterator();
    var entry;
    while ((entry=it.next()) !== $finished) {
        var key = entry.getKey();
        if (!other.contains(key)) { set.add(key); }
    }
    return set;
}

Map$proto.getInverse = function() {
    var inv = HashMap();
    var it = this.getIterator();
    var entry;
    var newSet = HashSet();
    while ((entry=it.next()) !== $finished) {
        var item = entry.getItem();
        var set = inv.put(Entry(item, newSet), true);
        if (set === null) {
            set = newSet;
            newSet = HashSet();
        }
        set.add(entry.getKey());
    }
    return inv;
}
Map$proto.mapItems = function(mapping) {
    function EmptyMap(orig) {
        var em = new EmptyMap.$$;
        IdentifiableObject(em);
        em.orig=orig;
        em.clone=function() { return this; }
        em.getItem=function() { return null; }
        em.getIterator=function() {
            function miter(iter) {
                var $i = new miter.$$;
                $i.iter = iter;
                $i.next = function() {
                    var e = this.iter.next();
                    return e===$finished ? e : Entry(e.getKey(), mapping(e.getKey(), e.getItem()));
                };
                return $i;
            }
            initTypeProto(miter, 'ceylon.language::MappedIterator', IdentifiableObject, Iterator);
            return miter(orig.getIterator());
        }
        em.getSize=function() { return this.orig.getSize(); }
        em.getString=function() { return String$('',0); }
        return em;
    }
    initTypeProto(EmptyMap, 'ceylon.language::EmptyMap', IdentifiableObject, Map);
    return EmptyMap(this);
}
exports.Map=Map;

function HashMap(entries, map) {
    if (map===undefined) { map = new HashMap.$$; }
    IdentifiableObject(map);
    map.map = {};
    map.size = 0;
    if (entries !== undefined) { map.putAll(entries); }
    return map;
}
initTypeProto(HashMap, 'ceylon.language::HashMap', IdentifiableObject, Map);
function copyHashMap(orig) {
    var map = HashMap();
    for (var hash in Object.keys(orig.map)) {
        map.map[hash] = orig.map[hash].slice(0);
    }
    map.size = orig.size;
    return map;
}
var HashMap$proto = HashMap.$$.prototype;
HashMap$proto.put = function(entry, keepOldItem) {
    var key = entry.getKey();
    var hash = key.getHash();
    var arr = this.map[hash];
    if (arr === undefined) {
        arr = [];
        this.map[hash] = arr;
    }
    for (var i=0; i<arr.length; ++i) {
        var e = arr[i];
        if (e.getKey().equals(key)) {
            if (!keepOldItem) { arr[i] = entry; }
            return e.getItem();
        }
    }
    arr.push(entry);
    ++this.size;
    return null;
}
HashMap$proto.putAll = function(entries) {
    var it = entries.getIterator();
    var entry;
    while ((entry=it.next()) !== $finished) { this.put(entry); }
}
HashMap$proto.getSize = function() { return this.size; }
HashMap$proto.getEmpty = function() { return this.size===0; }
HashMap$proto.getLast = function() {
    var hashs = Object.keys(this.map);
    if (hashs.length === 0) { return null; }
    var arr = this.map[hashs[hashs.length - 1]];
    return arr[arr.length - 1];
}
HashMap$proto.getIterator = function() { return HashMapIterator(this.map); }
HashMap$proto.getClone = function() { return this; }
HashMap$proto.item = function(key) {
    var hash = key.getHash();
    var arr = this.map[hash];
    if (arr !== undefined) {
        for (var i=0; i<arr.length; ++i) {
            var entry = arr[i];
            if (entry.getKey().equals(key)) { return entry.getItem(); }
        }
    }
    return null;
}
HashMap$proto.contains = function(elem) {
    if (isOfType(elem, 'ceylon.language::Entry')) {
        var item = this.item(elem.getKey());
        if (item !== null) { return item.equals(elem.getItem()); }
    }
    return false;
}
HashMap$proto.defines = function(key) { return this.item(key) !== null; }

function HashSet(elems, set) {
    if (set===undefined) { set = new HashSet.$$; }
    IdentifiableObject(set);
    set.map = HashMap();
    if (elems !== undefined) { set.addAll(elems); }
    return set;
}
initTypeProto(HashSet, 'ceylon.language::HashSet', IdentifiableObject, Set);
function hashSetFromMap(map) {
    var set = new HashSet.$$;
    IdentifiableObject(set);
    set.map = this;
    return set;
}
var HashSet$proto = HashSet.$$.prototype;
HashSet$proto.add = function(elem) { this.map.put(Entry(elem, true)); }
HashSet$proto.addAll = function(elems) {
    var it = elems.getIterator();
    var elem;
    while ((elem=it.next()) !== $finished) { this.map.put(Entry(elem, true)); }
}
HashSet$proto.getSize = function() { return this.map.size; }
HashSet$proto.getEmpty = function() { return this.map.size===0; }
HashSet$proto.getLast = function() {
    var entry = this.map.getLast();
    return (entry !== null) ? entry.getKey() : null;
}
HashSet$proto.getIterator = function() { return HashSetIterator(this.map); }
HashSet$proto.getClone = function() { return this; }
HashSet$proto.contains = function(elem) { return this.map.item(elem) !== null; }
HashSet$proto.union = function(other) {
    var set = hashSetFromMap(copyHashMap(this.map));
    set.addAll(other);
    return set;
}
HashSet$proto.intersection = function(other) {
    var set = HashSet();
    var it = this.getIterator();
    var elem;
    while ((elem=it.next()) !== $finished) {
        if (other.contains(elem)) { set.map.put(Entry(elem, true)); }
    }
    return set;
}
HashSet$proto.exclusiveUnion = function(other) {
    var set = this.complement(other);
    var it = other.getIterator();
    var elem;
    while ((elem=it.next()) !== $finished) {
        if (this.map.item(elem) === null) { set.map.put(Entry(elem, true)); }
    }
    return set;
}
HashSet$proto.complement = function(other) {
    var set = HashSet();
    var it = this.getIterator();
    var elem;
    while ((elem=it.next()) !== $finished) {
        if (!other.contains(elem)) { set.map.put(Entry(elem, true)); }
    }
    return set;
}

function HashMapIterator(map) {
    var it = new HashMapIterator.$$;
    it.map = map;
    it.hashs = Object.keys(map);
    it.hashIndex = 0;
    it.arrIndex = 0;
    return it;
}
initTypeProto(HashMapIterator, 'ceylon.language::HashMapIterator', IdentifiableObject, Iterator);
HashMapIterator.$$.prototype.next = function() {
    var hash = this.hashs[this.hashIndex];
    if (hash !== undefined) {
        var arr = this.map[hash];
        var entry = arr[this.arrIndex++];
        if (this.arrIndex >= arr.length) {
            ++this.hashIndex;
            this.arrIndex = 0;
        }
        return entry;
    }
    return $finished;
}
function HashSetIterator(map) {
    var it = new HashSetIterator.$$;
    it.mapIt = map.getIterator();
    return it;
}
initTypeProto(HashSetIterator, 'ceylon.language::HashSetIterator', IdentifiableObject, Iterator);
HashSetIterator.$$.prototype.next = function() {
    var entry = this.mapIt.next();
    return (entry !== $finished) ? entry.getKey() : $finished;
}

function LazyMap(entries, map) {
    if (map===undefined) {map = new LazyMap.$$;}
    IdentifiableObject(map);
    map.entries = entries===undefined?$empty:entries;
    return map;
}
initTypeProto(LazyMap, 'ceylon.language::LazyMap', IdentifiableObject, Map);
var LazyMap$proto = LazyMap.$$.prototype;
LazyMap$proto.getEmpty = function() { return this.entries.getEmpty(); }
LazyMap$proto.getSize = function() {
    var it = this.entries.getIterator();
    var count = 0;
    while (it.next() !== $finished) { ++count; }
    return count;
}
LazyMap$proto.getClone = function() { return this; }
LazyMap$proto.getIterator = function() { return this.entries.getIterator(); }
LazyMap$proto.item = function(key) {
    var it = this.entries.getIterator();
    var entry;
    while ((entry=it.next()) !== $finished) {
        if (entry.getKey().equals(key)) { return entry.getItem(); }
    }
    return null;
}
LazyMap$proto.equals = function(other) {
    var hmap = HashMap(this.entries);
    var it = other.getIterator();
    var count = 0;
    var entry;
    while ((entry=it.next()) !== $finished) {
        if (!hmap.contains(entry)) { return false; }
        ++count;
    }
    return count==hmap.getSize();
}
exports.LazyMap=LazyMap;

function LazySet(elems, set) {
    if (set===undefined) {set = new LazySet.$$;}
    IdentifiableObject(set);
    set.elems = elems===undefined?$empty:elems;
    return set;
}
initTypeProto(LazySet, 'ceylon.language::LazySet', IdentifiableObject, Set);
var LazySet$proto = LazySet.$$.prototype;
LazySet$proto.getEmpty = function() { return this.elems.getEmpty(); }
LazySet$proto.getSize = function() { return HashSet(this.elems).getSize(); }
LazySet$proto.getClone = function() { return this; }
LazySet$proto.getIterator = function() { return HashSet(this.elems).getIterator(); }
LazySet$proto.equals = function(other) {
    var hset = HashSet(this.elems);
    var it = other.getIterator();
    var elem;
    var count = 0;
    while ((elem=it.next()) !== $finished) {
        if (!hset.contains(elem)) { return false; }
        ++count;
    }
    return count==hset.getSize();
}
LazySet$proto.union = function(other) {
    var set = HashSet(this.elems);
    set.addAll(other);
    return set;
}
LazySet$proto.intersection = function(other) {
    var set = HashSet(this.elems);
    var result = HashSet();
    var it = other.getIterator();
    var elem;
    while ((elem=it.next()) !== $finished) {
        if (set.contains(elem)) { result.add(elem); }
    }
    return result;
}
LazySet$proto.exclusiveUnion = function(other) {
    return other.exclusiveUnion(HashSet(this.elems));
}
LazySet$proto.complement = function(other) {
    return other.complement(HashSet(this.elems));
}
exports.LazySet=LazySet;
