//>>built
define("xstyle/ext/event-delegate",[],function(){var e=!!document.addEventListener,a=document.createElement("div");matchesSelector=a.matchesSelector||a.webkitMatchesSelector||a.msMatchesSelector||a.mozMatchesSelector;return{onProperty:function(a,c,b){this.on(document,a.slice(2),b.fullSelector(),c)},on:function(a,c,b,f){function d(a){b=b||rule.fullSelector();matchesSelector.call(a.target,b)}e?a.addEventListener(c,d,!1):a.attachEvent(c,d)}}});
//@ sourceMappingURL=event-delegate.js.map