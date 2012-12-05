function Tuple(first, rest) {
    var that = new Tuple.$$;
    that.f$ = first;
    that.r$ = rest;
    return that;
}
initTypeProto(Tuple, 'ceylon.language::Tuple', IdentifiableObject, Sequence);
var Tuple$proto = Tuple.$$.prototype;

Tuple$proto.getFirst = function() {
    return this.f$;
}
Tuple$proto.getRest = function() {
    return this.r$;
}
Tuple$proto.item = function(index) {
    if (index > 0) {
        return this.r$.item(index-1);
    } else if (index === 0) {
        return this.f$;
    }
    return null;
}
Tuple$proto.getLastIndex = function() {
    var rli = this.r$.getLastIndex();
    return rli === null ? 0 : rli+1;
}
Tuple$proto.getReversed = function() {
    return this.r$.getReversed().withTrailing(this.f$);
}
Tuple$proto.segment = function(from, len) {
    if (from <= 0) {
        return len===1?Singleton(this.f$):this.r$.segment(0,len+from-1).withLeading(this.f$);
    }
    return this.r$.segment(from-1,len);
}
Tuple$proto.span = function(from, to) {
    if (from < 0 && to < 0) {
        return empty;
    } else if (from < 0) {
        from = 0;
    } else if (to < 0) {
        to = 0;
    }
    return from<=to ? this.segment(from,to-from+1) : this.segment(to,from-to+1).getReversed().getSequence();
}
Tuple$proto.spanTo = function(to) {
    return to<0 ? empty : this.span(0, to);
}
Tuple$proto.spanFrom = function(from) {
    return this.span(from, this.getSize());
}
Tuple$proto.getClone = function() { return this; }
Tuple$proto.getString = function() {
    var sb = StringBuilder();
    sb.appendAll(["Tuple(", this.f$.getString(), ",", this.r$.getString(), ")"]);
    return sb.getString();
}

exports.Tuple=Tuple;
