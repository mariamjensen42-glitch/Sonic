package com.cycling.data.online.lx

import com.cycling.domain.online.model.PluginMeta

/**
 * 注入到每个 LX 脚本运行时里的引导脚本。
 *
 * 内容是一份自包含的引导脚本（8454 字符），
 * 宿主与脚本之间只走一个普通函数 `lxHostInvoke(channel, payload)`，不把原生对象暴露给脚本。
 * 脚本可见的 `lx` API、console、定时器、utils 一个不差：
 *
 * - `lx.EVENT_NAMES` / `lx.on` / `lx.send` / `lx.request` / `lx.utils.*`
 * - `lx.env = 'desktop'`、`lx.version = '2.11.0'`（脚本会按这两个值走桌面分支）
 * - `setTimeout` / `setInterval` 用 `__lxSleep` 这个 suspend 绑定实现，避免嵌套 evaluate
 * - `__lxDispatch`：宿主把 `{source, action, info}` 交给脚本的 `request` 处理器
 */
object LxBootstrap {

    /** 宿主与脚本约定的 LX 版本号。 */
    const val LX_VERSION = "2.11.0"

    /** 脚本环境标识；宿主固定传 `desktop`。 */
    const val LX_ENV = "desktop"

    private const val PRO_TOKEN_PLACEHOLDER = "__LX_PRO_TOKEN__"

    fun js(proToken: String): String = JS.replace(PRO_TOKEN_PLACEHOLDER, escapeForJsSingleQuote(proToken))

    /** 用户的音源脚本必须包在 async IIFE 里，脚本才能用顶层 await。 */
    fun wrapUserScript(script: String): String = "(async function(){\n$script\n})();"

    /** 脚本元信息注入：`lx.currentScriptInfo` + 环境/版本。 */
    fun scriptInfoJs(meta: PluginMeta, rawScript: String): String {
        val json = org.json.JSONObject()
            .put("name", meta.name)
            .put("description", meta.description)
            .put("version", meta.version)
            .put("author", meta.author)
            .put("homepage", meta.homepage)
            .put("rawScript", rawScript)
        return buildString {
            append("lx.currentScriptInfo = ").append(json).append("; ")
            append("lx.env = ").append(org.json.JSONObject.quote(LX_ENV)).append("; ")
            append("lx.version = ").append(org.json.JSONObject.quote(LX_VERSION)).append(";")
        }
    }

