//>>built
define("dojox/data/AndOrReadStore","dojo/_base/declare dojo/_base/lang dojo/data/ItemFileReadStore dojo/data/util/filter dojo/_base/array dojo/_base/json".split(" "),function(v,d,x,y,z,t){return v("dojox.data.AndOrReadStore",[x],{_containsValue:function(n,s,t,h){return z.some(this.getValues(n,s),function(c){if(d.isString(h))return eval(h);if(null!==c&&!d.isObject(c)&&h){if(c.toString().match(h))return!0}else return t===c?!0:!1})},filter:function(n,s,v){var h=[];if(n.query){var c=t.fromJson(t.toJson(n.query));
if("object"==typeof c){var a=0,g;for(g in c)a++;if(1<a&&c.complexQuery){var a=c.complexQuery,p=!1;for(g in c)if("complexQuery"!==g){p||(a="( "+a+" )",p=!0);var k=n.query[g];d.isString(k)&&(k="'"+k+"'");a+=" AND "+g+":"+k;delete c[g]}c.complexQuery=a}}g=n.queryOptions?n.queryOptions.ignoreCase:!1;"string"!=typeof c&&(c=t.toJson(c),c=c.replace(/\\\\/g,"\\"));var c=c.replace(/\\"/g,'"'),a=d.trim(c.replace(/{|}/g,"")),b;if(a.match(/"? *complexQuery *"?:/)){for(var a=d.trim(a.replace(/"?\s*complexQuery\s*"?:/,
"")),p=["'",'"'],l,q=!1,c=0;c<p.length;c++)if(k=a.indexOf(p[c]),b=a.indexOf(p[c],1),l=a.indexOf(":",1),0===k&&-1!=b&&l<b){q=!0;break}q&&(a=a.replace(/^\"|^\'|\"$|\'$/g,""))}p=a;k=/^>=|^<=|^<|^>|^,|^NOT |^AND |^OR |^\(|^\)|^!|^&&|^\|\|/i;b=l="";var e=-1,q=!1,w="",f="";b="";for(c=0;c<s.length;++c){var m=!0,u=s[c];if(null===u)m=!1;else{a=p;for(l="";0<a.length&&!q;){for(b=a.match(k);b&&!q;)a=d.trim(a.replace(b[0],"")),b=d.trim(b[0]).toUpperCase(),b="NOT"==b?"!":"AND"==b||","==b?"\x26\x26":"OR"==b?"||":
b,b=" "+b+" ",l+=b,b=a.match(k);if(0<a.length)if(m=(b=a.match(/:|>=|<=|>|</g))&&b.shift(),e=a.indexOf(m),-1==e){q=!0;break}else{w=d.trim(a.substring(0,e).replace(/\"|\'/g,""));a=d.trim(a.substring(e+m.length));if(b=a.match(/^\'|^\"/)){b=b[0];e=a.indexOf(b);b=a.indexOf(b,e+1);if(-1==b){q=!0;break}f=a.substring(e+m.length,b);a=b==a.length-1?"":d.trim(a.substring(b+1))}else if(b=a.match(/\s|\)|,/)){for(var f=Array(b.length),r=0;r<b.length;r++)f[r]=a.indexOf(b[r]);e=f[0];if(1<f.length)for(r=1;r<f.length;r++)e=
Math.min(e,f[r]);f=d.trim(a.substring(0,e));a=d.trim(a.substring(e))}else f=d.trim(a),a="";b=":"!=m?this.getValue(u,w)+m+f:y.patternToRegExp(f,g);l+=this._containsValue(u,w,f,b)}}m=eval(l)}m&&h.push(u)}q&&(h=[])}else for(c=0;c<s.length;++c)g=s[c],null!==g&&h.push(g);v(h,n)}})});
//@ sourceMappingURL=AndOrReadStore.js.map