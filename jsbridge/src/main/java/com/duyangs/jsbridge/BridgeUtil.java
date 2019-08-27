package com.duyangs.jsbridge;

import android.webkit.WebView;

public class BridgeUtil {
	final static String YY_OVERRIDE_SCHEMA = "yy://";
	final static String YY_RETURN_DATA = YY_OVERRIDE_SCHEMA + "return/";//格式为   yy://return/{function}/returncontent
	final static String YY_FETCH_QUEUE = YY_RETURN_DATA + "_fetchQueue/";
	final static String EMPTY_STR = "";
	final static String UNDERLINE_STR = "_";
	final static String SPLIT_MARK = "/";
	
	final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";
	final static String JS_HANDLE_MESSAGE_FROM_JAVA = "javascript:WebViewJavascriptBridge._handleMessageFromNative('%s');";
	final static String JS_FETCH_QUEUE_FROM_JAVA = "javascript:WebViewJavascriptBridge._fetchQueue();";
	public final static String JAVASCRIPT_STR = "javascript:";

	// 例子 javascript:WebViewJavascriptBridge._fetchQueue(); --> _fetchQueue
	public static String parseFunctionName(String jsUrl){
		return jsUrl.replace("javascript:WebViewJavascriptBridge.", "").replaceAll("\\(.*\\);", "");
	}

	// 获取到传递信息的body值
	// url = yy://return/_fetchQueue/[{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
	public static String getDataFromReturnUrl(String url) {
		if(url.startsWith(YY_FETCH_QUEUE)) {
			// return = [{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
			return url.replace(YY_FETCH_QUEUE, EMPTY_STR);
		}

		// temp = _fetchQueue/[{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
		String temp = url.replace(YY_RETURN_DATA, EMPTY_STR);
		String[] functionAndData = temp.split(SPLIT_MARK);

		if(functionAndData.length >= 2) {
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < functionAndData.length; i++) {
				sb.append(functionAndData[i]);
			}
			// return = [{"responseId":"JAVA_CB_2_3957","responseData":"Javascript Says Right back aka!"}]
			return sb.toString();
		}
		return null;
	}

	// 获取到传递信息的方法
	// url = yy://return/_fetchQueue/[{"responseId":"JAVA_CB_1_360","responseData":"Javascript Says Right back aka!"}]
	public static String getFunctionFromReturnUrl(String url) {
		// temp = _fetchQueue/[{"responseId":"JAVA_CB_1_360","responseData":"Javascript Says Right back aka!"}]
		String temp = url.replace(YY_RETURN_DATA, EMPTY_STR);
		String[] functionAndData = temp.split(SPLIT_MARK);
		if(functionAndData.length >= 1){
			// functionAndData[0] = _fetchQueue
			return functionAndData[0];
		}
		return null;
	}

	
	
	/**
	 * js 文件将注入为第一个script引用
	 * @param view WebView
	 * @param url url
	 */
	public static void webViewLoadJs(WebView view, String url){
		String js = "var newscript = document.createElement(\"script\");";
		js += "newscript.src=\"" + url + "\";";
		js += "document.scripts[0].parentNode.insertBefore(newscript,document.scripts[0]);";
		view.loadUrl("javascript:" + js);
	}

	/**
	 * 这里只是加载lib包中assets中的 WebViewJavascriptBridge.js
	 * @param view webview
	 */
    public static void webViewLoadLocalJs(WebView view){
		String jsContent = "(function() {    if (window.WebViewJavascriptBridge) {        return;    }    var messagingIframe;    var bizMessagingIframe;    var sendMessageQueue = [];    var receiveMessageQueue = [];    var messageHandlers = {};    var CUSTOM_PROTOCOL_SCHEME = 'yy';    var QUEUE_HAS_MESSAGE = '__QUEUE_MESSAGE__/';    var responseCallbacks = {};    var uniqueId = 1;    function _createQueueReadyIframe(doc) {        messagingIframe = doc.createElement('iframe');        messagingIframe.style.display = 'none';        doc.documentElement.appendChild(messagingIframe);    }    function _createQueueReadyIframe4biz(doc) {        bizMessagingIframe = doc.createElement('iframe');        bizMessagingIframe.style.display = 'none';        doc.documentElement.appendChild(bizMessagingIframe);    }    function init(messageHandler) {        if (WebViewJavascriptBridge._messageHandler) {            throw new Error('WebViewJavascriptBridge.init called twice');        }        WebViewJavascriptBridge._messageHandler = messageHandler;        var receivedMessages = receiveMessageQueue;        receiveMessageQueue = null;        for (var i = 0; i < receivedMessages.length; i++) {            _dispatchMessageFromNative(receivedMessages[i]);        }    }    function send(data, responseCallback) {        _doSend({            data: data        }, responseCallback);    }    function registerHandler(handlerName, handler) {        messageHandlers[handlerName] = handler;    }    function callHandler(handlerName, data, responseCallback) {        _doSend({            handlerName: handlerName,            data: data        }, responseCallback);    }    function _doSend(message, responseCallback) {        if (responseCallback) {            var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();            responseCallbacks[callbackId] = responseCallback;            message.callbackId = callbackId;        }        sendMessageQueue.push(message);        messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;    }    function _fetchQueue() {        var messageQueueString = JSON.stringify(sendMessageQueue);        sendMessageQueue = [];        if (messageQueueString !== '[]') {            bizMessagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);        }    }    function _dispatchMessageFromNative(messageJSON) {        setTimeout(function() {            var message = JSON.parse(messageJSON);            var responseCallback;            if (message.responseId) {                responseCallback = responseCallbacks[message.responseId];                if (!responseCallback) {                    return;                }                responseCallback(message.responseData);                delete responseCallbacks[message.responseId];            } else {                if (message.callbackId) {                    var callbackResponseId = message.callbackId;                    responseCallback = function(responseData) {                        _doSend({                            responseId: callbackResponseId,                            responseData: responseData                        });                    };                }                var handler = WebViewJavascriptBridge._messageHandler;                if (message.handlerName) {                    handler = messageHandlers[message.handlerName];                }                try {                    handler(message.data, responseCallback);                } catch (exception) {                    if (typeof console != 'undefined') {                        console.log(\"WebViewJavascriptBridge: WARNING: javascript handler threw.\", message, exception);                    }                }            }        });    }    function _handleMessageFromNative(messageJSON) {        console.log(messageJSON);        if (receiveMessageQueue) {            receiveMessageQueue.push(messageJSON);        }        _dispatchMessageFromNative(messageJSON);           }    var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {        init: init,        send: send,        registerHandler: registerHandler,        callHandler: callHandler,        _fetchQueue: _fetchQueue,        _handleMessageFromNative: _handleMessageFromNative    };    var doc = document;    _createQueueReadyIframe(doc);    _createQueueReadyIframe4biz(doc);    var readyEvent = doc.createEvent('Events');    readyEvent.initEvent('WebViewJavascriptBridgeReady');    readyEvent.bridge = WebViewJavascriptBridge;    doc.dispatchEvent(readyEvent);})();";
		view.loadUrl("javascript:" + jsContent);
    }

}
