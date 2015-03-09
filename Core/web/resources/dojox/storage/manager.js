//>>built
define("dojox/storage/manager",["dijit","dojo","dojox"],function(f,c,d){c.provide("dojox.storage.manager");d.storage.manager=new function(){this.currentProvider=null;this.available=!1;this.providers=[];this._initialized=!1;this._onLoadListeners=[];this.initialize=function(){this.autodetect()};this.register=function(b,a){this.providers.push(a);this.providers[b]=a};this.setProvider=function(b){};this.autodetect=function(){if(!this._initialized){for(var b=c.config.forceStorageProvider||!1,a,e=0;e<this.providers.length;e++)if(a=
this.providers[e],b&&b==a.declaredClass){a.isAvailable();break}else if(!b&&a.isAvailable())break;a?(this.currentProvider=a,c.mixin(d.storage,this.currentProvider),d.storage.initialize(),this.available=this._initialized=!0):(this._initialized=!0,this.available=!1,this.currentProvider=null,this.loaded())}};this.isAvailable=function(){return this.available};this.addOnLoad=function(b){this._onLoadListeners.push(b);this.isInitialized()&&this._fireLoaded()};this.removeOnLoad=function(b){for(var a=0;a<this._onLoadListeners.length;a++)if(b==
this._onLoadListeners[a]){this._onLoadListeners.splice(a,1);break}};this.isInitialized=function(){return null!=this.currentProvider&&"dojox.storage.FlashStorageProvider"==this.currentProvider.declaredClass&&!1==d.flash.ready?!1:this._initialized};this.supportsProvider=function(b){try{var a=eval("new "+b+"()").isAvailable();return!a?!1:a}catch(c){return!1}};this.getProvider=function(){return this.currentProvider};this.loaded=function(){this._fireLoaded()};this._fireLoaded=function(){c.forEach(this._onLoadListeners,
function(b){try{b()}catch(a){}})};this.getResourceList=function(){var b=[];c.forEach(d.storage.manager.providers,function(a){b=b.concat(a.getResourceList())});return b}}});
//@ sourceMappingURL=manager.js.map