    private fun escapeForJsSingleQuote(value: String): String = value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private val JS = """(function () {
  'use strict';
  var G = typeof globalThis !== 'undefined' ? globalThis : this;
  G.window = G;

  function __lxHost(channel, payload) {
    return lxHostInvoke(String(channel), String(payload));
  }

  function mkConsole(level) {
    return function () {
      var parts = [];
      for (var i = 0; i < arguments.length; i++) {
        var a = arguments[i];
        if (typeof a === 'string') { parts.push(a); continue; }
        try { parts.push(JSON.stringify(a)); } catch (e) { parts.push(String(a)); }
      }
      try { __lxHost('__lxConsole', JSON.stringify({ level: level, text: parts.join(' ') })); } catch (e) {}
    };
  }
  G.console = {
    log: mkConsole('log'), info: mkConsole('info'), warn: mkConsole('warn'),
    error: mkConsole('error'), debug: mkConsole('debug'), trace: mkConsole('debug'),
    group: function () {}, groupEnd: function () {}, groupCollapsed: function () {},
    table: mkConsole('log'), dir: mkConsole('log'),
    time: function () {}, timeEnd: function () {}, assert: function () {}, count: function () {}
  };

  var B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  if (typeof G.atob !== 'function') {
    G.atob = function (input) {
      var str = String(input).replace(/=+$/, '');
      var output = '';
      if (str.length % 4 === 1) throw new Error('atob: invalid input');
      for (var bc = 0, bs, buffer, idx = 0; (buffer = str.charAt(idx++)); ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer, bc++ % 4) ? output += String.fromCharCode(255 & (bs >> ((-2 * bc) & 6))) : 0) {
        buffer = B64.indexOf(buffer);
        if (buffer === -1) throw new Error('atob: invalid char');
      }
      return output;
    };
  }
  if (typeof G.btoa !== 'function') {
    G.btoa = function (input) {
      var str = String(input);
      var output = '';
      for (var block, charCode, idx = 0, map = B64; str.charAt(idx | 0) || (map = '=', idx % 1); output += map.charAt(63 & (block >> (8 - (idx % 1) * 8)))) {
        charCode = str.charCodeAt(idx += 3 / 4);
        if (charCode > 0xFF) throw new Error('btoa: invalid char');
        block = (block << 8) | charCode;
      }
      return output;
    };
  }

  // setTimeout / setInterval：用 Kotlin suspend __lxSleep，避免嵌套 evaluate
  var timerSeq = 0;
  var timerFns = {};
  G.setTimeout = function (fn, ms) {
    var id = ++timerSeq;
    var args = Array.prototype.slice.call(arguments, 2);
    timerFns[id] = { fn: fn, args: args, repeat: false, alive: true };
    (function(tid, delayMs) {
      __lxSleep(delayMs || 0).then(function () {
        var t = timerFns[tid];
        if (!t || !t.alive) return;
        if (!t.repeat) { t.alive = false; delete timerFns[tid]; }
        try { t.fn.apply(null, t.args); } catch (e) { G.console.error('[timer]', String(e)); }
        if (t.repeat && t.alive) {
          var loop = function () {
            __lxSleep(delayMs || 1).then(function () {
              var tt = timerFns[tid];
              if (!tt || !tt.alive) return;
              try { tt.fn.apply(null, tt.args); } catch (e) { G.console.error('[timer]', String(e)); }
              if (tt.alive) loop();
            });
          };
          loop();
        }
      });
    })(id, ms || 0);
    return id;
  };
  G.clearTimeout = function (id) {
    if (timerFns[id]) { timerFns[id].alive = false; delete timerFns[id]; }
  };
  G.setInterval = function (fn, ms) {
    var id = ++timerSeq;
    var args = Array.prototype.slice.call(arguments, 2);
    timerFns[id] = { fn: fn, args: args, repeat: true, alive: true };
    var delayMs = ms || 1;
    var tick = function () {
      __lxSleep(delayMs).then(function () {
        var t = timerFns[id];
        if (!t || !t.alive) return;
        try { t.fn.apply(null, t.args); } catch (e) { G.console.error('[timer]', String(e)); }
        if (t.alive) tick();
      });
    };
    tick();
    return id;
  };
  G.clearInterval = G.clearTimeout;
  G.setImmediate = function (fn) { return G.setTimeout(fn, 0); };
  G.clearImmediate = G.clearTimeout;
  G.queueMicrotask = function (fn) { Promise.resolve().then(fn); };

  var handlers = {};

  function callUtil(name, args) {
    var result = __lxHost('__lxUtils', JSON.stringify({ name: name, args: Array.prototype.slice.call(args) }));
    try {
      var parsed = JSON.parse(result);
      if (parsed && parsed.__error) {
        var err = new Error(parsed.__error);
        err.__lxError = true;
        throw err;
      }
      return parsed;
    } catch (e) {
      if (e && (e.__lxError || (e.message && e.message.indexOf('__error') >= 0))) throw e;
      return result;
    }
  }

  G.lx = {
    EVENT_NAMES: { request: 'request', inited: 'inited', updateAlert: 'updateAlert' },
    env: 'desktop',
    version: '2.11.0',
    /**
     * 服务端签发的 Pro 凭证（永久赞助专属插件的取链/歌词请求携带，供自建取链服务验签）。
     * 未登录 / 非赞助用户为空字符串；由宿主在 bootstrap 时注入（模板参数 __LX_PRO_TOKEN__）。
     */
    proToken: '__LX_PRO_TOKEN__',
    currentScriptInfo: {},
    on: function (name, handler) {
      if (typeof name === 'string' && typeof handler === 'function') handlers[name] = handler;
    },
    send: function (name, data) {
      __lxHost('__lxEvent', JSON.stringify({ event: name, data: data === undefined ? null : data }));
      return Promise.resolve();
    },
    request: function (url, options, callback) {
      if (typeof options === 'function') { callback = options; options = {}; }
      var cancelled = false;
      lxHttpRequest(String(url), options || {}).then(function (resp) {
        if (cancelled) return;
        (callback || function () {})(null, resp, resp ? resp.body : undefined);
      }, function (err) {
        if (cancelled) return;
        var e = (err && err.message) ? err : new Error(String(err));
        (callback || function () {})(e);
      });
      return function () { cancelled = true; };
    },
    utils: {
      buffer: {
        from: function () { return callUtil('buffer.from', arguments); },
        bufToString: function () { return callUtil('buffer.bufToString', arguments); }
      },
      crypto: {
        md5: function (str) { return callUtil('crypto.md5', [str]); },
        randomBytes: function (size) { return callUtil('crypto.randomBytes', [size]); },
        aesEncrypt: function () { return callUtil('crypto.aesEncrypt', arguments); },
        aesDecrypt: function () { return callUtil('crypto.aesDecrypt', arguments); },
        rsaEncrypt: function () { return callUtil('crypto.rsaEncrypt', arguments); },
        aesEcbEncryptB64Key: function () { return callUtil('crypto.aesEcbEncryptB64Key', arguments); },
        aesEcbDecryptB64Key: function () { return callUtil('crypto.aesEcbDecryptB64Key', arguments); }
      },
      zlib: {
        inflate: function () { return callUtil('zlib.inflate', arguments); },
        deflate: function () { return callUtil('zlib.deflate', arguments); },
        inflateRaw: function () { return callUtil('zlib.inflateRaw', arguments); },
        deflateRaw: function () { return callUtil('zlib.deflateRaw', arguments); },
        gzip: function () { return callUtil('zlib.gzip', arguments); },
        gunzip: function () { return callUtil('zlib.gunzip', arguments); }
      },
      text: {
        gb18030: function () { return callUtil('text.gb18030', arguments); }
      },
      qrc: {
        decrypt: function () { return callUtil('qrc.decrypt', arguments); },
        toLrc: function () { return callUtil('qrc.toLrc', arguments); },
        toYrc: function () { return callUtil('qrc.toYrc', arguments); }
      }
    }
  };

  G.__lxDispatch = function (callId, payloadJson) {
    var handler = handlers['request'];
    if (!handler) { G.__lxReturn(callId, null, '脚本未注册 request 事件'); return; }
    var payload;
    try { payload = JSON.parse(payloadJson); } catch (e) { G.__lxReturn(callId, null, 'payload 解析失败: ' + e); return; }
    var result;
    try {
      result = handler(payload);
    } catch (e) {
      G.__lxReturn(callId, null, String(e && e.message ? e.message : e));
      return;
    }
    if (result && typeof result.then === 'function') {
      result.then(function (r) {
        var ok;
        try { ok = JSON.stringify(r === undefined ? null : r); } catch (e) { ok = 'null'; }
        G.__lxReturn(callId, ok, null);
      })['catch'](function (e) {
        G.__lxReturn(callId, null, String(e && e.message ? e.message : e));
      });
    } else {
      var ok;
      try { ok = JSON.stringify(result === undefined ? null : result); } catch (e) { ok = 'null'; }
      G.__lxReturn(callId, ok, null);
    }
  };
  G.__lxReturn = function (callId, okJson, err) {
    __lxHost('__lxReturn', JSON.stringify({ callId: callId, ok: okJson, err: err }));
  };
})();"""
}
