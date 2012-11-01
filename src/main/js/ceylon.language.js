(function(define) {
    define(function(require, exports, module) {

//the Ceylon language module
//#METAMODEL

function exists(x){}//IGNORE
function String$(x,y){}//IGNORE
function ArraySequence(x){}//IGNORE
function nonempty(x){}//IGNORE
function className(x){}//IGNORE
function isOfType(a,b){}//IGNORE
var larger,smaller,Sequence,Category,$empty,equal; //IGNORE

function getT$name() {return this.constructor.T$name;}
function getT$all() {return this.constructor.T$all;}
function initType(type, typeName) {
    var cons = function() {}
    type.$$ = cons;
    cons.T$name = typeName;
    cons.T$all = {}
    cons.T$all[typeName] = type;
    for (var i=2; i<arguments.length; ++i) {
        var superTypes = arguments[i].$$.T$all;
        for (var $ in superTypes) {cons.T$all[$] = superTypes[$]}
    }
    cons.prototype.getT$name$ = getT$name;
    cons.prototype.getT$all$ = getT$all;
}
function initTypeProto(type, typeName) {
    initType.apply(this, arguments);
    var args = [].slice.call(arguments, 2);
    args.unshift(type);
    inheritProto.apply(this, args);
}
var initTypeProtoI = initTypeProto;
function initExistingType(type, cons, typeName) {
    type.$$ = cons;
    cons.T$name = typeName;
    cons.T$all = {}
    cons.T$all[typeName] = type;
    for (var i=3; i<arguments.length; ++i) {
        var superTypes = arguments[i].$$.T$all;
        for (var $ in superTypes) {cons.T$all[$] = superTypes[$]}
    }
    var proto = cons.prototype;
    if (cons !== undefined) {
        try {
            cons.prototype.getT$name$ = getT$name;
            cons.prototype.getT$all$ = getT$all;
        } catch (exc) {
            // browser probably prevented access to the prototype
        }
    }
}
function initExistingTypeProto(type, cons, typeName) {
    var args = [].slice.call(arguments, 0);
    args.push(IdentifiableObject);
    initExistingType.apply(this, args);
    var proto = cons.prototype;
    if ((proto !== undefined) && (proto.getHash === undefined)) {
    	var origToString = proto.toString;
        try {
            inheritProto(type, IdentifiableObject);
            proto.toString = origToString;
        } catch (exc) {
            // browser probably prevented access to the prototype
        }
    }
}
function inheritProto(type) {
    var proto = type.$$.prototype;
    for (var i=1; i<arguments.length; ++i) {
        var superProto = arguments[i].$$.prototype;
        for (var $ in superProto) {proto[$] = superProto[$]}
    }
}
var inheritProtoI = inheritProto;
exports.initType=initType;
exports.initTypeProto=initTypeProto;
exports.initTypeProtoI=initTypeProtoI;
exports.initExistingType=initExistingType;
exports.initExistingTypeProto=initExistingTypeProto;
exports.inheritProto=inheritProto;
exports.inheritProtoI=inheritProtoI;

function Void(wat) {
    return wat;
}
initType(Void, 'ceylon.language::Void');
function Object$(wat) {
    return wat;
}
initTypeProto(Object$, 'ceylon.language::Object', Void);
var Object$proto = Object$.$$.prototype;
Object$proto.getString = function() { return String$(className(this) + "@" + this.getHash()); }
//Object$proto.getString=function() { String$(Object.prototype.toString.apply(this)) };
Object$proto.toString=function() { return this.getString().valueOf(); }

var identifiableObjectID=1;
function $identityHash(x) {
    var hash = x.identifiableObjectID;
    return (hash !== undefined)
            ? hash : (x.identifiableObjectID = identifiableObjectID++);
}

function Identifiable(obj) {}
initType(Identifiable, "ceylon.language::Identifiable");
var Identifiable$proto = Identifiable.$$.prototype;
Identifiable$proto.equals = function(that) {
    return isOfType(that, 'ceylon.language::Identifiable') && (that===this);
}
Identifiable$proto.getHash = function() { return $identityHash(this); }

function IdentifiableObject(obj) {
    return obj;
}
initTypeProto(IdentifiableObject, 'ceylon.language::IdentifiableObject', Object$, Identifiable);

//INTERFACES
function Cloneable(wat) {
    return wat;
}
initType(Cloneable, 'ceylon.language::Cloneable');
exports.Cloneable=Cloneable;

//#include callable.js

function Castable(wat) {
    return wat;
}
initType(Castable, 'ceylon.language::Castable');
exports.Castable=Castable;
function Closeable(wat) {
    return wat;
}
initType(Closeable, 'ceylon.language::Closeable');
exports.Closeable=Closeable;
function Comparable(wat) {
    return wat;
}
initType(Comparable, 'ceylon.language::Comparable');
/*var Comparable$proto = Comparable.$$.prototype;
Comparable$proto.largerThan = function(other) {
    return Boolean$(this.compare(other)===$larger);
}
Comparable$proto.smallerThan = function(other) {
    return Boolean$(this.compare(other)===$smaller);
}
Comparable$proto.asLargeAs = function(other) {
    return Boolean$(this.compare(other)!=$smaller);
}
Comparable$proto.asSmallAs = function(other) {
    return Boolean$(this.compare(other)!=$larger);
}*/
exports.Comparable=Comparable;

function Container(wat) {
    return wat;
}
initType(Container, 'ceylon.language::Container');
exports.Container=Container;
function ContainerWithFirstElement(wat) {
    return wat;
}
initType(ContainerWithFirstElement, 'ceylon.language::ContainerWithFirstElement');
exports.ContainerWithFirstElement=ContainerWithFirstElement;
function Correspondence(wat) {
    return wat;
}
initType(Correspondence, 'ceylon.language::Correspondence');
var Correspondence$proto=Correspondence.$$.prototype;
Correspondence$proto.defines = function(key) {
    return exists(this.item(key));
}
Correspondence$proto.definesEvery = function(keys) {
	if (keys === undefined) return true;
    for (var i=0; i<keys.length; i++) {
        if (!this.defines(keys[i])) {
            return false;
        }
    }
    return true;
}
Correspondence$proto.definesAny = function(keys) {
	if (keys === undefined) return true;
    for (var i=0; i<keys.length; i++) {
        if (this.defines(keys[i])) {
            return true;
        }
    }
    return false;
}
Correspondence$proto.items = function(keys) {
    if (nonempty(keys)) {
        var r=[];
        for (var i = 0; i < keys.length; i++) {
            r.push(this.item(keys[i]));
        }
        return ArraySequence(r);
    }
    return $empty;
}
Correspondence$proto.keys = function() {
    return TypeCategory(this, 'ceylon.language::Integer');
}
exports.Correspondence=Correspondence;

//#include iterable.js
//#include collections.js
//#include maps.js

function Summable(wat) {
    return wat;
}
initType(Summable, 'ceylon.language::Summable');
exports.Summable=Summable;
function Number$(wat) {
    return wat;
}
initType(Number$, 'ceylon.language::Number');
exports.Number=Number$;
function Invertable(wat) {
    return wat;
}
initType(Invertable, 'ceylon.language::Invertable');
exports.Invertable=Invertable;
function Numeric(wat) {
    return wat;
}
initTypeProto(Numeric, 'ceylon.language::Numeric', Number$, Comparable, Summable, Invertable);
exports.Numeric=Numeric;
function Ordinal(wat) {
    return wat;
}
initType(Ordinal, 'ceylon.language::Ordinal');
exports.Ordinal=Ordinal;
function Integral(wat) {
    return wat;
}
initTypeProto(Integral, 'ceylon.language::Integral', Numeric, Ordinal);
exports.Integral=Integral;
function Scalar(scalar) { return scalar; }
initTypeProto(Scalar, 'ceylon.language::Scalar', Numeric, Comparable, Number$);
exports.Scalar=Scalar;
function Exponentiable(exp) { return exp; }
initTypeProto(Exponentiable, 'ceylon.language::Exponentiable', Numeric);
exports.Exponentiable=Exponentiable;

function Exception(description, cause, wat) {
    if (wat===undefined) {wat=new Exception.$$;}
    wat.description = description;
    wat.cause = cause;
    return wat;
}
initTypeProto(Exception, 'ceylon.language::Exception', IdentifiableObject);
var Exception$proto = Exception.$$.prototype;
Exception$proto.getCause = function() {return this.cause;}
Exception$proto.getMessage = function() {
    return this.description ? this.description
           : (this.cause ? this.cause.getMessage() : String$("", 0));
}
Exception$proto.getString = function() {
    return String$(className(this) + ' "' + this.getMessage() + '"');
}

//#include numbers.js
//#include strings.js

function getNull() { return null }
function Boolean$(value) {return Boolean(value)}
initExistingTypeProto(Boolean$, Boolean, 'ceylon.language::Boolean');
function trueClass() {}
initType(trueClass, "ceylon.language::true", Boolean$);
function falseClass() {}
initType(falseClass, "ceylon.language::false", Boolean$);
Boolean.prototype.getT$name$ = function() {
    return (this.valueOf()?trueClass:falseClass).$$.T$name;
}
Boolean.prototype.getT$all$ = function() {
    return (this.valueOf()?trueClass:falseClass).$$.T$all;
}
Boolean.prototype.equals = function(other) {return other.constructor===Boolean && other==this;}
Boolean.prototype.getHash = function() {return this.valueOf()?1:0;}
var trueString = String$("true", 4);
var falseString = String$("false", 5);
Boolean.prototype.getString = function() {return this.valueOf()?trueString:falseString;}
function getTrue() {return true}
function getFalse() {return false}
var $true = true;
var $false = false;
function Finished() {}
initTypeProto(Finished, 'ceylon.language::Finished', IdentifiableObject);
var $finished = new Finished.$$;
$finished.string = String$("exhausted", 9);
$finished.getString = function() { return this.string; }
function getExhausted() { return $finished; }

function Comparison(name) {
    var that = new Comparison.$$;
    that.name = String$(name);
    return that;
}
initTypeProto(Comparison, 'ceylon.language::Comparison', IdentifiableObject);
var Comparison$proto = Comparison.$$.prototype;
Comparison$proto.getString = function() { return this.name; }

//#include functions.js
//#include functions2.js
//#include sequences.js
//#include process.js

function Range(first, last) {
    var that = new Range.$$;
    that.first = first;
    that.last = last;
    var dist = last.distanceFrom(first);
    that.size=(dist>0?dist:-dist)+1;
    return that;
}
initTypeProto(Range, 'ceylon.language::Range', Object$, Sequence, Category);
var Range$proto = Range.$$.prototype;
Range$proto.getFirst = function() { return this.first; }
Range$proto.getLast = function() { return this.last; }
Range$proto.getEmpty = function() { return false; }
Range$proto.getDecreasing = function() {
    return this.first.compare(this.last) === larger;
}
Range$proto.next = function(x) {
    return this.getDecreasing() ? x.getPredecessor() : x.getSuccessor();
}
Range$proto.getSize = function() { return this.size; }
Range$proto.getLastIndex = function() { return this.size-1; }
Range$proto.item = function(index) {
    var idx = 0;
    var x = this.first;
    while (idx < index) {
        if (x.equals(this.last)) { return null; }
        else {
            idx++;
            x = this.next(x);
        }
    }
    return x;
}
Range$proto.includes = function(x) {
    var compf = x.compare(this.first);
    var compl = x.compare(this.last);
    return this.getDecreasing() ? ((compf === equal || compf === smaller) && (compl === equal || compl === larger)) : ((compf === equal || compf === larger) && (compl === equal || compl === smaller));
}
Range$proto.contains = function(x) {
    if (typeof x.compare==='function' || (x.prototype && typeof x.prototype.compare==='function')) {
        return this.includes(x);
    }
    return false;
}
Range$proto.getRest = function() {
    if (this.first.equals(this.last)) return $empty;
    var n = this.next(this.first);
    return Range(n, this.last);
}
Range$proto.segment = function(from, len) {
    //only positive length for now
    if (len.compare(0) !== larger) return $empty;
    if (!this.defines(from)) return $empty;
    var x = this.first;
    for (var i=0; i < from; i++) { x = this.next(x); }
    var y = x;
    for (var i=1; i < len; i++) { y = this.next(y); }
    if (!this.includes(y)) { y = this.last; }
    return Range(x, y);
}
Range$proto.span = function(from, to) {
    var li = this.getLastIndex();
	to = (to==null || to==undefined) ? li : to;
    if (to<0) {
    	if (from<0) {
    		return $empty;
    	}
    	to = 0;
    }
    else if (to > li) {
    	if (from > li) {
    		return $empty;
    	}
    	to = li;
    }
    if (from < 0) {
    	from = 0;
    }
    else if (from > li) {
    	from = li;
    }
    var x = this.first;
    for (var i=0; i < from; i++) { x = this.next(x); }
    var y = this.first;
    for (var i=0; i < to; i++) { y = this.next(y); }
    return Range(x, y);
}
Range$proto.definesEvery = function(keys) {
    for (var i = 0; i < keys.getSize(); i++) {
        if (!this.defines(keys.item(i))) {
            return false;
        }
    }
    return true;
}
Range$proto.definesAny = function(keys) {
    for (var i = 0; i < keys.getSize(); i++) {
        if (this.defines(keys.item(i))) {
            return true;
        }
    }
    return false;
}
Range$proto.defines = function(idx) { return idx.compare(this.getSize()) === smaller; }
Range$proto.getString = function() { return String$(this.first.getString() + ".." + this.last.getString()); }
Range$proto.equals = function(other) {
    if (!other) { return false; }
    return this.first.equals(other.getFirst()) && this.last.equals(other.getLast());
}
Range$proto.getIterator = function() { return RangeIterator(this); }
Range$proto.getReversed = function() { return Range(this.last, this.first); }
Range$proto.skipping = function(skip) {
    var x=0;
    var e=this.first;
    while (x++<skip) {
        e=this.next(e);
    }
    return this.includes(e) ? new Range(e, this.last) : $empty;
}
Range$proto.taking = function(take) {
    if (take == 0) {
        return $empty;
    }
    var x=0;
    var e=this.first;
    while (++x<take) {
        e=this.next(e);
    }
    return this.includes(e) ? new Range(this.first, e) : this;
}
Range$proto.getSequence = function() { return this; }
Range$proto.getCoalesced = function() { return this; }
Range$proto.count = function(f) {
    var e = this.getFirst();
    var c = 0;
    while (this.includes(e)) {
        if (f(e)) {
            c++;
        }
        e = this.next(e);
    }
    return c;
}

function RangeIterator(range) {
    var that = new RangeIterator.$$;
    that.range = range;
    that.current = range.getFirst();
    that.next = (range.last>=range.first) ? RangeIterator$forwardNext : RangeIterator$backwardNext;
    return that;
}
initTypeProto(RangeIterator, 'ceylon.language::RangeIterator', IdentifiableObject, Iterator);
RangeIterator$forwardNext = function() {
    var rval = this.current;
    if (rval === $finished) {
        return rval;
    }
    if (rval.compare(this.range.last) === smaller) {
        this.current = rval.getSuccessor();
    } else {
        this.current = $finished;
    }
    return rval;
}
RangeIterator$backwardNext = function() {
    var rval = this.current;
    if (rval === $finished) {
        return rval;
    }
    if (rval.compare(this.range.last) === larger) {
        this.current = rval.getPredecessor();
    } else {
        this.current = $finished;
    }
    return rval;
}

function Entry(key, item) {
    var that = new Entry.$$;
    Object$(that);
    Void(that);
    that.key = key;
    that.item = item;
    return that;
}
initTypeProto(Entry, 'ceylon.language::Entry', Object$);
var Entry$proto = Entry.$$.prototype;
Entry$proto.getString = function() {
    return String$(this.key.getString() + "->" + this.item.getString());
}
Entry$proto.getKey = function() { return this.key; }
Entry$proto.getItem = function() { return this.item; }
Entry$proto.equals = function(other) {
    return other && isOfType(other, 'ceylon.language::Entry') && this.getKey().equals(other.getKey()) && this.getItem().equals(other.getItem());
}
Entry$proto.getHash = function() { (31 + this.key.getHash()) * 31 + this.item.getHash(); }

function getBottom() {
    throw Exception();
}

//#include tuples.js
//#include annotations.js

exports.Exception=Exception;
exports.Identifiable=Identifiable;
exports.identityHash=$identityHash;
exports.IdentifiableObject=IdentifiableObject;
exports.Object=Object$;
exports.Boolean=Boolean$;
exports.Comparison=Comparison;
exports.getNull=getNull;
exports.getTrue=getTrue;
exports.getFalse=getFalse;
exports.Finished=Finished;
exports.getExhausted=getExhausted;
exports.Range=Range;
exports.Entry=Entry;
exports.getBottom=getBottom;
exports.ContainerWithFirstElement=ContainerWithFirstElement;
    });
}(typeof define==='function' && define.amd ? 
    define : function (factory) {
    if (typeof exports!=='undefined') {
        factory(require, exports, module);
    } else {
        throw "no module loader";
    }
}));
