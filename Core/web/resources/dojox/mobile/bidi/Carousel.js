//>>built
define("dojox/mobile/bidi/Carousel",["dojo/_base/declare","./common","dojo/dom-style"],function(d,b,c){return d(null,{buildRendering:function(){this.inherited(arguments);this.isLeftToRight()||(this.navButton&&(c.set(this.btnContainerNode,"float","left"),this.disconnect(this._prevHandle),this.disconnect(this._nextHandle),this._prevHandle=this.connect(this.prevBtnNode,"onclick","onNextBtnClick"),this._nextHandle=this.connect(this.nextBtnNode,"onclick","onPrevBtnClick")),this.pageIndicator&&c.set(this.piw,
"float","left"))},_setTitleAttr:function(a){this.titleNode.innerHTML=this._cv?this._cv(a):a;this._set("title",a);this.textDir&&(this.titleNode.innerHTML=b.enforceTextDirWithUcc(this.titleNode.innerHTML,this.textDir),this.titleNode.style.textAlign="rtl"===this.dir.toLowerCase()?"right":"left")},_setTextDirAttr:function(a){if(a&&this.textDir!==a&&(this.textDir=a,this.titleNode.innerHTML=b.removeUCCFromText(this.titleNode.innerHTML),this.titleNode.innerHTML=b.enforceTextDirWithUcc(this.titleNode.innerHTML,
this.textDir),0<this.items.length))this.onComplete(this.items)}})});
//@ sourceMappingURL=Carousel.js.map