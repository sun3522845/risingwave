(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[540],{60378:function(e,n,t){(window.__NEXT_P=window.__NEXT_P||[]).push(["/data_sources",function(){return t(30960)}])},54767:function(e,n,t){"use strict";t.d(n,{KB:function(){return X},Yr:function(){return W},vk:function(){return F},vP:function(){return P},sW:function(){return B}});var r=t(47568),i=t(34051),c=t.n(i),o=t(85893),a=t(47741),s=t(66479),l=t(39653),u=t(50471),d=t(40639),m=t(67294),h=t(32067),f=t(54520),p=t(28387),x=(...e)=>e.filter(Boolean).join(" "),[v,j]=(0,p.k)({name:"TableStylesContext",errorMessage:"useTableStyles returned is 'undefined'. Seems you forgot to wrap the components in \"<Table />\" "}),w=(0,h.Gp)(((e,n)=>{const t=(0,h.jC)("Table",e),{className:r,...i}=(0,f.Lr)(e);return m.createElement(v,{value:t},m.createElement(h.m$.table,{role:"table",ref:n,__css:t.table,className:x("chakra-table",r),...i}))}));w.displayName="Table";var _=(0,h.Gp)(((e,n)=>{const{overflow:t,overflowX:r,className:i,...c}=e;return m.createElement(h.m$.div,{ref:n,className:x("chakra-table__container",i),...c,__css:{display:"block",whiteSpace:"nowrap",WebkitOverflowScrolling:"touch",overflowX:t??r??"auto",overflowY:"hidden",maxWidth:"100%"}})}));(0,h.Gp)(((e,n)=>{const{placement:t="bottom",...r}=e,i=j();return m.createElement(h.m$.caption,{...r,ref:n,__css:{...i.caption,captionSide:t}})})).displayName="TableCaption";var b=(0,h.Gp)(((e,n)=>{const t=j();return m.createElement(h.m$.thead,{...e,ref:n,__css:t.thead})})),k=(0,h.Gp)(((e,n)=>{const t=j();return m.createElement(h.m$.tbody,{...e,ref:n,__css:t.tbody})})),E=((0,h.Gp)(((e,n)=>{const t=j();return m.createElement(h.m$.tfoot,{...e,ref:n,__css:t.tfoot})})),(0,h.Gp)((({isNumeric:e,...n},t)=>{const r=j();return m.createElement(h.m$.th,{...n,ref:t,__css:r.th,"data-is-numeric":e})}))),y=(0,h.Gp)(((e,n)=>{const t=j();return m.createElement(h.m$.tr,{role:"row",...e,ref:n,__css:t.tr})})),N=(0,h.Gp)((({isNumeric:e,...n},t)=>{const r=j();return m.createElement(h.m$.td,{role:"gridcell",...n,ref:t,__css:r.td,"data-is-numeric":e})})),C=t(63679),S=t(9008),g=t.n(S),z=t(41664),T=t.n(z),G=t(5330);function $(e){var n,t,r;return"".concat(null===(n=e.columnDesc)||void 0===n?void 0:n.name," (").concat(null===(t=e.columnDesc)||void 0===t||null===(r=t.columnType)||void 0===r?void 0:r.typeName,")")}var O,D=(0,C.ZP)((function(){return t.e(171).then(t.t.bind(t,55171,23))})),F={name:"Depends",width:1,content:function(e){return(0,o.jsx)(T(),{href:"/streaming_graph/?id=".concat(e.id),children:(0,o.jsx)(a.zx,{size:"sm","aria-label":"view dependents",colorScheme:"teal",variant:"link",children:"D"})})}},P={name:"Primary Key",width:1,content:function(e){return e.pk.map((function(e){return e.index})).map((function(n){return e.columns[n]})).map((function(e){return $(e)})).join(", ")}},W={name:"Connector",width:3,content:function(e){return null!==(O=e.properties.connector)&&void 0!==O?O:"unknown"}},B=[F,{name:"Fragments",width:1,content:function(e){return(0,o.jsx)(T(),{href:"/streaming_plan/?id=".concat(e.id),children:(0,o.jsx)(a.zx,{size:"sm","aria-label":"view fragments",colorScheme:"teal",variant:"link",children:"F"})})}}];function X(e,n,t){var i=(0,s.pm)(),h=(0,m.useState)([]),f=h[0],p=h[1];(0,m.useEffect)((function(){function e(){return(e=(0,r.Z)(c().mark((function e(){return c().wrap((function(e){for(;;)switch(e.prev=e.next){case 0:return e.prev=0,e.t0=p,e.next=4,n();case 4:e.t1=e.sent,(0,e.t0)(e.t1),e.next=12;break;case 8:e.prev=8,e.t2=e.catch(0),i({title:"Error Occurred",description:e.t2.toString(),status:"error",duration:5e3,isClosable:!0}),console.error(e.t2);case 12:case"end":return e.stop()}}),e,null,[[0,8]])})))).apply(this,arguments)}return function(){e.apply(this,arguments)}(),function(){}}),[i,n]);var x=(0,l.qY)(),v=x.isOpen,j=x.onOpen,C=x.onClose,S=(0,m.useState)(),z=S[0],T=S[1],O=(0,o.jsxs)(u.u_,{isOpen:v,onClose:C,size:"3xl",children:[(0,o.jsx)(u.ZA,{}),(0,o.jsxs)(u.hz,{children:[(0,o.jsxs)(u.xB,{children:["Catalog of ",null===z||void 0===z?void 0:z.id," - ",null===z||void 0===z?void 0:z.name]}),(0,o.jsx)(u.ol,{}),(0,o.jsx)(u.fe,{children:v&&z&&(0,o.jsx)(D,{src:z,collapsed:1,name:null,displayDataTypes:!1})}),(0,o.jsx)(u.mz,{children:(0,o.jsx)(a.zx,{colorScheme:"blue",mr:3,onClick:C,children:"Close"})})]})]}),F=(0,o.jsxs)(d.xu,{p:3,children:[(0,o.jsx)(G.Z,{children:e}),(0,o.jsx)(_,{children:(0,o.jsxs)(w,{variant:"simple",size:"sm",maxWidth:"full",children:[(0,o.jsx)(b,{children:(0,o.jsxs)(y,{children:[(0,o.jsx)(E,{width:3,children:"Id"}),(0,o.jsx)(E,{width:5,children:"Name"}),(0,o.jsx)(E,{width:3,children:"Owner"}),t.map((function(e){return(0,o.jsx)(E,{width:e.width,children:e.name},e.name)})),(0,o.jsx)(E,{children:"Visible Columns"})]})}),(0,o.jsx)(k,{children:f.map((function(e){return(0,o.jsxs)(y,{children:[(0,o.jsx)(N,{children:(0,o.jsx)(a.zx,{size:"sm","aria-label":"view catalog",colorScheme:"teal",variant:"link",onClick:function(){var n;(n=e)&&(T(n),j())},children:e.id})}),(0,o.jsx)(N,{children:e.name}),(0,o.jsx)(N,{children:e.owner}),t.map((function(n){return(0,o.jsx)(N,{children:n.content(e)},n.name)})),(0,o.jsx)(N,{overflowWrap:"normal",children:e.columns.filter((function(e){return!e.isHidden})).map((function(e){return $(e)})).join(", ")})]},e.id)}))})]})})]});return(0,o.jsxs)(m.Fragment,{children:[(0,o.jsx)(g(),{children:(0,o.jsx)("title",{children:e})}),O,F]})}},30960:function(e,n,t){"use strict";t.r(n),t.d(n,{default:function(){return c}});var r=t(54767),i=t(16640);function c(){var e,n={name:"Row Format",width:3,content:function(n){var t;return null!==(e=null===(t=n.info)||void 0===t?void 0:t.rowFormat)&&void 0!==e?e:"unknown"}};return(0,r.KB)("Data Sources",i.mt,[r.Yr,n,r.vk])}}},function(e){e.O(0,[662,482,476,894,836,640,774,888,179],(function(){return n=60378,e(e.s=n);var n}));var n=e.O();_N_E=n}]);